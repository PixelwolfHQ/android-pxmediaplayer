package br.com.pixelwolf.pxmediaplayer.download

import android.app.Application
import br.com.pixelwolf.pxmediaplayer.PxMedia
import com.liulishuo.filedownloader.FileDownloader

class DownloadManager(
    application: Application,
    clientUserAgent: String
) : BaseDownloadManager<PxMedia>(clientUserAgent) {

    init {
        FileDownloader.setup(application)
    }

    fun downloadMedia(media: PxMedia) {
        startTask(media, media.mediaUrl, media.filePath)
    }

    fun pauseMediaDownload(media: PxMedia) {
        media.downloadId?.let { pauseDownload(it) }
    }

    fun cancelMediaDownload(media: PxMedia) {
        media.downloadId?.let { cancelDownload(it, media.filePath) }
    }

    fun releaseDownloads() {
        FileDownloader.getImpl().pauseAll()
    }

    companion object {
        fun isDownloadingMedia(media: PxMedia): Boolean {
            return media.downloadId?.let { isDownloading(it) } ?: false
        }

        fun getMediaDownloadStatus(media: PxMedia): DownloadStatus {
            return media.downloadId?.let { getStatus(it, media.filePath) } ?: DownloadStatus.INVALID
        }

        /**
         * Delete the downloaded media file if the media exists.
         *
         * @param media the downloaded media to delete
         * @return true if the media was deleted
         */
        fun deleteDownloadedMedia(media: PxMedia): Boolean { //TODO implement
//        if (media.isDownloaded()) {
//            // Continue to play item but using media url
//            PlaylistManager playlistManager = Playcast.INSTANCE.getPlaylistManager();
//            if (playlistManager.isPlayingItem(media)) {
//                MediaProgress mediaProgress = playlistManager.getCurrentProgress();
//                if (mediaProgress != null) {
//                    if (playlistManager.getCurrentPlaybackState() == PlaybackState.PLAYING) {
//                        playlistManager.play((int) mediaProgress.getPosition(), false);
//                    } else {
//                        playlistManager.play((int) mediaProgress.getPosition(), true);
//                    }
//                }
//            }
//            return new File(media.getFilePath()).delete();
//        }
            return false
        }
    }
}