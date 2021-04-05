package rs.highlande.app.tatatu.feature.post.util

import android.content.Context
import android.view.View
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.formatDateToAge
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.PostCommentItemBinding
import rs.highlande.app.tatatu.databinding.PostDetailBinding
import rs.highlande.app.tatatu.feature.post.detail.fragment.PostDetailFragment
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostComment

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */

fun loadCommonPostUi(context: Context, binding: PostDetailBinding, post: Post) {
    binding.apply {

        if (!post.caption.isNullOrBlank()) {
            postBodyTextView.visibility = View.VISIBLE
            postBodyTextView.text = post.caption
        } else {
            postBodyTextView.visibility = View.GONE
        }

        LogUtils.d("TAG", "TESTLIKEVIEWHOLDER: updating from LD: with binding: $binding and post: $post")

        postLikesImageView.isEnabled = post.liked
        postLikesCountTextView.text = post.likes.toString()

        postCommentsCountTextView.text = post.commentsCount.toString()
        postSharesCountTextView.text = post.sharesCount.toString()
        postAgeTextView.text = formatDateToAge(context, post.date)

        post.userData.let { user ->
            userNameTextView.text = user.username
            //TODO: 28/08 Uncomment when account verification is working
            /*if (user.verified) {
                userNameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_ttu_badge, 0)
            } else {
                userNameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0 )
            }*/


            userAvatarImageView.picture.setProfilePicture(user.picture)
            userAvatarImageView.celebrityIndicator.visibility = if (user.isCelebrity()) View.VISIBLE else View.GONE

            // TODO: 2019-08-05    handle MOMENTS
            userAvatarNewMomentImageView.visibility =
                if (user.hasNewMoment) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

        }

        if (post.isNews()) {
            postNewsTextView.visibility = View.VISIBLE
            if (!post.title.isNullOrBlank()) {
                postTitleTextView.visibility = View.VISIBLE
                postTitleTextView.text = post.title
            } else postTitleTextView.visibility = View.GONE
        } else {
            postNewsTextView.visibility = View.INVISIBLE
            postTitleTextView.visibility = View.GONE
        }
    }
}

fun loadPostCommentUi(context: Context, binding: PostCommentItemBinding, comment: PostComment, listener: PostDetailFragment.PostCommentListener? = null) {
    with(binding) {
        postCommentBodyTextView.text = comment.text
        commentAgeTextView.text = formatDateToAge(context, comment.date)
        commentReplyCountTextView.text = comment.commentsCount.toString()
        commentLikesImageView.isEnabled = comment.liked
        commentLikesCountTextView.text = comment.likesCount.toString()

        val user = comment.userData

        postCommentUserAvatarImageView.apply {
            picture.setProfilePicture(user.picture)
            celebrityIndicator.visibility = if (user.isCelebrity()) View.VISIBLE else View.GONE
        }

        // TODO: 2019-08-05    handle MOMENTS
        postCommentUserMomentImageView.visibility =
            if (user.hasNewMoment) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

        postCommentUserNameTextView.text = comment.userData.username

        if (listener != null) {
            postCommentUserAvatarImageView.setOnClickListener {
                listener.onCommentUserAvatarImageClick(comment)
            }
            commentReplyTextView.setOnClickListener {
                listener.onReplyClick(comment)
            }

            commentLikesSection.setOnClickListener {
                listener.onLikeTap(comment)
            }
        }
    }
}