package br.com.pixelwolf.pxmediaplayer.storage

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

internal class Storage(application: Application) {

    companion object {
        private const val TAG = "Storage"
    }

    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences(Key.PREF, Context.MODE_PRIVATE)
    }

    object Key {
        const val PREF = "PxMediaStorage"
        const val MEDIA_PROGRESS_POSITION = "media_%s_progress_position"
        const val MEDIA_PROGRESS_DURATION = "media_%s_progress_duration"
        const val MEDIA_PROGRESS_COMPLETED = "media_%s_progress_completed"
    }

    fun updateMediaProgress(mediaId: Long, progressPosition: Long, progressDuration: Long) {
        prefs[String.format(Key.MEDIA_PROGRESS_POSITION, mediaId)] = progressPosition
        prefs[String.format(Key.MEDIA_PROGRESS_DURATION, mediaId)] = progressDuration
    }

    fun setMediaCompleted(mediaId: Long) {
        prefs[String.format(Key.MEDIA_PROGRESS_COMPLETED, mediaId)] = true
    }

    fun getMediaCompleted(mediaId: Long): Boolean {
        return prefs[String.format(Key.MEDIA_PROGRESS_COMPLETED, mediaId)] ?: false
    }

    fun getMediaProgressPosition(mediaId: Long): Long? {
        return prefs[String.format(Key.MEDIA_PROGRESS_POSITION, mediaId)]
    }

    fun getMediaProgressDuration(mediaId: Long): Long? {
        return prefs[String.format(Key.MEDIA_PROGRESS_DURATION, mediaId)]
    }

    inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = this.edit()
        operation(editor)
        editor.apply()
    }

    /**
     * puts a key value pair in shared prefs if doesn't exists, otherwise updates value on given [key]
     */
    operator fun SharedPreferences.set(key: String, value: Any?) {
        when (value) {
            is String? -> edit { it.putString(key, value) }
            is Int -> edit { it.putInt(key, value) }
            is Boolean -> edit { it.putBoolean(key, value) }
            is Float -> edit { it.putFloat(key, value) }
            is Long -> edit { it.putLong(key, value) }
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

    /**
     * finds value on given key.
     * [T] is the type of value
     * @param defaultValue optional default value - will take null for strings, false for bool and -1 for numeric values if [defaultValue] is not specified
     */
    inline operator fun <reified T : Any> SharedPreferences.get(key: String, defaultValue: T? = null): T? {
        return when (T::class) {
            String::class -> getString(key, defaultValue as? String) as T?
            Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
            Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T?
            Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
            Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
            else -> throw UnsupportedOperationException("Not implemented yet")
        }
    }
}