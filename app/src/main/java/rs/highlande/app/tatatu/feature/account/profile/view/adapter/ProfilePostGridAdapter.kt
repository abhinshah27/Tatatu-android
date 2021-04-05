package rs.highlande.app.tatatu.feature.account.profile.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import kotlinx.android.synthetic.main.profile_grid_item.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.cache.PicturesCache
import rs.highlande.app.tatatu.core.ui.recyclerView.*
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.databinding.ProfileGridItemBinding
import rs.highlande.app.tatatu.databinding.ProfileGridItemNewPostBinding
import rs.highlande.app.tatatu.model.Post

class ProfilePostGridAdapter(
    clickListener: OnItemClickListener<Post>,
    private val visibilityManager: NestedItemVisibilityManager,
    private val isMyProfile: Boolean
) : BaseRecViewAdapter<Post, OnItemClickListener<Post>, ProfilePostGridAdapter.PostGridViewHolder>(clickListener),
    NestedItemVisibilityListener by visibilityManager, KoinComponent
{

    companion object {
        const val VIEW_TYPE_NEW_POST = 0
        const val VIEW_TYPE_REGULAR = 1

        val logTag = ProfilePostGridAdapter::class.java.simpleName
    }


    private val picturesCache: PicturesCache by inject()
    private val videoCache: AudioVideoCache by inject()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostGridViewHolder {
        return if (viewType == VIEW_TYPE_NEW_POST) {
            ProfileNewPostViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.profile_grid_item_new_post, parent, false
                )
            )
        } else {
            ProfileGridItemViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.profile_grid_item, parent, false
                )
            )
        }
    }


    override fun onBindViewHolder(
        holder: PostGridViewHolder,
        position: Int
    ) {
        super.onBindViewHolder(holder, position)

        var postPosition = position

        if (isMyProfile) {
            holder.adapterPosition = postPosition
            if (position > 0) {
                if (getItem(0).isCreateNewPost()) postPosition--
                holder.itemPosition = postPosition
            }
        } else holder.itemPosition = postPosition
    }


    override fun onViewAttachedToWindow(holder: PostGridViewHolder) {
        super.onViewAttachedToWindow(holder)

        holder.itemPosition?.let {
            if (!getItem(holder.adapterPosition ?: 0).isCreateNewPost())
                visibilityManager.addVisibleItem(holder, holder.itemPosition ?: 0)
        }
    }

    override fun onViewDetachedFromWindow(holder: PostGridViewHolder) {

        visibilityManager.removeVisibleItem(holder)

        super.onViewDetachedFromWindow(holder)
    }


    override fun onViewRecycled(holder: PostGridViewHolder) {

        holder.itemPosition = null

        super.onViewRecycled(holder)
    }



    override fun getItemViewType(position: Int): Int {
        return with(getItem(position)) {
            if (isCreateNewPost()) {
                VIEW_TYPE_NEW_POST
            } else {
                VIEW_TYPE_REGULAR
            }
        }
    }


    open inner class PostGridViewHolder(val itemBinding: ViewDataBinding): BaseViewHolder<Post, OnItemClickListener<Post>>(itemBinding.root) {

        var itemPosition: Int? = null
        var adapterPosition: Int? = null

        override fun onBind(item: Post, listener: OnItemClickListener<Post>?) {}

        override fun getImageViewsToRecycle(): Set<ImageView?> = setOf()
    }


    inner class ProfileNewPostViewHolder(itemBinding: ProfileGridItemNewPostBinding): PostGridViewHolder(itemBinding) {

        override fun onBind(item: Post, listener: OnItemClickListener<Post>?) {
            (itemBinding as? ProfileGridItemNewPostBinding)?.let {
                it.newPost.setOnClickListener {
                    listener!!.onItemClick(item)
                }
            }

        }

        override fun getImageViewsToRecycle(): Set<ImageView?> = setOf()
    }

    inner class ProfileGridItemViewHolder(itemBinding: ProfileGridItemBinding): PostGridViewHolder(itemBinding) {

        override fun onBind(item: Post, listener: OnItemClickListener<Post>?) {
            (itemBinding as? ProfileGridItemBinding)?.let {

                // if profile is Main User's use cache
                it.postPreview.setPicture(
                    if (isMyProfile)
                        item.getMediaObjects(picturesCache, videoCache)?.get(0)
                    else item.getPostPreview()
                )

                it.labelNews.visibility = if (item.isNews()) View.VISIBLE else View.GONE

                when {
                    item.isVideo() -> {
                        it.postType.visibility = View.VISIBLE
                        it.postType.setPicture(R.drawable.ic_thumbnail_play_video)
                        itemView.placeholder.setImageResource(R.drawable.ic_preloading_video)
                    }
                    item.hasMultipleMedia() -> {
                        it.postType.visibility = View.VISIBLE
                        it.postType.setPicture(R.drawable.ic_thumbnail_multiple_photos)
                        itemView.placeholder.setImageResource(R.drawable.ic_preloading_photo)
                    }
                    else -> {
                        itemView.placeholder.setImageResource(R.drawable.ic_preloading_photo)
                        it.postType.visibility = View.GONE
                    }
                }
                it.root.setOnClickListener {
                    listener!!.onItemClick(item)
                }
            }

        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return (itemBinding as? ProfileGridItemBinding)?.let {
                setOf(it.postPreview, it.postType)
            } ?: setOf()
        }
    }


}