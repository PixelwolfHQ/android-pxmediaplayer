package br.com.pixelwolf.pxmediaplayer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import br.com.pixelwolf.pxmediaplayer.PxMediaService
import br.com.pixelwolf.pxmediaplayer.R
import br.com.pixelwolf.pxmediaplayer.constants.NotificationConstants.MODE_READ_ONLY
import br.com.pixelwolf.pxmediaplayer.constants.NotificationConstants.NOW_PLAYING_CHANNEL
import br.com.pixelwolf.pxmediaplayer.constants.SeekableRemoteActions
import br.com.pixelwolf.pxmediaplayer.extensions.*
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationBuilder(
    private val context: Context,
    java: Class<PxMediaService>
) {

    companion object {
        private const val TAG = "NotificationBuilder"
    }

    private val glide: RequestManager = Glide.with(context)

    private val platformNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val skipToPreviousAction = NotificationCompat.Action(
        R.drawable.exo_controls_previous,
        context.getString(R.string.notification_skip_to_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )
    private val seekBackward = NotificationCompat.Action(
        R.drawable.ic_backward_10_black_24dp,
        context.getString(R.string.notification_seek_backward),
        PendingIntent.getService(
            context,
            0,
            Intent(context, java).apply { action = SeekableRemoteActions.ACTION_SEEK_BACKWARD_10S },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    )
    private val playAction = NotificationCompat.Action(
        R.drawable.exo_controls_play,
        context.getString(R.string.notification_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_PLAY
        )
    )
    private val pauseAction = NotificationCompat.Action(
        R.drawable.exo_controls_pause,
        context.getString(R.string.notification_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_PAUSE
        )
    )
    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.exo_controls_next,
        context.getString(R.string.notification_skip_to_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )
    private val seekForward = NotificationCompat.Action(
        R.drawable.ic_forward_30_black_24dp,
        context.getString(R.string.notification_seek_forward),
        PendingIntent.getService(
            context,
            0,
            Intent(context, java).apply { action = SeekableRemoteActions.ACTION_SEEK_FORWARD_30S },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    )
    private val stopPendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
        context,
        PlaybackStateCompat.ACTION_STOP
    )

    suspend fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata.description
        val playbackState = controller.playbackState

        val builder = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)

        // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
        var playPauseIndex = 0
        if (playbackState.isSkipToPreviousEnabled) {
            builder.addAction(seekBackward)
            ++playPauseIndex
        }
        if (playbackState.isPlaying) {
            builder.addAction(pauseAction)
        } else if (playbackState.isPlayEnabled) {
            builder.addAction(playAction)
        }
        if (playbackState.isSkipToNextEnabled) {
            builder.addAction(seekForward)
        }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(playPauseIndex)
            .setShowCancelButton(true)

        val largeIconBitmap: Bitmap? = try {
            description.iconUri?.let { resolveUriAsBitmap(it) }
        } catch (e: Exception) {
            null
        }

        return builder.setContentIntent(controller.sessionActivity)
            .setContentText(description.subtitle)
            .setContentTitle(description.title)
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(largeIconBitmap)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun shouldCreateNowPlayingChannel() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
        platformNotificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(
            NOW_PLAYING_CHANNEL,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        platformNotificationManager.createNotificationChannel(notificationChannel)
    }

    private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            val artUri: Uri = prepareArt(uri.toString())

            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(
                artUri, MODE_READ_ONLY
            ) ?: return@withContext null

            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            return@withContext BitmapFactory.decodeFileDescriptor(fileDescriptor).apply {
                parcelFileDescriptor.close()
            }
        }
    }

    private fun prepareArt(image: String): Uri {
        // Block on downloading artwork.
        val artFile = glide.applyDefaultRequestOptions(glideOptions)
            .downloadOnly()
            .load(image)
            .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
            .get()
        // Expose file via Local URI
        return artFile.asAlbumArtContentUri()
    }

}

private const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

private val glideOptions = RequestOptions()
    .fallback(R.drawable.default_art)
    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)