package rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.databinding.FollowListItemBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.util.setupFollowButtons
import rs.highlande.app.tatatu.feature.account.followFriendList.util.setupListUser
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.BaseFollowFragment
import rs.highlande.app.tatatu.model.User

class FollowingAdapter(clickListener: BaseFollowFragment.FollowClickListener):
    BaseRecViewAdapter<User, BaseFollowFragment.FollowClickListener, FollowingAdapter.FollowingViewHolder>(clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FollowingViewHolder(
        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.follow_list_item, parent, false)
    )

    fun updateItem(follower: User) {
        val position = getItems().indexOfFirst {
            it.uid == follower.uid
        }
        //TODO: Sometimes this is called with an invalid item position, causing an exception. Check why is happening
        if (position >= 0) {
            updateAt(position, follower)
        }
    }

    class FollowingViewHolder(val itemBinding: FollowListItemBinding): BaseViewHolder<User, BaseFollowFragment.FollowClickListener>(itemBinding.root) {

        override fun onBind(item: User, listener: BaseFollowFragment.FollowClickListener?) {
            with(itemBinding) {
                listener?.let { listener ->
                    this.user = item
                    setupListUser(followerAvatar, item)
                    setupFollowButtons(item, followStatusButtons, root, listener)
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