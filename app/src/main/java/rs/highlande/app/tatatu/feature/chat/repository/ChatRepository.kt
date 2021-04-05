package rs.highlande.app.tatatu.feature.chat.repository

import io.reactivex.Observable
import io.realm.*
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatRoom
import rs.highlande.app.tatatu.model.chat.Participant
import java.util.*

class ChatRepository: BaseRepository() {

    private val chatApi: ChatApi by inject()

    fun sendMessage(caller: OnServerMessageReceivedListener, message: ChatMessage): Observable<RequestStatus> {
        return chatApi.sendMessage(caller, message)
    }

    fun updateRoomsList(caller: OnServerMessageReceivedListener, userId: String): Observable<RequestStatus> {
        return chatApi.updateRoomsList(caller, userId)
    }

    fun fetchMessages(caller: OnServerMessageReceivedListener, unixTS: Long, chatRoomId: String,
                      after: ChatApi.FetchDirection): Observable<RequestStatus> {
        return chatApi.fetchMessages(caller, unixTS, chatRoomId, after)
    }

    fun fetchLocalMessages(realm: Realm, chatRoomId: String): RealmResults<ChatMessage> {
        return RealmUtils
            .readFromRealmWithIdSorted(
                realm,
                ChatMessage::class.java,
                "chatRoomID",
                chatRoomId,
                "creationDateObj",
                Sort.DESCENDING
            ) as RealmResults<ChatMessage>
    }

    fun sendChatHack(caller: OnServerMessageReceivedListener, delivery: ChatApi.HackType,
                     userId: String, chatRoomId: String): Observable<RequestStatus> {
        return chatApi.sendChatHack(caller, delivery, userId, chatRoomId)
    }

    fun setUserActivity(caller: OnServerMessageReceivedListener, userId: String, chatRoomId: String, activity: String? = null): Observable<RequestStatus> {
        return chatApi.setUserActivity(caller, userId, chatRoomId, activity)
    }

    fun setMessageRead(caller: OnServerMessageReceivedListener, uid: String, chatRoomId: String): Observable<RequestStatus> {
        return chatApi.setMessageRead(caller, uid, chatRoomId)
    }

    fun initializeNewRoom(caller: OnServerMessageReceivedListener, room: ChatRoom): Observable<RequestStatus>  {
        return chatApi.initializeNewRoom(caller, room)
    }

    fun getUsersForNewChats(caller: OnServerMessageReceivedListener,
                            userId: String,
                            text: String? = null,
                            page: Int = 0,
                            isCalls: Boolean): Observable<RequestStatus> {
        return chatApi.getUsersForNewChats(caller, userId, text, page * PAGINATION_SIZE, isCalls = isCalls)
    }

    fun getValidRoom(realm: Realm, chatRoomId: String): ChatRoom? {
        return RealmUtils.readFirstFromRealmWithId(realm, ChatRoom::class.java, "chatRoomID", chatRoomId) as? ChatRoom
    }

    fun saveMessage(realm: Realm, newMessage: ChatMessage) {
        realm.executeTransaction {
            it.insertOrUpdate(newMessage)
        }
    }

    fun updateChatRoomText(realm: Realm, chatRoom: ChatRoom, text: String) {
        realm.executeTransaction {
            chatRoom.text = text
            it.insertOrUpdate(chatRoom)
        }
    }

    fun updateChatRoomOwnerId(realm: Realm, chatRoom: ChatRoom, ownerID: String) {
        realm.executeTransaction {
            chatRoom.ownerID = ownerID
            it.insertOrUpdate(chatRoom)
        }
    }

    fun saveParticipant(realm: Realm, participant: Participant) {
        realm.executeTransaction {
            LogUtils.d("PARTICIPANTSAVED", it.copyToRealmOrUpdate(participant).toString())
        }
    }

    fun deleteRoom(caller: OnServerMessageReceivedListener, userId: String, chatRoomId: String): Observable<RequestStatus> {
        return chatApi.deleteRoom(caller, userId, chatRoomId)
    }

    fun exportChat(caller: OnServerMessageReceivedListener, chatIDs: ArrayList<String>): Observable<RequestStatus> {
        return chatApi.requestChatExport(caller, chatIDs)
    }

    fun getChatRooms(realm: Realm, searchString: String?): OrderedRealmCollection<ChatRoom> {
        return if (searchString.isNullOrBlank()) {
            RealmUtils
                .readFromRealmSorted(
                    realm,
                    ChatRoom::class.java,
                    "dateObj",
                    Sort.DESCENDING
                ) as RealmResults<ChatRoom>
        }
        else {
            realm.where(ChatRoom::class.java)
                .contains("roomName", searchString.toLowerCase(Locale.getDefault()).trim(), Case.INSENSITIVE)
                .sort("dateObj", Sort.DESCENDING)
                .findAll()
        }
    }

    fun setMessageThumbnail(realm: Realm, messageID: String, thumb: String) {
        realm.executeTransactionAsync {
            (RealmUtils
                .readFirstFromRealmWithId(
                    it,
                    ChatMessage::class.java,
                    "messageID",
                    messageID
                ) as? ChatMessage)?.videoThumbnail = thumb
        }
    }

    fun getToReadCount(): Int {
        var result = 0
        RealmUtils.useTemporaryRealm {
            result = RealmUtils.readFromRealm(it, ChatRoom::class.java)?.sum("toRead")?.toInt() ?: 0
        }
        return result
    }


}