package br.com.pixelwolf.pxmediaplayer.download.listeners

interface DownloadServiceListener {
    fun onDownloadServiceConnected()
    fun onDownloadServiceDisconnected()
}