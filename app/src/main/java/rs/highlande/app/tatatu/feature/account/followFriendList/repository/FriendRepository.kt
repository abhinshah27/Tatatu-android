package rs.highlande.app.tatatu.feature.account.followFriendList.repository

import android.os.Bundle
import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.account.followFriendList.api.FriendsApi
import rs.highlande.app.tatatu.model.User

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class FriendRepository : BaseRepository() {

    private val mFriendsApi: FriendsApi by inject()

    fun getFriendsList(userID: String, page: Int): Observable<List<User>> {
        return Observable.create { emitter ->
            val followRequestsList = fetchFriendsList()
            val startIndex = page * PAGINATION_SIZE
            val endIndex = startIndex + PAGINATION_SIZE

            if (endIndex > followRequestsList.size) {
                emitter.onNext(followRequestsList.subList(startIndex, followRequestsList.size))
            } else {
                emitter.onNext(followRequestsList.subList(startIndex, endIndex))
            }
            emitter.onComplete()
        }
    }

    fun getInvitesList(userID: String, page: Int): Observable<List<User>> {
        return Observable.create { emitter ->
            val followRequestsList = fetchInvitesList()
            val startIndex = page * PAGINATION_SIZE
            val endIndex = startIndex + PAGINATION_SIZE

            if (endIndex > followRequestsList.size) {
                emitter.onNext(followRequestsList.subList(startIndex, followRequestsList.size))
            } else {
                emitter.onNext(followRequestsList.subList(startIndex, endIndex))
            }
            emitter.onComplete()
        }
    }

    fun getFollowersAndFollowingCount(userID: String): Observable<Map<String, Int>> = Observable.just(mapOf(Pair("friends", fetchFriendsList().size), Pair("invites", fetchInvitesList().size)))

    fun getFriendsSearchResults(userId: String, query: String): Observable<List<User>> {
        val results = fetchFriendsList().filter {
            it.username.contains(query) || it.username.contains(query)
        }
        return Observable.just(results)
    }

    fun getInvitesSearchResults(userId: String, query: String): Observable<List<User>> {
        TODO()
        //TODO
        //        val results = fetchInvitesList().filter {
        //            it.username.contains(query) || it.detailsInfo.email!!.contains(query)
        //        }
        //        return Observable.just(results)
    }


    fun resendInvite(userID: String, requestedUserID: String) = Observable.just(true)

    private fun fetchFriendsList(): List<User> = listOf()

    private fun fetchInvitesList(): List<User> = listOf()

    //do Send Invitation
    fun getUsersInvited(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return mFriendsApi.getUsersInvited(caller, bundle)
    }
}