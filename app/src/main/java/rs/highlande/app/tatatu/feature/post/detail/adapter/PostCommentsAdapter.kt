package rs.highlande.app.tatatu.feature.post.detail.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.databinding.PostCommentInnerItemBinding
import rs.highlande.app.tatatu.databinding.PostCommentItemBinding
import rs.highlande.app.tatatu.feature.post.detail.fragment.PostDetailFragment
import rs.highlande.app.tatatu.feature.post.util.loadPostCommentUi
import rs.highlande.app.tatatu.model.PostComment

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostCommentsAdapter(val clickListener: PostDetailFragment.PostCommentListener, val context: Context):
    BaseRecViewAdapter<PostComment, PostDetailFragment.PostCommentListener, BaseViewHolder<PostComment, PostDetailFragment.PostCommentListener>>(clickListener) {

    init {
        setHasStableIds(true)
    }


    override fun getItemId(position: Int): Long {
        return getItems()[position].hashCode().toLong()
    }


    override fun getItemViewType(position: Int) = getItem(position).level

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<PostComment, PostDetailFragment.PostCommentListener> {
        return when(viewType) {
            1 -> PostInnerCommentViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.post_comment_inner_item, parent, false))
            else -> PostCommentViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.post_comment_item, parent, false))
        }
    }

    fun getItemPositionForAddition(parentCommentID: String) =
        (getItems().indexOfLast { if (it.isSubComment()) parentCommentID == it.parentCommentID else parentCommentID == it.id }) + 1

    fun updateItem(comment: PostComment) {
        val position = getItems().indexOfFirst {
            it.id == comment.id
        }
        //TODO: Sometimes this is called with an invalid item position, causing an exception. Check why is happening
        if (position >= 0) {
            updateAt(position, comment)
        }
        refreshItem(comment)
    }

    inner class PostCommentViewHolder(val itemBinding: PostCommentItemBinding): BaseViewHolder<PostComment, PostDetailFragment.PostCommentListener>(itemBinding.root) {
        override fun onBind(item: PostComment, listener: PostDetailFragment.PostCommentListener?) {
            loadPostCommentUi(context, itemBinding, item, listener)
            itemBinding.groupReply.visibility = View.VISIBLE
        }

        override fun getImageViewsToRecycle() = setOf(itemBinding.postCommentUserAvatarImageView.picture)

    }

    inner class PostInnerCommentViewHolder(val itemBinding: PostCommentInnerItemBinding): BaseViewHolder<PostComment, PostDetailFragment.PostCommentListener>(itemBinding.root) {
        override fun onBind(item: PostComment, listener: PostDetailFragment.PostCommentListener?) {
            loadPostCommentUi(context, itemBinding.commentBody, item, listener)
            itemBinding.commentBody.groupReply.visibility = View.GONE
        }

        override fun getImageViewsToRecycle() = setOf(itemBinding.commentBody.postCommentUserAvatarImageView.picture)

    }

}