package rs.highlande.app.tatatu.feature.post.like.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostLikeAdapter(val clickListener: BaseFollowFragment.FollowClickListener, val context: Context):
    BaseRecViewAdapter<User, BaseFollowFragment.FollowClickListener, PostLikeAdapter.PostLikeViewHolder>(clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PostLikeViewHolder(
        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.follow_list_item, parent, false))

    fun updateItem(user: User) {
        val position = getItems().indexOfFirst {
            it.uid == user.uid
        }
        //TODO: Sometimes this is called with an invalid item position, causing an exception. Check why is happening
        if (position >= 0) {
            updateAt(position, user)
        }
    }
    inner class PostLikeViewHolder(val itemBinding: FollowListItemBinding): BaseViewHolder<User, BaseFollowFragment.FollowClickListener>(itemBinding.root) {
        override fun onBind(item: User, listener: BaseFollowFragment.FollowClickListener?) {
            with(itemBinding) {
                listener?.let { listener ->
                    this.user = item
                    setupListUser(
                        followerAvatar,
                        item
                    )
                    followerAvatar.followerTTUBalanceTextView.visibility = View.GONE
                    setupFollowButtons(
                        item,
                        followStatusButtons,
                        root,
                        listener
                    )
                    root.setOnClickListener {
                        listener.onItemClick(item)
                    }
                }
                executePendingBindings()
            }
        }

        override fun getImageViewsToRecycle() = setOf(itemBinding.followerAvatar.roundAvatarImageView.picture)

    }

}