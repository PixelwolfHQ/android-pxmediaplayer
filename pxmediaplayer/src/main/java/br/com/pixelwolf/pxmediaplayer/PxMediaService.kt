package br.com.pixelwolf.pxmediaplayer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import br.com.pixelwolf.pxmediaplayer.adapter.MediaPlayerAdapter
import br.com.pixelwolf.pxmediaplayer.constants.MediaConstants
import br.com.pixelwolf.pxmediaplayer.constants.NotificationConstants.NOW_PLAYING_NOTIFICATION
import br.com.pixelwolf.pxmediaplayer.constants.SeekableRemoteActions
import br.com.pixelwolf.pxmediaplayer.extensions.currentPlayBackPosition
import br.com.pixelwolf.pxmediaplayer.listeners.MediaPlaybackListener
import br.com.pixelwolf.pxmediaplayer.notification.NotificationBuilder
import br.com.pixelwolf.pxmediaplayer.utils.MediaMetadataUtils
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PxMediaService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "PxMediaService"
    }

    private val mediaLibrary: MediaLibrary = MediaLibrary()
    private val mediaMetadataUtils: MediaMetadataUtils = MediaMetadataUtils()

    private var isForegroundService = false

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaPlayerAdapter: MediaPlayerAdapter
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var dataSourceFactory: DefaultDataSourceFactory
    private lateinit var notificationBuilder: NotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver

    override fun onCreate() {
        super.onCreate()

        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(sessionActivityPendingIntent)
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        }

        sessionToken = mediaSession.sessionToken

        dataSourceFactory = DefaultDataSourceFactory(
            this, Util.getUserAgent(this, "pxmediaplayer"), null
        )

        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(MediaControllerCallback())
        }

        mediaPlayerAdapter = MediaPlayerAdapter(
            context = this,
            mediaSession = mediaSession,
            dataSourceFactory = dataSourceFactory,
            mediaPlaybackListener = MediaPlaybackListener(mediaSession)
        )

        mediaSession.setCallback(
            MediaSessionCallback(
                mediaSession = mediaSession,
                mediaPlayerAdapter = mediaPlayerAdapter,
                mediaLibrary = mediaLibrary,
                mediaMetadataUtils = mediaMetadataUtils
            )
        )

        notificationBuilder = NotificationBuilder(this, PxMediaService::class.java)
        notificationManager = NotificationManagerCompat.from(this)

        becomingNoisyReceiver = BecomingNoisyReceiver(
            context = this, sessionToken = mediaSession.sessionToken
        )
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == MediaConstants.MEDIA_ROOT_ID_EMPTY) {
            Log.d(TAG, "onLoadChildren: oi 1")
            result.sendResult(mediaLibrary.getMediaItems())
        } else {
            Log.d(TAG, "onLoadChildren: oi 2")
            result.sendResult(mediaLibrary.getMediaItems())
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? = if (clientPackageName == applicationContext.packageName) {
        Log.d(TAG, "onGetRoot: oi 1")
        BrowserRoot(MediaConstants.MEDIA_ROOT_ID, null)
    } else {
        Log.d(TAG, "onGetRoot: oi 2")
        BrowserRoot(MediaConstants.MEDIA_ROOT_ID_EMPTY, null)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaPlayerAdapter.stop()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.run {
            isActive = false
            release()
        }

        serviceJob.cancel()
    }

    private fun removeNowPlayingNotification() {
        stopForeground(true)
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        private val TAG = "MusicService"

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d(TAG, "[onMetadataChanged]: changed")
            mediaController.playbackState?.let { state ->
                serviceScope.launch {
                    updateNotification(state)
                }
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.d(
                TAG,
                "[onPlaybackStateChanged]: changed ${state?.state != PlaybackStateCompat.STATE_BUFFERING && state?.state != PlaybackStateCompat.STATE_PLAYING}"
            )
            state?.let {
                serviceScope.launch {
                    updateNotification(state)
                }
            }
        }

        private suspend fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state

            // Skip building a notification when state is "none" and metadata is null.
            val notification = if (
                mediaController.metadata != null
                && updatedState != PlaybackStateCompat.STATE_NONE
            ) {
                notificationBuilder.buildNotification(mediaSession.sessionToken)
            } else {
                null
            }

            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    Log.d(TAG, "[updateNotification]: register")
                    becomingNoisyReceiver.register()

                    /**
                     * This may look strange, but the documentation for [Service.startForeground]
                     * notes that "calling this method does *not* put the service in the started
                     * state itself, even though the name sounds like it."
                     */
                    if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)

                        if (!isForegroundService) {
                            ContextCompat.startForegroundService(
                                applicationContext,
                                Intent(applicationContext, this@PxMediaService.javaClass)
                            )
                            startForeground(NOW_PLAYING_NOTIFICATION, notification)
                            isForegroundService = true
                        }
                    }
                }
                else -> {
                    Log.d(TAG, "[updateNotification]: unregister")
                    becomingNoisyReceiver.unregister()

                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false

                        // If playback has ended, also stop the service.
                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
                            stopSelf()
                        }

                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } else {
                            removeNowPlayingNotification()
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            handleRemoteAction(action, intent.extras)
        }
        return Service.START_NOT_STICKY
    }

    private fun handleRemoteAction(action: String, extras: Bundle?) {
        when (action) {
            SeekableRemoteActions.ACTION_SEEK_FORWARD_30S -> {
                mediaController.transportControls.seekTo(mediaController.playbackState.currentPlayBackPosition + 30000)
            }
            SeekableRemoteActions.ACTION_SEEK_BACKWARD_10S -> {
                mediaController.transportControls.seekTo(mediaController.playbackState.currentPlayBackPosition - 10000)
            }
        }
    }
}