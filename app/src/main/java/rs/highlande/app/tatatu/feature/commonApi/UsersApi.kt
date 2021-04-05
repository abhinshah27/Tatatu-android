package rs.highlande.app.tatatu.feature.commonApi

import android.os.Bundle
import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.PreferenceHelper

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-30.
 */
class UsersApi : BaseApi() {

    private val commonApi by inject<CommonApi>()
    private val preferences by inject<PreferenceHelper>()

    fun getMyUser(caller: OnServerMessageReceivedListener): Observable<RequestStatus> {
        val userID = preferences.getUserId()
        return if (!userID.isBlank())
            commonApi.getUser(
                caller,
                preferences.getUserId(),
                arrayOf(
                    CommonApi.UserInfo.GENERIC,
                    CommonApi.UserInfo.DETAIL,
                    CommonApi.UserInfo.BALANCE,
                    CommonApi.UserInfo.PRIVATE
                )
            ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Manage a follower request.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param requestID the post id
     * @param action can be 0 for decline, 1 for authorize, 2 for cancel my outgoing following request and 3 for unfollow
     */
    fun doManageFollowerRequest(
        caller: OnServerMessageReceivedListener,
        requestID: String,
        action: Int
    ): Observable<RequestStatus> {

        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("requestID", requestID)
                    putInt("action", action)
                },
                callCode = SERVER_OP_MANAGE_FOLLOWER_REQUEST,
                logTag = "DO MANAGE FOLLOWER REQUEST call",
                caller = caller
            )
        )
    }

    fun doMakeManageFollowerRequest(
        caller: OnServerMessageReceivedListener,
        userID: String,
        date: String): Observable<RequestStatus> {

        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("userID", userID)
                    putString("date", date)
                },
                callCode = SERVER_OP_MAKE_NEW_FOLLOWER_REQUEST,
                logTag = "DO MAKE FOLLOWER REQUEST call",
                caller = caller
            )
        )
    }

    /**
     * Requests an user follower list.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID the user id for which the list is requested
     * @param skip the number of comments to skip, 0 by default
     * @param limit the number of comments to fetch [PAGINATION_SIZE] by default
     */
    fun doGetFollowers(
        caller: OnServerMessageReceivedListener,
        userID: String,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE): Observable<RequestStatus> {

        return if (userID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    if (!userID.isNullOrBlank()) putString("userID", userID)
                    putInt(
                        "skip",
                        if (skip > -1) skip else 0
                    )
                    putInt(
                        "limit",
                        if (limit <= 0) skip
                        else {
                            PAGINATION_SIZE
                        }
                    )
                },
                callCode = SERVER_OP_GET_FOLLOWERS,
                logTag = "GET FOLLOWERS call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Requests an user following list.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID the user id for which the list is requested
     * @param skip the number of comments to skip, 0 by default
     * @param limit the number of comments to fetch [PAGINATION_SIZE] by default
     */
    fun doGetFollowing(
        caller: OnServerMessageReceivedListener,
        userID: String,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE): Observable<RequestStatus> {

        return if (userID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    if (!userID.isNullOrBlank()) putString("userID", userID)
                    putInt(
                        "skip",
                        if (skip > -1) skip else 0
                    )
                    putInt(
                        "limit",
                        if (limit <= 0) skip
                        else {
                            PAGINATION_SIZE
                        }
                    )
                },
                callCode = SERVER_OP_GET_FOLLOWING,
                logTag = "GET FOLLOWING call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Requests an user follow requests list.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param type the request type 0 for incomming requests and 1 for sent requests
     * @param skip the number of comments to skip, 0 by default
     * @param limit the number of comments to fetch [PAGINATION_SIZE] by default
     */
    fun doGetFollowRequest(
        caller: OnServerMessageReceivedListener,
        type: Int,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE): Observable<RequestStatus> {

        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putInt("type", type)
                    putInt(
                        "skip",
                        if (skip > -1) skip else 0
                    )
                    putInt(
                        "limit",
                        if (limit <= 0) skip
                        else {
                            PAGINATION_SIZE
                        }
                    )
                },
                callCode = SERVER_OP_GET_FOLLOW_REQUESTS,
                logTag = "GET FOLLOWING call",
                caller = caller
            )
        )
    }

    /**
     * Requests the total number of the specified type of follow requests
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param type the request type 0 for incomming requests and 1 for sent requests
     */
    fun doGetTotalFollowRequests(
        caller: OnServerMessageReceivedListener,
        type: Int): Observable<RequestStatus> {

        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putInt("type", type)
                },
                callCode = SERVER_OP_GET_TOTAL_FOLLOW_REQUESTS,
                logTag = "GET FOLLOWING call",
                caller = caller
            )
        )
    }

    /**
     * Performs a search among the user's followers/following
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param text the search query.
     * @param userID will search among the this user following/followers if present
     * @param skip the number of comments to skip, 0 by default
     * @param limit the number of comments to fetch [PAGINATION_SIZE] by default
     * @param action 0 for followers and 1 for following
     */

    fun doSearchFollowingFollowers(
        caller: OnServerMessageReceivedListener,
        text: String,
        userID: String,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE,
        action: Int = 0): Observable<RequestStatus> {

        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    if (!userID.isNullOrEmpty()) putString("userID", userID)
                    putString("text", text)
                    putInt(
                        "skip",
                        if (skip > -1) skip else 0
                    )
                    putInt(
                        "limit",
                        if (limit <= 0) skip
                        else {
                            PAGINATION_SIZE
                        }
                    )
                    putInt("action", action)
                },
                callCode = SERVER_OP_DO_SEARCH_FOLLOWING_FOLLOWERS,
                logTag = "GET FOLLOWING call",
                caller = caller
            )
        )
    }

    /**
     * Performs an user search
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param text the search query.
     */
    fun doSearchUsers(
        caller: OnServerMessageReceivedListener,
        text: String): Observable<RequestStatus> {

        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("text", text)
                },
                callCode = SERVER_OP_DO_SEARCH_USERS,
                logTag = "SEARCH USERS call",
                caller = caller
            )
        )
    }

    /**
     * Reports the specified user
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID will search among the this user following/followers if present
     */
    fun doReportUser(
        caller: OnServerMessageReceivedListener,
        userID: String): Observable<RequestStatus> {

        return if (!userID.isEmpty()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("userID", userID)
                },
                callCode = SERVER_OP_DO_REPORT_USER,
                logTag = "REPORT USER call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)

    }

    /**
     * Shares the specified user profile
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID of the user to be shared
     */
    fun doShareUserProfile(
        caller: OnServerMessageReceivedListener,
        userID: String): Observable<RequestStatus> {

        return if (!userID.isEmpty()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("userID", userID)
                },
                callCode = SERVER_OP_DO_SHARE_USER_PROFILE,
                logTag = "SHARE USER call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)

    }

    /**
     * Adds or remove an user from the blacklist
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID user added or removed from blacklist
     * @param action can be 0 to remove or 1 to add an user to the blacklist
     */
    fun doAddRemoveBlacklist(caller: OnServerMessageReceivedListener, userID: String, action: Int): Observable<RequestStatus> {

        return if (!userID.isEmpty()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("userID", userID)
                    putInt("action", action)
                },
                callCode = SERVER_OP_DO_ADD_REMOVE_BLACKLIST,
                logTag = "REPORT USER call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)

    }

}