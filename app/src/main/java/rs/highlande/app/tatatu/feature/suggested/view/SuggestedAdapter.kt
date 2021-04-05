package rs.highlande.app.tatatu.feature.suggested.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewDiffAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.databinding.SuggestedItemBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.util.setupListUser
import rs.highlande.app.tatatu.feature.commonView.UnFollowListener
import rs.highlande.app.tatatu.model.SuggestedPerson

/**
 * Adapter displaying the list of [SuggestedPerson]s in the Home screen.
 * @author mbaldrighi on 2019-06-28.
 * Modified to extend [BaseRecViewDiffAdapter] on 2019-07-17
 */
class SuggestedAdapter(
    suggestedDiffUtil: SuggestedPerson.SuggestedDiffCallback,
    clickListener: OnItemClickListener<SuggestedPerson>,
    private val followListener: UnFollowListener<SuggestedPerson>?
) : BaseRecViewAdapter<SuggestedPerson, OnItemClickListener<SuggestedPerson>, SuggestedAdapter.SuggestedVH2>(/*suggestedDiffUtil, */clickListener) {

    init {
        setHasStableIds(stableIds)
    }


    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedVH2{
        return SuggestedVH2(
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.suggested_item, parent, false),
            followListener
        )
    }

    override fun onViewRecycled(holder: SuggestedVH2) {
        super.onViewRecycled(holder)

        holder.binding.btnFollow.isEnabled = true
    }

    inner class SuggestedVH2(
        val binding: SuggestedItemBinding,
        private val followListener: UnFollowListener<SuggestedPerson>?
    ) : BaseViewHolder<SuggestedPerson, OnItemClickListener<SuggestedPerson>>(
        binding.root
    ) {

        override fun onBind(item: SuggestedPerson, listener: OnItemClickListener<SuggestedPerson>?) {

            with(binding) {
                person = item
                setupListUser(followerAvatar, item)
                btnFollow.setOnClickListener {
                    followListener?.onFollowClickedListener(item, it, adapterPosition)
                }
                if (listener != null)
                    itemView.setOnClickListener { listener.onItemClick(item) }
                executePendingBindings()
            }


        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf(binding.followerAvatar.roundAvatarImageView.picture)
        }
    }

}