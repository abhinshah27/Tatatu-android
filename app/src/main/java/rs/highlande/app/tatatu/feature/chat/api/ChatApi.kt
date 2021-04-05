package rs.highlande.app.tatatu.feature.chat.api

import android.os.Bundle
import io.reactivex.Observable
import org.json.JSONException
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.nowToDBDate
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatRoom
import java.util.*

/**
 * @author mbaldrighi on 10/29/2018.
 */
class ChatApi: BaseApi() {

    fun initializeNewRoom(caller: OnServerMessageReceivedListener, room: ChatRoom): Observable<RequestStatus> {
        if (room.ownerID.isNullOrEmpty() || room.participantIDs.isNullOrEmpty())
            return Observable.just(RequestStatus.ERROR)
        //TODO: Add Observable.just(RequestStatus.ERROR)
        return tracker.callServerChat(getChatRequestObject(Bundle().apply {
            putString("ownerID",room.ownerID)
            putString("date",room.date)
            if (!room.participantIDs.isEmpty()) {
                putStringArrayList("participantIDs", arrayListOf<String>().apply {
                    room.participantIDs.forEach {
                        add(it)
                    }
                })
            }

        },
            callCode = SERVER_OP_CHAT_INITIALIZE_ROOM,
            logTag = "INITIALIZE NEW ROOM call",
            caller = caller
        ))
    }

    fun sendMessage(caller: OnServerMessageReceivedListener, message: ChatMessage): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putSerializable("serialized1", message)
            },
                callCode = SERVER_OP_CHAT_SEND_MESSAGE,
                logTag = "SEND NEW MESSAGE call",
                caller = caller
            )
        )
    }

    fun updateRoomsList(caller: OnServerMessageReceivedListener, userId: String): Observable<RequestStatus> {
        if (userId.isNullOrEmpty()) return Observable.just(RequestStatus.ERROR)


        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putString("userID", userId)
            },
                callCode = SERVER_OP_CHAT_UPDATE_LIST,
                logTag = "UPDATE CHATS LIST call",
                caller = caller
            )
        )
    }

    @Throws(JSONException::class)
    fun deleteRoom(caller: OnServerMessageReceivedListener, userId: String, chatRoomId: String): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putString("userID", userId)
                putString("chatRoomID", chatRoomId)
            },
                callCode = SERVER_OP_CHAT_DELETE_ROOM,
                logTag = "DELETE ROOM call",
                caller = caller
            )
        )
    }

    enum class FetchDirection(val value: Int) { BEFORE(0), AFTER(1)}

    fun fetchMessages(caller: OnServerMessageReceivedListener, unixTimeStamp: Long, chatRoomId: String, dir: FetchDirection): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putLong("unixtimestamp", unixTimeStamp)
                putString("chatRoomID", chatRoomId)
                putInt("direction", dir.value)
            },
                callCode = SERVER_OP_CHAT_FETCH_MESSAGES,
                logTag = "FETCH MESSAGES call",
                caller = caller
            )
        )
    }

    enum class HackType(val value: Int) { DELIVERY(0), DOC_OPENED(2) }

    fun sendChatHack(caller: OnServerMessageReceivedListener, hackType: HackType, userId: String, chatRoomId: String, messageID: String? = null): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putInt("ackType", hackType.value)
                putString("userID", userId)
                putString("roomID", chatRoomId)
                if (!messageID.isNullOrBlank()) putString("messageID", messageID)
            },
                callCode = SERVER_OP_CHAT_HACK,
                logTag = "SEND HACK of type ${if (hackType == HackType.DELIVERY) "DELIVERY" else "null"} call",
                caller = caller
            )
        )
    }

    fun setUserActivity(caller: OnServerMessageReceivedListener, participantId: String, chatRoomId: String, activity: String? = null): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putString("userID", participantId)
                putString("chatID", chatRoomId)
                if (!activity.isNullOrBlank()) putString("activity", activity)
            },
                callCode = SERVER_OP_CHAT_SEND_USER_ACTIVITY,
                logTag = "SEND USER ACTIVITY call",
                caller = caller
            )
        )
    }

    fun setMessageRead(caller: OnServerMessageReceivedListener, participantId: String, chatRoomId: String): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putString("userID", participantId)
                putString("chatRoomID", chatRoomId)
                putString("date", nowToDBDate())
            },
                callCode = SERVER_OP_CHAT_SET_MESSAGES_READ,
                logTag = "SET MESSAGES READ FOR ROOM call",
                caller = caller
            )
        )
    }

    fun setUserOnline(caller: OnServerMessageReceivedListener, userId: String): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putString("userID", userId)
            },
                callCode = SERVER_OP_CHAT_SET_USER_ONLINE,
                logTag = "SET USER ONLINE call",
                caller = caller
            )
        )
    }

    fun getUsersForNewChats(
        caller: OnServerMessageReceivedListener,
        userId: String,
        text: String? = null,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE,
        isCalls: Boolean
    ): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putString("userID", userId)
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
                if (!text.isNullOrEmpty()) putString("text", text)
            },
                callCode = if (isCalls) SERVER_OP_CHAT_GET_NEW_CALLS else SERVER_OP_CHAT_GET_NEW_CHATS,
                logTag = if (isCalls) "GET NEW CALLS call" else "GET NEW CHATS call",
                caller = caller
            )
        )
    }

    fun requestChatExport(
        caller: OnServerMessageReceivedListener,
        chatIDs: ArrayList<String>
    ): Observable<RequestStatus> {
        return tracker.callServerChat(
            getChatRequestObject(Bundle().apply {
                putStringArrayList("chatIDs", chatIDs)
            },
                callCode = SERVER_OP_CHAT_EXPORT_CHATS,
                logTag = "EXPORT CHATS call",
                caller = caller
            )
        )
    }


    companion object {

        /**
         * Retrieves a [SocketRequest] created ad hoc to channel a CHAT request, overriding [SocketRequest]
         * constructor and passing [SocketRequest.isChat] `true` by default.
         * All params as per [SocketRequest].
         */
        fun getChatRequestObject(
            vararg bundles: Bundle?,
            callCode: Int,
            logTag: String,
            name: String = SERVER_CODE_NAME,
            dbName: String = SERVER_CODE_DB_NAME,
            caller: OnServerMessageReceivedListener?
        ): SocketRequest {

            return SocketRequest(
                bundles = *bundles,
                callCode = callCode,
                logTag = logTag,
                name = name,
                dbName = dbName,
                caller = caller,
                isChat = true
            )

        }

    }

}