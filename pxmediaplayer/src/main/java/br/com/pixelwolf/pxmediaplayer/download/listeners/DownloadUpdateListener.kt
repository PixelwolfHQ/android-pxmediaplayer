package br.com.pixelwolf.pxmediaplayer.download.listeners

import br.com.pixelwolf.pxmediaplayer.download.DownloadStatus

interface DownloadUpdateListener<T> {
    fun onDownloadUpdate(
        media: T,
        status: DownloadStatus?,
        progress: Float,
        speed: Int
    )
}