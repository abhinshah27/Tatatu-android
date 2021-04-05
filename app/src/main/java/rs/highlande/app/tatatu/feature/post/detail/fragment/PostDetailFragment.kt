package rs.highlande.app.tatatu.feature.post.detail.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.greenrobot.eventbus.EventBus
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.ui.*
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecyclerListener
import rs.highlande.app.tatatu.core.ui.recyclerView.PLAYABLE_THRESHOLD
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoObject
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoViewHolder
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.FragmentPostDetailBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.post.PostActivity
import rs.highlande.app.tatatu.feature.post.common.PostBottomSheetFragment
import rs.highlande.app.tatatu.feature.post.detail.adapter.PostCommentsAdapter
import rs.highlande.app.tatatu.feature.post.detail.viewModel.PostDetailViewModel
import rs.highlande.app.tatatu.feature.post.like.fragment.PostLikeFragment
import rs.highlande.app.tatatu.feature.post.timeline.fragment.PostTimelineFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetListener
import rs.highlande.app.tatatu.feature.post.util.loadCommonPostUi
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostComment
import rs.highlande.app.tatatu.model.Relationship
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.event.PostChangeEvent

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
open class PostDetailFragment : BaseFragment(), VideoViewHolder {


    private val videoPlayer by lazy {
        context?.let {
            if (it.isValid()) VideoPlayerHelper(context = it) else null
        }
    }

    private val videoCache: AudioVideoCache by inject()

    private var sendNullBusEvent = false


    companion object {

        val logTag = PostDetailFragment::class.java.simpleName

        fun newInstance() = PostDetailFragment()
    }

    protected lateinit var postDetailBinding: FragmentPostDetailBinding
    protected lateinit var postCommentsAdapter: PostCommentsAdapter

    protected val postDetailViewModel: PostDetailViewModel by viewModel()
    protected var selectedPost: Post? = null
    private var position = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postDetailBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_post_detail, container, false)
        postDetailBinding.root.apply {
            configureLayout(this)
            return this
        }
    }

    private fun onNewCommentClick() {
        if (getUser() != null) postDetailViewModel.saveComment(getUser()!!, postDetailBinding.newComment.newCommentEditText.text.toString().trim())
        postDetailBinding.newComment.newCommentEditText.text?.clear()
        postDetailBinding.newComment.newCommentEditText.clearFocus()
        postDetailBinding.newComment.newCommentEditText.hideKeyboard()
    }

    open fun subscribeToLiveData() {
        postDetailViewModel.currentPostLiveData.observe(viewLifecycleOwner, Observer {

            selectedPost = it

            postDetailViewModel.refreshPostAuthor(it.userData.uid)

            LogUtils.d("here", "we are")
            loadCommonPostUi(context!!, postDetailBinding.postDetail, it)
            val gestureDetector = getGestureDetector(it)
            postDetailBinding.postDetail.root.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            if (it.isVideo()) {
                setVideoCachedPosition()
                onBindVideoItem(it, false)
            } else {
                if (!it.mediaItems.isNullOrEmpty()) postDetailBinding.postDetail.postImageView.setPicture(it.mediaItems[0].mediaEndpoint)
            }
        })

        postDetailViewModel.postCommentsLiveData.observe(viewLifecycleOwner, Observer {
            postDetailBinding.srl.isRefreshing = false
            if (it.second) postCommentsAdapter.addAll(it.first)
            else postCommentsAdapter.updateRange(it.first)
        })

        postDetailViewModel.postLikeLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                with(it.second) {
                    postDetailBinding.postDetail.postLikesImageView.isEnabled = liked
                    postDetailBinding.postDetail.postLikesCountTextView.text = likes.toString()
                }

                EventBus.getDefault().post(PostChangeEvent(it.second))
            }
        })

        postDetailViewModel.postReportedLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                showMessage(getString(R.string.result_action_post_report))
                postDetailViewModel.postReportedLiveData.value = false
            }
        })

        postDetailViewModel.newCommentSavedLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                postDetailViewModel.getCachedCurrentPost()?.let { post ->
                    postDetailViewModel.newComment.apply {

                        this?.let {
                            // TODO: 2019-08-05    LIKELY TO BE CHANGED: check if logic chat-style is still valid
                            val position = if (this.isSubComment()) {
                                postCommentsAdapter.getItemPositionForAddition(this.parentCommentID!!)
                            } else {
                                // add new comment chat-style to the bottom
                                -1
                            }
                            val newCount = Integer.parseInt(postDetailBinding.postDetail.postCommentsCountTextView.text.toString()).plus(1)
                            postDetailBinding.postDetail.postCommentsCountTextView.text = newCount.toString()
                            if (it.level == 1) {
                                if (!it.parentCommentID.isNullOrEmpty()) {
                                    increaseParentCommentReplyCount(it.parentCommentID!!)
                                }
                            }
                            if (position > -1)
                                postCommentsAdapter.addAt(position, this)
                            else
                                postCommentsAdapter.add(this)
                            postDetailBinding.commentList.scrollToPosition(position)

                            EventBus.getDefault().post(PostChangeEvent(post))
                        }
                    }
                }
            } else {
                showError(getString(R.string.error_generic))
            }
        })

        postDetailViewModel.commentLikeLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                postCommentsAdapter.updateItem(postDetailViewModel.currentComment!!)
                postDetailViewModel.getCachedCurrentPost()?.let { post ->
                    EventBus.getDefault().post(PostChangeEvent(post))
                }
            }
        })

        postDetailViewModel.postUnFollowedLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                activity?.supportFragmentManager?.popBackStack(PostTimelineFragment.logTag, 0)

                // INFO: 2019-08-12    Bus event with null Post -> unfollow -> refresh timeline
                sendNullBusEvent = true
            } else {
                showError(getString(R.string.error_generic))
            }
        })

        postDetailViewModel.postAuthorUpdated.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                if (it.second != null && it.third != null) {
                    LogUtils.d(logTag, "Post author up-to-date")
                } else {
                    showError(getString(R.string.error_generic))
                }
            }
        })

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (arguments != null) {
            position = arguments!!.getInt(BUNDLE_KEY_POSITION)
        }

        subscribeToLiveData()

    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        postDetailViewModel.getPostComments()
        postDetailBinding.srl.isRefreshing = true
    }

    override fun onPause() {
        postDetailViewModel.videoCache = pauseVideo().second
        super.onPause()
    }


    override fun configureLayout(view: View) {
        postDetailBinding.newComment.newCommentEditText.disableNestedScrollOnView()
        postCommentsAdapter = PostCommentsAdapter(PostCommentListener(), context!!)

        postDetailBinding.commentList.apply {
            adapter = postCommentsAdapter
            addOnScrollListener(postDetailViewModel.scrollListener)
            postDetailBinding.scrollView.setOnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, _, _ ->
                v?.let {
                    if (it.getChildAt(v.childCount - 1) != null) {
                        postDetailViewModel.scrollListener.onScrolled(this, scrollX, scrollY)
                    }
                }
            }
        }

        postDetailViewModel.getCurrentPost()

        configureToolbar()

        postDetailBinding.postDetail.postOptionsSection.setOnClickListener {
            getBottomSheet()?.let { bottomSheet ->
                bottomSheet.apply {
                    exitTransition = R.anim.slide_out_to_left
                    reenterTransition = R.anim.slide_in_from_right
                }
                bottomSheet.show(childFragmentManager, "bottomSheet")
            }
        }

        postDetailBinding.postDetail.postLikesSection.setOnClickListener {
            postDetailViewModel.getCachedCurrentPost()?.let { post ->
                postDetailViewModel.handleLikeUnlikePost(post)
            }
        }

        postDetailBinding.postDetail.postGoToLikesSection.setOnClickListener {
            postDetailViewModel.getCachedCurrentPost()?.let { post ->
                if (post.likes > 0)
                    addReplaceFragment(
                        R.id.container,
                        PostLikeFragment.newInstance(),
                        false,
                        true,
                        NavigationAnimationHolder()
                    )
            }
        }

        postDetailBinding.postDetail.postSharesSection.setOnClickListener {
            handleShare()
        }

        postDetailBinding.newComment.profilePicture.setOnClickListener {
            if (getUser()?.isValid() == true) AccountActivity.openProfileFragment(context!!, getUser()!!.uid)
        }

        postDetailBinding.postDetail.userAvatarImageView.setOnClickListener {
            selectedPost?.let {
                AccountActivity.openProfileFragment(context!!, it.userData.uid)
            }
        }

        postDetailBinding.postDetail.userAvatarNewMomentImageView.setOnClickListener {
            selectedPost?.let {
                AccountActivity.openProfileFragment(context!!, it.userData.uid)
            }
        }

        postDetailBinding.srl.setOnRefreshListener {
            postDetailViewModel.getPostComments()
        }

    }

    protected open fun configureToolbar() {
        (activity as? PostActivity)?.apply {
            setSupportActionBar(postDetailBinding.toolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
            supportActionBar!!.setDisplayShowHomeEnabled(false)

            postDetailBinding.toolbar.title.setText(R.string.tb_post)
            postDetailBinding.toolbar.backArrow.setOnClickListener {
                activity!!.onBackPressed()
            }
        }
    }

    override fun bindLayout() {}


    private val commentTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {}
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            postDetailBinding.newComment.newCommentButton.isEnabled = p0.toString().trim().isNotEmpty()
        }

    }

    private val bottomSheetListener = object : BasePostSheetListener {
        override fun onBottomSheetReady(bottomSheet: BasePostSheetFragment) {
            if (bottomSheet is PostBottomSheetFragment) {
                bottomSheet.binding.apply {
                    bottomSheetInapropiate.setOnClickListener {
                        postDetailViewModel.flagInappropriate()
                        bottomSheet.dismiss()
                    }
                    bottomSheetUnfollow.setOnClickListener {
                        postDetailViewModel.unfollow()
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
        postDetailViewModel.postDeeplinkLiveData.observe(viewLifecycleOwner, Observer {
            hideLoader()
            if (!it.first.isNullOrBlank() && !it.second.isNullOrBlank()) handleShareResult(it.first as String, it.second as String)
            postDetailViewModel.postDeeplinkLiveData.removeObservers(viewLifecycleOwner)
        })
        showLoader(R.string.progress_share)
        postDetailViewModel.share()
    }

    protected fun handleShareResult(url: String, shareCount: String) {
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
            startActivity(Intent.createChooser(this, getString(R.string.share_with_label)))
        }
        postDetailViewModel.getCachedCurrentPost()?.let { post ->
            post.sharesCount = Integer.parseInt(shareCount)
            EventBus.getDefault().post(PostChangeEvent(post))
        }
    }

    protected open fun getBottomSheet(): BasePostSheetFragment? {
        return selectedPost?.let {
            val canUnfollow = postDetailViewModel.postAuthorUpdated.value?.second?.let { user ->
                user.detailsInfo.relationship == Relationship.FOLLOWING ||  user.detailsInfo.relationship == Relationship.FRIENDS
            } ?: false

            PostBottomSheetFragment.newInstance(it, canUnfollow, bottomSheetListener)
        } ?: run {
            null
        }
    }

    protected open fun getGestureDetector(post: Post): GestureDetector {
        return GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                postDetailViewModel.handleLikeUnlikePost(post)
                return true
            }
        })
    }

    inner class PostCommentListener : BaseRecyclerListener {

        fun onCommentUserAvatarImageClick(comment: PostComment) {
            AccountActivity.openProfileFragment(context!!, comment.userData.uid)
        }

        fun onReplyClick(comment: PostComment) {
            postDetailViewModel.parentCommentID = if (comment.isSubComment()) comment.parentCommentID else comment.id
            postDetailBinding.newComment.newCommentEditText.requestFocus()
            postDetailBinding.newComment.newCommentEditText.showKeyboard()
        }

        fun onLikeTap(comment: PostComment) {
            postDetailViewModel.handleLikeUnlikeComment(comment)
        }
    }

    override fun observeOnMyUserAction(user: User) {
        postDetailBinding.newComment.profilePicture.apply {
            picture.setProfilePicture(user.picture)
            celebrityIndicator.visibility = if (user.isCelebrity()) View.VISIBLE else View.GONE
        }
        postDetailBinding.newComment.newCommentButton.isEnabled = false
        postDetailBinding.newComment.newCommentButton.setOnClickListener {
            onNewCommentClick()
        }
        postDetailBinding.newComment.newCommentEditText.addTextChangedListener(commentTextWatcher)
    }

    //Fire EventBus for updating data
    override fun onDestroyView() {

        stopVideo()

        if (sendNullBusEvent) EventBus.getDefault().post(PostChangeEvent(null))
        else {
            if (position != -1) {
                EventBus.getDefault().post(PostChangeEvent(selectedPost, position))
            } else {
                EventBus.getDefault().post(PostChangeEvent(selectedPost))
            }
        }
        super.onDestroyView()
    }


    //region = Video management =
    override var videoObject: VideoObject? = null

    override fun isPlaying() = videoPlayer?.isPlaying() == true

    override fun pauseVideo(): Pair<String, Long> {
        try {
            videoPlayer?.pause(getPlayIcon())
            return getCachePair()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "" to 0L
    }


    override fun startVideo(cachedPosition: Long?) {
        videoPlayer?.start(getPlayIcon(), cachedPosition ?: 0)
    }

    override fun stopVideo() {
        videoPlayer?.stop()
    }

    override fun getCachePair(): Pair<String, Long> =
        (videoObject?.getUniqueID() ?: "") to (videoPlayer?.getCurrentPosition() ?: 0L)

    override fun getCurrentPosition() =
        postDetailViewModel.videoCache ?: videoPlayer?.getCurrentPosition() ?: 0L

    override fun onClick() {
        videoPlayer?.onClick(getPlayIcon())
    }

    override fun videoPlay(play: Boolean) {
        videoPlayer?.mExoPlayer = getVideoView()
        videoPlayer?.videoPlay(videoObject?.getVideoObject(videoCache)?.get(0), getPlayIcon(), play)
    }

    override fun getVideoView(): PlayerView = postDetailBinding.postDetail.postExoplayer

    override fun getPlayIcon(): AppCompatImageView = postDetailBinding.postDetail.postPlay

    override fun setVideoView(playerView: PlayerView) {}

    override fun setPlayIcon(playIcon: AppCompatImageView) {}

    override fun hasVideoBeenPausedByUser() = videoPlayer?.hasVideoBeenPausedByUser ?: false

    override fun onBindVideoItem(item: VideoObject, hasVideoPlaying: Boolean) {
        videoObject = item

        postDetailBinding.postDetail.apply {

            postExoplayerContainer.visibility = View.VISIBLE
            postPlay.visibility = View.VISIBLE
            postImageView.visibility = View.INVISIBLE

            videoPlay(!hasVideoPlaying && (getVideoView().getVisiblePercentage() > PLAYABLE_THRESHOLD))
            postPlay.setOnClickListener {
                onClick()
            }

            val playerGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    onClick()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    postDetailViewModel.handleLikeUnlikePost(item as Post)
                    return true
                }
            })

            postExoplayer.setOnTouchListener { _, event ->
                playerGestureDetector.onTouchEvent(event)
                true
            }
        }
    }

    private fun increaseParentCommentReplyCount(commentId: String) {
        val parentComment = postCommentsAdapter.getItems().find {
            it.id == commentId
        }
        parentComment?.let {
            it.commentsCount += 1
            postCommentsAdapter.updateItem(it)
        }
    }

    private fun setVideoCachedPosition() {
        videoPlayer?.assignedPosition = postDetailViewModel.videoCache
    }

    //endregion


}