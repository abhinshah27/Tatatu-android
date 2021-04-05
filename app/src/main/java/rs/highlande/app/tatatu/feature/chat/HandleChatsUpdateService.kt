/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat

import android.app.Service
import android.content.Context
import android.content.Intent
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import org.json.JSONArray
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_FETCH_MESSAGES
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_HACK
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_UPDATE_LIST
import rs.highlande.app.tatatu.core.service.BaseService
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatRoom

/**
 * [Service] subclass whose duty is to update the chat rooms list and their persisted messages.
 * @author mbaldrighi on 11/01/2017.
 */
class HandleChatsUpdateService : BaseService(), OnServerMessageReceivedListener {

    private val repository: ChatRepository by inject()
    private val preferenceHelper: PreferenceHelper by inject()

    private val chatIds = mutableListOf<String>()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val user = preferenceHelper.getUser()
        if (user != null && user.isValid()) {
            if (!user.uid.isBlank()) {
                addDisposable(
                    repository.updateRoomsList(this, user.uid)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {},
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
        }
        else exitOps()

        return START_STICKY
    }

    //region == Receiver Callback ==

    private val updateChatsLambda = { jsonArray: JSONArray?, realm: Realm ->
        if (jsonArray != null) {
            realm.executeTransaction {

                ChatRoom.handleDeletedRooms(realm, jsonArray)

                if (jsonArray.length() > 0) {
                    for (i in 0 until jsonArray.length()) {
                        val room = ChatRoom.getRoom(jsonArray.optJSONObject(i))
                        if (room != null && room.isValid()) {
                            it.insertOrUpdate(room)
                            chatIds.add(room.chatRoomID!!)
                            room.chatRoomID?.let { chatRoomId ->
                                if (!chatRoomId.isBlank() && RealmUtils.isValid(realm)) {
                                    val unixTS = ChatRoom.getRightTimeStamp(chatRoomId,
                                        realm, ChatApi.FetchDirection.AFTER)
                                    addDisposable(
                                        repository.fetchMessages(
                                            this,
                                            unixTS!!,
                                            chatRoomId,
                                            ChatApi.FetchDirection.AFTER
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
                        }
                    }
                } else exitOps()
            }
        }
    }

    private val updateMessagesLambda = { jsonArray: JSONArray?, realm: Realm ->
        if (jsonArray != null && jsonArray.length() > 0) {
            val userId = preferenceHelper.getUserId()
            var chatRoomId = ""
            realm.executeTransaction {
                for (i in 0 until jsonArray.length()) {
                    ChatMessage.getMessage(jsonArray.optJSONObject(i))?.let { message ->
                        it.insertOrUpdate(message)
                        if (chatRoomId.isBlank()) chatRoomId = message.chatRoomID!!
                    }
                }
            }
            if (!userId.isBlank() && !chatRoomId.isBlank()) {
                addDisposable(
                    repository.sendChatHack(this, ChatApi.HackType.DELIVERY, userId, chatRoomId)
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

    var count = 0

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        when (callCode) {
            SERVER_OP_CHAT_UPDATE_LIST -> {
                RealmUtils.useTemporaryRealm {
                    updateChatsLambda(response, it)
                }
            }

            SERVER_OP_CHAT_FETCH_MESSAGES -> {
                RealmUtils.useTemporaryRealm {
                    updateMessagesLambda(response, it)

                }

                if (++count == chatIds.size) exitOps()
            }

            SERVER_OP_CHAT_HACK -> {
                LogUtils.d(logTag, "$count/${chatIds.size} delivery notifications sent to server")
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        when (callCode) {
            SERVER_OP_CHAT_UPDATE_LIST -> exitOps()

            SERVER_OP_CHAT_FETCH_MESSAGES -> {
                if (++count == chatIds.size) exitOps()
            }

            SERVER_OP_CHAT_HACK -> {
                LogUtils.d(logTag, "Delivery hack failed @ no. $count")
            }

            0 -> {
                if (chatIds.isEmpty()) exitOps()
            }
        }
    }

    //endregion


    private fun exitOps() {
        stopSelf()
    }


    companion object {
        val logTag = HandleChatsUpdateService::class.java.simpleName

        @JvmStatic
        fun startService(context: Context) {
            try {
                context.startService(Intent(context, HandleChatsUpdateService::class.java))
            } catch (e: IllegalStateException) {
                LogUtils.e(logTag, "Cannot start background service: " + e.message, e)
            }
        }
    }

}
