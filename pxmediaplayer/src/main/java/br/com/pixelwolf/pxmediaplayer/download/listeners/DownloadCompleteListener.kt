package br.com.pixelwolf.pxmediaplayer.download.listeners

interface DownloadCompleteListener<T> {
    fun onDownloadComplete(tag: T)
}