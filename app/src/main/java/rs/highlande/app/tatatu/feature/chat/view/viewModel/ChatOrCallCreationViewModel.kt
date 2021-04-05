package rs.highlande.app.tatatu.feature.chat.view.viewModel

import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import io.reactivex.schedulers.Schedulers
import io.realm.RealmList
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_GET_NEW_CALLS
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_GET_NEW_CHATS
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_INITIALIZE_ROOM
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.ui.SearchTextWatcher
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.chat.ChatRoom

class ChatOrCallCreationViewModel(
    val chatRepository: ChatRepository,
    val usersRepository: UsersRepository
): BaseViewModel() {

    enum class ChatOrCallType { CHAT, CALL }

    var chatRoom: ChatRoom? = null
    var selectedUser: User? = null

    var currentPage = 0
    var ignoreSearch: Boolean = false
    var isSearch: Boolean = false
    var cachedOriginalList: MutableList<User>? = null
    var replaceItemsOnNextUpdate: Boolean = false

    val newChatRoomLiveData = MutableLiveData<Boolean>()
    val newChatsLiveData = MutableLiveData<List<User>>()
    val errorLiveData = MutableLiveData<Boolean>()

    val scrollListener = object : EndlessScrollListener() {
        override fun onLoadMore() {
            LogUtils.v("onLoadMore", currentPage)
            replaceItemsOnNextUpdate = false
            getUsersForNewChats()
        }
    }


    var viewType: ChatOrCallType? = null


    fun isScreenForCalls() = viewType == ChatOrCallType.CALL


    fun initializeNewRoom(user: User) {
        RealmUtils.useTemporaryRealm { realm ->
            val participant = usersRepository.getParticipant(realm, user, true)
            participant?.let {
                selectedUser = user
                val ownerID = usersRepository.fetchCachedMyUserId()
                val ids = RealmList<String>()
                ids.add(participant.id)
                val chatRoom = ChatRoom(
                    ownerID,
                    ids,
                    null
                )
                addDisposable(
                    chatRepository.initializeNewRoom(this, chatRoom)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {},
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
        }
    }

    fun getUsersForNewChats(query: String? = null) {
        isSearch = false
        if (replaceItemsOnNextUpdate) currentPage = 0
        LogUtils.v("PAGE", currentPage)
        addDisposable(chatRepository.getUsersForNewChats(this, usersRepository.fetchCachedMyUserId(), query, currentPage, isScreenForCalls())
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace() }
            )
        )
    }

    fun performSearch(query: String) {
        ignoreSearch = false
        isSearch = true
        addDisposable(chatRepository.getUsersForNewChats(this, usersRepository.fetchCachedMyUserId(), query, isCalls = isScreenForCalls())
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace() }
            )
        )
    }

    fun createTextChangeObservable(editText: EditText?, actionsOnStringEmpty: () -> Unit) {
        addDisposable(SearchTextWatcher.createTextChangeObservable(
            editText,
            actionsOnStringEmpty = actionsOnStringEmpty,
            actionsOnValueEmitted = { performSearch(it) },
            tag = logTag
        ))
    }



    fun cancelSearch() {
        ignoreSearch = true
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when(callCode) {
            SERVER_OP_CHAT_GET_NEW_CHATS, SERVER_OP_CHAT_GET_NEW_CALLS -> {
                if (isSearch && ignoreSearch) {
                    isSearch = false
                    return
                }

                val result = mutableListOf<User>()
                if (response != null && response.length() > 0) {
                    for (i in 0 until response.length()) {
                        User.get(response.optJSONObject(i))?.let {
                            result.add(it)
                        }
                    }

                    scrollListener.canFetch = (result.size % PAGINATION_SIZE == 0)

                    newChatsLiveData.postValue(result)
                } else newChatsLiveData.postValue(emptyList())
            }
            SERVER_OP_CHAT_INITIALIZE_ROOM -> {
                val j = response?.optJSONObject(0)
                if (j != null) {
                    chatRoom = ChatRoom.getRoom(j)
                    chatRoom!!.ownerID = usersRepository.fetchCachedMyUserId()
                }
                RealmUtils.useTemporaryRealm { realm ->
                    realm.executeTransaction { async ->
                        async.insertOrUpdate(
                            (chatRoom as ChatRoom)
                                .apply { getParticipant()?.nickname = selectedUser?.username }
                        )
                    }
                }
                selectedUser?.let {
                    cachedOriginalList?.remove(it)
                    newChatsLiveData.postValue(cachedOriginalList)
                }
                newChatRoomLiveData.postValue(chatRoom?.isValid() == true && selectedUser != null)
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        errorLiveData.postValue(true)
    }


    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    companion object {
        val logTag = ChatOrCallCreationViewModel::class.java.simpleName
    }

}