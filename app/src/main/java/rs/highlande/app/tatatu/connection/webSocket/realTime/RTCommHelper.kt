/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.connection.webSocket.realTime

import android.content.Context
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.BaseSoundPoolManager
import rs.highlande.app.tatatu.feature.chat.WebLinkRecognizer
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.feature.chat.chatRoomList.ChatRoomsFragment
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.chat.view.ChatMessagesFragment
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.notification.service.FirebaseService
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatRecipientStatus
import rs.highlande.app.tatatu.model.chat.ChatRoom
import rs.highlande.app.tatatu.model.chat.Participant
import java.util.*

/**
 * TODO: 2019-07-12    understand how to register class as listener in [WebSocketTracker].
 * Helper class acting as listener and forwarder for new real time communication messages.
 * @author mbaldrighi on 2019-07-12.
 */
class RTCommHelper(
    private val mContext: Context
) : KoinComponent, OnServerMessageReceivedListener/*, Handler.Callback*/,
    DisposableHandler by CompositeDisposableHandler() {


    private val userRepo: UsersRepository by inject()
    private val chatRepo: ChatRepository by inject()
    private val webLinkRecognizer: WebLinkRecognizer by inject()
    private val foregroundManager by inject<ForegroundManager>()

    private var initiatorMessage: ChatMessage? = null


    private val chatSoundPool by lazy {
        BaseSoundPoolManager(mContext, arrayOf(R.raw.chat_incoming_message), null)
    }

    //    var listener: RealTimeCommunicationListener? = null
    // TODO: Change logic make it more flexible. Use eventBus instead
    var listenerChat: RealTimeChatListener? = null


    // TODO: 2019-12-16    CHECK: when code goes through handleSuccess/ErrorResponse(), is it in
    //  background thread?
    //private var mRealm: Realm? = null

    init {
        //mRealm = RealmUtils.checkedRealm

        chatSoundPool.init()
    }


    companion object {

        val logTag = RTCommHelper::class.java.simpleName

    }


    //region == Class custom methods ==

    //region == RTCOM methods ==

//    private val mHandler = Handler(this)
//    var hasInsert = false


//    override fun handleMessage(msg: Message?): Boolean {
//
//        when (msg?.what) {
//
//            // TODO: 2019-07-12     check if still needed
////            BaseHandlePushedBlockService.Type.DATA.value -> {
////                HLPosts.getInstance().cleanRealmPosts(mRealm, mContext)
////
////                callSetDataRead()
////
////                hasInsert = msg.arg1 == 1
////
////                if (listener != null && listener is TimelineFragment) {
////                    (listener as TimelineFragment).onNewDataPushed(hasInsert)
////                    return true
////                }
////            }
//
//        }
//
//        return false
//    }
//
//
//    /**
//     * TODO: 2019-07-12    REALTIME
//     * Calls server to set data bundle to READ status after update.
//     */
//    private fun callSetDataRead() {}


//    private fun onPostAdded(json: JSONObject?) {
//        if (json != null) {
//            val pid = json.optString("_id")
//            if (!pid.isNullOrBlank()) {
//
//
//                // TODO: 2019-07-12    implement new post
////                val posts = HLPosts.getInstance()
////                posts.setPost(json, mRealm, true)
////
////                if (listener != null && listener is TimelineFragment) {
////                    val p = posts.getPost(pid)
////                    var position = posts.visiblePosts.indexOf(p)
////                    if (position == -1) position = 0
////                    listener!!.onPostAdded(p, position)
////                }
//
//            }
//        }
//    }
//
//    private fun onPostUpdated(json: JSONObject?) {
//        if (json != null) {
//            val pid = json.optString("_id")
//            if (!pid.isNullOrBlank()) {
//
//
//                // TODO: 2019-07-12    implement update post
////                val posts = HLPosts.getInstance()
////                posts.setPost(json, mRealm, posts.isPostToBePersisted(pid))
////
////                if (listener != null && listener is TimelineFragment) {
////                    val p = posts.getPost(pid)
////                    val position = posts.visiblePosts.indexOf(p)
////                    if (position != -1)
////                        listener!!.onPostUpdated(pid, position)
////                }
//            }
//        }
//    }
//
//    // TODO: 2019-07-12    check if still needed
////    private fun onPostUpdatedInteractions(json: JSONObject?, operationId: Int) {
////        if (json != null) {
////            val pid = json.optString("postID")
////            if (Utils.isStringValid(pid)) {
////                val posts = HLPosts.getInstance()
////                val p = posts.getPost(pid)
////                if (p != null) {
////                    when (operationId) {
////                        Constants.SERVER_OP_RT_UPDATE_HEARTS -> {
////                            val heart = InteractionHeart().returnUpdatedInteraction(json)
////                            if (!Utils.isStringValid(heart.id))
////                                return
////                            if (p.interactionsHeartsPost == null)
////                                p.interactionsHeartsPost = RealmList()
////                            val ih = posts.getHeartInteractionById(pid, heart.id)
////                            ih?.updateForRealTime(heart) ?: p.interactionsHeartsPost.add(heart)
////                            p.countHeartsUser = p.countHeartsUser + heart.count!!
////                            p.countHeartsPost = p.countHeartsPost + heart.count!!
////                        }
////
////                        Constants.SERVER_OP_RT_NEW_COMMENT -> {
////                            val comment = InteractionComment().returnUpdatedInteraction(json)
////                            if (!Utils.isStringValid(comment.id))
////                                return
////                            if (p.interactionsComments == null)
////                                p.interactionsComments = RealmList()
////                            val oldComment = posts.getCommentInteractionById(pid, comment.id)
////                            if (oldComment == null) {
////                                p.interactionsComments.add(comment)
////                                p.countComments = p.countComments + 1
////                            }
////                        }
////
////                        Constants.SERVER_OP_RT_UPDATE_COMMENTS -> {
////                            val comment1 = InteractionComment().returnUpdatedInteraction(json)
////                            if (!Utils.isStringValid(comment1.id))
////                                return
////                            if (p.interactionsComments == null)
////                                p.interactionsComments = RealmList()
////                            val oldComment1 = posts.getCommentInteractionById(pid, comment1.id)
////                            oldComment1?.update(comment1)
////                            if (!comment1.isVisible) {
////                                p.countComments = p.countComments - 1
////                                p.isYouLeftComments = p.checkYouLeftComments(HLUser().readUser(mRealm)?.id)
////                            }
////                        }
////
////                        Constants.SERVER_OP_RT_NEW_SHARE -> {
////                            val share = InteractionShare().returnUpdatedInteraction(json)
////                            if (!Utils.isStringValid(share.id))
////                                return
////                            if (p.interactionsShares == null)
////                                p.interactionsShares = RealmList()
////                            p.interactionsShares.add(share)
////                            p.countShares = p.countShares + 1
////                        }
////
////                        Constants.SERVER_OP_RT_UPDATE_TAGS -> {
////                            val tag = Tag().returnUpdatedTag(json)
////                            if (!Utils.isStringValid(tag.id))
////                                return
////                            if (p.tags == null)
////                                p.tags = RealmList()
////                            p.tags.add(tag)
////                        }
////
////                        Constants.SERVER_OP_RT_EDIT_POST -> {
////                            val post = Post().returnUpdatedPost(json)
////                            if (!Utils.isStringValid(pid))
////                                return
////                            val oldPost = posts.getPost(pid)
////                            oldPost.update(post)
////                        }
////                    }
////
////                    posts.setPost(p, mRealm, posts.isPostToBePersisted(pid))
////
////                    if (listener != null) {
////                        if (listener is TimelineFragment) {
////                            val position = posts.visiblePosts.indexOf(p)
////                            if (position != -1)
////                                listener!!.onPostUpdated(pid, position)
////                        }
////                        else if (listener is InteractionsViewerActivity) {
////                            listener!!.onPostUpdated(pid, -1)
////                        }
////                    }
////                }
////            }
////        }
////    }
//
//
//    private fun onPostDeleted(json: JSONObject?) {
//        if (json != null) {
//            val pid = json.optString("_id")
//            if (!pid.isNullOrBlank()) {
//
//                // TODO: 2019-07-12    implement delete post
////                val posts = HLPosts.getInstance()
////
////                if (listener != null && listener is TimelineFragment) {
////                    val p = posts.getPost(pid)
////                    if (p != null) {
////                        val position = posts.visiblePosts.indexOf(p)
////                        if (position != -1)
////                            listener!!.onPostDeleted(position)
////                    }
////                }
////
////                posts.deletePost(pid, mRealm, true)
//            }
//        }
//    }
//
//    // TODO: 2019-07-12    check if still needed
////    private fun onDataPushed(jsonArray: JSONArray?) {
////        if (jsonArray != null)
////            HandlePushedDataService.startService(mContext, mHandler, jsonArray.toString())
////    }

    //endregion


    //region == CHAT methods ==

    private fun onNewMessage(json: JSONObject?) {
        if (json != null) {

            var room: ChatRoom?
            val message = ChatMessage.getMessage(json)

            message?.let { mess ->
                mess.deliveryDateObj = Date()
                RealmUtils.useTemporaryRealm { realm ->
                    realm.executeTransaction {

                        val roomPair = ChatRoom.checkRoom(mess.chatRoomID!!, it)
                        if (roomPair.first) {
                            it.insertOrUpdate(mess)

                            // start web link recognition
                            webLinkRecognizer.recognize(mess.text, mess.messageID)

                            room = roomPair.second
                            // increase by one the unread messages count
                            room?.toRead = room!!.toRead + 1

                            doActionOnNewMessage(mess)
                        }
                        else {

                            // stores the message to use it later when updating the rooms
                            initiatorMessage = mess

//                            // INFO: 4/2/19    Assigns correct ownerID (if message received it cannot
//                            //  be the user's) and Fixes wrong call to Room initialization onNewMessage
//                            //  received
//                            room = ChatRoom(
//                                mess.senderID,
//                                RealmList<String>().apply { this.add(mess.senderID) },
//                                mess.chatRoomID
//                            )
//
//                            addDisposable(
//                                chatRepo.updateRoomsList(this, room!!)
//                                    .subscribeOn(Schedulers.trampoline())
//                                    .observeOn(Schedulers.trampoline())
//                                    .subscribe(
//                                        {},
//                                        { thr -> thr.printStackTrace() }
//                                    )
//                            )

                            addDisposable(
                                chatRepo.updateRoomsList(this, userRepo.fetchCachedMyUserId())
                                    .subscribeOn(Schedulers.trampoline())
                                    .observeOn(Schedulers.trampoline())
                                    .subscribe(
                                        {},
                                        { thr -> thr.printStackTrace() }
                                    )
                            )
                        }
                    }
                }
            }

        }
    }

    private fun doActionOnNewMessage(mess: ChatMessage) {
        callSetNewMessageDelivered(mess.chatRoomID!!)

        if (listenerChat != null) {
            when (listenerChat) {
                is ChatRoomsFragment -> {
                    listenerChat!!.onNewMessage(mess)

                    if (TTUApp.canVibrate) vibrateForChat(mContext)
                    chatSoundPool.playOnce(R.raw.chat_incoming_message)
                }
                is ChatMessagesFragment -> {
                    listenerChat!!.onNewMessage(mess)

                    if ((listenerChat as ChatMessagesFragment).viewModel.participantId != mess.senderID) {
                        sendNotification(mess)
                    }
                    else {
                        if (TTUApp.canVibrate) vibrateForChat(mContext)
                        chatSoundPool.playOnce(R.raw.chat_incoming_message)
                    }
                }
            }
        } else sendNotification(mess)
    }


    private fun sendNotification(mess: ChatMessage) {
        RealmUtils.useTemporaryRealm {
            if (mess.chatRoomID.isNullOrEmpty()) return@useTemporaryRealm
            val room = RealmUtils.readFirstFromRealmWithId(
                it,
                ChatRoom::class.java,
                "chatRoomID",
                mess.chatRoomID!!) as? ChatRoom

            FirebaseService.doChatNotification(
                mContext,
                mutableMapOf<String, String>().apply {
                    this["actionType"] = NOTIFICATION_TYPE_CHAT
                    this["id"] = mess.messageID
                    this["body"] = mess.text
                    if (!room?.roomName.isNullOrEmpty()) this["title"] = room!!.roomName!!
                },
                foregroundManager
            )
        }
    }

//    intent.putExtra(BUNDLE_NOTIFICATION_TYPE, payload["actionType"])
//    intent.putExtra(BUNDLE_NOTIFICATION_COUNT, payload["badge"])
//    intent.putExtra(BUNDLE_NOTIFICATION_ID, payload["id"])
//    intent.putExtra(BUNDLE_NOTIFICATION_APP_FOREGROUND, foregroundManager.isForeground)
//    intent.action = payload["actionType"].plus(payload["id"])

    private fun onStatusUpdated(json: JSONObject?) {
        if (json != null) {

            // TODO: 2019-07-12    implement status update
            val userId = json.optString("userID", "")
            val status = json.optInt("status", ChatRecipientStatus.OFFLINE.value)
            val date = json.optString("date", "")

            RealmUtils.useTemporaryRealm { realm ->
                val user = RealmUtils.readFirstFromRealmWithId(
                    realm,
                    Participant::class.java,
                    "userID",
                    userId
                ) as? Participant
                realm.executeTransaction {
                    user?.chatStatus = status
                    user?.lastSeenDate = date
                }
            }

            if (listenerChat != null && isStringValid(userId)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        listenerChat!!.onStatusUpdated(userId, status, date)
                    }
                }
            }
        }
    }

    private fun onActivityUpdated(json: JSONObject?) {
        if (json != null) {
            val userId = json.optString("userID", "")
            val chatId = json.optString("chatID", "")
            val activity = json.optString("activity", "")

            if (listenerChat != null && areStringsValid(userId, chatId)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        listenerChat!!.onActivityUpdated(userId, chatId, activity)
                    }
                }
            }
        }
    }

    private fun onMessageDeliveredOrRead(json: JSONObject?, read: Boolean = false) {
        if (json != null) {
            val chatId = json.optString("chatRoomID", "")
            val userId = json.optString("userID", "")
            val date = json.optString("date", "")

            val id = userRepo.fetchCachedMyUserId()
            if (!id.isBlank()) {
                RealmUtils.useTemporaryRealm {
                    val messages = it.where(ChatMessage::class.java)
                        ?.equalTo("chatRoomID", chatId)
                        ?.and()
                        ?.equalTo("senderID", id)
                        ?.findAll()

                    if (messages?.isNotEmpty() == true) {
                        it.executeTransaction {
                            for (message in messages) {
//                                date.getDateFromDB().apply {
                                with(date.getDateFromDB()) {
                                    if (message.getStatusEnum() != ChatMessage.Status.ERROR &&
                                        message.getStatusEnum() != ChatMessage.Status.SENDING) {
                                        if (read) {
                                            if (!message.isRead()) message.readDateObj = this
                                        } else {
                                            if (!message.isDelivered()) message.deliveryDateObj = this
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (listenerChat != null && areStringsValid(userId, chatId, date)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        if (read) listenerChat!!.onMessageRead(chatId, userId, date)
                        else listenerChat!!.onMessageDelivered(chatId, userId, date)
                    }
                }
            }
        }
    }

    private fun onDocumentOpened(json: JSONObject?) {
        if (json != null) {

            val chatId = json.optString("chatRoomID", "")
            val userId = json.optString("userID", "")
            val date = json.optString("date", "")
            val messageID = json.optString("messageID", "")

            RealmUtils.useTemporaryRealm { realm ->
                val message = RealmUtils.readFirstFromRealmWithId(realm, ChatMessage::class.java, "messageID", messageID) as? ChatMessage
                if (message != null) {
                    realm.executeTransaction {
                        if (message.getStatusEnum() != ChatMessage.Status.ERROR &&
                            message.getStatusEnum() != ChatMessage.Status.SENDING &&
                            !message.isOpened()
                        ) {
                            message.openedDateObj = date.getDateFromDB()
                        }
                    }
                }
            }

            if (listenerChat != null && areStringsValid(userId, chatId, date)) {
                when (listenerChat) {
                    is ChatMessagesFragment,
                    is ChatRoomsFragment -> {
                        listenerChat!!.onMessageRead(chatId, userId, date)
                    }
                }
            }
        }
    }

    //endregion


    fun restoreRealmInstance() {
        //mRealm = RealmUtils.checkedRealm
    }

    fun closeRealmInstance() {
        //RealmUtils.closeRealm(mRealm)
    }


    //endregion


    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        if (response == null || response.length() == 0) return

        try {
            val json = response.optJSONObject(0)

            when (callCode) {

                /* RTCOM */
//                SERVER_OP_RT_NEW_POST, SERVER_OP_RT_NEW_SHARE -> if (json != null) onPostAdded(json)
//                SERVER_OP_RT_DELETE_POST -> if (json != null) onPostDeleted(json)
//                SERVER_OP_RT_UPDATE_POST, SERVER_OP_RT_EDIT_POST -> if (json != null) onPostUpdated(json)
//                SERVER_OP_RT_UPDATE_HEARTS, SERVER_OP_RT_UPDATE_SHARES,
//                SERVER_OP_RT_UPDATE_TAGS, SERVER_OP_RT_UPDATE_COMMENTS,
//                SERVER_OP_RT_NEW_COMMENT -> if (json != null) onPostUpdatedInteractions(json, operationId)
//                SERVER_OP_RT_PUSH_DATA -> onDataPushed(responseObject)
//                SERVER_OP_RT_PUSH_CHANNELS -> onFeedChannelsPushed(responseObject)

                /* CHAT*/
                SERVER_OP_RT_CHAT_DELIVERY -> onNewMessage(json)
                SERVER_OP_RT_CHAT_UPDATE_STATUS -> onStatusUpdated(json)
                SERVER_OP_RT_CHAT_UPDATE_ACTIVITY -> onActivityUpdated(json)
                SERVER_OP_RT_CHAT_MESSAGE_DELIVERED -> onMessageDeliveredOrRead(json)
                SERVER_OP_RT_CHAT_MESSAGE_READ -> onMessageDeliveredOrRead(json, true)
                SERVER_OP_RT_CHAT_DOCUMENT_OPENED -> onDocumentOpened(json)

                SERVER_OP_CHAT_HACK -> LogUtils.d(logTag, "Send Chat hack SUCCESS")
                SERVER_OP_CHAT_UPDATE_LIST -> {
                    RealmUtils.useTemporaryRealm {
                        it.executeTransaction { realm ->
                            for (i in 0 until response.length()) {
                                val j = response.optJSONObject(i)
                                if (j != null && j.length() > 0) {

                                    val id = j.optString("chatRoomID")

                                    if (id == initiatorMessage?.chatRoomID) {
                                        ChatRoom.getRoom(j)?.let { room ->
                                            realm.insertOrUpdate(room)
                                        }
                                        break
                                    }
                                }
                            }

                            initiatorMessage?.let { mess ->
                                realm.insertOrUpdate(mess)

                                // start web link recognition
                                webLinkRecognizer.recognize(mess.text, mess.messageID)

                                doActionOnNewMessage(mess)
                            }

                            initiatorMessage = null
                        }
                    }
                }

//                SERVER_OP_CHAT_INITIALIZE_ROOM -> {
//                    val j = response.optJSONObject(0)
//                    val chatRoom = if (j != null) {
//                        ChatRoom.getRoom(j)?.apply {
//                            ownerID = userRepo.fetchCachedMyUserId()
//                        }
//                    } else null
//
//                    chatRoom?.let {
//                        RealmUtils.useTemporaryRealm { realm ->
//                            realm.executeTransaction { async ->
//                                async.insertOrUpdate(
//                                    it.apply { getParticipant()?.nickname = it.roomName }
//                                )
//                            }
//                        }
//                    }
//
//                    initiatorMessage?.let {
//                        doActionOnNewMessage(it)
//                    }
//                }
            }
        }
        catch (e: JSONException) {
            LogUtils.e(logTag, e.message, e)

//            if (listener is HLActivity)
//                (listener as HLActivity).showGenericError()
//            else if (listenerChat is HLActivity)
//                (listenerChat as HLActivity).showGenericError()
//            else if (listener is HLFragment && Utils.isContextValid((listener as HLFragment).activity)) {
//                if ((listener as HLFragment).activity is HLActivity)
//                    ((listener as HLFragment).activity as HLActivity).showGenericError()
//            }
//            else if (listenerChat is HLFragment && Utils.isContextValid((listenerChat as HLFragment).activity)) {
//                if ((listenerChat as HLFragment).activity is HLActivity)
//                    ((listenerChat as HLFragment).activity as HLActivity).showGenericError()
//            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {

        when (callCode) {
            /* CHAT*/
            SERVER_OP_RT_CHAT_DELIVERY,
            SERVER_OP_RT_CHAT_UPDATE_STATUS,
            SERVER_OP_RT_CHAT_UPDATE_ACTIVITY,
            SERVER_OP_RT_CHAT_MESSAGE_DELIVERED,
            SERVER_OP_RT_CHAT_MESSAGE_READ,
            SERVER_OP_RT_CHAT_DOCUMENT_OPENED -> {
                LogUtils.e(logTag, "Real-Time communication ERROR for operation : " + callCode +
                        " with code: " + errorCode)
            }

            SERVER_OP_CHAT_HACK -> LogUtils.d(logTag, "Send Chat hack FAIL")
        }
    }

    /**
     * Calls server to set new message status to DELIVERED after reception.
     * @param
     */
    private fun callSetNewMessageDelivered(chatRoomId: String) {
        if (areStringsValid(chatRoomId, userRepo.fetchCachedMyUserId()))
            addDisposable(
                chatRepo.sendChatHack(
                    this,
                    ChatApi.HackType.DELIVERY,
                    userRepo.fetchCachedMyUserId(),
                    chatRoomId
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