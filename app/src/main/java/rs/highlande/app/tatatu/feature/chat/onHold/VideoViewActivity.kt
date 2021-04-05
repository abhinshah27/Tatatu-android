/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.onHold

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ui.PlayerView
import com.vincent.videocompressor.LogUtils
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.HLMediaType

/**
 * @author mbaldrighi on 12/30/2019.
 */
class VideoViewActivity : BaseActivity(), ShareHelper.ShareableProvider {

    private val vModel: VideoViewVModel by inject()

    private val videoCache: AudioVideoCache by inject()

    private var thumbnail: String? = null
    private var transitionName: String? = null
    private var postOrMessageId: String? = null
    private var fromChat: Boolean = false

    private var mThumbnailView: ImageView? = null

    private var progressView: View? = null
    private var mThumbnailLayout: View? = null
    private var mThumbnailBackground: View? = null
    private var progressMessage: TextView? = null

    private var exoView: PlayerView? = null

    private var wantsLandscape: Boolean = false

    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val noisyAudioStreamReceiver = BecomingNoisyReceiver()

    private var closeBtn: View? = null
    private var safeCutout = 0

    private var mShareHelper: ShareHelper? = null

    val duration: Long
        get() = vModel.exoPlayer.duration

    val currentPosition: Long
        get() = vModel.exoPlayer.currentPosition


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_view_exo_constr)

        val decorView = window.decorView
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher
        val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        decorView.systemUiVisibility = uiOptions
        isImmersiveActivity = true

        if (hasPie()) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            findViewById<View>(R.id.root_content).setOnApplyWindowInsetsListener { v, insets ->
                if (insets.displayCutout == null)
                    params.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER

                this.safeCutout = insets.systemWindowInsetTop
                manageCutout()

                insets.consumeDisplayCutout()
            }
        }

        manageIntent()

        mThumbnailView = findViewById(R.id.video_view_thumbnail)
        mThumbnailBackground = findViewById(R.id.placeholder_background)
        mThumbnailLayout = findViewById(R.id.video_view_thumbnail_layout)
        mThumbnailLayout!!.visibility = if (vModel.lastPosition > 0) View.GONE else View.VISIBLE
        object: CustomViewTarget<ImageView, Drawable>(mThumbnailView!!) {
            override fun onResourceCleared(placeholder: Drawable?) {}

            override fun onLoadFailed(errorDrawable: Drawable?) {
                mThumbnailBackground?.setBackgroundColor(
                    getColor(resources, R.color.divider_on_white)
                )
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                mThumbnailBackground?.setBackgroundColor(Color.BLACK)
                getView().context?.let {
                    if (it.isValid())
                        getView().setImageDrawable(resource)
                }
            }
        }.setPicture(thumbnail, RequestOptions.fitCenterTransform())

        progressView = findViewById(R.id.progress)
        progressMessage = findViewById(R.id.progress_message)

        exoView = findViewById(R.id.video_view)
        if (hasLollipop())
            exoView!!.transitionName = transitionName

        exoView!!.player = vModel.exoPlayer
        vModel.start()

        closeBtn = findViewById(R.id.close_btn)
        closeBtn!!.setOnClickListener { v -> onBackPressed() }
        findViewById<View>(R.id.share_btn).setOnClickListener { v -> mShareHelper!!.initOps(fromChat) }

        mShareHelper =
            ShareHelper(
                this,
                this
            )

        vModel.bufferLiveData.observe(this, Observer {

            if (!it.first) {
                // video is not buffering
                progressView!!.visibility = View.GONE
                mThumbnailLayout!!.visibility = View.GONE
            } else {
                // video is buffering
                progressMessage!!.text =
                    progressMessage!!.context.getString(
                        R.string.buffering_video_perc,
                        it.second.toString()
                    )

                if (it.second == 100)
                    progressView!!.visibility = View.GONE
                else if (!it.third)
                    progressView!!.visibility = View.VISIBLE
            }

        })

    }

    override fun onResume() {
        super.onResume()

        //AnalyticsUtils.trackScreen(this, AnalyticsUtils.FEED_VIDEO_VIEWER)

        mShareHelper!!.onResume()

        manageCutout()

//        if (vModel.exoPlayer == null) {
//            exoPlayer = preparePlayerAndPlay()
//            exoView!!.player = exoPlayer
//            start()
//        }
    }

    override fun onPause() {
        vModel.release()

        super.onPause()
    }

    override fun onStop() {

        try {
            unregisterReceiver(noisyAudioStreamReceiver)
        } catch (e: IllegalArgumentException) {
            LogUtils.e(LOG_TAG, e.message, e)
        }

        mShareHelper!!.onStop()

        super.onStop()
    }

    override fun onBackPressed() {
        val `in` = Intent()
        `in`.putExtra(EXTRA_PARAM_1, currentPosition)
        setResult(Activity.RESULT_OK, `in`)
        finish()
    }

    override fun manageIntent() {
        val intent = intent
        if (intent != null) {


            if (intent.hasExtra(EXTRA_PARAM_1))
                vModel.urlToLoad = intent.getStringExtra(EXTRA_PARAM_1)
            if (intent.hasExtra(EXTRA_PARAM_2))
                thumbnail = intent.getStringExtra(EXTRA_PARAM_2)
            if (intent.hasExtra(EXTRA_PARAM_3))
                transitionName = intent.getStringExtra(EXTRA_PARAM_3)
            if (intent.hasExtra(EXTRA_PARAM_4))
                vModel.lastPosition = intent.getLongExtra(EXTRA_PARAM_4, 0)
            if (intent.hasExtra(EXTRA_PARAM_5))
                wantsLandscape = intent.getBooleanExtra(EXTRA_PARAM_5, false)
            if (intent.hasExtra(EXTRA_PARAM_6))
                postOrMessageId = intent.getStringExtra(EXTRA_PARAM_6)
            if (intent.hasExtra(EXTRA_PARAM_7))
                fromChat = intent.getBooleanExtra(EXTRA_PARAM_7, false)

            val obj = videoCache.getMedia(vModel.urlToLoad, HLMediaType.VIDEO)
            if (obj is Uri)
                vModel.uriToLoad = obj
        }
    }



    override fun configureLayout() {}
    override fun bindLayout() {}



    private fun manageCutout() {
        val padding = resources.getDimensionPixelSize(R.dimen.activity_margin)
        val lp1 = closeBtn!!.layoutParams as ConstraintLayout.LayoutParams
        lp1.topMargin = safeCutout
        closeBtn!!.layoutParams = lp1
        closeBtn!!.setPaddingRelative(padding, padding, padding, padding)

        // shareBn is automatically positioned because of its constraints (linked to closeBtn)

    }



    private inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action || hasLollipop() && AudioManager.ACTION_HEADSET_PLUG == intent.action) {

                vModel.exoPlayer.playWhenReady = false
            }
        }
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            registerReceiver(noisyAudioStreamReceiver, intentFilter)
            return true
        } else if (event.keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            try {
                unregisterReceiver(noisyAudioStreamReceiver)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }


    //region == SHARE ==

    override fun getProgressView(): View? {
        return null
    }

    override fun afterOps() {}

    override fun getUserID() = getUser()?.uid

    override fun getPostOrMessageID() = postOrMessageId

    companion object {

        val LOG_TAG = VideoViewActivity::class.java.canonicalName
    }

    //endregion

}
