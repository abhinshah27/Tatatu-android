package rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.BuildConfig
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.ui.showDialogGeneric
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_ID
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_MANAGE_ACTION
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_PERSON_AVATAR
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_PERSON_ID
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_PERSON_NAME
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_ROOM_NAME
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_STATUS
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_TYPE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_USAGE_TYPE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.NOTIFICATION_ACTION_CALL_ACCEPT_START_ACT
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.NOTIFICATION_ACTION_CALL_FIRST_START_ACT
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.NOTIFICATION_ACTION_CALL_REJECT_START_ACT
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.SERVICE_NOTIFICATION_ID
import rs.highlande.app.tatatu.feature.voiceVideoCalls.view.CallActivity
import rs.highlande.app.tatatu.model.IncomingCall
import rs.highlande.app.tatatu.model.User
import java.io.IOException
import java.util.*


/**
 * The endpoint to retrieve Twilio SDK STAGING token.
 */
// TODO: 2019-12-17    replace with STAGING instead of DEBUG
const val BASE_URL = "http://accessocasa.ddns.net:5000/api"

/**
 * The endpoint to retrieve Twilio SDK PROD token.
 */
const val BASE_URL_PROD = "https://calls.tatatu.com/api"


/**
 * Object used to handle Twilio SDK call token.
 */
object CallsSDKTokenUtils : KoinComponent {

    private val preferenceHelper: PreferenceHelper by inject()

    val logTag = CallsSDKTokenUtils::class.java.simpleName

    private val TOKEN_URL = "${if (BuildConfig.USE_PROD_CONNECTION) BASE_URL_PROD else BASE_URL}/token"


    var tokenListener: CallsSDKTokenListener? = null
    private lateinit var callerIdentity: String

    var callToken: String? = preferenceHelper.getCallsToken()


    fun init(identity: String, listener: CallsSDKTokenListener? = null) {
        this.callerIdentity = identity
        retrieveAccessTokenFromServer(listener)
    }


    private val onHTTPCallback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            tokenListener?.tokenResponse()
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                callToken = response.body()?.string()
                if (!callToken.isNullOrBlank())
                    preferenceHelper.storeCallsToken(callToken!!)
                else
                    LogUtils.d(CommonTAG, "Retrieving Twilio token: ERROR")
            } else LogUtils.d(CommonTAG, "Retrieving Twilio token: ERROR")
            tokenListener?.tokenResponse(callToken)
        }
    }

    private fun retrieveAccessTokenFromServer(tokenListener: CallsSDKTokenListener? = null) {
        this.tokenListener = tokenListener
        val client = OkHttpClient()
        val request = Request.Builder().url("$TOKEN_URL?identity=$callerIdentity").build()
        client.newCall(request).enqueue(onHTTPCallback)
    }

    interface CallsSDKTokenListener {
        fun tokenResponse(token: String? = null)
        //        fun sendMessageResponse(call: Call, response: Response? = null, action: String)
    }

}


object CallsNotificationUtils : KoinComponent {
    private val preferenceHelper: PreferenceHelper by inject()
    val logTag = CallsNotificationUtils::class.java.simpleName
    var mManageCallListener: ManageCallListener? = null

    private val NOTIFICATION_URL = "${if (BuildConfig.USE_PROD_CONNECTION) BASE_URL_PROD else BASE_URL}/sendvoippush?"
    private val NOTIFICATION_URL_MANAGE = "${if (BuildConfig.USE_PROD_CONNECTION) BASE_URL_PROD else BASE_URL}/ManageCall?"


    //Constant values for ACTIONS
    const val CALL_ACTION_DECLINED = "declined"
    const val CALL_ACTION_CLOSED = "closed"
    const val CALL_ACTION_ANSWERED = "answeredCall"     // makes the server send push to YOUR OTHER DEVICES

    //The keys sent by the model.IncomingCall model class
    private const val KEY_NOTIFY_CALLER_ID = "callerID"
    private const val KEY_NOTIFY_CALLER_NAME = "callerName"
    private const val KEY_NOTIFY_CALLER_AVATAR = "avatarURL"
    private const val KEY_NOTIFY_CALLER_WALL = "wallURL"
    private const val KEY_NOTIFY_ROOM_NAME = "roomName"
    private const val KEY_NOTIFY_CALL_TYPE = "isVideo"
    private const val KEY_NOTIFY_CALL_ID = "callID"
    private const val KEY_NOTIFY_CALL_ACTION = "action"


    fun processPayload(serviceContext: Context, message: RemoteMessage?) {
        val messageData = message!!.data
        LogUtils.d(CommonTAG, messageData.toString())
        if (messageData.containsKey(KEY_NOTIFY_CALL_ACTION)) {
            // then this notif is the result of ...ManageCall()

            // INFO: 2020-01-30    TEST
//            if (messageData[KEY_NOTIFY_CALL_ACTION] != CALL_ACTION_CLOSED)
                broadcastManageNotification(messageData)

        } else {
            // then this notif is the result of the FIRST INCOMING CALL NOTIFICATION
            val incomingCall = IncomingCall(messageData[KEY_NOTIFY_CALL_ID], messageData[KEY_NOTIFY_CALLER_ID], messageData[KEY_NOTIFY_CALLER_NAME], messageData[KEY_NOTIFY_CALLER_AVATAR],
                //                messageData[KEY_NOTIFY_CALLER_WALL],
                roomName = messageData[KEY_NOTIFY_ROOM_NAME], callType = if (messageData[KEY_NOTIFY_CALL_TYPE] == "1") VoiceVideoCallType.VIDEO
                else VoiceVideoCallType.VOICE)
            LogUtils.d(CommonTAG, "From: ${incomingCall.fromIdentity}, ${incomingCall.fromIdentityName}," + "\n${incomingCall.fromIdentityAvatar},\n${incomingCall.fromIdentityWall}")
            LogUtils.d(CommonTAG, "Room Name: " + incomingCall.roomName)
            if (!incomingCall.roomName.isNullOrBlank()) broadcastCallNotification(serviceContext, incomingCall)
        }
    }


    fun sendCallNotification(context: BaseActivity, callerID: String, callerName: String, calledIdentity: User, callType: VoiceVideoCallType = VoiceVideoCallType.VOICE) {
        LogUtils.e(CommonTAG, "sendCallNotification() --> call for Token")
        val calledId = calledIdentity.uid
        val calledName = calledIdentity.name
        val calledAvatar = calledIdentity.picture
        //        val calledWall = calledIdentity.wallImageLink
        val uuid = UUID.randomUUID().toString()

        context.run {
            showLoader(this.getString(R.string.loading_call, calledName))
        }

        val client = OkHttpClient()
        LogUtils.e(CommonTAG, "sendCallNotification() --> uuid--> $uuid" + "callerName=$callerName" + "&callerID=$callerID" + "&userID=$calledId" + "&isVideo=${(if (callType == VoiceVideoCallType.VIDEO) 1 else 0)}" + "&roomName=$calledId" + "&callID=$uuid")

        val request = Request.Builder().url(NOTIFICATION_URL + "callerName=$callerName" + "&callerID=$callerID" + "&userID=$calledId" + "&isVideo=${(if (callType == VoiceVideoCallType.VIDEO) 1 else 0)}" + "&roomName=$calledId" + "&callID=$uuid").build()
        LogUtils.d(CommonTAG, request.url().toString())

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                LogUtils.e(CommonTAG, "Send VOIP push FAIL", e)

                context.run {
                    runOnUiThread {
                        hideLoader()
                        showMessage(R.string.error_calls_start)
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                context.run { runOnUiThread { hideLoader() } }
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    LogUtils.d(CommonTAG, "Send VOIP push SUCCESS -> $responseBody")
                    when (responseBody) {
                        "OK" -> {
                            LogUtils.e(CommonTAG, "onResponse() --> start call Activity \n responseBody --> $responseBody")
                            context.startActivity(Intent(context, CallActivity::class.java).apply {
                                putExtra(BUNDLE_KEY_CALL_TYPE, callType)
                                //need to add
                                putExtra(BUNDLE_KEY_USAGE_TYPE, VoiceVideoUsageType.OUTGOING)
                                putExtra(BUNDLE_KEY_CALL_STATUS, VoiceVideoStatus.DEFAULT)
                                putExtra(BUNDLE_KEY_CALL_PERSON_ID, calledId)
                                putExtra(BUNDLE_KEY_CALL_PERSON_NAME, calledName)
                                putExtra(BUNDLE_KEY_CALL_PERSON_AVATAR, calledAvatar)
                                //                                    putExtra(CALL_NOTIFICATION_IDENTITY_WALL, calledWall)
                                putExtra(BUNDLE_KEY_CALL_ROOM_NAME, calledId)
                                putExtra(BUNDLE_KEY_CALL_ID, uuid)

                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                action = NOTIFICATION_ACTION_CALL_FIRST_START_ACT
                            })
                        }
                        "KO" -> {
                            context.run {
                                runOnUiThread {
                                    showDialogGeneric(this, context.getString(R.string.error_calls_busy, calledName), buttonPositive = context.getString(R.string.btn_ok), onPositive = DialogInterface.OnClickListener { dialog, _ -> dialog?.dismiss() }, show = true)
                                }
                            }
                        }
                    }
                } else {
                    LogUtils.e(CommonTAG, "CN->CallsUtils FN--> onResponse() --> $response")
                    LogUtils.e(CommonTAG, "Send VOIP push FAIL: Response unsuccessful")
                    context.run {
                        runOnUiThread {
                            showMessage(R.string.error_calls_start)
                        }
                    }
                }
            }
        })
    }


    fun sendCallManageNotification(userID: String, action: String, callID: String, wantsToken: Boolean = false) {
        LogUtils.e(CommonTAG, "CN->CallsUtils FN--> sendCallManageNotification() --> $NOTIFICATION_URL_MANAGE + userID=$userID" + "&action=$action" + "&callID=$callID" + if (wantsToken) "&token=${preferenceHelper.getDeviceToken()}" else "")
        val client = OkHttpClient()
        val request = Request.Builder().url(NOTIFICATION_URL_MANAGE + "userID=$userID" + "&action=$action" + "&callID=$callID" + if (wantsToken) "&token=${preferenceHelper.getDeviceToken()}" else "").build()
        LogUtils.d(CommonTAG, request.url().toString())

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                mManageCallListener?.manageStopService(action, e.message)
                LogUtils.e(CommonTAG, "Send MANAGE NOTIFICATION FAILURE: $action")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //                    CallsSDKTokenUtils.tokenListener?.sendMessageResponse(call,response,action)
                } else LogUtils.d(CommonTAG, "Send MANAGE NOTIFICATION FAILURE: $action")
                mManageCallListener?.manageStopService(action, response.isSuccessful)
            }
        })

    }


    private fun Intent.addBundleExtras(incomingCall: IncomingCall): Intent {

        putExtra(BUNDLE_KEY_CALL_PERSON_ID, incomingCall.fromIdentity)
        putExtra(BUNDLE_KEY_CALL_PERSON_NAME, incomingCall.fromIdentityName)
        putExtra(BUNDLE_KEY_CALL_PERSON_AVATAR, incomingCall.fromIdentityAvatar)
        //            this.putExtra(CALL_NOTIFICATION_IDENTITY_WALL, incomingCall.fromIdentityWall)
        putExtra(BUNDLE_KEY_CALL_ROOM_NAME, incomingCall.roomName)
        putExtra(BUNDLE_KEY_CALL_TYPE, incomingCall.callType)
        putExtra(BUNDLE_KEY_USAGE_TYPE, VoiceVideoUsageType.INCOMING)
        putExtra(BUNDLE_KEY_CALL_ID, incomingCall.id)
        return this
    }

    //Broadcast the Call Notification to the Activity
    private fun broadcastCallNotification(context: Context, incomingCall: IncomingCall) {

        val notificationIntent = Intent().addBundleExtras(incomingCall).apply {
            action = NOTIFICATION_ACTION_CALL_FIRST_START_ACT
        }

        try {
            if (!isMyServiceRunning(context, CallServices::class.java)) {
                CallServices.startService(context, notificationIntent.apply { setClass(context, CallServices::class.java) })
            }
        } catch (e: IllegalStateException) {

            // Attempt to start service with app in the background
            LogUtils.e(CommonTAG, "Attempt to start Calls service in background", e)
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent.apply {
                setClass(context, CallActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }, 0)

            // initialize pending intents
            val rejectPending = PendingIntent.getActivity(context, 0, Intent().addBundleExtras(incomingCall).apply {
                setClass(context, CallActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                action = NOTIFICATION_ACTION_CALL_REJECT_START_ACT
            }, 0)

            val acceptPending = PendingIntent.getActivity(context, 0, Intent().addBundleExtras(incomingCall).apply {
                setClass(context, CallActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                action = NOTIFICATION_ACTION_CALL_ACCEPT_START_ACT
            }, 0)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(SERVICE_NOTIFICATION_ID, CallServices.getStartupIncomingNotification(context, incomingCall.callType, pendingIntent, acceptPending, rejectPending))
        }

    }

    //Broadcast the Call Notification to the Activity
    private fun broadcastManageNotification(messageData: Map<String, String>) {
        val callID = messageData[KEY_NOTIFY_CALL_ID]
        val action = messageData[KEY_NOTIFY_CALL_ACTION]

        if (areStringsValid(callID, action)) {
            if (callID != CallServices.currentCallID) return
            val mBundle = Bundle().apply {
                putString(BUNDLE_KEY_CALL_ID, callID)
                putString(BUNDLE_KEY_CALL_MANAGE_ACTION, action)
            }
            mManageCallListener?.manageCall(mBundle)
        }

    }


    interface ManageCallListener {
        fun manageCall(bundle: Any? = null)
        fun manageStopService(action: Any? = null, status: Any? = null)
    }

    /**
     * Build a notification ad hoc for calls.
     * @param primaryPendingIntent the pending intent common to all CALL notifications
     * @return the [Notification.Builder]
     */
    fun getBaseBuilder(context: Context, primaryPendingIntent: PendingIntent, callType: VoiceVideoCallType, usageType: VoiceVideoUsageType, ongoing: Boolean = true, autoCancel: Boolean = false, title: String? = null, body: String? = null, contentViewExpanded: RemoteViews? = null, contentViewCollapsed: RemoteViews? = null): NotificationCompat.Builder {

        val channelID = context.getString(R.string.notification_channel_id_calls)
        if (hasOreo()) {
            val callInviteChannel = NotificationChannel(channelID, context.getString(R.string.notif_channel_calls_description), NotificationManager.IMPORTANCE_HIGH)
            callInviteChannel.description = context.getString(R.string.notif_channel_calls_description)
            callInviteChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            callInviteChannel.enableVibration(false)
            callInviteChannel.enableLights(true)
            callInviteChannel.lightColor = Color.RED
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(callInviteChannel)
        }

        return NotificationCompat.Builder(context, channelID).setSmallIcon(R.drawable.ic_notification_tray).setContentTitle(if (!title.isNullOrBlank()) title
            else {
                context.getString(R.string.call_notif_title, context.getString(R.string.app_name))
            }).setContentText(if (!body.isNullOrBlank()) body
            else {
                context.getString(if (usageType == VoiceVideoUsageType.OUTGOING) {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_out_vc
                    else R.string.call_notif_body_out_vd
                } else {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_inc_vc
                    else R.string.call_notif_body_inc_vd
                })
            }).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(autoCancel).setOngoing(ongoing).setTicker(context.getString(R.string.call_notif_title, context.getString(R.string.app_name))).setContentIntent(primaryPendingIntent).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_CALL).setColor(ContextCompat.getColor(context, R.color.colorAccent)).apply {

                // INFO: 2020-01-23    should cover all cases

                if (contentViewExpanded != null && contentViewCollapsed != null) {
                    setCustomBigContentView(contentViewExpanded)
                    setCustomContentView(contentViewCollapsed)
                    setStyle(NotificationCompat.DecoratedCustomViewStyle())
                } else if (contentViewExpanded != null) {
                    setCustomContentView(contentViewExpanded)
                    setStyle(NotificationCompat.DecoratedCustomViewStyle())
                } else if (contentViewCollapsed != null) {
                    setCustomContentView(contentViewCollapsed)
                    setStyle(NotificationCompat.DecoratedCustomViewStyle())
                }
            }
    }
}