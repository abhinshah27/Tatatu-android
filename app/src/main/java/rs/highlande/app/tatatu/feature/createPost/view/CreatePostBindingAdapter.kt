package rs.highlande.app.tatatu.feature.createPost.view

import android.annotation.SuppressLint
import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.core.util.setProfilePicture

/**
 * Created by Abhin.
 */

@SuppressLint("CheckResult")
@BindingAdapter("createPostImagePath")
fun setImageUrl(image: AppCompatImageView, url: String?) {
    if (!url.isNullOrBlank()) {
        image.setPicture(url, RequestOptions().placeholder(R.color.white).error(R.color.white).diskCacheStrategy(DiskCacheStrategy.NONE))
    }
}

@BindingAdapter("videoDuration")
fun setVideoDuration(textView: AppCompatTextView, duration: String?) {
    if (!duration.isNullOrBlank()) {
        textView.getParentActivity()
        textView.text = duration
    }
}

@BindingAdapter("contactImagePath")
fun setContactImageUrl(image: AppCompatImageView, url: String?) {
    if (!url.isNullOrBlank()) {
        image.setProfilePicture(url)
    }
}


@BindingAdapter("contactName")
fun setContactName(textView: AppCompatTextView, name: String?) {
    if (!name.isNullOrBlank()) {
        textView.getParentActivity()
        textView.text = name
    }
}

fun View.getParentActivity(): AppCompatActivity? {
    var context = this.context
    while (context is ContextWrapper) {
        if (context is AppCompatActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}



