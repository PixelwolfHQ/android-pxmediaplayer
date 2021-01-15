package br.com.pixelwolf.pxmediaplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import br.com.pixelwolf.pxmediaplayer.extensions.*
import br.com.pixelwolf.pxmediaplayer.utils.MediaMetadataUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class MediaLibrary() {

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
        val pxmedias = buildMedias()
        pxmedias.forEach {
            addMedia(it)
            Log.d(TAG, "getMediaItems: ${it.title}")
        }


        return medias.values.map {
            MediaBrowserCompat.MediaItem(
                it.description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
        }
    }

    fun getMetadata(mediaId: String): MediaMetadataCompat? = medias[mediaId]

    private fun addMedia(pxMedia: PxMedia) {
        val mediaDuration: Long = pxMedia.duration ?: 0L

        val media = MediaMetadataCompat.Builder().apply {
            id = pxMedia.id
            mediaUri = pxMedia.mediaUrl
            title = pxMedia.title
            artist = pxMedia.artist
            displayIconUri = pxMedia.artUrl
            duration = mediaDuration

            downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
        }.build()

        addMedia(media)
    }

    private fun buildMedias(): List<PxMedia> = listOf(
        PxMedia(
            id = "711-os-reflexos-da-revolucao-iraniana-2",
            title = "Os Reflexos da Revolução Iraniana",
            artist = "Jovem Nerd",
            duration = 4916203,
            artUrl = "https://uploads.jovemnerd.com.br/wp-content/uploads/2020/02/NC_711_Os-Reflexos-da-Revolucao-Islamica_giga-1.jpg",
            mediaUrl = "https://nerdcast.jovemnerd.com.br/nerdcast_711_revolucao_iraniana.mp3",
            publishDate = System.currentTimeMillis()
        ),
        PxMedia(
            id = "711-os-reflexos-da-revolucao-iraniana",
            title = "Os Reflexos da Revolução Iraniana",
            artist = "Jovem Nerd",
            duration = 4916203,
            artUrl = "https://uploads.jovemnerd.com.br/wp-content/uploads/2020/02/NC_711_Os-Reflexos-da-Revolucao-Islamica_giga-1.jpg",
            mediaUrl = "/storage/emulated/0/system_data/pxmediacache/28ff2a3c5a0ea894e240cba39d4af59f",
            publishDate = System.currentTimeMillis()
        ),
        PxMedia(
            id = "drop_and_roll",
            title = "Drop and Roll",
            artist = "Silent Partner",
            duration = 121128,
            mediaUrl = "https://storage.googleapis.com/automotive-media/Drop_and_Roll.mp3",
            publishDate = System.currentTimeMillis()
        ),
        PxMedia(
            id = "hey_sailor",
            title = "Hey Sailor",
            artist = "Letter Box",
            duration = 193646,
            mediaUrl = "https://storage.googleapis.com/automotive-media/Hey_Sailor.mp3",
            publishDate = System.currentTimeMillis()
        )
    )

}