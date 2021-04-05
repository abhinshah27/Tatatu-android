/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.connection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_NOTIFICATIONS
import rs.highlande.app.tatatu.core.service.BaseService
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_PAGE_SIZE
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.notification.repository.NotificationRepository
import rs.highlande.app.tatatu.model.NotificationSimpleResponse
import rs.highlande.app.tatatu.model.event.NotificationEvent


/**
 * Create by Abhin
 */
class NotificationService : BaseService() {

    private val repo: NotificationRepository by inject()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fetchNotification()
        return START_STICKY
    }

    /**
     * Calls th API to send socket subscription.
     */
    private fun fetchNotification() {
        val mBundle = Bundle()
        mBundle.apply {
            putInt(BUNDLE_KEY_PAGE_SIZE, PAGINATION_SIZE)
        }
        addDisposable(repo.getNotifications(this, 0).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, "Get Notification: SUCCESS")
            else LogUtils.e(logTag, "Get Notification call: FAILED with $it")
        }, { thr -> thr.printStackTrace() }))
    }


    //region == Receiver Callback ==

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_GET_NOTIFICATIONS -> {
                response?.let {
                    val notificationList = getNotificationArrayList(response.toString())
                    val toRead = response.getJSONObject(0).getInt("toRead")
                    EventBus.getDefault().post(NotificationEvent(notificationList, toRead))
                    LogUtils.d(CommonTAG, "notification Service Response--> ${Gson().toJson(notificationList)}")
                    stopSelf()
                    return
                }

                LogUtils.d(CommonTAG, "notification Service Response --> null")
                stopSelf()
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when (callCode) {
            SERVER_OP_GET_NOTIFICATIONS -> {
                LogUtils.d(CommonTAG, "Get Notification Service error Response --> ${Gson().toJson(description)}")
                stopSelf()
            }
        }
    }

    //endregion


    companion object {
        val logTag = NotificationService::class.java.simpleName

        fun startService(context: Context) {
            LogUtils.d(logTag, "NOTIFICATIONS service: startService()")
            try {
                context.startService(Intent(context, NotificationService::class.java))
            } catch (e: IllegalStateException) {
                LogUtils.e(logTag, "Cannot start background service: " + e.message, e)
            }
        }
    }

    private fun getNotificationArrayList(json: String): ArrayList<NotificationSimpleResponse> {
        val type = object : TypeToken<ArrayList<NotificationSimpleResponse>>() {}.type
        return Gson().fromJson(json, type)
    }
}
