/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.connection

import android.content.Context
import android.content.Intent
import android.os.Handler
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_SOCKET_SUBSCR
import rs.highlande.app.tatatu.core.service.BaseService
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository


/**
 * Child class of [BaseService] with the duty to subscribe client to Real-Time communication socket,
 * and Chat socket.
 *
 * @author mbaldrighi on 2019-07-11.
 */
open class SubscribeToSocketService : BaseService() {

    private val chatApi: ChatApi by inject()

    private val usersRepository: UsersRepository by inject()

    protected var isChat = false
    protected lateinit var userId: String

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        isChat = intent?.getBooleanExtra(KEY_ISCHAT, false) ?: false

        userId = usersRepository.fetchCachedMyUserId()

        try {
            callSubscription(userId)
        } catch (e: Exception) {
            e.printStackTrace()

            stopSelf()
        }

        return START_STICKY
    }

    /**
     * Calls th API to send socket subscription.
     */
    private fun callSubscription(id: String) {
        addDisposable(
            commonApi.subscribeToSocket(this, id, isChat, this)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        LogUtils.d(LOG_TAG, "Subscription request status: $it")

                        if (it == RequestStatus.ERROR || it == RequestStatus.NO_CONNECTION) {
                            if (isChat) TTUApp.subscribedToSocketChat = false
                            else TTUApp.subscribedToSocket = false

                            stopSelf()
                        }
                    },
                    {
                        LogUtils.e(LOG_TAG, "Subscription request status ERROR")
                        if (isChat) TTUApp.subscribedToSocketChat = false
                        else TTUApp.subscribedToSocket = false
                        it.printStackTrace()
                    }
                )
        )
    }


    protected fun isUserIdInitialized() = ::userId.isInitialized


    //region == Receiver Callback ==

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        when (callCode) {
            SERVER_OP_SOCKET_SUBSCR -> {
                successHandler?.sendEmptyMessage(/*if (isChat) SUCCESS_CHAT else */SUCCESS_RTCOM)

//                if (isChat) TTUApp.subscribedToSocketChat = true
//                else
                    TTUApp.subscribedToSocket = true

                LogUtils.d(LOG_TAG, "SUBSCRIPTION ${""/* if (isChat) "CHAT" else ""*/} to socket SUCCESS")

//                if (!isChat)
                    stopSelf()
//                else {
//                    chatApi.setUserOnline(this, userId)
//                }
            }
//            SERVER_OP_CHAT_SET_USER_ONLINE -> {
//                LogUtils.d(LOG_TAG, "CHAT SET USER ONLINE SUCCESS")
//                stopSelf()
//            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        when (callCode) {
            SERVER_OP_SOCKET_SUBSCR -> {
//                if (isChat) TTUApp.subscribedToSocketChat = false
//                else
                    TTUApp.subscribedToSocket = false
                LogUtils.e(LOG_TAG, "SUBSCRIPTION ${""/* if (isChat) "CHAT" else ""*/} to socket FAILED")
                stopSelf()
            }
//            SERVER_OP_CHAT_SET_USER_ONLINE -> {
//                LogUtils.e(LOG_TAG, "CHAT SET USER ONLINE FAILED")
//                stopSelf()
//            }
        }
    }

    //endregion


    companion object {

        val LOG_TAG = SubscribeToSocketService::class.java.simpleName

        const val SUCCESS_RTCOM = 0
//        const val SUCCESS_CHAT = 1
//        const val KEY_ISCHAT = "key_isChat"

        private var successHandler: Handler? = null


        fun startService(context: Context, handler: Handler?) {
            LogUtils.d(LOG_TAG, "SUBSCRIPTION to socket: startService()")
            try {
                successHandler = handler
                context.startService(Intent(context, SubscribeToSocketService::class.java))
            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }

        }
//        fun startService(context: Context, handler: Handler?, isChat: Boolean = false) {
//            LogUtils.d(LOG_TAG, "SUBSCRIPTION ${ if (isChat) "CHAT" else ""} to socket: startService()")
//            try {
//                successHandler = handler
//                context.startService(
//                    Intent(
//                        context,
//                        SubscribeToSocketService::class.java
//                    ).apply { putExtra(KEY_ISCHAT, isChat) }
//                )
//            } catch (e: IllegalStateException) {
//                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
//            }
//
//        }
    }

}
