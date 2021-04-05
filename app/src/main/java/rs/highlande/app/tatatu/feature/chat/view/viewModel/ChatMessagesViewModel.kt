package rs.highlande.app.tatatu.feature.chat.view.viewModel

import android.app.Application
import android.net.Uri
import android.view.MotionEvent
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import org.json.JSONArray
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_FETCH_MESSAGES
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_SEND_MESSAGE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_SET_MESSAGES_READ
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_UPDATE_LIST
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.ActionType
import rs.highlande.app.tatatu.feature.chat.AudioRecordingHelper
import rs.highlande.app.tatatu.feature.chat.MediaUploadManager
import rs.highlande.app.tatatu.feature.chat.MediaUploadManagerListener
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.chat.view.ChatMessagesFragment
import rs.highlande.app.tatatu.feature.commonRepository.UploadingInterFace
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatMessageType
import rs.highlande.app.tatatu.model.chat.ChatRoom
import rs.highlande.app.tatatu.model.createPost.uploadImage.UploadMediaResponse
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ChatMessagesViewModel(
    application: Application,
    val chatRepository: ChatRepository,
    val sharedPreferences: PreferenceHelper,
    val resourceHelper: ResourcesHelper
):
    BaseAndroidViewModel(application),
    AudioRecordingHelper.RecordingCallbacks,
    MediaUploadManagerListener,
    UploadingInterFace {

    var participantId: String? = null
        get() {
            if (field == null) {
                RealmUtils.useTemporaryRealm {
                    field = chatRepository.getValidRoom(it, chatRoomId!!)?.participantIDs?.first()
                }
            }
            return field
        }

    var participantAvatar: String? = null
        get() {
            if (field == null) {
                RealmUtils.useTemporaryRealm {
                    field = chatRepository.getValidRoom(it, chatRoomId!!)?.participants?.first()?.avatarURL
                }
            }
            return field
        }

    var participantName: String? = null
        get() {
            if (field == null) {
                RealmUtils.useTemporaryRealm {
                    field = chatRepository.getValidRoom(it, chatRoomId!!)?.participants?.first()?.name
                }
            }
            return field
        }

    var participantUsername: String? = null
        get() {
            if (field == null) {
                RealmUtils.useTemporaryRealm {
                    field = chatRepository.getValidRoom(it, chatRoomId!!)?.getRoomName()
                }
            }
            return field
        }

    val user: User? by lazy {
        sharedPreferences.getUser()
    }

    val mediaUploadManager = MediaUploadManager(this, this)

    var chatRoomId: String? = null
    var chatRoomMainThread: ChatRoom? = null
    var currentMessageId: String? = null
    private var itemsCount = 0

    var scrollListener = object: EndlessScrollListener(type = Type.CHAT) {
        override fun onLoadMore() {
            fetchMessages()
        }
    }

    val viewModelRealm: Realm = RealmUtils.checkedRealm!!

    lateinit var audioHelper: AudioRecordingHelper

    var isCancelingAudio = false

    val chatRoomUpdateLiveData = MutableLiveData<Pair<Int, String>>()
    val messageUpdateLiveData = MutableLiveData<ChatRoom>()
    val chatsToReadUpdateLiveData = MutableLiveData<Int>()
    val genericErrorLiveData = MutableLiveData<Boolean>()
    val updateListErrorLiveData = MutableLiveData<Boolean>()
    val chatRecordingAudioLiveData = MutableLiveData<AudioRecordingStatus>()
    val newMessageReadyLiveData = MutableLiveData<ChatMessage>()
    val uploadMediaProgressUpdateLiveData: MutableLiveData<Triple<Boolean, ActionType?, Long>> = mutableLiveData(Triple(false, null, 0L))
    val uploadMediaFinishLiveData: MutableLiveData<Pair<Boolean, Boolean>> = mutableLiveData(false to false)

    var exceptionCaught = false

    val messageObservable: ObservableField<String> = ObservableField("")


    var playingVideos = ConcurrentHashMap<String, Long>()




    fun fetchMessages(fetchDirection: ChatApi.FetchDirection = ChatApi.FetchDirection.BEFORE,
                      fromServer: Boolean = true): RealmResults<ChatMessage>? {
        chatRoomId?: return null

        var messages: RealmResults<ChatMessage>? = null
        RealmUtils.useTemporaryRealm {
            if (fromServer) {
                addDisposable(
                    chatRepository.fetchMessages(
                        this,
                        ChatRoom.getRightTimeStamp(chatRoomId!!, it, fetchDirection)?: 0L,
                        chatRoomId!!, fetchDirection)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {},
                            { thr -> thr.printStackTrace() }
                        )
                )
            } else {
                messages = chatRepository.fetchLocalMessages(viewModelRealm, chatRoomId!!)

                if (messages?.isEmpty() == true) {
                    addDisposable(
                        chatRepository.fetchMessages(
                            this,
                            ChatRoom.getRightTimeStamp(chatRoomId!!, it, fetchDirection)?: 0L,
                            chatRoomId!!, fetchDirection)
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

        return messages
    }

    fun sendMessage(chatMessage: ChatMessage) {
        if (user == null) return
        if (chatMessage.getDirectionType(user!!.uid) == ChatMessage.DirectionType.OUTGOING) {
            addDisposable(
                chatRepository
                    .sendMessage(
                        this,

                        // INFO: 2020-01-16    Gson cannot serialize Realm-managed objects
                        if (RealmObject.isManaged(chatMessage))
                            viewModelRealm.copyFromRealm(chatMessage)
                        else chatMessage

                    )
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {},
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    fun uploadMessageMedia(mediaPath: String, isVideo: Boolean = false) {
        mediaUploadManager.getUploadMedia(getApplication(), mediaPath, isVideo = isVideo)
    }

    fun setMessageRead(setIncomingRT: Boolean = false) {
        if (user == null || chatRoomId == null) return

        if (!chatRoomId.isNullOrBlank() && (setIncomingRT || ChatRoom.areThereUnreadMessages(user!!.uid, chatRoomId!!, viewModelRealm))) {
            addDisposable(chatRepository.setMessageRead(this, participantId!!, chatRoomId!!)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
            )
        }
    }

    fun setUserActivity(activity: String? = null) {
        if (user == null || chatRoomId == null) return
        addDisposable(chatRepository.setUserActivity(this, user!!.uid, chatRoomId!!, activity)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace() }
            )
        )
    }

    fun updateRoomsList() {
        if (user == null) return
        addDisposable(chatRepository.updateRoomsList(this, user!!.uid)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace() }
            )
        )
    }

    fun createOutgoingMessage(mediaPayload: String = "", mediaType: Int = 0): ChatMessage? {
        if (participantId == null || messageObservable.get() == null) return null

        var newMessage: ChatMessage? = null
        RealmUtils.useTemporaryRealm { realm ->
            newMessage = ChatMessage().getNewOutgoingMessage(
                user?.uid,
                participantId,
                chatRoomId,
                messageObservable.get()!!,
                mediaType,
                mediaPayload
            )

            chatRepository.saveMessage(realm, newMessage!!)
        }

        return newMessage
    }

    fun updateChatRoom(newMessage: ChatMessage) {
        val text = when (newMessage.getMessageType()) {
            ChatMessageType.TEXT -> {
                if (newMessage.hasWebLink()) {
                    "" //TODO  choose string
                } else newMessage.text
            }
//            ChatMessageType.AUDIO -> resourceHelper.getString(R.string.chat_room_first_line_audio_out)
            ChatMessageType.VIDEO -> resourceHelper.getString(R.string.chat_room_first_line_video)
            ChatMessageType.PICTURE -> resourceHelper.getString(R.string.chat_room_first_line_picture)
//            ChatMessageType.LOCATION -> resourceHelper.getString(R.string.chat_room_first_line_location_out)
//            ChatMessageType.DOCUMENT -> resourceHelper.getString(R.string.chat_room_first_line_document_out)
            ChatMessageType.MISSED_VIDEO -> {
                if (newMessage.isIncoming(user?.uid)) {
                    resourceHelper.getString(
                        R.string.chat_text_incoming_missed_video,
                        participantId
                    )
                } else {
                    resourceHelper.getString(R.string.chat_text_outgoing_missed_video)
                }
            }
            ChatMessageType.MISSED_VOICE -> {
                if (newMessage.isIncoming(user?.uid)) {
                    resourceHelper.getString(
                        R.string.chat_text_incoming_missed_voice,
                        participantId
                    )
                } else {
                    resourceHelper.getString(R.string.chat_text_outgoing_missed_voice)
                }
            }
            else -> ""
        }

        if (!chatRoomId.isNullOrBlank()) {
            RealmUtils.useTemporaryRealm {
                chatRepository.getValidRoom(it, chatRoomId!!)?.let { room ->
                    chatRepository.updateChatRoomText(it, room, text)
                }
            }
        }

        newMessageReadyLiveData.postValue(newMessage)
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when(callCode) {
            SERVER_OP_CHAT_FETCH_MESSAGES -> {
                addDisposable(Observable.just(response)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {
                            RealmUtils.useTemporaryRealm { realm ->
                                itemsCount = it!!.length()
                                scrollListener.canFetch = (itemsCount == PAGINATION_SIZE)
                                realm.executeTransactionAsync { realm ->
                                    for (i in 0 until it.length()) {
                                        val j = it.optJSONObject(i)
                                        if (j != null && j.length() > 0) {
                                            val message = ChatMessage.getMessage(j)
                                            realm.insertOrUpdate(message)
                                        }
                                    }
                                }
                            }
                        },
                        { thr -> thr.printStackTrace()}
                    )
                )
            }
            SERVER_OP_CHAT_UPDATE_LIST -> {
                addDisposable(Observable.just(response)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {
                            if(it!!.length() == 0) {
                                updateListErrorLiveData.postValue(true)
                            } else {
                                RealmUtils.useTemporaryRealm { realm ->
                                    realm.executeTransactionAsync { realm ->
                                        for (i in 0 until it.length()) {
                                            val j = it.optJSONObject(i)
                                            if (j != null && j.length() > 0) {
                                                val room = ChatRoom.getRoom(j)
                                                room?.let { room ->
                                                    room.ownerID = user!!.uid
                                                    realm.insertOrUpdate(room)
                                                    if (room.chatRoomID == chatRoomId) {
                                                        chatRepository.getValidRoom(realm, chatRoomId!!)?.let {
                                                            chatRoomUpdateLiveData.postValue(it.recipientStatus to it.date)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        { thr -> thr.printStackTrace()}
                    )
                )
            }
            SERVER_OP_CHAT_SEND_MESSAGE -> {
                addDisposable(Observable.just(response)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {
                            RealmUtils.useTemporaryRealm { realm ->
                                val jsonObj = it!!.optJSONObject(0)
                                val id = jsonObj?.optString("messageID")
                                val unixTS = jsonObj?.optLong("unixtimestamp")
                                if (!id.isNullOrBlank()) {
                                    realm.executeTransaction {
                                        val message = RealmUtils.readFirstFromRealmWithId(
                                            it,
                                            ChatMessage::class.java,
                                            "messageID",
                                            id
                                        ) as? ChatMessage
                                        message?.unixtimestamp = unixTS
                                        message?.sentDateObj = Date(unixTS!!)
                                    }
                                }
                            }
                        },
                        { thr -> thr.printStackTrace()}
                    )
                )
            }

            SERVER_OP_CHAT_SET_MESSAGES_READ -> {
                addDisposable(Observable.just(response)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {
                            RealmUtils.useTemporaryRealm { realm ->
                                if (!chatRoomId.isNullOrBlank()) {
                                    chatRepository.getValidRoom(realm, chatRoomId!!)?.let { room ->
                                        realm.executeTransaction { room.toRead = 0 }
                                    }
                                }

                                val chats = RealmUtils.readFromRealm(realm, ChatRoom::class.java)
                                var toRead = 0
                                if (chats != null && !chats.isEmpty()) {
                                    chats.forEach {
                                        if (it is ChatRoom) toRead += it.toRead }
                                }
                                chatsToReadUpdateLiveData.postValue(toRead)
                            }
                        },
                        { thr -> thr.printStackTrace()}
                    )
                )
            }
        }
    }

    override fun handleErrorResponse(
        idOp: String,
        callCode: Int,
        errorCode: Int,
        description: String?
    ) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when(callCode) {
            SERVER_OP_CHAT_SEND_MESSAGE -> {
                // TODO: 10/29/2018    build some retry logic + some UI reaction/interaction
            }

            SERVER_OP_CHAT_SET_MESSAGES_READ -> LogUtils.d(ChatMessagesFragment.logTag, "Error setting all messages read for ChatRoom (id: $chatRoomId")
        }
    }


    /**
     * Refreshes [ChatRoom] instance if necessary
     */
    fun getValidRoom(): ChatRoom? {

        var room: ChatRoom? = null
        RealmUtils.useTemporaryRealm {
            if (!chatRoomId.isNullOrBlank())
                room = chatRepository.getValidRoom(it, chatRoomId!!)
        }

        return room
    }


    /**
     * Refreshes [ChatRoom] instance if necessary
     */
    // TODO: 2019-12-29    to be re-thought, because in old codebase everything was basically done in main thread
    fun getValidRoomMainThread(): ChatRoom? {
        if (chatRoomId.isNullOrBlank()) return null

        if (chatRoomMainThread == null || !RealmObject.isValid(chatRoomMainThread!!))
            chatRoomMainThread = chatRepository.getValidRoom(viewModelRealm, chatRoomId!!)
        return chatRoomMainThread
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun setMessageThumbnail(thumb: String) {
        if (currentMessageId == null) return
        addDisposable(Observable.just(chatRepository.setMessageThumbnail(viewModelRealm, currentMessageId!!, thumb))
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {},
                { thr -> thr.printStackTrace()}
            )
        )
    }

    class AudioRecordingStatus(
        val audioRecordingStatus: AudioRecordingEnum,
        val exceptionCaught: Boolean = false,
        val fileCreated: Boolean = false,
        val isCancel: Boolean = false,
        val motionEvent: MotionEvent? = null,
        val movingLeft: Boolean = false
    ) {

        enum class AudioRecordingEnum {
            START_RECORDING, STOP_RECORDING, SLIDE_TO_CANCEL
        }
    }

    private lateinit var chatMessageType: ChatMessageType

    override fun onStartRecording() {
        exceptionCaught = false
        addDisposable(Observable.just(
            setUserActivity(
                resourceHelper.getString(R.string.chat_activity_recording_audio)
            ))
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {
                    chatRecordingAudioLiveData.postValue(AudioRecordingStatus(AudioRecordingStatus.AudioRecordingEnum.START_RECORDING))
                },
                { thr -> thr.printStackTrace()}
            )
        )
    }

    override fun onStopRecording(mediaFileUri: String?, exceptionCaught: Boolean) {
        this.exceptionCaught = exceptionCaught
        addDisposable(Observable.just(
            setUserActivity())
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {
                    if (!exceptionCaught) {
                        if (!mediaFileUri.isNullOrBlank()) {
                            val u = Uri.parse(mediaFileUri)
                            val file = File(u.path!!)
                            if (file.exists()) {
                                if (isCancelingAudio) {
                                    if (file.delete()) {
                                        chatRecordingAudioLiveData.postValue(AudioRecordingStatus(AudioRecordingStatus.AudioRecordingEnum.STOP_RECORDING, fileCreated = true, isCancel = true))
                                    }
                                    isCancelingAudio = false
                                } else {
                                    LogUtils.d(this::class.java.name, "Audio recording SUCCESSFUL: uploading media")
                                    chatMessageType = ChatMessageType.AUDIO
                                    chatRecordingAudioLiveData.postValue(AudioRecordingStatus(AudioRecordingStatus.AudioRecordingEnum.STOP_RECORDING, fileCreated = true))
                                }
                            }
                        } else chatRecordingAudioLiveData.postValue(AudioRecordingStatus(AudioRecordingStatus.AudioRecordingEnum.STOP_RECORDING, fileCreated = false))
                    }
                    else {
                        chatRecordingAudioLiveData.postValue(AudioRecordingStatus(AudioRecordingStatus.AudioRecordingEnum.STOP_RECORDING, exceptionCaught))
                    }

                    chatRecordingAudioLiveData.postValue(AudioRecordingStatus(AudioRecordingStatus.AudioRecordingEnum.STOP_RECORDING))
                },
                { thr -> thr.printStackTrace()}
            )
        )
    }

    override fun onSlideToCancel(motionEvent: MotionEvent, movingLeft: Boolean) {
        chatRecordingAudioLiveData.postValue(AudioRecordingStatus(AudioRecordingStatus.AudioRecordingEnum.SLIDE_TO_CANCEL))
    }

    override fun postProgress(event: Triple<Boolean, ActionType?, Long>) {
        uploadMediaProgressUpdateLiveData.postValue(event)
    }

    override fun postError(error: Pair<Boolean, ActionType?>) {
        uploadMediaFinishLiveData.postValue(true to false)
    }

    override fun getSuccessResponse(response: UploadMediaResponse?) {
        uploadMediaFinishLiveData.postValue(true to true)
        if (response == null) return
        if (!response.mData?.preview.isNullOrEmpty() && !response.mediaType.isNullOrEmpty()) {
            createOutgoingMessage(
                response.mData!!.preview!!,
                mapMediaTypeForChat(response.mediaType!!).value
            )?.apply {
                updateChatRoom(this)
            }
        }
    }

    override fun getErrorResponse(error: String?) {
        uploadMediaFinishLiveData.postValue(true to false)
    }

    override fun onCleared() {
        RealmUtils.closeRealm(viewModelRealm)
        super.onCleared()
    }

}