package rs.highlande.app.tatatu.feature.account.followFriendList.util

import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import kotlinx.android.synthetic.main.follow_status_buttons.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.CommonRoundAvatarWithUsernameBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.BaseFollowFragment
import rs.highlande.app.tatatu.feature.notification.view.fragment.NotificationFragment
import rs.highlande.app.tatatu.model.DetailUserInfo
import rs.highlande.app.tatatu.model.MainUserInfo
import rs.highlande.app.tatatu.model.Relationship
import rs.highlande.app.tatatu.model.User

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */

fun setupFollowButtons(user: User,followStatusButtonView: View, rootView: View, listener: BaseFollowFragment.FollowClickListener) {
    with(followStatusButtonView) {
        user.detailsInfo.relationship.let {
            followingButton.visibility = View.GONE
            followRelationIcon.visibility = View.GONE
            followRelationButton.visibility = View.GONE
            requestedButton.visibility = View.GONE
            if (it != Relationship.MYSELF) {
                when(it) {
                    Relationship.FOLLOWER -> {
                        followRelationButton.visibility = View.VISIBLE
                    }
                    Relationship.NA -> {
                        followRelationButton.visibility = View.VISIBLE
                        followRelationButton.text = rootView.context.getString(R.string.profile_follow)
                    }
                    Relationship.FRIENDS -> {
                        followRelationIcon.visibility = View.VISIBLE
                    }
                    Relationship.PENDING_FOLLOW -> {
                        requestedButton.visibility = View.VISIBLE
                    }
                    else -> {
                        followingButton.visibility = View.VISIBLE
                    }
                }
                followRelationButton.setOnClickListener {
                    listener.onActionClick(user)
                }
                followingButton.setOnClickListener {
                    listener.onActionClick(user)
                }
                followRelationIcon.setOnClickListener {
                    listener.onActionClick(user)
                }
                requestedButton.setOnClickListener {
                    listener.onActionClick(user)
                }
            }
        }
    }
}
fun setupFollowButtons(userInfo: MainUserInfo, detailsInfo: DetailUserInfo,followStatusButtonView: View, rootView: View, listener: NotificationFragment.NotificationItemClickListener) {
    with(followStatusButtonView) {
        detailsInfo.relationship.let {
            followingButton.visibility = View.GONE
            followRelationIcon.visibility = View.GONE
            followRelationButton.visibility = View.GONE
            requestedButton.visibility = View.GONE
            if (it != Relationship.MYSELF) {
                when(it) {
                    Relationship.FOLLOWER -> {
                        followRelationButton.visibility = View.VISIBLE
                    }
                    Relationship.NA -> {
                        followRelationButton.visibility = View.VISIBLE
                        followRelationButton.text = rootView.context.getString(R.string.profile_follow)
                    }
                    Relationship.FRIENDS -> {
                        followRelationIcon.visibility = View.VISIBLE
                    }
                    Relationship.PENDING_FOLLOW -> {
                        requestedButton.visibility = View.VISIBLE
                    }
                    else -> {
                        followingButton.visibility = View.VISIBLE
                    }
                }
                followRelationButton.setOnClickListener {
                    listener.onActionClick(userInfo.uid, detailsInfo)
                }
                followingButton.setOnClickListener {
                    listener.onActionClick(userInfo.uid, detailsInfo)
                }
                followRelationIcon.setOnClickListener {
                    listener.onActionClick(userInfo.uid, detailsInfo)
                }
                requestedButton.setOnClickListener {
                    listener.onActionClick(userInfo.uid, detailsInfo)
                }
            }
        }
    }
}

fun setupListUser(followerAvatarBinding: CommonRoundAvatarWithUsernameBinding, user: MainUserInfo) {
    with(followerAvatarBinding) {
        roundAvatarImageView.apply {
            picture.setProfilePicture(user.picture)
            celebrityIndicator.visibility = if (user.isCelebrity()) { View.VISIBLE } else { View.GONE }
        }

        // INFO: 2019-08-28    Account verification currently disabled
        userNameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            /*if (user.verified) R.drawable.ic_ttu_badge else*/ 0,
            0
        )

        roundAvatarMomentImageView.visibility = if (user.hasNewMoment) { View.VISIBLE } else { View.INVISIBLE }
    }
}