package rs.highlande.app.tatatu.feature.account.settings.view.viewModel

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.setPicture

/**
 * Created by Abhin.
 */
@BindingAdapter("settingSrc")
fun setImageResource(image: AppCompatImageView, url: Drawable?) {
    if (url != null) {
        image.background = url
    }
}

@BindingAdapter("settingsText")
fun setSettingsName(textView: AppCompatTextView, text: String?) {
    if (text!!.isNotEmpty()) {
        textView.text = text
    }
}

@BindingAdapter("settingsDesText")
fun setSettingsDesText(textView: AppCompatTextView, text: String?) {
    if (text!!.isNotEmpty()) {
        textView.text = text
    }
}


@SuppressLint("CheckResult")
@BindingAdapter("blockUserImage")
fun setImageUrl(image: AppCompatImageView, url: String?) {
    if (!url.isNullOrBlank()) {
        image.setPicture(url, RequestOptions().circleCrop().placeholder(R.drawable.moment_tab_selector).error(R.drawable.moment_tab_selector).diskCacheStrategy(DiskCacheStrategy.NONE))
    }
}
