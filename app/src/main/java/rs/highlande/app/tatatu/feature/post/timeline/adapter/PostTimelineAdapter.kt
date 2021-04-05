package rs.highlande.app.tatatu.feature.post.timeline.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseVideoPlayerDiffAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoViewHolderManager
import rs.highlande.app.tatatu.feature.post.timeline.fragment.PostTimelineFragment
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostDiffCallback
import rs.highlande.app.tatatu.model.PostType

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostTimelineAdapter(diffCallback: PostDiffCallback, val clickListener: PostTimelineFragment.PostTimelineListener) : BaseVideoPlayerDiffAdapter<Post, PostTimelineFragment.PostTimelineListener, PostTimelineViewHolder>(diffCallback, clickListener) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        TYPE_IMAGE -> PostTimelinePhotoVH(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.post_timeline_item, parent, false), clickListener)
        TYPE_VIDEO -> PostTimelineVideoVH(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.post_timeline_item, parent, false), clickListener, VideoViewHolderManager(parent.context))

        else -> super.createViewHolder(parent, viewType)
    }


    fun findItemPositionById(postId: String) = getItems().indexOfFirst { it.uid == postId }

    override fun getItemViewType(position: Int): Int {

        val post = getItem(position) as Post

        return when (post.type) {
            PostType.IMAGE -> TYPE_IMAGE
            PostType.VIDEO -> TYPE_VIDEO
            else -> super.getItemViewType(position)
        }
    }


    companion object {
        val logTag = PostTimelineAdapter::class.java.simpleName

        const val TYPE_IMAGE = 0
        const val TYPE_VIDEO = 1
    }

}