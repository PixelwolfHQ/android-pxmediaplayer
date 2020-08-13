package br.com.pixelwolf.pxmediaplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import br.com.pixelwolf.pxmediaplayer.extensions.id
import java.util.*

class MediaLibrary {

    companion object {
        private const val TAG = "MediaLibrary"
    }

    private val medias: TreeMap<String, MediaMetadataCompat> = TreeMap()

    fun addMedia(media: MediaMetadataCompat) {
        medias[media.id] = media
    }

    fun addPlaylist(playlist: List<MediaMetadataCompat>) {
        playlist.forEach { medias[it.id] = it }
    }

    fun removeMedia(media: MediaMetadataCompat) {
        medias.remove(media.id)
    }

    fun clear() {
        medias.clear()
    }

    fun getMediaItem(mediaId: String): MediaBrowserCompat.MediaItem? {
        return medias[mediaId]?.description?.let { description ->
            MediaBrowserCompat.MediaItem(
                description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
    }

    fun getMediaItems(): List<MediaBrowserCompat.MediaItem> {
        return medias.values.map {
            MediaBrowserCompat.MediaItem(
                it.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
    }

    fun getMetadata(mediaId: String): MediaMetadataCompat? = medias[mediaId]

}