/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.connection

import android.content.Context
import android.content.Intent
import android.os.Handler
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CHAT_SET_USER_ONLINE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_SOCKET_SUBSCR
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.chat.api.ChatApi


/**
 * Child class of [SubscribeToSocketService] with the duty to subscribe client to Chat socket.
 *
 * @author mbaldrighi on 2019-12-13.
 */
class SubscribeToSocketServiceChat : SubscribeToSocketService() {

    private val chatApi: ChatApi by inject()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isChat = true
        return super.onStartCommand(intent, flags, startId)
    }


    //region == Receiver Callback ==

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        when (callCode) {
            SERVER_OP_SOCKET_SUBSCR -> {
                successHandler?.sendEmptyMessage(SUCCESS_CHAT)

                TTUApp.subscribedToSocketChat = true

                LogUtils.d(LOG_TAG, "SUBSCRIPTION CHAT to socket SUCCESS")

                if (isUserIdInitialized() && !userId.isBlank())
                    chatApi.setUserOnline(this, userId)
            }
            SERVER_OP_CHAT_SET_USER_ONLINE -> {
                LogUtils.d(LOG_TAG, "CHAT SET USER ONLINE SUCCESS")
                stopSelf()
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        when (callCode) {
            SERVER_OP_SOCKET_SUBSCR -> {
                TTUApp.subscribedToSocketChat = false
                LogUtils.e(LOG_TAG, "SUBSCRIPTION CHAT to socket FAILED")
                stopSelf()
            }
            SERVER_OP_CHAT_SET_USER_ONLINE -> {
                LogUtils.e(LOG_TAG, "CHAT SET USER ONLINE FAILED")
                stopSelf()
            }
        }
    }

    //endregion


    companion object {

        val LOG_TAG = SubscribeToSocketServiceChat::class.java.simpleName

        const val SUCCESS_CHAT = 1

        private var successHandler: Handler? = null


        fun startService(context: Context, handler: Handler?) {
            LogUtils.d(LOG_TAG, "SUBSCRIPTION CHAT to socket: startService()")
            try {
                successHandler = handler
                context.startService(
                    Intent(
                        context,
                        SubscribeToSocketServiceChat::class.java
                    )
                )
            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }

        }
    }

}
