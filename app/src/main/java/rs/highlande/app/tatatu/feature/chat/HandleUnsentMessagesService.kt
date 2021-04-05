/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.notification.service.FirebaseService
import rs.highlande.app.tatatu.model.chat.ChatMessage


const val UNSENT_NOTIFICATION_ID = 102

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
class HandleUnsentMessagesService : IntentService(HandleUnsentMessagesService::class.java.simpleName), KoinComponent {

    private val foregroundManager by inject<ForegroundManager>()

    companion object {

        private val LOG_TAG = HandleUnsentMessagesService::class.java.simpleName

        @JvmStatic
        fun startService(context: Context) {
            try{
                context.startService(Intent(context, HandleUnsentMessagesService::class.java))
            } catch (e: IllegalStateException) {
                LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        RealmUtils.useTemporaryRealm {
            if (ChatMessage.handleUnsentMessages(it)) sendNotification()
            else {
                // cancel the notification
                (this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(UNSENT_NOTIFICATION_ID)
            }
        }
    }


    private fun sendNotification() {
        val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val msg = getString(R.string.notification_unsent_messages_text)

        val contentIntent = FirebaseService
            .buildIntent(
                this,
                mapOf("actionType" to NOTIFICATION_TYPE_CHAT),
                foregroundManager
            )

        if (hasOreo()) createDefaultNotificationChannel()
        val mBuilder = NotificationCompat.Builder(this, getString(R.string.notification_channel_id_chat_unsent))
            .setSmallIcon(R.drawable.ic_notification_tray)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setContentTitle(getString(R.string.notification_unsent_messages_title))
            .setColor(ResourcesCompat.getColor(resources, R.color.colorAccent, null))
            .setContentText(msg)
            .setTicker(msg)
            .setAutoCancel(true)
            .setDefaults(0)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(NOTIFICATION_GROUP_CHAT)

        mNotificationManager.notify(UNSENT_NOTIFICATION_ID, mBuilder.build())
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createDefaultNotificationChannel() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // The id of the channel.
        val id = getString(R.string.notification_channel_id_chat_unsent)
        // The user-visible name of the channel.
        val name = getString(R.string.notification_unsent_messages_title)
        // The user-visible description of the channel.
        val description = getString(R.string.notif_channel_chat_unsent_description)
        val importance = NotificationManager.IMPORTANCE_LOW

        val mChannel = NotificationChannel(id, name, importance)
        // Configure the notification channel.
        mChannel.description = description
        mChannel.enableLights(true)
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.lightColor = Color.RED
        mChannel.enableVibration(false)
        mNotificationManager.createNotificationChannel(mChannel)
    }


}
