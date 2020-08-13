package br.com.pixelwolf.pxmediaplayer

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import br.com.pixelwolf.pxmediaplayer.adapter.MediaPlayerAdapter
import br.com.pixelwolf.pxmediaplayer.constants.BundleConstants
import br.com.pixelwolf.pxmediaplayer.constants.CommandConstants
import br.com.pixelwolf.pxmediaplayer.constants.MediaConstants
import br.com.pixelwolf.pxmediaplayer.extensions.*
import br.com.pixelwolf.pxmediaplayer.utils.MediaMetadataUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MediaSessionCallback(
    private val mediaSession: MediaSessionCompat,
    private val mediaPlayerAdapter: MediaPlayerAdapter,
    private val mediaLibrary: MediaLibrary,
    private val mediaMetadataUtils: MediaMetadataUtils
) : MediaSessionCompat.Callback() {

    companion object {
        private const val TAG = "MediaSessionCallback"
    }

    private val playlist: MutableList<MediaSessionCompat.QueueItem> = mutableListOf()
    private var queueIndex: Int = -1
    private var preparedMedia: MediaMetadataCompat? = null

    private val mediaController: MediaControllerCompat = mediaSession.controller

    override fun onPrepare() {
        if (queueIndex < 0 && playlist.isEmpty()) return

        val queueItem: MediaSessionCompat.QueueItem = playlist[queueIndex]

        queueItem.description.mediaId?.let { mediaId ->
            preparedMedia = mediaLibrary.getMetadata(mediaId)

            mediaSession.setMetadata(preparedMedia)
        }

        if (!mediaSession.isActive) mediaSession.isActive = true
    }

    override fun onPlay() {
        if (isReadyToPlay()) {
            preparedMedia?.let { media ->
                if (mediaController.playbackState == null || !mediaController.playbackState.isPaused) {
                    mediaPlayerAdapter.playFromMedia(media)
                } else {
                    mediaPlayerAdapter.play()
                }
            } ?: run {
                prepareAndPlay()
            }
        }
    }

    override fun onPause() {
        mediaPlayerAdapter.pause()
    }

    override fun onStop() {
        mediaPlayerAdapter.stop()
        preparedMedia = null
        mediaSession.isActive = false
    }

    override fun onAddQueueItem(description: MediaDescriptionCompat?) {
        description?.let {
            queueIndex = if (queueIndex == -1) 0 else queueIndex

            playlist.add(
                MediaSessionCompat.QueueItem(
                    description, description.hashCode().toLong()
                )
            )

            mediaSession.setQueue(playlist)
        }
    }

    override fun onSkipToNext() {
        queueIndex = (++queueIndex % playlist.size)
        preparedMedia = null

        if (mediaController.playbackState.isPlaying) {
            mediaPlayerAdapter.skipToNext()
            onPlay()
        } else {
            mediaPlayerAdapter.skipToNext()
            onPrepare()
            preparedMedia?.let { mediaPlayerAdapter.playFromMedia(it, false) }
        }
    }

    override fun onSkipToPrevious() {
        queueIndex = if (queueIndex > 0) queueIndex - 1 else playlist.size - 1
        preparedMedia = null

        if (mediaController.playbackState.isPlaying) {
            mediaPlayerAdapter.skipToPrevious()
            onPlay()
        } else {
            mediaPlayerAdapter.skipToPrevious()
            onPrepare()
            preparedMedia?.let { mediaPlayerAdapter.playFromMedia(it, false) }
        }
    }

    override fun onSeekTo(pos: Long) {
        preparedMedia?.let { media ->
            when {
                pos > media.duration -> onSkipToNext()
                pos < 0 -> mediaPlayerAdapter.seekTo(0, mediaController.playbackState.isPlaying)
                else -> mediaPlayerAdapter.seekTo(pos, mediaController.playbackState.isPlaying)
            }
        }
    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        when (command) {
            MediaConstants.COMMAND_PLAY_SONG_AT -> {
                extras?.getInt(BundleConstants.QUEUE_INDEX)?.let { index ->
                    queueIndex = index
                    onPause()
                    preparedMedia = null
                    onPlay()
                }
            }
            MediaConstants.COMMAND_ADD_SONG_TO_QUEUE -> {
                extras?.getParcelable<PxMedia>(BundleConstants.MEDIA)?.let { pxMedia ->
                    GlobalScope.launch {
                        addMedia(pxMedia)
                        cb?.send(CommandConstants.COMMAND_SUCCESS, Bundle.EMPTY)
                    }
                }
            }
            MediaConstants.COMMAND_ADD_PLAYLIST_TO_QUEUE -> {
                GlobalScope.launch {
                    extras?.getParcelableArrayList<PxMedia>(BundleConstants.PLAYLIST)?.forEach {
                        addMedia(it)
                    }
                    cb?.send(CommandConstants.COMMAND_SUCCESS, Bundle.EMPTY)
                }
            }
            else -> throw IllegalStateException("Invalid command")
        }
    }

    private fun isReadyToPlay() = playlist.isNotEmpty()

    private fun prepareAndPlay() {
        if (isReadyToPlay()) {
            onPrepare()
            onPlay()
        }
    }

    private suspend fun addMedia(pxMedia: PxMedia) {
        val mediaDuration: Long = pxMedia.duration
            ?: mediaMetadataUtils.retrieveMediaDuration(pxMedia.mediaUrl)

        val media = MediaMetadataCompat.Builder().apply {
            id = pxMedia.id
            mediaUri = pxMedia.mediaUrl
            title = pxMedia.title
            artist = pxMedia.artist
            displayIconUri = pxMedia.artUrl
            duration = mediaDuration

            downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
        }.build()

        mediaLibrary.addMedia(media)
        onAddQueueItem(media.description)
    }
}