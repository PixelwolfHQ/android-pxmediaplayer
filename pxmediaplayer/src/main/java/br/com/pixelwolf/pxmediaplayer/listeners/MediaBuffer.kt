package br.com.pixelwolf.pxmediaplayer.listeners

object MediaBuffer {

    private const val TAG = "Buffer"

    private var callback: BufferListener? = null

    fun registerCallback(callback: BufferListener) {
        this.callback = callback
    }

    fun onBufferChanged(bufferPosition: Long) {
        callback?.onBufferChanged(bufferPosition)
    }

    abstract class BufferListener {
        abstract fun onBufferChanged(bufferPosition: Long)
    }
}