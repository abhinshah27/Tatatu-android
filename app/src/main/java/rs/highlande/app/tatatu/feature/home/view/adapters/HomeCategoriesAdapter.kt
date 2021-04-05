package rs.highlande.app.tatatu.feature.home.view.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.*
import rs.highlande.app.tatatu.model.HomeDataObject
import rs.highlande.app.tatatu.model.HomeDiffCallback
import rs.highlande.app.tatatu.model.StreamingCategory

/**
 * Adapter displaying the list of [StreamingCategory]s in the Home screen.
 * @author mbaldrighi on 2019-06-28.
 */
class HomeCategoriesAdapter(diffCallback: HomeDiffCallback, clickListener: OnItemClickListener<HomeDataObject>) :
    BaseRecViewDiffAdapter<HomeDataObject, OnItemClickListener<HomeDataObject>, HomeCategoriesAdapter.CategoryVH>(diffCallback, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        return CategoryVH(
            inflate(R.layout.home_item_category, parent)
        )
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_FIRST
            (itemCount - 1) -> VIEW_TYPE_LAST
            else -> super.getItemViewType(position)
        }
    }

    inner class CategoryVH(
        itemView: View
    ) : BaseViewHolder<HomeDataObject, OnItemClickListener<HomeDataObject>>(itemView) {

        override fun onBind(item: HomeDataObject, listener: OnItemClickListener<HomeDataObject>?) {

            if (item is StreamingCategory) {
                (itemView as TextView).text = item.label

                if (listener != null)
                    itemView.setOnClickListener { listener.onItemClick(item) }
            }
        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf()
        }
    }

}