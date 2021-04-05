package rs.highlande.app.tatatu.feature.post.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.PostDetailShareBottomSheetBinding
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetListener
import rs.highlande.app.tatatu.model.Post


class PostShareBottomSheetFragment: BasePostSheetFragment() {

    companion object {
        fun newInstance(post: Post, listener: BasePostSheetListener): PostShareBottomSheetFragment {
            val fragment = PostShareBottomSheetFragment()
            fragment.post = post
            fragment.listener = listener
            return fragment
        }

    }

    lateinit var binding: PostDetailShareBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.bind(view!!)!!

        return view
    }

    override fun getLayoutId() = R.layout.post_detail_share_bottom_sheet
}
