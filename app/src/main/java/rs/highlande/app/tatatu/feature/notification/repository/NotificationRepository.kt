package rs.highlande.app.tatatu.feature.notification.repository

import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.BuildConfig
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.notification.api.NotificationApi

/**
 * Created by Abhin.
 */
class NotificationRepository : BaseRepository() {
    private val mNotificationApi : NotificationApi by inject()

    //get NotificationS
    fun getNotifications(caller: OnServerMessageReceivedListener, page: Int): Observable<RequestStatus> {
        return mNotificationApi.getNotification(caller, skip = page * PAGINATION_SIZE)
    }

    //get Notification
    fun getNotifications(caller: OnServerMessageReceivedListener, page: Int = 0, size: Int = 1): Observable<RequestStatus> {
        return mNotificationApi.getNotification(caller, skip = page * size, limit = size)
    }

    fun storeDeviceToken(caller: OnServerMessageReceivedListener, deviceToken: String, isVoip: Boolean = false): Observable<RequestStatus> {
        return mNotificationApi.storeDeviceToken(
            caller,
            deviceToken,
            if (BuildConfig.USE_PROD_CONNECTION) "p" else "d",
            isVoip)
    }
}