package br.com.pixelwolf.pxmediaplayer.listeners

import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat

class MediaPlaybackListener(
    private val mediaSession: MediaSessionCompat
) : PlaybackInfoListener() {

    private val transportControls: MediaControllerCompat.TransportControls by lazy {
        mediaSession.controller.transportControls
    }

    override fun onPlaybackCompleted() {
        transportControls.skipToNext()
        transportControls.play()
    }

}