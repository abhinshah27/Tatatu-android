/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.view.MotionEvent
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.BaseHelper
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.deleteMediaFile
import rs.highlande.app.tatatu.core.util.isStringValid
import java.io.IOException


/**
 * @author mbaldrighi on 11/13/2018.
 */
class AudioRecordingHelper(context: Context, private val recordingListener: RecordingCallbacks): BaseHelper(context), TapAndHoldGestureListener.OnHoldListener {

    companion object {
        val LOG_TAG = AudioRecordingHelper::class.java.simpleName
    }

    private var recorder: MediaRecorder? = null

    private var recording: Boolean = false

    var mediaFileUri: String? = null


    override fun onHoldActivated() {
        onRecord(true)
    }

    override fun onHoldReleased() {
        recording = false
        onRecord(false)
    }

    override fun onSlideEvent(motionEvent: MotionEvent, movingLeft: Boolean) {
        recordingListener.onSlideToCancel(motionEvent, movingLeft)
    }


    //region - Recorder/Player methods -

    private fun onRecord(start: Boolean) {
        if (start) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        if (isStringValid(mediaFileUri)) {

            if (recorder ==  null) recorder = MediaRecorder()

            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder?.setOutputFile(Uri.parse(mediaFileUri).path)

            try {
                recorder?.prepare()
                recorder?.start()

                recordingListener.onStartRecording()
            }
            catch (e: IOException) {
                LogUtils.e(LOG_TAG, "prepare() failed with message: " + e.message, e)
            }

            return
        }

        if (contextRef.get() is BaseActivity)
            (contextRef.get() as BaseActivity).apply {
                showError(getString(R.string.error_generic))
            }
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null

            recordingListener.onStopRecording(mediaFileUri)
        } catch (e: Exception) {
            e.printStackTrace()
            (contextRef.get() as? BaseActivity)?.showMessage(R.string.error_recording_audio)
            recordingListener.onStopRecording(mediaFileUri, true)

            recorder?.release(); recorder = null

            deleteMediaFile(mediaFileUri, true)
            mediaFileUri = null
        }
    }

    //endregion


    interface RecordingCallbacks {
        fun onStartRecording()
        fun onStopRecording(mediaFileUri: String?, exceptionCaught: Boolean = false)
        fun onSlideToCancel(motionEvent: MotionEvent, movingLeft: Boolean)
    }

}