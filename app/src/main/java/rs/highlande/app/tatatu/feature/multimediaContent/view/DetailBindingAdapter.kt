package rs.highlande.app.tatatu.feature.multimediaContent.view

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.databinding.BindingAdapter
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.setBlurredPicture
import rs.highlande.app.tatatu.core.util.setPicture
import kotlin.math.abs

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-19.
 */


@BindingAdapter("readableDuration")
fun convertToReadableMinutes(tv: TextView, duration: Long?) {
    LogUtils.e("TEST", "" + duration)

    if (duration != null && duration > 0) {
        abs(duration / 1000).let {
            if (it >= 60) {
                abs(it / 60).let { minDuration ->
                    if (it > 1) {
                        tv.text = tv.context.getString(R.string.video_duration_prural, minDuration.toString())
                    } else {
                        tv.text = tv.context.getString(R.string.video_duration, minDuration.toString())
                    }
                }
            } else {
                if (it > 1) {
                    tv.text = tv.context.getString(R.string.video_duration_second_prural, abs(it).toString())  // TODO :
                } else {
                    tv.text = tv.context.getString(R.string.video_duration_second, abs(it).toString())
                }
            }
        }
    }
}

@BindingAdapter("videoPoster")
fun setPoster(iv: ImageView, poster: String?) {
    if (!poster.isNullOrBlank()) {
        iv.setPicture(poster)
    }
}

@BindingAdapter("blurredPoster")
fun setBlurredPoster(iv: ImageView, poster: String?) {
    if (!poster.isNullOrBlank()) {
        iv.setBlurredPicture(poster)
    }
}

@BindingAdapter("videoDetails")
fun setVideoDetails(tv: TextView, details: String) {
    tv.text = details

    //    if (details.isNotBlank()) {
    //        val builder = StringBuilder()
    //        for (detail in 0 until (details.size - 1)) {
    //            builder.append(detail).append(" | ")
    //        }
    //        builder.append(details[details.size - 1]).let {
    //            tv.text = it.toString()
    //        }
    //    }
}

@BindingAdapter("hideDirector")
fun handleDirector(group: Group, director: String?) {
    group.visibility = if (director.isNullOrBlank()) View.GONE else View.VISIBLE
}

// INFO: 2019-07-22    currently no writers in video model
@BindingAdapter("hideWriters")
fun handleWriters(group: Group, writers: String?) {
    group.visibility = /*if (writers.isNullOrBlank()) */View.GONE/* else View.VISIBLE*/
}

@BindingAdapter("hideStars")
fun handleStars(group: Group, stars: String?) {
    group.visibility = if (stars.isNullOrBlank()) View.GONE else View.VISIBLE
}