package rs.highlande.app.tatatu.feature.home.view.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.common_item_post.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewDiffAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.model.HomeDataObject
import rs.highlande.app.tatatu.model.HomeDiffCallback
import rs.highlande.app.tatatu.model.Post

/**
 * Adapter displaying the list of [Post]s in the Home screen.
 * @author mbaldrighi on 2019-06-27.
 */
class HomePostsAdapter(diffCallback: HomeDiffCallback, clickListener: OnItemClickListener<HomeDataObject>) :
    BaseRecViewDiffAdapter<HomeDataObject, OnItemClickListener<HomeDataObject>, HomePostsAdapter.PostViewHolder>(diffCallback, clickListener) {

    init {
        setHasStableIds(stableIds)
    }

    override fun getItemId(position: Int): Long {
        return getItems()[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            inflate(
                if (viewType == TYPE_NEW_POST) R.layout.common_item_post_new else R.layout.common_item_post,
                parent
            ),
            viewType == TYPE_NEW_POST
        )
    }


    override fun getItemViewType(position: Int): Int {
        return if ((getItem(position) as Post).isCreateNewPost()) TYPE_NEW_POST else TYPE_POST
    }


    inner class PostViewHolder(
        itemView: View,
        private val isNew: Boolean
    ) : BaseViewHolder<HomeDataObject, OnItemClickListener<HomeDataObject>>(itemView) {

        override fun onBind(item: HomeDataObject, listener: OnItemClickListener<HomeDataObject>?) {

            if (item is Post) {
                itemView.postTitle.apply {
                    if (!isNew) text = item.userData.username
                }
                itemView.postSubTitle?.let {
                    it.text = item.userData.name
                }

                itemView.labelNews?.visibility = if (item.isNews()) View.VISIBLE else View.GONE

                if (listener != null)
                    itemView.setOnClickListener { listener.onItemClick(item) }

                if (!isNew) {
                    when {
                        item.isVideo() || item.hasMultipleMedia() -> {
                            itemView.postType.visibility = View.VISIBLE
                            itemView.postType.setImageResource(if (item.isVideo()) R.drawable.ic_thumbnail_play_video else R.drawable.ic_thumbnail_multiple_photos)

                            if (item.isVideo())
                                itemView.placeholder.setImageResource(R.drawable.ic_preloading_video)
                        }
                        else -> {
                            itemView.postType.visibility = View.GONE
                            itemView.placeholder.setImageResource(R.drawable.ic_preloading_photo)
                        }
                    }
                    itemView.postPreview.setPicture(item.preview)
                }
            }
        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf(itemView.postPreview)
        }
    }


    companion object {
        const val TYPE_NEW_POST = 0
        const val TYPE_POST = 1
    }

}