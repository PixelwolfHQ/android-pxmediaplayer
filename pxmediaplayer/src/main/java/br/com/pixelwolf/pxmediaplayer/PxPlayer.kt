package br.com.pixelwolf.pxmediaplayer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import br.com.pixelwolf.pxmediaplayer.extensions.currentPlayBackPosition
import br.com.pixelwolf.pxmediaplayer.extensions.isBuffering
import br.com.pixelwolf.pxmediaplayer.listeners.MediaBuffer
import br.com.pixelwolf.pxmediaplayer.storage.Storage

class PxPlayer(application: Application, serviceComponent: ComponentName) {

    companion object {
        private const val TAG = "PxPlayer"

        @Volatile
        private var instance: PxPlayer? = null

        private var storage: Storage? = null

        fun getInstance(application: Application) =
            instance ?: synchronized(this) {
                instance ?: PxPlayer(
                    application,
                    ComponentName(application, PxMediaService::class.java)
                ).also { instance = it }
            }

        internal fun getStorage(application: Application): Storage {
            return storage ?: Storage(application).also { storage = it }
        }
    }

    private lateinit var mediaController: MediaControllerCompat

    private val progressHandler = Handler()

    fun addBufferListener() {
        MediaBuffer.registerCallback(MediaBufferListener())
    }

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(application)
    private val mediaBrowser = MediaBrowserCompat(
        application,
        serviceComponent,
        mediaBrowserConnectionCallback,
        null
    ).apply {
        connect()
    }

    private val mediaProgress = object : Runnable {
        override fun run() {
            progressHandler.postDelayed(this, 100L)
            progress.postValue(mediaController.playbackState.currentPlayBackPosition)
        }
    }

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    val rootId: String
        get() = mediaBrowser.root

    val isConnected = MutableLiveData<Boolean>()
        .apply { postValue(false) }

    val playbackState = MutableLiveData<PlaybackStateCompat>().apply {
        postValue(EMPTY_PLAYBACK_STATE)
    }

    val nowPlaying = MutableLiveData<MediaMetadataCompat>().apply {
        postValue(NOTHING_PLAYING)
    }

    val progress = MutableLiveData<Long>().apply { postValue(0) }

    val isLoading = MutableLiveData<Boolean>().apply { postValue(false) }

    val bufferProgress = MutableLiveData<Long>().apply { postValue(0) }

    fun subscribe(mediaId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(mediaId, callback)
    }

    fun unsubscribe(mediaId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(mediaId, callback)
    }

    fun disconnect() {
        mediaBrowser.disconnect()
    }

    fun sendCommand(command: String, parameters: Bundle?) =
        sendCommand(command, parameters) { _, _ -> }

    fun sendCommand(
        command: String,
        parameters: Bundle?,
        resultCallback: ((Int, Bundle?) -> Unit)
    ) = if (mediaBrowser.isConnected) {
        mediaController.sendCommand(command, parameters, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                resultCallback(resultCode, resultData)
            }
        })
        true
    } else {
        false
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }.also {
                Log.d(TAG, "[onConnected]: Connection successful")

                isConnected.postValue(true)
            }
        }

        override fun onConnectionSuspended() {
            isConnected.postValue(false)
        }

        override fun onConnectionFailed() {
            Log.d(TAG, "[onConnectionFailed]: Connection failed")

            isConnected.postValue(false)
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state?.state == PlaybackStateCompat.STATE_PLAYING) {
                progressHandler.post(mediaProgress)
            } else {
                if (state?.state == PlaybackStateCompat.STATE_STOPPED) {
                    progress.postValue(0)
                }
                progress.postValue(state?.position)
                progressHandler.removeCallbacks(mediaProgress)
            }

            Log.d(TAG, "[onPlaybackStateChanged]: isBuffering -> ${state?.isBuffering}")

            isLoading.postValue(state?.isBuffering)

            bufferProgress.postValue(state?.bufferedPosition)

            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            nowPlaying.postValue(metadata ?: NOTHING_PLAYING)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            Log.d(TAG, "[onQueueChanged]: queue has changed")
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    private inner class MediaBufferListener : MediaBuffer.BufferListener() {
        override fun onBufferChanged(bufferPosition: Long) {
            bufferProgress.postValue(bufferPosition)
        }
    }

}