package br.com.pixelwolf.playersample

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import com.liulishuo.filedownloader.model.FileDownloadStatus
import com.liulishuo.filedownloader.notification.BaseNotificationItem
import com.liulishuo.filedownloader.util.FileDownloadHelper

//TODO: Update static strings with resources
class DownloadNotificationItem(id: Int, title: String, desc: String) :
    BaseNotificationItem(id, title, desc) {

    override fun show(statusChanged: Boolean, status: Int, isShowProgress: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildNotificationChannel()
        }

        val builder = NotificationCompat.Builder(FileDownloadHelper.getAppContext(), CHANNEL_ID)

        var desc = desc
        when (status.toByte()) {
            FileDownloadStatus.pending -> desc += " Transferência Pendente"
            FileDownloadStatus.progress -> desc += " Transferindo ${((sofar / total.toFloat()) * 100).toInt()}%"
            FileDownloadStatus.retry -> desc += " Tentando transferir novamente..."
            FileDownloadStatus.error -> desc += " Erro na transferência"
            FileDownloadStatus.paused -> desc += " Transferência pausada"
            FileDownloadStatus.completed -> desc += " Transferência finalizada"
            FileDownloadStatus.warn -> desc += " Erro na transferência"
        }

        builder.setDefaults(Notification.DEFAULT_LIGHTS)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_MIN)
            .setContentTitle(title)
            .setColor(ContextCompat.getColor(FileDownloadHelper.getAppContext(), R.color.colorPrimary))
            .setContentText(desc)

        if (statusChanged) {
            builder.setTicker(desc)
        }

        builder.setProgress(total, sofar, !isShowProgress)
        manager.notify(id, builder.build())
    }

    /**
     * Builds the notification channelDTO using the default name and description if the channelDTO
     * hasn't already been created.
     */
    @TargetApi(Build.VERSION_CODES.O)
    private fun buildNotificationChannel() {
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {

            val channel =
                NotificationChannel(CHANNEL_ID, "JukeboxDownload", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Progresso do download de mídias"
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private val CHANNEL_ID = "DownloadMediaNotificationChannel"
    }
}