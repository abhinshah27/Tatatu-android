package rs.highlande.app.tatatu.feature.post.timeline.adapter

import android.content.Context
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.core.ui.recyclerView.*
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.getVisiblePercentage
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.databinding.PostTimelineItemBinding
import rs.highlande.app.tatatu.feature.post.timeline.fragment.PostTimelineFragment
import rs.highlande.app.tatatu.feature.post.util.loadCommonPostUi
import rs.highlande.app.tatatu.model.Post

/**
 * TODO - File description
 * @author mbaldrighi on 2019-08-08.
 */


open class PostTimelineViewHolder(val itemBinding: PostTimelineItemBinding, val clickListener: PostTimelineFragment.PostTimelineListener) : BaseViewHolder<Post, PostTimelineFragment.PostTimelineListener>(itemBinding.root) {

    protected var context: Context = itemBinding.root.context

    override fun getImageViewsToRecycle() = setOf(itemBinding.postDetail.postImageView, itemBinding.postDetail.userAvatarImageView.picture)


    override fun onBind(item: Post, listener: PostTimelineFragment.PostTimelineListener?) {

        LogUtils.d(logTag, "VIDEO_TEST: onBind() parent: $this")

        val context = itemBinding.root.context

        loadCommonPostUi(context, itemBinding.postDetail, item)
        itemBinding.postDetail.postBodyTextView.isSingleLine = false
        itemBinding.postDetail.postBodyTextView.ellipsize  = TextUtils.TruncateAt.END
        itemBinding.postDetail.postBodyTextView.maxLines = 5

        itemBinding.postDetail.postOptionsSection.setOnClickListener {
            clickListener.onMenuTap(item)
        }

        itemBinding.postTranslateTextView.setOnClickListener {
            Toast.makeText(context, "Action translate", Toast.LENGTH_SHORT).show()
        }

        itemBinding.postDetail.postLikesSection.setOnClickListener {
            clickListener.onLikesTap(this, item)
        }

        itemBinding.postDetail.postGoToLikesSection.setOnClickListener {
            clickListener.onGoToLikesTap(this, item)
        }

        itemBinding.postDetail.postSharesSection.setOnClickListener {
            clickListener.onSharesTap(item)
        }

        itemBinding.postViewCommentsTextView.setOnClickListener {
            clickListener.onSingleTap(item)
        }

        itemBinding.postDetail.userAvatarImageView.setOnClickListener {
            clickListener.onUserAvatarImageClick(item)
        }



        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                clickListener.onSingleTap(item)
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                clickListener.onDoubleTap(this@PostTimelineViewHolder, item)
                return true
            }
        })

        itemView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    open fun onAttach() {}
    open fun onDetach() {}

    companion object {
        val logTag = PostTimelineViewHolder::class.java.simpleName
    }
}


class PostTimelinePhotoVH(itemBinding: PostTimelineItemBinding, clickListener: PostTimelineFragment.PostTimelineListener) : PostTimelineViewHolder(itemBinding, clickListener) {


    override fun onBind(item: Post, listener: PostTimelineFragment.PostTimelineListener?) {
        super.onBind(item, listener)

        itemBinding.postDetail.postImageView.setPicture(item.mediaItems[0].mediaEndpoint)
    }

    companion object {
        val logTag = PostTimelinePhotoVH::class.java.simpleName
    }
}


class PostTimelineVideoVH(itemBinding: PostTimelineItemBinding, clickListener: PostTimelineFragment.PostTimelineListener, private val videoManager: VideoViewHolderManager) : PostTimelineViewHolder(itemBinding, clickListener), VideoViewHolder by videoManager {

    override fun onBindVideoItem(item: VideoObject, hasVideoPlaying: Boolean) {
        super.onBind(item as Post, clickListener)

        videoObject = item

        LogUtils.d(logTag, "VIDEO_TEST: onBind() child: $this")

        itemBinding.postDetail.apply {

            videoManager.apply {
                setPlayIcon(itemBinding.postDetail.postPlay)
                setVideoView(itemBinding.postDetail.postExoplayer)
            }

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
                    clickListener.onDoubleTap(this@PostTimelineVideoVH, item)
                    return true
                }
            })

            postExoplayer.setOnTouchListener { _, event ->
                playerGestureDetector.onTouchEvent(event)
                true
            }
        }
    }


    companion object {
        val logTag = PostTimelineVideoVH::class.java.simpleName
    }

}