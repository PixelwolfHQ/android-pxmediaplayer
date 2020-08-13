package br.com.pixelwolf.pxmediaplayer

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PxMedia(
    val id: String,
    val mediaUrl: String,
    val title: String,
    val artist: String,
    val duration: Long? = null,
    val publishDate: Long? = null,
    val artUrl: String? = null,
    var filePath: String? = null,
    var downloadId: Int? = null,
    var isDownloaded: Boolean = false
) : Parcelable