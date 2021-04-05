package rs.highlande.app.tatatu.core.manager

import org.greenrobot.eventbus.EventBus
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.core.util.nowToDBDate
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.Relationship
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.event.UserFollowEvent

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class RelationshipManager(private val usersRepository: UsersRepository) {

    private enum class FollowerAction {
        DECLINE, AUTHORIZE, CANCEL_OUTGOING_REQUEST, UNFOLLOW
    }


    fun manageRelationship(user: User, caller: OnServerMessageReceivedListener? = null): User {
        user.detailsInfo.relationship = when(user.detailsInfo.relationship) {
            Relationship.FOLLOWER -> {
                manageNewFollowerRequest(caller!!, user)
                if (user.isPrivate()) {
                    Relationship.PENDING_FOLLOW
                } else {
                    EventBus.getDefault().post(UserFollowEvent(1))
                    Relationship.FRIENDS
                }
            }
            Relationship.FRIENDS -> {
                val requestID = user.detailsInfo.requestID
                if (!requestID.isNullOrBlank()) {
                    EventBus.getDefault().post(UserFollowEvent(1, false))
                    manageUnFollowRequest(caller!!, requestID)
                    Relationship.FOLLOWER
                } else {
                    user.detailsInfo.relationship
                }
            }
            Relationship.PENDING_FOLLOW, Relationship.FOLLOWING -> {
                val requestID = user.detailsInfo.requestID
                if (!requestID.isNullOrBlank()) {
                    if (user.detailsInfo.relationship == Relationship.FOLLOWING)
                        EventBus.getDefault().post(UserFollowEvent(1, false))
                    manageUnFollowRequest(caller!!, requestID)
                    Relationship.NA
                } else {
                    user.detailsInfo.relationship
                }
            }
            Relationship.NA -> {
                manageNewFollowerRequest(caller!!, user)
                when {
                    user.isPrivate() -> Relationship.PENDING_FOLLOW
                    user.detailsInfo.relationship == Relationship.FOLLOWER -> {
                        EventBus.getDefault().post(UserFollowEvent(1))
                        Relationship.FRIENDS
                    }
                    else -> {
                        EventBus.getDefault().post(UserFollowEvent(1))
                        Relationship.FOLLOWING
                    }
                }
            }
            Relationship.DECLINED -> TODO() //TODO 31/07: Handle declined
            Relationship.MYSELF -> TODO() //TODO 12/08: Handle myself
        }
        return user
    }

    fun getRelationshipAction(relationship: Relationship): RelationshipAction {
        return when(relationship) {
            Relationship.FOLLOWING,
            Relationship.FRIENDS -> {
                RelationshipAction.FOLLOWING_FRIENDS_ACTION
            }
            Relationship.FOLLOWER -> {
                RelationshipAction.FOLLOW_BACK_ACTION
            }
            Relationship.PENDING_FOLLOW -> {
                RelationshipAction.REQUEST_CANCEL_ACTION
            }
            Relationship.NA -> {
                RelationshipAction.FOLLOW_ACTION
            }
            Relationship.DECLINED -> TODO() //TODO 31/07: Handle declined
            Relationship.MYSELF -> TODO() //TODO 12/08: Handle myself
        }
    }

    private fun updateFollowerRequest(caller: OnServerMessageReceivedListener, requestID: String, followerAction: FollowerAction) {
        usersRepository.manageFollowerRequest(caller, requestID, followerAction.ordinal)
    }

    fun manageDeclineRequest(caller: OnServerMessageReceivedListener, requestID: String) {
        updateFollowerRequest(caller, requestID, FollowerAction.DECLINE)
    }

    fun manageAuthorizeRequest(caller: OnServerMessageReceivedListener, requestID: String) {
        EventBus.getDefault().post(UserFollowEvent(0))

        updateFollowerRequest(caller, requestID, FollowerAction.AUTHORIZE)
    }

    fun manageUnFollowRequest(caller: OnServerMessageReceivedListener, requestID: String) {
        updateFollowerRequest(caller, requestID, FollowerAction.UNFOLLOW)
    }

    fun manageNewFollowerRequest(caller: OnServerMessageReceivedListener, user: User) {
        usersRepository.manageNewFollowerRequest(caller, user.uid, nowToDBDate())
    }

    fun manageNewFollowerRequest(caller: OnServerMessageReceivedListener, userID: String) {
        if (userID.isNotBlank()) {
            EventBus.getDefault().post(UserFollowEvent(1))
            usersRepository.manageNewFollowerRequest(caller, userID, nowToDBDate())
        }
    }

    fun manageCancelOutgoingRequest(caller: OnServerMessageReceivedListener, user: User): User  {
        user.detailsInfo.requestID?.let {
            updateFollowerRequest(caller, it, FollowerAction.CANCEL_OUTGOING_REQUEST)
            user.detailsInfo.relationship = Relationship.NA
        }
        return user
    }

    fun isFriendOrMyself(it: User) = (it.detailsInfo.relationship == Relationship.FRIENDS || it.detailsInfo.relationship == Relationship.MYSELF)

}

enum class RelationshipAction {
    FOLLOWING_FRIENDS_ACTION, FOLLOW_BACK_ACTION, REQUEST_CANCEL_ACTION, FOLLOW_ACTION, UNFOLLOW
}