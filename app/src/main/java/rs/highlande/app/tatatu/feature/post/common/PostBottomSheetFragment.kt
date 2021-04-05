package rs.highlande.app.tatatu.feature.post.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.PostBottomSheetBinding
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetListener
import rs.highlande.app.tatatu.model.Post


class PostBottomSheetFragment: BasePostSheetFragment() {

    private var canUnfollow = false

    companion object {

        const val BUNDLE_KEY_CAN_UNFOLLOW = "can_unfollow"

        fun newInstance(post: Post, canUnfollow: Boolean, listener: BasePostSheetListener): PostBottomSheetFragment {
            val fragment = PostBottomSheetFragment()
            fragment.post = post
            fragment.listener = listener
            return fragment.apply { arguments = Bundle().apply { putBoolean(BUNDLE_KEY_CAN_UNFOLLOW, canUnfollow) } }
        }
    }

    lateinit var binding: PostBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        canUnfollow = arguments?.getBoolean(BUNDLE_KEY_CAN_UNFOLLOW) ?: false

        binding = DataBindingUtil.bind(view!!)!!
        binding.bottomSheetInapropiate.visibility = if (post.isNews()) View.GONE else View.VISIBLE
        binding.bottomSheetUnfollow.visibility = if (canUnfollow) View.VISIBLE else View.GONE

        return view
    }

    override fun getLayoutId() = R.layout.post_bottom_sheet

}