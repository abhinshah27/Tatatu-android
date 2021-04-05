package rs.highlande.app.tatatu.feature.post.detail.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import kotlinx.android.synthetic.main.post_detail.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_EDIT_POST
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_POSITION
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostActivity
import rs.highlande.app.tatatu.feature.post.PostActivity
import rs.highlande.app.tatatu.feature.post.common.MyPostBottomSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetListener
import rs.highlande.app.tatatu.model.Post

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class MyPostDetailFragment : PostDetailFragment() {

    companion object {
        fun newInstance() = MyPostDetailFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postDetailBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_post_detail, container, false)
        postDetailBinding.root.apply {
            configureLayout(this)
            return this
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userAvatarNewMomentImageView.setOnClickListener {
            if (getUser()?.isValid() == true) AccountActivity.openProfileFragment(context!!, getUser()!!.uid)
        }
    }

    override fun configureToolbar() {
        (activity as? PostActivity)?.apply {
            setSupportActionBar(postDetailBinding.toolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
            supportActionBar!!.setDisplayShowHomeEnabled(false)
            setHasOptionsMenu(true)

            postDetailBinding.toolbar.title.setText(R.string.tb_my_post)
            postDetailBinding.toolbar.backArrow.setOnClickListener {
                activity!!.onBackPressed()
            }
        }
    }

    override fun subscribeToLiveData() {
        super.subscribeToLiveData()
        postDetailViewModel.currentPostLiveData.observe(viewLifecycleOwner, Observer {

            selectedPost = it

            // INFO: 2019-08-12    INSIGHTS not present for the moment
            postDetailBinding.postDetail.postTTUBalanceGroup.visibility = View.GONE

        })
        postDetailViewModel.postDeletedLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                activity!!.onBackPressed()
            }
        })
    }

    private val bottomSheetListenerOwn = object : BasePostSheetListener {
        override fun onBottomSheetReady(bottomSheet: BasePostSheetFragment) {
            if (bottomSheet is MyPostBottomSheetFragment) {
                bottomSheet.binding.apply {
                    bottomSheetDelete.setOnClickListener {
                        postDetailViewModel.deletePost()
                    }
                    bottomSheetEdit.setOnClickListener {
                        openEditPost(bottomSheet.post)
                        bottomSheet.dismiss()
                    }
                    bottomSheetShare.setOnClickListener {
                        postDetailViewModel.postDeeplinkLiveData.observe(viewLifecycleOwner, Observer {
                            hideLoader()
                            if (!it.first.isNullOrBlank() && !it.second.isNullOrBlank())
                                handleShareResult(it.first as String, it.second as String)
                            postDetailViewModel.postDeeplinkLiveData.removeObservers(viewLifecycleOwner)
                        })
                        showLoader(R.string.progress_share)
                        postDetailViewModel.share()
                        bottomSheet.dismiss()
                    }
                }
            }
        }
    }

    //open the edit post
    private fun openEditPost(post: Post) {
        val bundle = Bundle().apply {
            putSerializable(BUNDLE_KEY_EDIT_POST, post)
            putInt(BUNDLE_KEY_POSITION, -1)
        }
        if (getUser()?.isValid() == true) CreatePostActivity.openEditPostFragment(context!!, getUser()!!.uid, bundle)
    }

    override fun getBottomSheet(): MyPostBottomSheetFragment? {
        return selectedPost?.let {
            MyPostBottomSheetFragment.newInstance(it, bottomSheetListenerOwn)
        } ?: run {
            null
        }
    }
}