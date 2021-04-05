package rs.highlande.app.tatatu.feature.chat.onHold

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-12-30.
 */
class VideoViewVModel(
    application: Application
) : BaseAndroidViewModel(application), Player.EventListener {

    val exoPlayer: SimpleExoPlayer by lazy { preparePlayerAndPlay() }

    val bufferLiveData
            = mutableLiveData(Triple(false, 0, false))

    var urlToLoad: String? = null
    var uriToLoad: Uri? = null
    var lastPosition: Long = 0

    private var isPlaying: Boolean = false
    private var playedOnce = false


    private fun preparePlayerAndPlay(): SimpleExoPlayer {

        val player = ExoPlayerFactory.newSimpleInstance(getApplication<TTUApp>())

        player.repeatMode = Player.REPEAT_MODE_OFF

        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build(),
            true
        )

        player.addListener(this)

        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
            getApplication<TTUApp>(),
            Util.getUserAgent(getApplication<TTUApp>(), getApplication<TTUApp>().getString(R.string.app_name))
        )

        // This is the MediaSource representing the media to be played.
        val videoSource = ExtractorMediaSource.Factory(dataSourceFactory)
            .createMediaSource(if (uriToLoad != null) uriToLoad else Uri.parse(urlToLoad))
        // Prepare the player with the source.
        player.prepare(videoSource)

        return player
    }



    //region == Media Section ===

    fun start() {
        if (lastPosition > 0)
            seekTo(lastPosition.toInt())

        exoPlayer.playWhenReady = true
    }

    fun pause() {
        lastPosition = exoPlayer.currentPosition

        exoPlayer.playWhenReady = false
    }

    fun release() {
        pause()

        exoPlayer.stop()
        exoPlayer.release()
    }

    private fun seekTo(i: Int) {
        exoPlayer.seekTo(i.toLong())
    }

    //endregion


    //region == Player.Listener ==

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}

    override fun onTracksChanged(
        trackGroups: TrackGroupArray?,
        trackSelections: TrackSelectionArray?
    ) {}

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

        when (playbackState) {
            Player.STATE_READY -> {
                isPlaying = playWhenReady
                bufferLiveData.value = Triple(!isPlaying, 0, false)
            }

            Player.STATE_BUFFERING -> {
                bufferLiveData.value = Triple(!true, exoPlayer.bufferedPercentage, playedOnce)
            }

            Player.STATE_ENDED -> {
                seekTo(0)
                playedOnce = true
                pause()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {}

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

    override fun onPlayerError(error: ExoPlaybackException?) {}

    override fun onPositionDiscontinuity(reason: Int) {}

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

    override fun onSeekProcessed() {}

    //endregion


    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(bufferLiveData)
    }
}