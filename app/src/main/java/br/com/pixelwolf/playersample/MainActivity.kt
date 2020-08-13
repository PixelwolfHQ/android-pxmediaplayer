package br.com.pixelwolf.playersample

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import br.com.pixelwolf.pxmediaplayer.PxMedia
import br.com.pixelwolf.pxmediaplayer.PxPlayer
import br.com.pixelwolf.pxmediaplayer.constants.BundleConstants
import br.com.pixelwolf.pxmediaplayer.constants.MediaConstants
import br.com.pixelwolf.pxmediaplayer.download.BaseDownloadManager.Companion.generateSaveFilePath
import br.com.pixelwolf.pxmediaplayer.download.DownloadManager
import br.com.pixelwolf.pxmediaplayer.download.DownloadStatus
import br.com.pixelwolf.pxmediaplayer.download.listeners.DownloadCompleteListener
import br.com.pixelwolf.pxmediaplayer.download.listeners.DownloadUpdateListener
import br.com.pixelwolf.pxmediaplayer.extensions.duration
import br.com.pixelwolf.pxmediaplayer.extensions.title
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var isUserSeeking: Boolean = false

    private lateinit var pxPlayer: PxPlayer
    private lateinit var downloadManager: DownloadManager

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaBrowserCompat.MediaItem>
        ) {
            children.forEach {
                Log.d(TAG, "[onChildrenLoaded]: ${it.description.title}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pxPlayer = PxPlayer.getInstance(application)
        downloadManager = DownloadManager(application, "PxMediaPlayerDemo")

        observers()

        action_play_pause.setOnClickListener {
            pxPlayer.transportControls.run { handlePlayPause() }
            pxPlayer.addBufferListener()
        }

        action_next.setOnClickListener {
            pxPlayer.transportControls.skipToNext()
        }

        action_previous.setOnClickListener {
            pxPlayer.transportControls.skipToPrevious()
        }

        action_seek_forward.setOnClickListener {
            pxPlayer.transportControls.seekTo(
                (pxPlayer.progress.value ?: 0) + 30000
            )
        }

        action_seek_backward.setOnClickListener {
            pxPlayer.transportControls.seekTo(
                (pxPlayer.progress.value ?: 0) - 30000
            )
        }

        action_stop.setOnClickListener {
            pxPlayer.transportControls.stop()
        }

        action_add_medias.setOnClickListener {
            progress_add_media.isVisible = true
            val medias = buildMedias()
            val bundle = Bundle().apply {
                putParcelableArrayList(BundleConstants.PLAYLIST, ArrayList<PxMedia>(medias))
            }
            pxPlayer.sendCommand(
                MediaConstants.COMMAND_ADD_PLAYLIST_TO_QUEUE,
                bundle
            ) { resultCode, b ->
                progress_add_media.isVisible = false
                Log.d(TAG, "[addPlaylistToQueue]: result -> $resultCode")
            }
        }

        action_download.setOnClickListener {
            downloadMedia()
        }

        seekBarListener()
    }

    override fun onDestroy() {
        pxPlayer.isConnected.removeObservers(this)
        pxPlayer.nowPlaying.removeObservers(this)
        pxPlayer.playbackState.removeObservers(this)
        pxPlayer.progress.removeObservers(this)
        pxPlayer.isLoading.removeObservers(this)

        pxPlayer.unsubscribe(mediaId = pxPlayer.rootId, callback = subscriptionCallback)
        pxPlayer.disconnect()
        super.onDestroy()
    }

    private fun observers() {
        pxPlayer.isConnected.observe(this, Observer { isConnected ->
            if (isConnected) {
                Log.d(TAG, "[onCreate]: px player is connected")
                pxPlayer.subscribe(mediaId = pxPlayer.rootId, callback = subscriptionCallback)
            }
        })

        pxPlayer.nowPlaying.observe(this, Observer {
            text_title.text = it.title
            text_duration.text = timestampToMSS(this, it.duration)
            seek_progress.max = (it.duration / 1000).toInt()
        })

        pxPlayer.playbackState.observe(this, Observer {
            action_play_pause.setImageDrawable(
                getDrawable(
                    when (it.state) {
                        PlaybackStateCompat.STATE_PLAYING -> R.drawable.ic_pause_black_24dp
                        else -> R.drawable.ic_play_arrow_black_24dp
                    }
                )
            )
        })

        pxPlayer.progress.observe(this, Observer { progress ->
            if (!isUserSeeking) seek_progress.progress = (progress / 1000).toInt()
        })

        pxPlayer.bufferProgress.observe(this, Observer { bufferProgress ->
            if (!isUserSeeking) seek_progress.secondaryProgress = (bufferProgress / 1000).toInt()
        })

        pxPlayer.isLoading.observe(this, Observer { isBuffering ->
            progress_buffering.isVisible = isBuffering
        })
    }

    private fun MediaControllerCompat.TransportControls.handlePlayPause() {
        pxPlayer.playbackState.value?.let { state ->
            when (state.state) {
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_PAUSED -> play()
                PlaybackStateCompat.STATE_PLAYING -> pause()
            }
        }
    }

    private fun buildMedias(): List<PxMedia> = listOf(
        PxMedia(
            id = "711-os-reflexos-da-revolucao-iraniana-2",
            title = "Os Reflexos da Revolução Iraniana",
            artist = "Jovem Nerd",
            duration = 4916203,
            artUrl = "https://uploads.jovemnerd.com.br/wp-content/uploads/2020/02/NC_711_Os-Reflexos-da-Revolucao-Islamica_giga-1.jpg",
            mediaUrl = "https://nerdcast.jovemnerd.com.br/nerdcast_711_revolucao_iraniana.mp3",
            publishDate = System.currentTimeMillis()
        ),
        PxMedia(
            id = "711-os-reflexos-da-revolucao-iraniana",
            title = "Os Reflexos da Revolução Iraniana",
            artist = "Jovem Nerd",
            duration = 4916203,
            artUrl = "https://uploads.jovemnerd.com.br/wp-content/uploads/2020/02/NC_711_Os-Reflexos-da-Revolucao-Islamica_giga-1.jpg",
            mediaUrl = "/storage/emulated/0/system_data/pxmediacache/28ff2a3c5a0ea894e240cba39d4af59f",
            publishDate = System.currentTimeMillis()
        ),
        PxMedia(
            id = "drop_and_roll",
            title = "Drop and Roll",
            artist = "Silent Partner",
            duration = 121128,
            mediaUrl = "https://storage.googleapis.com/automotive-media/Drop_and_Roll.mp3",
            publishDate = System.currentTimeMillis()
        ),
        PxMedia(
            id = "hey_sailor",
            title = "Hey Sailor",
            artist = "Letter Box",
            duration = 193646,
            mediaUrl = "https://storage.googleapis.com/automotive-media/Hey_Sailor.mp3",
            publishDate = System.currentTimeMillis()
        )
    )

    private fun timestampToMSS(context: Context, position: Long): String {
        val totalSeconds = floor(position / 1E3).toInt()
        val minutes = totalSeconds / 60
        val remainingSeconds = totalSeconds - (minutes * 60)
        return if (position < 0) context.getString(R.string.duration_unknown)
        else context.getString(R.string.duration_format).format(minutes, remainingSeconds)
    }

    private fun seekBarListener() {
        seek_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                text_progress.text = timestampToMSS(this@MainActivity, progress * 1000L)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    pxPlayer.transportControls.seekTo(seekBar.progress * 1000L)
                }
                isUserSeeking = false
            }

        })
    }

    private fun downloadMedia() {
        downloadManager.setDownloadCompleteListener(object : DownloadCompleteListener<PxMedia> {
            override fun onDownloadComplete(tag: PxMedia) {
                Log.d(TAG, "[onDownloadComplete]: download completed")
            }
        })

        downloadManager.addUpdaterListener(object : DownloadUpdateListener<PxMedia> {
            override fun onDownloadUpdate(
                media: PxMedia,
                status: DownloadStatus?,
                progress: Float,
                speed: Int
            ) {
                Log.d(TAG, "[onDownloadUpdate]: status   -> ${status?.name}")
                Log.d(TAG, "[onDownloadUpdate]: progress -> $progress")
                Log.d(TAG, "[onDownloadUpdate]: speed    -> $speed")
            }
        })

        downloadManager.downloadMedia(
            PxMedia(
                id = "711-os-reflexos-da-revolucao-iraniana",
                title = "Os Reflexos da Revolução Iraniana",
                artist = "Jovem Nerd",
                duration = 4916203,
                artUrl = "https://uploads.jovemnerd.com.br/wp-content/uploads/2020/02/NC_711_Os-Reflexos-da-Revolucao-Islamica_giga-1.jpg",
                mediaUrl = "https://nerdcast.jovemnerd.com.br/nerdcast_711_revolucao_iraniana.mp3",
                publishDate = System.currentTimeMillis()
            ).apply {
                filePath = generateSaveFilePath(url = mediaUrl, context = this@MainActivity).also {
                    Log.d(
                        TAG,
                        "[downloadMedia]: filepath -> $it"
                    )
                }
            }
        )
    }
}
