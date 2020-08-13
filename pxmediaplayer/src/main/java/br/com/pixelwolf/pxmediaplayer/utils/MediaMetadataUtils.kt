package br.com.pixelwolf.pxmediaplayer.utils

import android.media.MediaMetadataRetriever
import android.util.Log
import br.com.pixelwolf.pxmediaplayer.extensions.toLongOrZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaMetadataUtils {

    companion object {
        private const val TAG = "MediaMetadataUtils"
    }

    suspend fun retrieveMediaDuration(mediaUri: String): Long {
        return withContext(Dispatchers.Default) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(mediaUri, hashMapOf())
            return@withContext mediaMetadataRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            ).toLongOrZero().also {
                Log.d(TAG, "[retrieveMediaDuration]: $mediaUri -> $it")
            }
        }
    }

}