package rs.highlande.app.tatatu.feature.post.timeline.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecyclerListener
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.FragmentPostsTimelineBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostActivity
import rs.highlande.app.tatatu.feature.post.common.MyPostBottomSheetFragment
import rs.highlande.app.tatatu.feature.post.common.PostBottomSheetFragment
import rs.highlande.app.tatatu.feature.post.detail.fragment.MyPostDetailFragment
import rs.highlande.app.tatatu.feature.post.detail.fragment.NewsPostDetailFragment
import rs.highlande.app.tatatu.feature.post.detail.fragment.PostDetailFragment
import rs.highlande.app.tatatu.feature.post.like.fragment.PostLikeFragment
import rs.highlande.app.tatatu.feature.post.timeline.adapter.PostTimelineAdapter
import rs.highlande.app.tatatu.feature.post.timeline.adapter.PostTimelineViewHolder
import rs.highlande.app.tatatu.feature.post.timeline.viewmodel.PostTimelineViewModel
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetListener
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostDiffCallback
import rs.highlande.app.tatatu.model.Relationship
import rs.highlande.app.tatatu.model.event.PostChangeEvent

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostTimelineFragment : BaseFragment() {

    private lateinit var postsTimelineBinding: FragmentPostsTimelineBinding
    private lateinit var postTimelineAdapter: PostTimelineAdapter
    private val postTimelineViewModel: PostTimelineViewModel by viewModel()

    private var likedViewHolders = mutableMapOf<String, PostTimelineViewHolder>()

    private var frgBeenStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // adapter creation moved here to preserve a memory persistence after REPLACING the fragment
        postTimelineAdapter = PostTimelineAdapter(PostDiffCallback(), PostTimelineListener()).apply {
            playingVideos = postTimelineViewModel.playingVideos
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postsTimelineBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_posts_timeline, container, false)
        postsTimelineBinding.root.apply {
            configureLayout(this)
            return this
        }

    }

    override fun configureLayout(view: View) {
        arguments?.let {
            it.getString(BUNDLE_KEY_USER_ID)?.let { userID ->
                postTimelineViewModel.userID = userID
            }
            it.getString(BUNDLE_KEY_TIMELINE_TYPE)?.let {
                try {
                    postTimelineViewModel.timelineType = CommonApi.TimelineType.valueOf(it)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }

        postsTimelineBinding.postsList.apply {
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            addItemDecoration(DividerItemDecoration(context, (layoutManager as LinearLayoutManager).orientation))
            addOnScrollListener(postTimelineViewModel.scrollListener)
            addOnScrollListener(postTimelineAdapter.videoVisibilityScrollListener)
            adapter = postTimelineAdapter
        }
        postsTimelineBinding.toolbar.title.setText(R.string.tb_all_posts)
        postsTimelineBinding.toolbar.backArrow.setOnClickListener {
            activity!!.onBackPressed()
        }
        postTimelineViewModel.getTimeline()
        postsTimelineBinding.srl.setOnRefreshListener { postTimelineViewModel.refreshTimeline() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        subscribeToLiveData()
    }

    override fun bindLayout() {}

    fun subscribeToLiveData() {
        postTimelineViewModel.postTimelineLiveData.observe(viewLifecycleOwner, Observer {

            if (postsTimelineBinding.srl.isRefreshing)
                postsTimelineBinding.srl.isRefreshing = false

            LogUtils.d(logTag, "testTIMELINE: Action=${it.second}")
            LogUtils.d(logTag, "testTIMELINE: adapter=$postTimelineAdapter and sizeBEFORE=${postTimelineAdapter.itemCount}")

            when (it.second) {
                PostTimelineViewModel.Action.LOAD_MORE -> {
                    if (frgBeenStopped) postTimelineAdapter.updateRange(it.first)
                    else postTimelineAdapter.addAll(it.first)
                }
                PostTimelineViewModel.Action.REFRESH -> postTimelineAdapter.submitList(it.first)
                PostTimelineViewModel.Action.NONE -> {
                    postTimelineAdapter.updateRange(it.first)
//                    if (postTimelineAdapter.itemCount == 0) postTimelineAdapter.addAll(it.first)
//                    else postTimelineAdapter.updateRange(it.first)
                }
            }

            // reset property
            frgBeenStopped = false

            LogUtils.d(logTag, "testTIMELINE: currentPostId=${postTimelineViewModel.currentPostId}")

            if (!postTimelineViewModel.currentPostId.isBlank()) {
                val position = postTimelineAdapter.findItemPositionById(postTimelineViewModel.currentPostId)
                if (position >= 0) {
                    postsTimelineBinding.postsList.scrollToPosition(position)
                }
                postTimelineViewModel.currentPostId = ""
            } else {
                arguments?.let { bundle ->
                    val postId = bundle.getString(BUNDLE_KEY_POST_ID, "")
                    if (!postId.isNullOrEmpty()) {
                        val position = postTimelineAdapter.findItemPositionById(postId)
                        if (position >= 0) {
                            postsTimelineBinding.postsList.scrollToPosition(position)
                        }
                        bundle.remove(BUNDLE_KEY_POST_ID)
                    }
                }
            }
        })
        postTimelineViewModel.postLikeLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                postTimelineAdapter.apply {
                    likedViewHolders.remove(it.second.uid)?.let { holder ->
                        with(holder.itemBinding.postDetail) {
                            LogUtils.d(logTag, "TESTLIKEVIEWHOLDER: updating from LD: with binding: $this and post: ${it.second}")
                            postLikesImageView.isEnabled = it.second.liked
                            postLikesCountTextView.text = it.second.likes.toString()

                        }
                    }
                }
            }
        })

        // TODO: 2019-08-12    currently not used
        postTimelineViewModel.postLikeConnected.observe(viewLifecycleOwner, Observer {
            if (it.first) {
//                val index = findItemPositionById(it.second.uid)
//                notifyItemChanged(index)
            }
        })

        postTimelineViewModel.hasPostsToRemoveLiveData.observe(viewLifecycleOwner, Observer {
            it.forEach { post ->
                postTimelineAdapter.remove(post)
            }
        })
        postTimelineViewModel.postDeletedLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                it.second?.let { post ->
                    postTimelineAdapter.remove(post)
                }
            } else {
                showError(getString(R.string.error_generic))
            }
        })

        postTimelineViewModel.postReportedLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                showMessage(getString(R.string.result_action_post_report))
                postTimelineViewModel.postReportedLiveData.value = false
            }
        })

        postTimelineViewModel.postUnFollowedLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                postTimelineViewModel.refreshTimeline()
            } else {
                showError(getString(R.string.error_generic))
            }
        })

        postTimelineViewModel.postAuthorUpdated.observe(viewLifecycleOwner, Observer {

            LogUtils.d("TEST_BSHEET", "Received in View --> $it")

            if (it.first) {
                if (it.second != null && it.third != null) {

                    val canUnfollow = it.second?.let { user ->

                        user.detailsInfo.relationship == Relationship.FOLLOWING || user.detailsInfo.relationship == Relationship.FRIENDS

                    } ?: false

                    val frg = this@PostTimelineFragment.childFragmentManager.findFragmentByTag("bottomSheet")

                    LogUtils.d("TEST_BSHEET", "Fragment with tag \"bottomSheet\" --> $frg")

                    if (frg == null) {
                        val bSheet = PostBottomSheetFragment
                            .newInstance(it.third as Post, canUnfollow, bottomSheetListener)

                        LogUtils.d("TEST_BSHEET", "Bottom sheet opening --> $bSheet")

                        bSheet.show(this@PostTimelineFragment.childFragmentManager, "bottomSheet")
                    }
                    postTimelineViewModel.resetBottomSheetRequest()

                } else {
                    showError(getString(R.string.error_generic))
                }
            }
        })
    }

    private fun checkPostList() {
        postTimelineViewModel.fetchPostsToRemove()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        EventBus.getDefault().register(this)
        checkPostList()
    }

    override fun onPause() {

        postTimelineAdapter.apply {
            cachePositions()
            postTimelineViewModel.playingVideos = playingVideos
            stopVideos(false)
        }

        super.onPause()
    }

    override fun onStop() {
        frgBeenStopped = true

        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onDestroyView() {
        postTimelineAdapter.stopVideos(true)

        super.onDestroyView()
    }


    inner class PostTimelineListener : BaseRecyclerListener {

        fun onSingleTap(post: Post) {
            val userID = when {
                getUser() != null -> getUser()!!.uid
                postTimelineViewModel.getCachedUserID().isNotEmpty() -> {
                    LogUtils.d(tag, "User not available, getting cached userID")
                    postTimelineViewModel.getCachedUserID()
                }
                else -> {
                    LogUtils.d(tag, "UserID not available")
                    showError(getString(R.string.error_generic))
                    return
                }
            }
            postTimelineViewModel.setCurrentPost(post)
            val fragment = when {
                post.isMainUserAuthor(userID) -> MyPostDetailFragment.newInstance()
                post.isNews() -> NewsPostDetailFragment.newInstance()
                else -> PostDetailFragment()
            }

            addReplaceFragment(R.id.container, fragment, false, true, NavigationAnimationHolder())

        }

        fun onDoubleTap(holder: PostTimelineViewHolder, post: Post) {
            likedViewHolders[post.uid] = holder
            postTimelineViewModel.setCurrentPost(post)
            postTimelineAdapter.cachePositions()
            postTimelineViewModel.handleLikeUnlikePost(post)
        }

        fun onMenuTap(post: Post) {
            postTimelineViewModel.setCurrentPost(post)
            if (post.isMainUserAuthor(getUser()?.uid)) {
                MyPostBottomSheetFragment.newInstance(post, bottomSheetListenerOwn)
                    .show(this@PostTimelineFragment.childFragmentManager, "bottomSheet")
            } else {
                postTimelineViewModel.refreshPostAuthor(post.userData.uid)
            }
        }

        fun onLikesTap(holder: PostTimelineViewHolder, post: Post) {
            likedViewHolders[post.uid] = holder
            postTimelineViewModel.setCurrentPost(post)
            postTimelineAdapter.cachePositions()
            postTimelineViewModel.handleLikeUnlikePost(post)
        }

        fun onGoToLikesTap(holder: PostTimelineViewHolder, post: Post) {
            if (post.likes > 0) {
                likedViewHolders[post.uid] = holder
                postTimelineViewModel.setCurrentPost(post)
                postTimelineAdapter.cachePositions()
                addReplaceFragment(
                    R.id.container,
                    PostLikeFragment.newInstance(),
                    false,
                    true,
                    NavigationAnimationHolder()
                )
            } else {
                onSingleTap(post)
            }
        }

        fun onSharesTap(post: Post) {
            postTimelineViewModel.setCurrentPost(post)
            handleShare()
        }

        fun onUserAvatarImageClick(post: Post) {
            AccountActivity.openProfileFragment(context!!, post.userData.uid)
        }
    }

    private fun openEditPost(post: Post) {
        val bundle = Bundle().apply {
            putParcelable(BUNDLE_KEY_EDIT_POST, post)
            putInt(BUNDLE_KEY_POSITION, postTimelineAdapter.findItemPositionById(postId = post.uid))
        }
        if (getUser()?.isValid() == true) CreatePostActivity.openEditPostFragment(context!!, getUser()!!.uid, bundle)
    }

    private val bottomSheetListener = object : BasePostSheetListener {
        override fun onBottomSheetReady(bottomSheet: BasePostSheetFragment) {
            if (bottomSheet is PostBottomSheetFragment) {
                bottomSheet.binding.apply {
                    bottomSheetInapropiate.setOnClickListener {
                        postTimelineViewModel.flagInappropriate()
                        bottomSheet.dismiss()
                    }
                    bottomSheetUnfollow.setOnClickListener {
                        postTimelineViewModel.unfollow()
                        bottomSheet.dismiss()
                    }
                    bottomSheetShare.setOnClickListener {
                        handleShare()
                        bottomSheet.dismiss()
                    }
                }
            }
        }
    }

    private val bottomSheetListenerOwn = object : BasePostSheetListener {
        override fun onBottomSheetReady(bottomSheet: BasePostSheetFragment) {
            if (bottomSheet is MyPostBottomSheetFragment) {
                bottomSheet.binding.apply {
                    bottomSheetDelete.setOnClickListener {
                        postTimelineViewModel.deletePost()
                        bottomSheet.dismiss()
                    }
                    bottomSheetEdit.setOnClickListener {
                        openEditPost(bottomSheet.post)
                        bottomSheet.dismiss()
                    }
                    bottomSheetShare.setOnClickListener {
                        handleShare()
                        bottomSheet.dismiss()
                    }
                }
            }
        }
    }

    fun handleShare() {
        postTimelineViewModel.postDeeplinkLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                hideLoader()
                if (!it.first.isNullOrBlank() && !it.second.isNullOrBlank())
                    handleShareResult(it.first as String, it.second as String)
                postTimelineViewModel.postDeeplinkLiveData.value = null
                postTimelineViewModel.postDeeplinkLiveData.removeObservers(viewLifecycleOwner)
            }
        })
        showLoader(R.string.progress_share)
        postTimelineViewModel.share()
    }

    fun handleShareResult(url: String, shareCount: String) {
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
            startActivity(Intent.createChooser(this, getString(R.string.share_with_label)))
        }
        if (!postTimelineViewModel.currentPostId.isBlank()) {
            val position = postTimelineAdapter.findItemPositionById(postTimelineViewModel.currentPostId)
            if (position >= 0) {
                val post = postTimelineAdapter.getSingleItem(position)
                post.sharesCount = Integer.parseInt(shareCount)
                postTimelineAdapter.updateItem(post)
                postTimelineAdapter.notifyItemChanged(position)
            }
            postTimelineViewModel.currentPostId = ""
        }
    }

    //updating data current screen


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChangeEvent(event: PostChangeEvent?) {
        event?.let {
            it.post?.let { post -> postTimelineAdapter.updateItem(post) } ?: postTimelineViewModel.refreshTimeline()
        }
    }


    companion object {

        val logTag = PostTimelineFragment::class.java.simpleName

        fun newInstance(userId: String, postId: String? = null, type: String? = null): PostTimelineFragment {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_USER_ID, userId)
            if (!postId.isNullOrEmpty()) {
                bundle.putString(BUNDLE_KEY_POST_ID, postId)
            }
            if (!type.isNullOrEmpty()) {
                bundle.putString(BUNDLE_KEY_TIMELINE_TYPE, type)
            }
            val instance = PostTimelineFragment()
            instance.arguments = bundle
            return instance
        }
    }

}