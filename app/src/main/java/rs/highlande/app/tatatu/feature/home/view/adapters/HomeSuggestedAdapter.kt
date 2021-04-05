package rs.highlande.app.tatatu.feature.home.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewDiffAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.HomeItemSuggestedWithPostBinding
import rs.highlande.app.tatatu.feature.commonView.UnFollowListener
import rs.highlande.app.tatatu.model.HomeDataObject
import rs.highlande.app.tatatu.model.HomeDiffCallback
import rs.highlande.app.tatatu.model.SuggestedPerson

/**
 * Adapter displaying the list of [SuggestedPerson]s in the Home screen.
 * @author mbaldrighi on 2019-06-28.
 */
class HomeSuggestedAdapter(
    diffCallback: HomeDiffCallback,
    clickListener: OnItemClickListener<HomeDataObject>,
    private val followListener: UnFollowListener<SuggestedPerson>?
) : BaseRecViewDiffAdapter<HomeDataObject, OnItemClickListener<HomeDataObject>, HomeSuggestedAdapter.SuggestedVH>(diffCallback, clickListener) {

    init {
        setHasStableIds(false)
    }

    override fun getItemId(position: Int): Long {
        return getItems()[position].hashCode().toLong()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedVH {
        return SuggestedVH(
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.home_item_suggested_with_post, parent, false),
            followListener
        )
    }

    override fun onViewRecycled(holder: SuggestedVH) {
        super.onViewRecycled(holder)

        holder.binding.btnFollow.isEnabled = true
    }


    inner class SuggestedVH(
        val binding: HomeItemSuggestedWithPostBinding,
        private val followListener: UnFollowListener<SuggestedPerson>?
    ) : BaseViewHolder<HomeDataObject, OnItemClickListener<HomeDataObject>>(binding.root) {

        override fun onBind(item: HomeDataObject, listener: OnItemClickListener<HomeDataObject>?) {

            if (item is SuggestedPerson) {

                binding.person = item

                binding.profilePicture.picture.setProfilePicture(item.picture)
                binding.profilePicture.celebrityIndicator.visibility = if (item.isCelebrity()) View.VISIBLE else View.GONE

                binding.placeholder.setImageResource(if (item.isVideo()) R.drawable.ic_preloading_video else R.drawable.ic_preloading_photo)
                binding.postPreview.setPicture(item.mediaURL)

                binding.btnFollow.setOnClickListener {
                    followListener?.onFollowClickedListener(item, it, adapterPosition)
                }

                if (listener != null)
                    itemView.setOnClickListener { listener.onItemClick(item) }

                binding.executePendingBindings()

            }
        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf(binding.profilePicture.picture, binding.postPreview)
        }
    }

}