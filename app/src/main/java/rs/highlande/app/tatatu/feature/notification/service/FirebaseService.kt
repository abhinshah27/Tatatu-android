package rs.highlande.app.tatatu.feature.notification.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_STORE_TOKEN
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.home.view.ChatToReadEvent
import rs.highlande.app.tatatu.feature.notification.repository.NotificationRepository
import rs.highlande.app.tatatu.feature.notification.view.activity.NotificationActivity
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsNotificationUtils
import rs.highlande.app.tatatu.model.event.NotificationEvent
import kotlin.random.Random

class FirebaseService: FirebaseMessagingService(), KoinComponent, OnServerMessageReceivedListener {

    private val foregroundManager by inject<ForegroundManager>()
    private val notificationRepository by inject<NotificationRepository>()


    override fun onNewToken(token: String) {
        LogUtils.d(logTag, "FCM token: $token")

        val disposable = notificationRepository.storeDeviceToken(this, token)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribeWith(object : DisposableObserver<RequestStatus>() {
                override fun onComplete() {
                    dispose()
                }

                override fun onNext(t: RequestStatus) = Unit

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    dispose()
                }
            })
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        if (callCode == SERVER_OP_STORE_TOKEN)
            LogUtils.d(logTag, "New FCM token stored")
    }

    override fun handleErrorResponse(
        idOp: String,
        callCode: Int,
        errorCode: Int,
        description: String?
    ) {
        if (callCode == SERVER_OP_STORE_TOKEN)
            LogUtils.d(logTag, "ERROR in storing the new token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        LogUtils.d(logTag, "FCM notification payload: ${message.data}")

        when (message.data["notificationType"]) {

            NOTIFICATION_TYPE_CALL -> CallsNotificationUtils.processPayload(this, message)
            else -> {

                LogUtils.d("dataNOTIFICATION", message.data["actionType"])
                LogUtils.d("dataNOTIFICATION", message.data["id"])

                if (message.data["actionType"] == NOTIFICATION_TYPE_CHAT)
                    doChatNotification(this, message, foregroundManager)
                else
                    doSocialNotification(message)
            }
        }

    }

    private fun doSocialNotification(message: RemoteMessage) {

        val notificationManager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(getString(R.string.notification_channel_id), CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.enableLights(true)
            channel.lightColor = Color.RED

            // INFO: 2019-10-03    temporarily add default sound for notification found in the phone
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder().setFlags(AudioAttributes.USAGE_NOTIFICATION).build())

            // INFO: 2019-10-03    temporarily add random vibration pattern
//            channel.enableVibration(true)
//            channel.vibrationPattern = longArrayOf(0, 300)

            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = buildIntent(this, message.data, foregroundManager)

        NotificationCompat.Builder(applicationContext, getString(R.string.notification_channel_id)).apply {
            setSmallIcon(R.drawable.ic_notification_tray)
            color = Color.RED
            setContentTitle(message.data["title"])
            setContentText(message.data["body"])
            setStyle(NotificationCompat.BigTextStyle())
            pendingIntent?.let {
                setContentIntent(pendingIntent)
            }
            setAutoCancel(true)
            setGroup(NOTIFICATION_GROUP)
            NotificationManagerCompat.from(applicationContext).notify(Random(message.sentTime).nextInt(), build())
        }

        NotificationCompat.Builder(applicationContext, getString(R.string.notification_channel_id)).apply {
            setSmallIcon(R.drawable.ic_notification_tray)
            color = Color.RED
            setContentTitle(message.data["title"])
            setContentText(message.data["body"])
            priority = NotificationCompat.PRIORITY_HIGH
            setStyle(NotificationCompat.BigTextStyle())
            setAutoCancel(true)
            setGroup(NOTIFICATION_GROUP)
            setGroupSummary(true)
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_SUMMARY, build())
        }

        message.data["badge"]?.let {
            EventBus.getDefault().post(NotificationEvent(null, Integer.parseInt(it)))
        }
    }


    companion object {
        val logTag = FirebaseService::class.java.simpleName
        fun buildIntent(
            context: Context,
            payload: Map<String, String>,
            foregroundManager: ForegroundManager
        ): PendingIntent? {

            if (!payload.isNullOrEmpty() && validateActionType(payload["actionType"])) {
                val intent = Intent(context, NotificationActivity::class.java)
                intent.putExtra(BUNDLE_NOTIFICATION_TYPE, payload["actionType"])
                intent.putExtra(BUNDLE_NOTIFICATION_COUNT, payload["badge"])
                intent.putExtra(BUNDLE_NOTIFICATION_ID, payload["id"])
                intent.putExtra(BUNDLE_NOTIFICATION_APP_FOREGROUND, foregroundManager.isForeground)
                intent.action = payload["actionType"].plus(payload["id"])
                return PendingIntent.getActivity(context, NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            }
            return null
        }

        //TODO 14/01/20: Needs the chatroom id
//        fun buildChatIntent(context: Context): PendingIntent? {
//            val intent = Intent(context, NotificationActivity::class.java)
//            return PendingIntent.getActivity(context, NOTIFICATION_CHAT_REPLY_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
//        }

        private fun validateActionType(actionType: String?): Boolean {
            return if (!actionType.isNullOrEmpty()) {
                return (actionType == NOTIFICATION_TYPE_FEED ||
                        actionType == NOTIFICATION_TYPE_PROFILE ||
                        actionType == NOTIFICATION_TYPE_CHAT)
            } else {
                false
            }
        }

        fun doChatNotification(context: Context, message: RemoteMessage, foregroundManager: ForegroundManager) {
            doChatNotification(context, message.data, foregroundManager)
        }

        fun doChatNotification(context: Context, payload: Map<String, String>, foregroundManager: ForegroundManager) {

            val notificationManager = NotificationManagerCompat.from(context)

            val channelID: String = context.getString(R.string.notification_channel_id_chat)
            if (hasOreo()) {
                val chatChannel = NotificationChannel(
                    channelID,
                    context.getString(R.string.notif_channel_chat_description),
                    NotificationManager.IMPORTANCE_HIGH
                )
                chatChannel.description = context.getString(R.string.notif_channel_chat_description)
                chatChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                chatChannel.enableVibration(true)
                chatChannel.vibrationPattern = longArrayOf(0, VIBE_SHORT)
                chatChannel.setSound(
                    Uri.parse("android.resource://" + context.packageName + "/" + R.raw.chat_incoming_message),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .build()
                )
                chatChannel.enableLights(true)
                chatChannel.lightColor = Color.RED
                notificationManager.createNotificationChannel(chatChannel)
            }



            val msg = payload["body"]
            val name = payload["title"]


            // TODO: 2020-01-13    check if this "id" is room id. otherwise ask for chat room id
            //  to be able to extract the user picture
            val id = payload["id"]


            val `when` = System.currentTimeMillis()

            val style =
                if (hasPie()) {
                    NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
                        .addMessage(msg, `when`, Person.Builder().setName(name).build())
                } else {
                    NotificationCompat.MessagingStyle("Me")
                        .addMessage(NotificationCompat.MessagingStyle.Message(msg, `when`, name))
                }

//            INFO: 14/01/20 Commented while chatRoomID is not present on notification payload
//            val remoteInput = RemoteInput.Builder("KEY_REPLY").setLabel("Reply").build()
//            val action = Notification.Action.Builder(android.R.drawable.sym_action_chat, "REPLY",
//                buildChatIntent(context)).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build()

            val notification =
                NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id_chat))
                    .setSmallIcon(R.drawable.ic_notification_tray)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setStyle(style)
//                    .setContentText(msg)
                    .setTicker(msg)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVibrate(longArrayOf(0, VIBE_SHORT))
                    .setSound(
                        Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.chat_incoming_message),
                        AudioManager.STREAM_NOTIFICATION
                    )
                    .setLights(Color.YELLOW, 600, 1000)
                    .setAutoCancel(true)
                    .setDefaults(0)
                    .setWhen(`when`)
                    .setContentIntent(buildIntent(context, payload, foregroundManager))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setGroup(NOTIFICATION_GROUP_CHAT)

                    // TODO: 2020-01-13    check if VALID. Don't remember why I made the distinction 24+ and before
                    .apply {
                        if (!hasNougat() || name.isNullOrBlank())
                            setContentText(context.getString(R.string.chat_notification_text_below_24))
                    }

                    .build()

            val summaryNotification =
                NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id_chat))
                    .setContentTitle("Title")
                    .setSmallIcon(R.drawable.ic_notification_tray)
                    .setStyle(NotificationCompat.BigTextStyle()) //specify which group this notification belongs to
                    .setGroup(NOTIFICATION_GROUP_CHAT) //set this notification as the summary for the group
                    .setGroupSummary(true)

                    // TODO: 2020-01-13    check if VALID. Don't remember why I made the distinction 24+ and before
                    .apply {
                        if (!hasNougat() || name.isNullOrBlank())
                            setContentText(context.getString(R.string.chat_notification_text_below_24))
                    }

                    .build()

            // post notifications
            notificationManager.notify(name.hashCode(), notification)
            notificationManager.notify(NOTIFICATION_SUMMARY_CHAT, summaryNotification)

            EventBus.getDefault().post(ChatToReadEvent())
        }

    }

}