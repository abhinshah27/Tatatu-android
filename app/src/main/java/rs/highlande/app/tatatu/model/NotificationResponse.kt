package rs.highlande.app.tatatu.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created by Abhin.
 */
data class NotificationResponse(
    var viewType: Int = 0,
    var imageUrl: String = "",
    var title: String = "",
    var date: String = "",
    var newAdded: Boolean = false,
    var des: String = ""
)

//data class NotificationSimpleResponse(@SerializedName("UserNotifications") @Expose var notifications: ArrayList<UserNotification>? = null, @SerializedName("PaginationToken") @Expose var paginationToken: String? = null, @SerializedName("toRead") @Expose var toRead: Int? = null)

class NotificationSimpleResponse {
    @SerializedName("notifications")
    @Expose
    var notifications: ArrayList<UserNotification>? = null
    @SerializedName("toRead")
    @Expose
    var toRead: Int? = null
}

data class UserNotification(
    @SerializedName("notificationText")
    @Expose
    var notificationText: String? = null,
    @SerializedName("notificationType")
    @Expose
    var notificationType: String? = null,
    @SerializedName("IsRead")
    @Expose
    var isRead: Boolean? = null,
    @SerializedName("notificationDate")
    @Expose
    var notificationDate: String? = null,
    @SerializedName("notificationID")
    @Expose
    var notificationID: String? = null,
    @SerializedName("referenceID")
    @Expose
    var referenceID: String? = null,
    @SerializedName("MainUserInfo")
    @Expose
    var userInfo: MainUserInfo? = null,
    @SerializedName("DetailsUserInfo")
    @Expose
    var detailsInfo: DetailUserInfo? = null
)