package rs.highlande.app.tatatu.feature.commonApi

import android.content.Context
import android.os.Bundle
import io.reactivex.Observable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import rs.highlande.app.tatatu.BuildConfig
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.getSecureID
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.core.util.DEFAULT_ELEMENTS_HOME
import rs.highlande.app.tatatu.core.util.JsonToSerialize
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-11.
 */
class CommonApi : BaseApi() {

    @Throws(JSONException::class)
    fun subscribeToSocket(context: Context, userId: String, isChat: Boolean, caller: OnServerMessageReceivedListener):
            Observable<RequestStatus> {

        val bundle = Bundle().apply {
            putString("userID", userId)
            putString("deviceID", getSecureID(context))
            putString("v",  BuildConfig.BASE_VERSION_NAME)
        }

        return tracker.callServer(
            SocketRequest(
                bundle,
                callCode = SERVER_OP_SOCKET_SUBSCR,
                logTag = if (isChat) "SOCKET SUBSCRIPTION CHAT call" else "SOCKET SUBSCRIPTION call",
                name = SERVER_CODE_NAME_SUBSCR,
                caller = caller,
                isChat = isChat
            ),
            isChat
        )
    }


    fun parseWebLink(link: String?, messageID: String?, caller: OnServerMessageReceivedListener):
            Pair<Observable<RequestStatus>, String?> {
        return if (!link.isNullOrBlank()) {

            val bundle = Bundle().apply {
                putString("link", link)
                if (!messageID.isNullOrBlank())
                    putString("messageID",  messageID)
            }

            val req = SocketRequest(
                bundle,
                callCode = SERVER_OP_GET_PARSED_WEB_LINK,
                logTag = "PARSE WEBLINK call",
                caller = caller
            )

            tracker.callServer(req) to req.id
        }
        else Observable.just(RequestStatus.ERROR) to null
    }


    //region == TIMELINE ==

    enum class TimelineType {
        HOME, TIMELINE, PROFILE
    }

    /**
     * Fetches Timeline posts from server.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID the user id to be specified if the requested timeline is someone else's (optional)
     * @param skip the number of items to be skipped for pagination purposes (optional)
     */
    fun getTimeline(
        caller: OnServerMessageReceivedListener,
        userID: String? = null,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE,
        type: TimelineType = TimelineType.TIMELINE,
        isHomePage: Boolean = true
    ): Observable<RequestStatus> {

        return tracker.callServer(
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
                            if (type == TimelineType.HOME) DEFAULT_ELEMENTS_HOME else PAGINATION_SIZE
                        }
                    )
                    putBoolean("isHomePage", isHomePage)
                },
                callCode = SERVER_OP_GET_TIMELINE,
                logTag = "GET TIMELINE call",
                caller = caller
            )
        )
    }

    /**
     * Fetches profile Timeline posts from server.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID the user id to be specified if the requested timeline is someone else's (optional)
     * @param skip the number of items to be skipped for pagination purposes (optional)
     */
    fun getTimelineForProfile(
        caller: OnServerMessageReceivedListener,
        userID: String? = null,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE
    ): Observable<RequestStatus> {

        return tracker.callServer(
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
                            limit
                        }
                    )
                },
                callCode = SERVER_OP_GET_TIMELINE_FOR_PROFILE,
                logTag = "GET PROFILE TIMELINE call",
                caller = caller
            )
        )
    }

    // endregion


    //region == USER ==

    /**
     * Fetches User object composed differently depending on which value of [UserInfo] are given the server.
     * @param caller the [OnServerMessageReceivedListener] listening for the response.
     * @param userID the user id of the user we want the information.
     * @param userInfo the values of [UserInfo].
     */
    fun getUser(
        caller: OnServerMessageReceivedListener,
        userID: String,
        userInfo: Array<UserInfo>
    ): Observable<RequestStatus> {

        return tracker.callServer(
            SocketRequest(
                Bundle().apply { putSerializable("serialized1", UserRequestObject(userID, userInfo)) },
                callCode = SERVER_OP_GET_USER,
                logTag = "GET MY USER call",
                caller = caller
            )
        )

    }

    /**
     * Enum holding the information of the possible values that server can handle to return the wanted user.
     */
    enum class UserInfo(val value: String) {
        GENERIC("MainUserInfo"), DETAIL("DetailsUserInfo"), BALANCE("BalanceUserInfo"), PRIVATE("PrivateUserInfo")
    }

    /**
     * Utility class necessary to serialize a user request body.
     * @param userID the user id of the user we want the information.
     * @param userInfo the values of [UserInfo].
     */
    private data class UserRequestObject(
        val userID: String,
        val userInfo: Array<UserInfo>
    ) : JsonToSerialize {

        override fun serializeToJsonObject(): JSONObject {
            return JSONObject().apply {
                put("userID", userID)
                put("userInfo", JSONArray().apply {
                    for (elem in userInfo) { put(elem.value) }
                })
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as UserRequestObject

            if (!userInfo.contentEquals(other.userInfo)) return false
            if (userID != other.userID) return false

            return true
        }

        override fun hashCode(): Int {
            var result = userInfo.contentHashCode()
            result = 31 * result + (userID.hashCode())
            return result
        }

    }

    //endregion

}