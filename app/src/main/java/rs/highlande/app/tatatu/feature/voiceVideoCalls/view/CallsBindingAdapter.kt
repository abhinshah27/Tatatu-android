package rs.highlande.app.tatatu.feature.voiceVideoCalls.view

import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceVideoCallType

/**
 * TODO - File description
 * @author mbaldrighi on 2019-12-13.
 */


@BindingAdapter(value = ["isClick","isCameraClick", "callType"], requireAll = true)
fun speakerCamera(imageView: ImageView, isClick: Boolean, isCameraClick: Boolean, callType: VoiceVideoCallType) {
    when (callType) {
        VoiceVideoCallType.VOICE -> {
            when (isClick) {
                true -> imageView.setImageResource(R.drawable.ic_speaker_active)
                false -> imageView.setImageResource(R.drawable.ic_speaker_icon)
            }
        }
        VoiceVideoCallType.VIDEO -> {
            when (isCameraClick) {
                false -> imageView.setImageResource(R.drawable.ic_flip_camera_icon)
                true -> imageView.setImageResource(R.drawable.ic_flip_camera_icon_active)
            }
        }
    }
}

@BindingAdapter(value = ["isClickVideo", "callTypeVideo"], requireAll = true)
fun videoCamera(imageView: ImageView, isClick: Boolean?, callType: VoiceVideoCallType) {
    when (callType) {
        VoiceVideoCallType.VOICE -> {
            imageView.setImageResource(R.drawable.ic_video_call_icon_rested)
        }
        VoiceVideoCallType.VIDEO -> {
            when (isClick) {
                false -> imageView.setImageResource(R.drawable.ic_video_call_mute_disable)
                true -> imageView.setImageResource(R.drawable.ic_video_call_mute_disable_active)
            }
        }
    }
}


@BindingAdapter("profileAvatar")
fun profileAvatar(imageView: ImageView, url: String? = null) {
    if (!url.isNullOrEmpty()) {
        imageView.setProfilePicture(url)
    }
}

@BindingAdapter(value = ["appChronometer", "appChronometerTime"], requireAll = true)
fun appChronometer(chronometer: Chronometer, isVisible: Int? = View.GONE, isTime: String? = null) {
    if (isVisible != null && isVisible == View.VISIBLE) {
        if (isTime.isNullOrEmpty() || isTime.equals("null", true)) {
            chronometer.base = SystemClock.elapsedRealtime()
        } else {
            chronometer.base = isTime.toLong()
        }
        chronometer.start()
    } else {
        chronometer.stop()
    }
}