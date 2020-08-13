package br.com.pixelwolf.pxmediaplayer.adapter

import android.content.Context
import android.os.Handler
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import br.com.pixelwolf.pxmediaplayer.extensions.toMediaSource
import br.com.pixelwolf.pxmediaplayer.listeners.MediaBuffer
import br.com.pixelwolf.pxmediaplayer.listeners.MediaPlaybackListener
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class MediaPlayerAdapter(
    private val context: Context,
    private val mediaSession: MediaSessionCompat,
    private val dataSourceFactory: DefaultDataSourceFactory,
    private val mediaPlaybackListener: MediaPlaybackListener
) {

    companion object {
        private const val TAG = "MediaPlayerAdapter"
    }

    private val bufferProgressHandler = Handler()

    private val bufferProgress = object : Runnable {
        override fun run() {
            bufferProgressHandler.postDelayed(this, 1000L)
            exoPlayer?.let { MediaBuffer.onBufferChanged(it.bufferedPosition) }
        }
    }

    private var exoPlayer: SimpleExoPlayer? = null

    private fun initExoPlayer() {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context).apply {
            setAudioAttributes(createAudioAttributes(), true)
            addListener(createExoPlayerEventListener())
        }
    }

    private fun createAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    private fun createExoPlayerEventListener() = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_ENDED -> {
                    setNewState(PlaybackStateCompat.STATE_PAUSED)
                    mediaPlaybackListener.onPlaybackCompleted()
                }
                ExoPlayer.STATE_BUFFERING -> setNewState(PlaybackStateCompat.STATE_BUFFERING)
                ExoPlayer.STATE_READY -> play()
            }
        }
    }

    fun playFromMedia(mediaMetadata: MediaMetadataCompat, play: Boolean = true) {
        if (exoPlayer == null) initExoPlayer()
        val dataSource = mediaMetadata.toMediaSource(dataSourceFactory)
        exoPlayer?.prepare(dataSource)
        if (play) play() else pause()
    }

    fun play() {
        exoPlayer?.playWhenReady = true
        bufferProgressHandler.removeCallbacks(bufferProgress)
        bufferProgressHandler.post(bufferProgress)

        setNewState(PlaybackStateCompat.STATE_PLAYING)
    }

    fun skipToNext() {
        setNewState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)
    }

    fun skipToPrevious() {
        setNewState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS)
    }

    fun seekTo(pos: Long, play: Boolean) {
        exoPlayer?.seekTo(pos)
        if (play) setNewState(PlaybackStateCompat.STATE_PLAYING)
        else setNewState(PlaybackStateCompat.STATE_PAUSED)
    }

    fun pause() {
        exoPlayer?.playWhenReady = false

        setNewState(PlaybackStateCompat.STATE_PAUSED)
    }

    fun stop() {
        bufferProgressHandler.removeCallbacks(bufferProgress)

        pause()

        setNewState(PlaybackStateCompat.STATE_STOPPED)

        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun setNewState(@PlaybackStateCompat.State newState: Int) {
        exoPlayer?.let {
            val playbackPosition = it.contentPosition
            val bufferPosition = it.bufferedPosition

            val stateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder().apply {
                setActions(getAvailableActions(newState))
                setBufferedPosition(bufferPosition)
                setState(newState, playbackPosition, 1.0f, SystemClock.elapsedRealtime())
            }

            mediaSession.setPlaybackState(stateBuilder.build())
            Log.d(TAG, "[setNewState]: current state -> $newState")
        }
    }

    @PlaybackStateCompat.Actions
    private fun getAvailableActions(@PlaybackStateCompat.State state: Int): Long {
        val actions: Long = PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        return when (state) {
            PlaybackStateCompat.STATE_STOPPED -> {
                actions or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
            }
            PlaybackStateCompat.STATE_PLAYING -> {
                actions or PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                actions or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_STOP
            }
            else -> {
                actions or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE
            }
        }
    }

}