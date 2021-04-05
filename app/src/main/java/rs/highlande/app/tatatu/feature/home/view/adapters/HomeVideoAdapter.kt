package rs.highlande.app.tatatu.feature.home.view.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.home_item_streaming.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewDiffAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.model.*

/**
 * Adapter displaying the list of [SuggestedPerson]s in the Home screen.
 * @author mbaldrighi on 2019-06-28.
 */
class HomeVideoAdapter(diffCallback: HomeDiffCallback, private val uiType: HomeUIType, clickListener: OnItemClickListener<HomeDataObject>) :
    BaseRecViewDiffAdapter<HomeDataObject, OnItemClickListener<HomeDataObject>, HomeVideoAdapter.ItemVideoVH>(diffCallback, clickListener) {

    init {
        setHasStableIds(stableIds)
    }

    override fun getItemId(position: Int): Long {
        return getItems()[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVideoVH {
        return ItemVideoVH(
            inflate(R.layout.home_item_streaming, parent)
        )
    }

    /**
     * For [HomeUIType.SERIES] we will have a different layout -> different ViewHolder.
     * Prepares for this future scenario.
     */
    override fun getItemViewType(position: Int): Int {
        return when (uiType) {
            HomeUIType.VIDEOS -> TYPE_VIDEO
            else -> super.getItemViewType(position)
        }
    }

    inner class ItemVideoVH(
        itemView: View
    ) : BaseViewHolder<HomeDataObject, OnItemClickListener<HomeDataObject>>(
        itemView
    ) {

        override fun onBind(item: HomeDataObject, listener: OnItemClickListener<HomeDataObject>?) {

            if (item is TTUVideo) {
                itemView.label.visibility = View.GONE

                itemView.preview.setPicture(item.poster)

                if (listener != null)
                    itemView.setOnClickListener { listener.onItemClick(item) }
            }
        }

//        override fun bindMargins(item: HomeDataObject, mStart: Int?, mTop: Int?, mEnd: Int?, mBottom: Int?)  {
//            (itemView.layoutParams as RecyclerView.LayoutParams).apply {
//                if (isItemFirst(item)) {
//                    mStart?.let { marginStart = it }
//                }
//                else if (isItemLast(item)) {
//                    mEnd?.let { marginEnd = it }
//                }
//            }
//        }
//
//        override fun clearMargins() {
//            (itemView.layoutParams as RecyclerView.LayoutParams).apply {
//                marginStart = 0
//                marginEnd = itemView.resources.getDimensionPixelSize(R.dimen.padding_margin_small_6)
//            }
//        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf(itemView.preview)
        }
    }


    companion object {
        const val TYPE_VIDEO = 0
        const val TYPE_SERIES = 1
        const val TYPE_MUSIC_VIDEO = 2
    }

}