package br.com.pixelwolf.pxmediaplayer.download

import android.content.Context
import android.os.Build
import android.os.Environment
import br.com.pixelwolf.pxmediaplayer.download.listeners.DownloadCompleteListener
import br.com.pixelwolf.pxmediaplayer.download.listeners.DownloadNotificationProvider
import br.com.pixelwolf.pxmediaplayer.download.listeners.DownloadServiceListener
import br.com.pixelwolf.pxmediaplayer.download.listeners.DownloadUpdateListener
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadConnectListener
import com.liulishuo.filedownloader.FileDownloader
import com.liulishuo.filedownloader.model.FileDownloadStatus
import com.liulishuo.filedownloader.notification.BaseNotificationItem
import com.liulishuo.filedownloader.notification.FileDownloadNotificationHelper
import com.liulishuo.filedownloader.notification.FileDownloadNotificationListener
import com.liulishuo.filedownloader.util.FileDownloadUtils
import java.io.File
import java.io.IOException
import java.util.*

open class BaseDownloadManager<T>(private val clientUserAgent: String) {
    private var connectListener: FileDownloadConnectListener? = null
    private var downloadNotificationProvider: DownloadNotificationProvider<T>? = null
    private var downloadCompleteListener: DownloadCompleteListener<T>? = null

    protected fun startTask(tag: T, url: String?, filePath: String?) {
        FileDownloader.getImpl().create(url)
            .setPath(filePath)
            .addHeader("User-Agent", clientUserAgent)
            .setAutoRetryTimes(5)
            .setListener(downloadListener)
            .setTag(tag)
            .start()
    }

    /**
     * Start the download service. This method must be called in order to start the file downloader
     * service and bind automatically when any download task is request to start.
     *
     *
     * This method is usually called from onCreate() method of an activity or fragment.
     */
    fun startService() {
        FileDownloader.getImpl().bindService()
        if (connectListener != null) {
            FileDownloader.getImpl().removeServiceConnectListener(connectListener)
        }
        connectListener = object : FileDownloadConnectListener() {
            override fun connected() {
                notifyServiceConnectedListeners()
            }

            override fun disconnected() {
                notifyServiceDisconnectedListeners()
            }
        }
        FileDownloader.getImpl().addServiceConnectListener(connectListener)
    }

    /**
     * Stops the download service. This method must be called in order to stop the tasks sent to the
     * file downloader service.
     *
     *
     * This method is usually called from onDestroy() method of an activity or fragment.
     */
    fun stopService() {
        FileDownloader.getImpl().removeServiceConnectListener(connectListener)
        downloadListener.helper.clear()
        connectListener = null
        pauseAll()
    }

    fun pauseAll() {
        FileDownloader.getImpl().pauseAll()
    }

    fun pauseDownload(downloadId: Int) {
        FileDownloader.getImpl().pause(downloadId)
    }

    fun cancelDownload(downloadId: Int, filePath: String?) {
        FileDownloader.getImpl().clear(downloadId, filePath)
    }

    private val updateListeners =
        ArrayList<DownloadUpdateListener<T>>()
    private val serviceListeners =
        ArrayList<DownloadServiceListener>()

    fun addUpdaterListener(listener: DownloadUpdateListener<T>) {
        if (!updateListeners.contains(listener)) {
            updateListeners.add(listener)
        }
    }

    fun removeUpdaterListener(listener: DownloadUpdateListener<T>?): Boolean {
        return updateListeners.remove(listener)
    }

    fun addServiceListener(listener: DownloadServiceListener) {
        if (!serviceListeners.contains(listener)) {
            serviceListeners.add(listener)
        }
    }

    fun removeServiceListener(listener: DownloadServiceListener?): Boolean {
        return serviceListeners.remove(listener)
    }

    fun setDownloadNotificationProvider(downloadNotificationProvider: DownloadNotificationProvider<T>?) {
        this.downloadNotificationProvider = downloadNotificationProvider
    }

    fun setDownloadCompleteListener(downloadCompleteListener: DownloadCompleteListener<T>) {
        this.downloadCompleteListener = downloadCompleteListener
    }

    private val downloadListener: FileDownloadNotificationListener =
        object :
            FileDownloadNotificationListener(FileDownloadNotificationHelper<BaseNotificationItem>()) {
            override fun create(task: BaseDownloadTask): BaseNotificationItem? {
                return if (downloadNotificationProvider != null) {
                    downloadNotificationProvider?.onBuildNotification(
                        task.id,
                        task.tag as T
                    )
                } else null
            }

            override fun pending(
                task: BaseDownloadTask,
                soFarBytes: Int,
                totalBytes: Int
            ) {
                super.pending(task, soFarBytes, totalBytes)
                notifyUpdateListeners(task)
            }

            override fun started(task: BaseDownloadTask) {
                super.started(task)
                notifyUpdateListeners(task)
            }

            override fun connected(
                task: BaseDownloadTask,
                etag: String,
                isContinue: Boolean,
                soFarBytes: Int,
                totalBytes: Int
            ) {
                super.connected(task, etag, isContinue, soFarBytes, totalBytes)
                notifyUpdateListeners(task)
            }

            override fun progress(
                task: BaseDownloadTask,
                soFarBytes: Int,
                totalBytes: Int
            ) {
                super.progress(task, soFarBytes, totalBytes)
                notifyUpdateListeners(task)
            }

            override fun completed(task: BaseDownloadTask) {
                super.completed(task)
                downloadCompleteListener?.onDownloadComplete(task.tag as T)
                notifyUpdateListeners(task)
            }

            override fun paused(
                task: BaseDownloadTask,
                soFarBytes: Int,
                totalBytes: Int
            ) {
                super.paused(task, soFarBytes, totalBytes)
                notifyUpdateListeners(task)
            }

            override fun error(task: BaseDownloadTask, e: Throwable) {
                super.error(task, e)
                e.printStackTrace()
                notifyUpdateListeners(task)
            }

            override fun warn(task: BaseDownloadTask) {
                notifyUpdateListeners(task)
            }
        }

    private fun notifyUpdateListeners(task: BaseDownloadTask) {
        updateListeners.forEach {
            it.onDownloadUpdate(
                task.tag as T,
                mapDownloadStatus(task.status),
                getProgress(task.id),
                task.speed
            )
        }
    }

    private fun notifyServiceConnectedListeners() {
        serviceListeners.forEach {
            it.onDownloadServiceConnected()
        }
    }

    private fun notifyServiceDisconnectedListeners() {
        serviceListeners.forEach {
            it.onDownloadServiceDisconnected()
        }
    }

    companion object {
        val isReady: Boolean
            get() = FileDownloader.getImpl().isServiceConnected

        fun downloadExists(downloadId: Int, filePath: String?): Boolean {
            return getStatus(
                downloadId,
                filePath
            ) === DownloadStatus.COMPLETED
        }

        @JvmStatic
        fun isDownloading(downloadId: Int): Boolean {
            return FileDownloadStatus.isIng(
                FileDownloader.getImpl().getStatusIgnoreCompleted(
                    downloadId
                ).toInt()
            )
        }

        @JvmStatic
        fun getStatus(downloadId: Int, filePath: String?): DownloadStatus {
            return mapDownloadStatus(
                FileDownloader.getImpl().getStatus(
                    downloadId,
                    filePath
                )
            )
        }

        fun getProgress(downloadId: Int): Float {
            return FileDownloader.getImpl().getSoFar(downloadId) /
                    FileDownloader.getImpl().getTotal(downloadId).toFloat()
        }

        @JvmOverloads
        fun generateSaveFilePath(
            url: String,
            context: Context,
            customPathName: String = "pxmediacache"
        ): String {
            val file = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                File(
                    Environment.getExternalStorageDirectory().toString() + "/system_data",
                    customPathName
                )
            } else {
                val root = File("/system_data", customPathName)
                context.getExternalFilesDir(Environment.getExternalStorageState(root))
            }

            return file?.let {
                if (!it.exists()) it.mkdirs()

                FileDownloadUtils.generateFilePath(
                    it.path,
                    FileDownloadUtils.generateFileName(url)
                )
            } ?: throw IOException("File creation error.")
        }

        fun mapDownloadStatus(status: Byte): DownloadStatus {
            return when (status) {
                FileDownloadStatus.pending -> DownloadStatus.PENDING
                FileDownloadStatus.started -> DownloadStatus.STARTED
                FileDownloadStatus.connected -> DownloadStatus.CONNECTED
                FileDownloadStatus.progress -> DownloadStatus.PROGRESS
                FileDownloadStatus.blockComplete -> DownloadStatus.BLOCK_COMPLETE
                FileDownloadStatus.retry -> DownloadStatus.RETRY
                FileDownloadStatus.error -> DownloadStatus.ERROR
                FileDownloadStatus.paused -> DownloadStatus.PAUSED
                FileDownloadStatus.completed -> DownloadStatus.COMPLETED
                FileDownloadStatus.warn -> DownloadStatus.WARN
                else -> DownloadStatus.INVALID
            }
        }
    }

}