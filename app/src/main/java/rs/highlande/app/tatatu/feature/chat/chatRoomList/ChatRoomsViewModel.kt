package rs.highlande.app.tatatu.feature.chat.chatRoomList

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.realm.*
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_DELETE_ROOM
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_EXPORT_CHATS
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_UPDATE_LIST
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.chat.view.ChatEditEvent
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatRoom
import rs.highlande.app.tatatu.model.chat.Participant
import java.util.*

class ChatRoomsViewModel(val chatRepository: ChatRepository, val usersRepository: UsersRepository): BaseViewModel() {

    private var activitiesMap = mutableMapOf<String, String>()

    val roomDeletedLiveData = MutableLiveData<Pair<Boolean, String>>()
    val chatListUpdatedLiveData = MutableLiveData<Boolean>()
    val genericErrorLiveData = MutableLiveData<Boolean>()
    val chatEditModeUpdate = MutableLiveData<Pair<Boolean, Int>>()
    val chatExportResultLiveData = MutableLiveData<Boolean>()

    val viewModelRealm: Realm = RealmUtils.checkedRealm!!

    val selectedChats = mutableSetOf<String>()
    var editMode: Boolean = false
        set(value) {
            field = value
            selectedChats.clear()
            chatEditModeUpdate.postValue(false to 0)
        }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun updateRoomsList() {
        addDisposable(chatRepository.updateRoomsList(this, usersRepository.fetchCachedMyUserId())
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace() }
            )
        )
    }

    fun deleteRoom(chatForDeletion: String) {
        if (chatForDeletion == null) return
        addDisposable(chatRepository.deleteRoom(this, usersRepository.fetchCachedMyUserId(), chatForDeletion)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace() }
            )
        )
    }

    fun exportChats() {
        addDisposable(chatRepository.exportChat(this, ArrayList(selectedChats))
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace() }
            ))
    }

    fun updateRoomText(room: ChatRoom, text: String, creationDateObj: Date) {
        RealmUtils.useTemporaryRealm {
            it.executeTransaction {
                room.text = text
                room.dateObj = creationDateObj
            }
        }
    }

    fun updateRoomStatus(room: ChatRoom?, status: Int, date: String) {
        RealmUtils.useTemporaryRealm {
            it.executeTransaction {
                room?.participants?.get(0)?.lastSeenDate = date
                room?.participants?.get(0)?.chatStatus = status
                room?.recipientStatus = status
            }
        }
    }

    fun updateRoomActivity(room: ChatRoom?, chatId: String, activity: String) {
        val previousActivity = activitiesMap[chatId]

        RealmUtils.useTemporaryRealm {
            it.executeTransaction {
                if (!activity.isBlank()) {
                    activitiesMap[chatId] = room?.text ?: ""
                    room?.text = activity
                }
                else if (!previousActivity.isNullOrBlank())
                    room?.text = previousActivity
            }
        }
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        when (callCode) {
            SERVER_OP_CHAT_UPDATE_LIST -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            RealmUtils.useTemporaryRealm { realm ->
                                if (it!!.length() == 0 || it.optJSONObject(0) == null)
                                    genericErrorLiveData.postValue(true)
                                realm.executeTransactionAsync { async ->
                                    ChatRoom.handleDeletedRooms(async, it)

                                    for (i in 0 until it.length()) {
                                        val room = ChatRoom.getRoom(it.optJSONObject(i))
                                        if (room!!.isValid()) {
                                            async.insertOrUpdate(room)
                                        }
                                    }
                                    chatListUpdatedLiveData.postValue(true)
                                }
                            }
                        },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
            SERVER_OP_CHAT_DELETE_ROOM -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            val chatRoomID = it?.getJSONObject(0)?.getString("_id") ?: return@subscribe
                            roomDeletedLiveData.postValue(true to chatRoomID)
                        },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
            SERVER_OP_CHAT_EXPORT_CHATS -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe({
                        chatExportResultLiveData.postValue(true)
                    },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
        }
    }

    fun handleSelectedRoomsDeletion() {
        if (selectedChats.isEmpty()) {
            EventBus.getDefault().post(ChatEditEvent(error = true))
            return
        }
        addDisposable(Observable.just(deleteSelectedRooms())
            .observeOn(Schedulers.computation())
            .subscribeOn(Schedulers.computation())
            .subscribe({}, { thr -> thr.printStackTrace() })
        )
    }

    fun deleteRoomLocally(chatRoomID: String) {
        viewModelRealm.executeTransaction { realm ->
            val room =
                RealmUtils.readFirstFromRealmWithId(
                    realm,
                    ChatRoom::class.java,
                    "chatRoomID",
                    chatRoomID
                ) as? ChatRoom

            room?.let { room ->
                room.participantIDs.forEach { id ->
                    RealmUtils
                        .readFromRealmWithId(
                            realm,
                            Participant::class.java,
                            "id",
                            id
                        )
                        ?.deleteAllFromRealm()
                }
                RealmObject.deleteFromRealm(room)
                RealmUtils
                    .readFromRealmWithId(
                        realm,
                        ChatMessage::class.java,
                        "chatRoomID",
                        chatRoomID
                    )
                    ?.deleteAllFromRealm()
            }
        }
    }

    fun handleSelectedRoomsExport() {
        if (selectedChats.isEmpty()) {
            EventBus.getDefault().post(ChatEditEvent(error = true))
            return
        }
        exportChats()
    }

    private fun deleteSelectedRooms() {
        selectedChats.forEach { chatRoomID ->
            deleteRoom(chatRoomID)
        }
        selectedChats.clear()
    }

    fun getRooms(query: String? = null): OrderedRealmCollection<ChatRoom> {
        return chatRepository.getChatRooms(viewModelRealm, query)
    }

    fun getFilterQuery(text: String): RealmQuery<ChatRoom>? {
        val realm = RealmUtils.checkedRealm ?: return null
        val query = realm.where(ChatRoom::class.java)
        if(!text.toLowerCase().trim().isBlank()) {
            query.contains("roomName", text, Case.INSENSITIVE)
        }
        return query
    }

    fun addChatToSelection(chatRoomID: String) {
        selectedChats.add(chatRoomID)
        chatEditModeUpdate.postValue(editMode to selectedChats.size)
    }

    fun removeChatFromSelection(chatRoomID: String) {
        selectedChats.remove(chatRoomID)
        chatEditModeUpdate.postValue(editMode to selectedChats.size)
    }

    override fun onCleared() {
        RealmUtils.closeRealm(viewModelRealm)
        super.onCleared()
    }

}