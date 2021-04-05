package rs.highlande.app.tatatu.feature.multimediaContent.view

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.ViewGroup
import com.brightcove.player.edge.Catalog
import com.brightcove.player.edge.VideoListener
import com.brightcove.player.event.EventType
import com.brightcove.player.mediacontroller.BrightcoveMediaController
import com.brightcove.player.model.Video
import com.brightcove.player.network.HttpRequestConfig
import com.brightcove.player.view.BrightcovePlayer
import com.brightcove.ssai.SSAIComponent
import kotlinx.android.synthetic.main.activity_video_player.*
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ACCOUNT_ID
import rs.highlande.app.tatatu.connection.http.AD_CONFIG_ID
import rs.highlande.app.tatatu.connection.http.PARAM_AD_CONFIG_ID
import rs.highlande.app.tatatu.connection.http.POLICY_KEY
import rs.highlande.app.tatatu.core.ui.BaseImmersiveActivityImpl
import rs.highlande.app.tatatu.core.ui.ImmersiveImplHandler
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.model.TTUVideo


/**
 * Implements the basic player activity for streaming content.
 * @author mbaldrighi on 2019-07-22.
 */
class VideoPlayerActivity :
    BrightcovePlayer(),
    Handler.Callback,
    BaseImmersiveActivityImpl by ImmersiveImplHandler(){

    //TODO 19/08: For some reason by viewModel wont work. See why
    private val viewModel: PlaylistDetailViewModel by inject()

    private var videoId: String = ""

    private var currentPosition: Int = 0
    private var firstRun: Boolean = false


    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_video_player)
        super.onCreate(savedInstanceState)

        setDecorView(window.decorView)

        val mediaController = BrightcoveMediaController(brightcoveVideoView, R.layout.custom_brightcove_media_controller)
        brightcoveVideoView.setMediaController(mediaController)

        brightcoveVideoView.setOnTouchListener { _, event ->
            videoClose.visibility = if (mediaController.isShowing) View.GONE else View.VISIBLE
            false
        }

        // automatically sends video to fullscreen
        fullScreen()


        manageIntent()

        savedInstanceState?.let {
            videoId = it.getString(BUNDLE_KEY_VIDEO_ID, "")
            currentPosition = it.getInt(BUNDLE_KEY_VIDEO_POSITION)
        }

        viewModel.currentTTUVideo = TTUVideo().apply {
            id = videoId
        }

        val eventEmitter = player.eventEmitter

        player.onControllerHide()

        eventEmitter.on(EventType.SHOW_PLAYER_OPTIONS) { LogUtils.d("seek controls", "showing") }
        eventEmitter.on(EventType.HIDE_PLAYER_OPTIONS) { LogUtils.d("seek controls", "showing") }
        eventEmitter.on(EventType.PAUSE) { stopVideoUpdates() }
        eventEmitter.on(EventType.PLAY) { resumeVideoUpdates() }
        eventEmitter.on(EventType.DID_PLAY) {
            if (firstRun) {
                startVideoUpdates()
                //TODO 22/08: Find a better way to handle this. Implement custom mediacontroller?
                val videoCloseRunnable = Runnable { videoClose.visibility = View.GONE }
                handler.postDelayed(videoCloseRunnable, 3000)
            }
        }
        eventEmitter.on(EventType.DID_SET_VIDEO) {
            if (currentPosition > 0)
                player.seekTo(currentPosition)

            firstRun = true
            player.start()
        }

        val catalog = Catalog(eventEmitter, ACCOUNT_ID, POLICY_KEY)
        val adsPlugin = SSAIComponent(this, player)

        (ad_frame as? ViewGroup)?.let{
            adsPlugin.addCompanionContainer(ad_frame)
        }

        val httpRequestBody =  HttpRequestConfig.Builder()
            .addQueryParameter(PARAM_AD_CONFIG_ID, AD_CONFIG_ID)
            .build()

        catalog.findVideoByID(videoId, httpRequestBody, object : VideoListener() {

            // Add the video found to the queue with add().
            // Start playback of the video with start().
            override fun onVideo(video: Video) {
                viewModel.currentTTUVideo?.let {
                    it.duration = video.duration.toLong()
                }

                adsPlugin.processVideo(video)
            }

            override fun onError(s: String) {
                LogUtils.e(logTag, s)
            }
        })

        videoClose.setOnClickListener {
            onBackPressed()
        }


    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        handleImmersiveOnFocusChange(hasFocus)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(BUNDLE_KEY_VIDEO_ID, videoId)
        outState.putInt(BUNDLE_KEY_VIDEO_POSITION, player.currentPosition)
    }


    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }


    override fun onResume() {
        super.onResume()
        resumeVideoUpdates()
    }

    override fun onPause() {
        stopVideoUpdates()
        super.onPause()
    }

    private fun manageIntent() {
        videoId = intent?.getStringExtra(BUNDLE_KEY_VIDEO_ID) ?: ""
    }


    override fun handleMessage(p0: Message): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        val logTag = VideoPlayerActivity::class.java.simpleName

        const val EARNING_CALL_INTERVAL = 60000L

        const val BUNDLE_KEY_VIDEO_ID = "video_id"
        const val BUNDLE_KEY_VIDEO_POSITION = "position"
    }

    fun sendVideoEvent() {
        viewModel.sendPlayerEarnings()
    }

    private fun resumeVideoUpdates() {
        if (!firstRun && !player.isPlaying) {
            LogUtils.d(logTag, "resumed playing")
            handler.removeCallbacks(runnable)
            handler.post(runnable)
        }
    }

    private fun startVideoUpdates() {
        LogUtils.d(logTag, "start playing")
        firstRun = false
        handler.removeCallbacks(runnable)
        handler.post(runnable)
    }

    private fun stopVideoUpdates() {
        LogUtils.d(logTag, "stop playing")
        handler.removeCallbacks(runnable)
    }

    private val runnable = object : Runnable {
        override fun run() {
            if (player.isPlaying) {
                sendVideoEvent()
            }
            handler.postDelayed(this, EARNING_CALL_INTERVAL)
        }
    }

}