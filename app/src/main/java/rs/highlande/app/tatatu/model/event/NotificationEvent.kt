package rs.highlande.app.tatatu.model.event

import rs.highlande.app.tatatu.model.NotificationSimpleResponse

/**
 * Created by Abhin.
 */
data class NotificationEvent(var mNotificationList: ArrayList<NotificationSimpleResponse>?=null,
                             var mToReadCount: Int?=null)