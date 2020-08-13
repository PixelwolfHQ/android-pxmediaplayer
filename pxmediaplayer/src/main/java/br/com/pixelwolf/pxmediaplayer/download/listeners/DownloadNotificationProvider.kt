package br.com.pixelwolf.pxmediaplayer.download.listeners

import com.liulishuo.filedownloader.notification.BaseNotificationItem

interface DownloadNotificationProvider<T> {
    fun onBuildNotification(id: Int, media: T): BaseNotificationItem?
}