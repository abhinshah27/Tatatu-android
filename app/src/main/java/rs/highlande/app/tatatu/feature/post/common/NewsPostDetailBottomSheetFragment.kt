package rs.highlande.app.tatatu.feature.post.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.NewsPostDetailBottomSheetBinding
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetListener
import rs.highlande.app.tatatu.model.Post


class NewsPostDetailBottomSheetFragment: BasePostSheetFragment() {

    companion object {
        fun newInstance(post: Post, listener: BasePostSheetListener): NewsPostDetailBottomSheetFragment {
            val fragment = NewsPostDetailBottomSheetFragment()
            fragment.post = post
            fragment.listener = listener
            return fragment
        }

    }

    lateinit var binding: NewsPostDetailBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.bind(view!!)!!

        return view
    }


    override fun getLayoutId() = R.layout.news_post_detail_bottom_sheet
}
