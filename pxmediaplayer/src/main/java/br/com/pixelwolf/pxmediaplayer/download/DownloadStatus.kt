package br.com.pixelwolf.pxmediaplayer.download

enum class DownloadStatus {
    PENDING,
    STARTED,
    CONNECTED,
    PROGRESS,
    BLOCK_COMPLETE,
    RETRY,
    ERROR,
    PAUSED,
    COMPLETED,
    WARN,
    INVALID
}