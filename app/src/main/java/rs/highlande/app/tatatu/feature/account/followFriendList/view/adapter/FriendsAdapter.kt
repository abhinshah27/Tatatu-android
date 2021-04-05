package rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.formatTTUTokens
import rs.highlande.app.tatatu.databinding.FriendsListItemBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.util.setupListUser
import rs.highlande.app.tatatu.model.AccountType
import rs.highlande.app.tatatu.model.FriendsUser
import rs.highlande.app.tatatu.model.MainUserInfo

class FriendsAdapter(clickListener: OnItemClickListener<FriendsUser>) : BaseRecViewAdapter<FriendsUser, OnItemClickListener<FriendsUser>, FriendsAdapter.FriendsViewHolder>(clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FriendsViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.friends_list_item, parent, false))

    class FriendsViewHolder(val itemBinding: FriendsListItemBinding) : BaseViewHolder<FriendsUser, OnItemClickListener<FriendsUser>>(itemBinding.root) {

        override fun onBind(item: FriendsUser, listener: OnItemClickListener<FriendsUser>?) {
            with(itemBinding) {
                item.mainUserInfo?.let {
                    val mainUserInfo = MainUserInfo().apply {
                        picture = it.picture!!
                        accountType = AccountType.toEnum(it.accountType)
                        verified = it.verified!!
                    }
                    followerAvatar.name = it.name
                    followerAvatar.username = it.username
                    setupListUser(followerAvatar, mainUserInfo)
                }
                followerTTUBalanceTextView.text = formatTTUTokens(item.balanceUserInfo?.balance!!)

                listener?.let {
                    root.setOnClickListener { listener.onItemClick(item) }
                }
                executePendingBindings()
            }
        }


        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf(itemBinding.followerAvatar.roundAvatarImageView.picture)
        }
    }

}