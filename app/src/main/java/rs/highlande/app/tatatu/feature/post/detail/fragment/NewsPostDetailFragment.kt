package rs.highlande.app.tatatu.feature.post.detail.fragment

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewFragment
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewModel
import rs.highlande.app.tatatu.model.Post

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class NewsPostDetailFragment : PostDetailFragment() {

    companion object {
        fun newInstance() = NewsPostDetailFragment()
    }

    private val webViewModel: WebViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postDetailBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_post_detail, container, false)
        postDetailBinding.root.apply {
            configureLayout(this)
            return this
        }
    }

    private fun openWebView(url: String) {
        if (url.isNotBlank()) {
            webViewModel.mWebUrl = url
            webViewModel.mToolbarName = url
            addReplaceFragment(
                R.id.container,
                WebViewFragment.newInstance(WebViewFragment.WVType.NEWS),
                false,
                true,
                NavigationAnimationHolder()
            )
        }
    }


    override fun getGestureDetector(post: Post): GestureDetector {
        return GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                openWebView(post.link ?: "")
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                postDetailViewModel.handleLikeUnlikePost(post)
                return true
            }
        })
    }
}