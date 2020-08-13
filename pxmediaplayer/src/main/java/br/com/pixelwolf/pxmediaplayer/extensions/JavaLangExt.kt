package br.com.pixelwolf.pxmediaplayer.extensions

import android.net.Uri

/**
 * Helper extension to convert a potentially null [String] to a [Uri] falling back to [Uri.EMPTY]
 */
fun String?.toUri(): Uri = this?.let { Uri.parse(it) } ?: Uri.EMPTY

/**
 * Helper extension to convert a potentially null [String] to a [Long] falling back to 0
 */
fun String?.toLongOrZero() = this?.toLongOrNull() ?: 0