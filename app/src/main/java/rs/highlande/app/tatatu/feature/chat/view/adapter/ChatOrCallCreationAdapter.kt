/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.hasDeviceCamera
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.ItemChatCallCreationBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.model.User


class ChatOrCallCreationAdapter(
    listener: OnChatCallsListener,
    private val isCalls: Boolean = false
): BaseRecViewAdapter<User, ChatOrCallCreationAdapter.OnChatCallsListener, ChatOrCallCreationAdapter.ChatCreationVH>(listener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatCreationVH {
        return ChatCreationVH(
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_chat_call_creation, parent, false)
        )
    }

    override fun getItemId(position: Int): Long {
        return getItems()[position].hashCode().toLong()
    }

    inner class ChatCreationVH(val binding: ItemChatCallCreationBinding): BaseViewHolder<User, OnChatCallsListener>(binding.root) {

        override fun onBind(item: User, listener: OnChatCallsListener?) {
            binding.apply {

                container.roundAvatarImageView.apply {
                    picture.setProfilePicture(item.picture)
                    setOnClickListener {
                        AccountActivity.openProfileFragment(context, item.uid)
                    }
                }
                container.name = item.name
                container.username = item.username

                if (isCalls) {
                    call.visibility = View.VISIBLE
                    videoCall.visibility =
                        if (hasDeviceCamera(binding.root.context)) View.VISIBLE
                        else View.GONE

                    call.setOnClickListener { listener?.onCallClicked(item) }
                    videoCall.setOnClickListener { listener?.onVideoCallClick(item) }
                } else {
                    root.setOnClickListener { listener?.onItemClick(item) }
                }


            }
        }

        override fun getImageViewsToRecycle(): Set<ImageView?> =
            setOf(binding.container.roundAvatarImageView.picture)
    }

    interface OnChatCallsListener: OnItemClickListener<User> {
        fun onCallClicked(user: User)
        fun onVideoCallClick(user: User)
    }

}