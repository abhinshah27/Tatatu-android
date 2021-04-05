package rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.databinding.FollowRequestListItemBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.util.setupListUser
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.BaseFollowFragment
import rs.highlande.app.tatatu.model.User

class FollowRequestsAdapter(clickListener: BaseFollowFragment.FollowClickListener):
    BaseRecViewAdapter<User, BaseFollowFragment.FollowClickListener, FollowRequestsAdapter.FollowRequestsViewHolder>(clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FollowRequestsViewHolder(
        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.follow_request_list_item, parent, false)
    )

    class FollowRequestsViewHolder(val itemBinding: FollowRequestListItemBinding): BaseViewHolder<User, BaseFollowFragment.FollowClickListener>(itemBinding.root) {

        override fun onBind(item: User, listener: BaseFollowFragment.FollowClickListener?) {
            with(itemBinding) {
                listener?.let { listener ->
                    this.user = item
                    setupListUser(followerAvatar, item)

                    with(followerConfirmBackButton) {
                        isEnabled = true
                        setOnClickListener {
                            isEnabled = false
                            listener.onActionClick(item)
                        }
                    }
                    root.setOnClickListener {
                        listener.onItemClick(item)
                    }
                }
                executePendingBindings()
            }
        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf(
                itemBinding.followerAvatar.roundAvatarImageView.picture
            )
        }
    }

}