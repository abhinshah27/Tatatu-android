package rs.highlande.app.tatatu.core.util

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File


/**
 * Created by Abhin.
 */
class VideoPlayerHelper(var mExoPlayer: PlayerView? = null, var context: Context) {

    var hasVideoBeenPausedByUser = false

    private var mIsVideoPauseByUser: IsVideoPauseByUser? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mDefaultDataSourceFactory: DefaultDataSourceFactory? = null

    var assignedPosition: Long? = null

    //play the video player
    fun videoPlay(videoUrl: Any? = null, img_play: AppCompatImageView?, play: Boolean = true) {
        LogUtils.d(logTag, "VideoUrl-->$videoUrl")

        when (videoUrl) {
            is String -> Uri.parse(videoUrl as String?)
            is File -> Uri.parse(videoUrl.path)
            is Uri -> videoUrl
            else -> null
        }?.let { uri ->
            mExoPlayer?.let {
                if (it.player == null) {
                    mSimpleExoPlayer =
                        ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
                    it.player = mSimpleExoPlayer!!.apply {
                        setAudioAttributes(AudioAttributes.DEFAULT, true)

                        addAnalyticsListener(object : AnalyticsListener {
                            override fun onPlayerStateChanged(
                                eventTime: AnalyticsListener.EventTime?,
                                playWhenReady: Boolean,
                                playbackState: Int
                            ) {
                                super.onPlayerStateChanged(eventTime, playWhenReady, playbackState)

                                when (playbackState) {

                                    Player.STATE_BUFFERING -> {
                                        LogUtils.d(
                                            logTag,
                                            "VIDEO_TEST: buffering = $bufferedPercentage"
                                        )
                                    }

                                    Player.STATE_READY -> {
                                        if (playWhenReady) start(img_play, currentPosition)
                                    }
                                }
                            }
                        })

                    }
                    it.requestFocus()
                    it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    mDefaultDataSourceFactory =
                        DefaultDataSourceFactory(
                            context,
                            Util.getUserAgent(context, context.packageName)
                        )
                }

                img_play?.visibility = View.VISIBLE

                //set the media
                ExtractorMediaSource
                    .Factory(mDefaultDataSourceFactory)
                    .createMediaSource(uri)
                    ?.let { source ->
                        mSimpleExoPlayer?.shuffleModeEnabled = true
                        it.player?.isCurrentWindowDynamic
                        val loopingSource = LoopingMediaSource(source) //run in loop
                        mSimpleExoPlayer?.prepare(loopingSource)
                        mSimpleExoPlayer?.playWhenReady = play
                    }

                mIsVideoPauseByUser?.isVideoPauseByUser(false)
                hasVideoBeenPausedByUser = false
            }
        }
    }


    fun setListener(isVideoPauseByUser: IsVideoPauseByUser) {
        mIsVideoPauseByUser = isVideoPauseByUser
    }


    //ExoPlayer Play
    fun isPlaying(): Boolean {
        return if (mExoPlayer?.player != null) {
            mExoPlayer!!.player.playWhenReady
        } else false
    }

    //ExoPlayer Start
    fun start(imgPlay: AppCompatImageView?, cachedPosition: Long = 0) {
        mExoPlayer?.let{
            it.player?.let { player ->
                imgPlay?.visibility = View.GONE
                player.seekTo(cachedPosition)
                player.playWhenReady = true
            }
        }
    }

    //ExoPlayer Pause
    fun pause(img_play: AppCompatImageView?) {
        mExoPlayer?.let {
            if (it.player != null && isPlaying()) {
                it.player!!.playWhenReady = false
                img_play?.visibility = View.VISIBLE
            }
        }
    }

    fun resume(img_play: AppCompatImageView?) {
        if (!isPlaying()) {
            start(img_play, getCurrentPosition())
            img_play?.visibility = View.GONE
        }
    }

    //ExoPlayer Stop
    fun stop() {
        mExoPlayer?.let {
            if (isPlaying()) it.player?.playWhenReady = false

            it.player = null
            mSimpleExoPlayer?.release()
            mSimpleExoPlayer = null
        }
    }

    //ExoPlayer Current Position
    fun getCurrentPosition(): Long {
        return assignedPosition ?: mExoPlayer?.let {
            if (it.player?.duration == 0L) 0
            else it.player?.currentPosition ?: 0
        } ?: 0
    }

    //ExoPlayer total duration
    fun getDuration(): Long {
        return mExoPlayer?.let {
            return if (it.player?.duration == 0L) 0
            else it.player?.duration ?: 0
        } ?: 0
    }

    fun onClick(img_play: AppCompatImageView) {
        if (isPlaying()) {
            img_play.visibility = View.VISIBLE
            mIsVideoPauseByUser?.isVideoPauseByUser(true)
            hasVideoBeenPausedByUser = true
            pause(img_play)
        } else {
            img_play.visibility = View.GONE
            mIsVideoPauseByUser?.isVideoPauseByUser(false)
            hasVideoBeenPausedByUser = false
            start(img_play, getCurrentPosition())
        }
    }

    interface IsVideoPauseByUser {
        fun isVideoPauseByUser(isVideoPause: Boolean)
    }

    companion object {
        val logTag = VideoPlayerHelper::class.java.simpleName
    }

}