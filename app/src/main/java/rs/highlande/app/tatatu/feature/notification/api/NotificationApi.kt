package rs.highlande.app.tatatu.feature.notification.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_NOTIFICATIONS
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_STORE_TOKEN
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE

/**
 * Created by Abhin.
 */
class NotificationApi : BaseApi() {

    //get Notification
    fun getNotification(caller: OnServerMessageReceivedListener,
                        notificationID: String? = null,
                        skip: Int = 0,
                        limit: Int = PAGINATION_SIZE): Observable<RequestStatus> {
        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    if (notificationID.isNullOrEmpty()) putString("notificationID", notificationID)
                    putInt(
                        "skip",
                        if (skip > -1) skip else 0
                    )
                    putInt(
                        "limit",
                        if (limit > 0) limit else PAGINATION_SIZE
                    )
                },
                callCode = SERVER_OP_GET_NOTIFICATIONS,
                logTag = "Get Notification",
                caller = caller))
    }

    fun storeDeviceToken(
        caller: OnServerMessageReceivedListener,
        deviceToken: String,
        mod: String,
        isVoip: Boolean = false): Observable<RequestStatus> {
            return if (deviceToken.isNotBlank()) tracker.callServer(
                SocketRequest(
                    Bundle().apply {
                        putString("deviceToken", deviceToken)
                        putString("mod", mod)
                        putString("so", "android")
                        putBoolean("isVoip", isVoip)
                    },
                    callCode = SERVER_OP_STORE_TOKEN,
                    logTag = "STORE TOKEN call",
                    caller = caller
                )
            ) else Observable.just(RequestStatus.ERROR)
    }
}