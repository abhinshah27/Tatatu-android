package rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.databinding.InviteListItemBinding
import rs.highlande.app.tatatu.model.Users

class InvitesAdapter(clickListener: OnItemClickListener<Users>) :
    BaseRecViewAdapter<Users, OnItemClickListener<Users>, InvitesAdapter.InviteViewHolder>(
        clickListener
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = InviteViewHolder(
        DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.invite_list_item,
            parent,
            false
        )
    )

    class InviteViewHolder(val itemBinding: InviteListItemBinding) :
        BaseViewHolder<Users, OnItemClickListener<Users>>(itemBinding.root) {

        override fun onBind(item: Users, listener: OnItemClickListener<Users>?) {
            with(itemBinding) {

                inviteNameTextView.text = item.name
                inviteEmailTextView.text = item.indentifier
                resendInviteButton.setOnClickListener {
                    listener?.onItemClick(item)
                }

                if (item.canResend()) {
                    resendInviteButton.isEnabled = true
                    listener?.let { listener ->
                        with(resendInviteButton) {
                            isEnabled = true
                            setOnClickListener {
                                isEnabled = false
                                listener.onItemClick(item)
                            }
                        }
                    }
                } else {
                    resendInviteButton.isEnabled = false
                }

            }
        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf()
        }
    }

}