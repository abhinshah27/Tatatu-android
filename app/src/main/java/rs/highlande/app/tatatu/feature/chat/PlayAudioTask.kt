package rs.highlande.app.tatatu.feature.chat

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.view.View
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.isStringValid
import java.lang.ref.WeakReference

fun getAudioTask(
    mediaPlayer: MediaPlayer, playBtn: View, pauseBtn: View, progressView: View,
    playing: Boolean, lastPlayerPosition: Int, /*siriAnimation: LottieAnimationView,*/ pauseFrame: Int
): PlayAudioTask {
    return PlayAudioTask(
        mediaPlayer,
        playBtn,
        pauseBtn,
        progressView,
        playing,
        lastPlayerPosition,
        /*siriAnimation,*/
        pauseFrame
    )
}


class PlayAudioTask internal constructor(
    private val mediaPlayer: MediaPlayer?,
    playBtn: View,
    pauseBtn: View,
    progressView: View,
    private var playing: Boolean?,
    private var lastPlayerPosition: Int?,
    /*siriAnimation: LottieAnimationView,*/
    private val pauseFrame: Int?
) :
    AsyncTask<Any, Void, Boolean>() {
    private val playBtn: WeakReference<View>
    private val pauseBtn: WeakReference<View>
    private val progressView: WeakReference<View>
//    private val siriAnimation: WeakReference<LottieAnimationView>

    init {
        this.playBtn = WeakReference(playBtn)
        this.pauseBtn = WeakReference(pauseBtn)
        this.progressView = WeakReference(progressView)
//        this.siriAnimation = WeakReference(siriAnimation)
    }

    override fun onPreExecute() {
        super.onPreExecute()

        playBtn.get()?.visibility = View.GONE
        progressView.get()?.visibility = View.VISIBLE

//        siriAnimation.get()?.frame = pauseFrame!!

        if (mediaPlayer!!.isPlaying) {
            playBtn.get()?.visibility = View.GONE
        } else {
//            siriAnimation.get()?.cancelAnimation()
        }
    }

    @SuppressLint("WrongThread")
    override fun doInBackground(vararg objects: Any): Boolean? {
        var prepared: Boolean? = null

        if (mediaPlayer != null) {
            try {
                val obj = objects[0]
                if (obj is Uri)
                    playBtn.get()?.context?.apply {
                        mediaPlayer.setDataSource(this, obj)
                    }
                else if (obj is String && isStringValid(obj))
                    mediaPlayer.setDataSource(obj)

                mediaPlayer.setOnCompletionListener { mediaPlayer ->
                    playBtn.get()?.visibility = View.VISIBLE
                    pauseBtn.get()?.visibility = View.GONE

                    mediaPlayer.stop()
                    mediaPlayer.reset()
//                    siriAnimation.get()?.cancelAnimation()
//                    siriAnimation.get()?.frame = 0

                    playing = false
                    lastPlayerPosition = 0
                }

                mediaPlayer.prepare()
                prepared = true

            } catch (e: Exception) {
                LogUtils.e(
                    "MyAudioStreamingApp", if (isStringValid(e.message))
                        e.message
                    else
                        ""
                )
                prepared = false
            }

        }

        return prepared
    }

    override fun onPostExecute(aBoolean: Boolean?) {
        super.onPostExecute(aBoolean)

        progressView.get()?.visibility = View.GONE

        if (mediaPlayer != null) {
            if (lastPlayerPosition != null)
                mediaPlayer.seekTo(lastPlayerPosition!!)
//            if (pauseFrame != null) {
//                siriAnimation.get()?.frame = pauseFrame
//                siriAnimation.get()?.resumeAnimation()
//            } else
//                siriAnimation.get()?.playAnimation()

            mediaPlayer.start()
            pauseBtn.get()?.visibility = View.VISIBLE
        }

        playing = true
    }
}
