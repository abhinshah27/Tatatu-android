package rs.highlande.app.tatatu.feature.voiceVideoCalls.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.twilio.video.Room
import com.vincent.videocompressor.LogUtils
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.service.BaseService
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.splash.view.SplashActivity
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.*
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsNotificationUtils.CALL_ACTION_ANSWERED
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsNotificationUtils.CALL_ACTION_CLOSED
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsNotificationUtils.CALL_ACTION_DECLINED
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsNotificationUtils.sendCallManageNotification
import rs.highlande.app.tatatu.feature.voiceVideoCalls.view.CallActivity


/**
 * Created by Abhin.
 */
class CallServices : BaseService(), CallsSDKTokenUtils.CallsSDKTokenListener, CallsNotificationUtils.ManageCallListener {

    enum class NotificationType { STARTUP, ONGOING, MISSED_CALL, ACCEPT_CALL }

    private val userRepo: UsersRepository by inject()

    //Target we publish for clients to send messages to IncomingHandler.
    var mMessenger: Messenger? = null

    internal var callType = VoiceVideoCallType.VOICE
    private var callStatus = VoiceVideoStatus.DEFAULT
    private var usageType = VoiceVideoUsageType.INCOMING
    var roomName: String? = null
    var participantId: String? = null
    var showedName: String? = null
    var showedAvatar: String? = null
    var callStartedTime: Any? = null
    var isAskConvertAudioToVideoDialog = false

    private var isMute = false
    private var isSpeaker = false
    private var isDestroy = false
    private var isStartCallImmediately = false
    private var isOnResumeCall = false
    private var soundPoolManager: SoundPoolManager? = null
    private lateinit var audioManager: AudioManager
    private var helper: VoiceVideoCallHelper? = null
    private var mNotificationBuilder: NotificationCompat.Builder? = null
    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val notificationIntent by lazy {
        PendingIntent.getService(this, 0, Intent(this, CallServices::class.java).apply { action = ACTION_MAIN }, 0)
    }

    private val closeIntent by lazy {
        PendingIntent.getService(this, 0, Intent(this, CallServices::class.java).apply {
            action = NOTIFICATION_ACTION_CALL_CLOSE
        }, 0)
    }

    private val declineIntent by lazy {
        PendingIntent.getService(this, 0, Intent(this, CallServices::class.java).apply {
            action = NOTIFICATION_ACTION_CALL_DECLINE
        }, 0)
    }

    private val acceptIntent by lazy {
        PendingIntent.getService(this, 0, Intent(this, CallServices::class.java).apply {
            action = NOTIFICATION_ACTION_CALL_ACCEPT
        }, 0)
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    override fun onBind(p0: Intent?): IBinder? {
        LogUtils.e(CommonTAG, "onBind() --> ")
        val mMessenger = Messenger(IncomingHandler(this))
        return mMessenger.binder
    }

    override fun onCreate() {
        super.onCreate()
        isDestroy = false
        CallsNotificationUtils.mManageCallListener = this
        LogUtils.e(CommonTAG, "CN->CallServices FN--> onCreate() --> ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.e(CommonTAG, "CN->CallServices FN--> onStartCommand() --> ${intent?.action}")
        manageIntent(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun manageIntent(intent: Intent?) {
        LogUtils.e(CommonTAG, "service started manageIntent() intent?.action--> ${intent?.action}")

        intent?.let {
            if (intent.hasExtra(BUNDLE_KEY_CALL_TYPE)) callType = intent.getSerializableExtra(BUNDLE_KEY_CALL_TYPE) as VoiceVideoCallType
            if (intent.hasExtra(BUNDLE_KEY_USAGE_TYPE)) usageType = intent.getSerializableExtra(BUNDLE_KEY_USAGE_TYPE) as VoiceVideoUsageType
            if (intent.hasExtra(BUNDLE_KEY_CALL_ROOM_NAME)) roomName = intent.getStringExtra(BUNDLE_KEY_CALL_ROOM_NAME) ?: ""

            if (intent.hasExtra(BUNDLE_KEY_CALL_PERSON_ID)) participantId = intent.getStringExtra(BUNDLE_KEY_CALL_PERSON_ID) ?: ""
            LogUtils.d(logTag, "testPARTICIPANTID: $participantId in manageIntent()")


            if (intent.hasExtra(BUNDLE_KEY_CALL_PERSON_NAME)) showedName = intent.getStringExtra(BUNDLE_KEY_CALL_PERSON_NAME) ?: ""
            if (intent.hasExtra(BUNDLE_KEY_CALL_PERSON_AVATAR)) showedAvatar = intent.getStringExtra(BUNDLE_KEY_CALL_PERSON_AVATAR) ?: ""

            if (intent.hasExtra(BUNDLE_KEY_CALL_ID)) currentCallID = intent.getStringExtra(BUNDLE_KEY_CALL_ID)

            if (helper == null) {
                initHelper(userRepo)
            }

            when (it.action) {
                NOTIFICATION_ACTION_CALL_ACCEPT -> {
                    withOutPermissionRingingAction(false)
                    //close the notification try
                    sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

                    callStartedTime = SystemClock.elapsedRealtime()
                    if (callType == VoiceVideoCallType.VOICE && !isAudioPermissionGranted(this)) {
                        openActivity(true)
                        return
                    } else if (callType == VoiceVideoCallType.VIDEO) {
                        openActivity(isVideoAccept = true)
                        return
                    }

                    if (helper == null) {
                        //                        isStartCallImmediately = true
                        callStatus = VoiceVideoStatus.DEFAULT
                        initHelper(userRepo)
                    }
                    if ((this.applicationContext as TTUApp).foregroundManager.isForeground && isCallActivityForeground) {
                        callStatus = VoiceVideoStatus.ACCEPT
                        sendDataToActivity(ACTION_CALL_ACCEPT)
                    }
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> manageIntent() --> connectToRoom")
                    helper?.connectToRoom(roomName)
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> manageIntent() --> ACCEPT_CALL")
                    showNotification(NotificationType.ACCEPT_CALL)


                    if (currentCallID != null) {
                        // sends the manage call to notify server that all other unanswered devices must end the call
                        sendCallManageNotification(userRepo.fetchCachedMyUserId(), CALL_ACTION_ANSWERED, currentCallID!!, true)
                    } else {
                        LogUtils.e(logTag, "currentCallID: NULL")
                    }
                }
                NOTIFICATION_ACTION_CALL_DECLINE -> {
                    stopService(CALL_ACTION_DECLINED)
                }
                NOTIFICATION_ACTION_CALL_CLOSE -> {
                    stopService(CALL_ACTION_CLOSED)
                }

                NOTIFICATION_ACTION_CALL_SPEAKER -> {
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> manageIntent() --> CALL_SPEAKER")
                    isSpeaker = !isSpeaker
                    helper?.handleSpeakersToggle(isSpeaker)
                    serviceSpeakerMuteMessageSend(ACTION_SPEAKER, isSpeaker, mMessenger)
                    showNotification(NotificationType.ACCEPT_CALL)
                }

                NOTIFICATION_ACTION_CALL_MUTE -> {
                    isMute = !isMute
                    helper?.toggleMute(isMute)
                    serviceSpeakerMuteMessageSend(ACTION_MUTE, isMute, mMessenger)
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> manageIntent() --> CALL_MUTE")
                    showNotification(NotificationType.ACCEPT_CALL)
                }

                NOTIFICATION_ACTION_CALL_FIRST_START_ACT -> {
                    if (usageType == VoiceVideoUsageType.INCOMING) {
                        startForeground(SERVICE_NOTIFICATION_ID, getStartupNotification(this, callType, usageType, notificationIntent, acceptIntent, declineIntent, closeIntent))
                    } else {
                        //                        openActivity()
                    }
                }

                ACTION_MAIN -> {
                    //stop ringing if start
                    withOutPermissionRingingAction(false)
                    if ((this.applicationContext as TTUApp).foregroundManager.isForeground && isCallActivityForeground) {
                        sendDataToActivity(ACTION_CALL_MAIN)
                    } else {
                        openActivity()
                    }
                }

                NOTIFICATION_ACTION_CALL_MANAGE -> {
                    LogUtils.e("Tag", "manageIntent() --> NOTIFICATION_ACTION_CALL_MANAGE Enter")
                    // TODO: 2019-12-18    SEND RELATED server calls
                    //  through CallNotificationUtils.sendCallManageNotification()
                    it.getStringExtra(BUNDLE_KEY_CALL_MANAGE_ACTION)?.let { action ->
                        LogUtils.e("Tag", "manageIntent() --> NOTIFICATION_ACTION_CALL_MANAGE --> $action")
                        when (action) {
                            CALL_ACTION_ANSWERED -> {
                                LogUtils.e("Tag", "manageIntent() --> NOTIFICATION_ACTION_CALL_MANAGE --> CALL_ACTION_ANSWERED")
                                // connect to the room and start the call
                                LogUtils.e(CommonTAG, "CN->CallServices FN--> manageIntent() connectToRoom --> CALL_ACTION_ANSWERED -> $CALL_ACTION_ANSWERED")
                                helper?.connectToRoom(roomName)
                                sendCallManageNotification(userRepo.fetchCachedMyUserId(), CALL_ACTION_ANSWERED, currentCallID!!, true)
                            }

                            CALL_ACTION_CLOSED, CALL_ACTION_DECLINED -> {
                                LogUtils.e("Tag", "manageIntent() --> NOTIFICATION_ACTION_CALL_MANAGE --> CALL_ACTION_CLOSED,CALL_ACTION_DECLINED")
                                stopService()
                            }
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    // TODO: 2019-12-17    pass to Activity UI data such as name, avatar, etc.
    internal fun openActivity(isAudioPermissionRequired: Boolean = false, isVideoAccept: Boolean = false) {
        //stop ringing if start
        withOutPermissionRingingAction(false)
        LogUtils.e(CommonTAG, "CN->CallServices FN--> openActivity() --> ")
        startActivity(Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = ACTION_MAIN
            putExtra(BUNDLE_KEY_CALL_PERSON_ID, participantId)
            putExtra(BUNDLE_KEY_USAGE_TYPE, usageType)
            putExtra(BUNDLE_KEY_CALL_TYPE, callType)
            putExtra(BUNDLE_KEY_CALL_STATUS, callStatus)
            putExtra(BUNDLE_KEY_CALL_PERSON_NAME, showedName)
            putExtra(BUNDLE_KEY_CALL_PERSON_AVATAR, showedAvatar)
            putExtra(BUNDLE_KEY_CALL_ROOM_NAME, roomName)
            putExtra(BUNDLE_KEY_CALL_ID, currentCallID)
            putExtra(BUNDLE_KEY_CALL_TIME, callStartedTime.toString())
            putExtra(BUNDLE_KEY_AUDIO_PERMISSION_REQUIRED, isAudioPermissionRequired)
            putExtra(BUNDLE_KEY_VIDEO_ACCEPT, isVideoAccept)
            putExtra(BUNDLE_KEY_CALL_MUTE, isMute)
            putExtra(BUNDLE_KEY_DIALOG, isAskConvertAudioToVideoDialog)
        })

        if (mMessenger != null && isAskConvertAudioToVideoDialog) {
            sendDataToActivity(ACTION_CALL_MAIN)
        }
    }

    var helperInitiating = false
    fun initHelper(usersRepository: UsersRepository) {
        LogUtils.e(CommonTAG, "CN->CallServices FN--> initHelper() --> ")

        if (isAudioPermissionGranted(this)) {
            LogUtils.e(CommonTAG, "CN->CallServices FN--> initHelper() --> helperInitiating: $helperInitiating for service: $this")
            if (!helperInitiating) {

                helperInitiating = true

                LogUtils.e(CommonTAG, "CN->CallServices FN--> initHelper() --> Permission Granted")
                if (!CallsSDKTokenUtils.callToken.isNullOrBlank()) {
                    LogUtils.e(
                        CommonTAG,
                        "CN->CallServices FN--> initHelper() --> token: ${CallsSDKTokenUtils.callToken}"
                    )

//                    val testToken = CallsSDKTokenUtils.callToken!!.substring(1) //only for testing
//                    LogUtils.e(CommonTAG, "CN->CallServices FN--> tokenResponse() --> testToken-->$testToken")

                    helper = VoiceVideoCallHelper(
                        this,
                        CallsSDKTokenUtils.callToken/*testToken*/,
                        callType,
                        usageType,
                        roomName,
                        isStartCallImmediately
                    )

                    helperInitiating = false
                } else {
                    LogUtils.e(
                        CommonTAG,
                        "CN->CallServices FN--> initHelper() --> Generate new token"
                    )
                    CallsSDKTokenUtils.init(
                        usersRepository.fetchCachedMyUserId(),
                        this as CallsSDKTokenUtils.CallsSDKTokenListener
                    )
                }
            }
        } else {
            LogUtils.e(CommonTAG, "CN->CallServices FN--> initHelper() --> withOutPermissionRingingAction()")
            withOutPermissionRingingAction()
        }
    }

    // TODO: 2019-12-17    pass to Activity UI data such as name, avatar, etc.
    internal fun sendDataToActivity(action: Int) {
        val mBundle = Bundle().apply {
            putString(BUNDLE_KEY_CALL_PERSON_ID, participantId)
            putSerializable(BUNDLE_KEY_USAGE_TYPE, usageType)
            putSerializable(BUNDLE_KEY_CALL_TYPE, callType)
            putSerializable(BUNDLE_KEY_CALL_STATUS, callStatus)
            putString(BUNDLE_KEY_CALL_PERSON_NAME, showedName)
            putString(BUNDLE_KEY_CALL_PERSON_AVATAR, showedAvatar)
            putString(BUNDLE_KEY_CALL_ROOM_NAME, roomName)
            putString(BUNDLE_KEY_CALL_ID, currentCallID)
            putString(BUNDLE_KEY_CALL_TIME, callStartedTime.toString())
            putBoolean(BUNDLE_KEY_CALL_MUTE, isMute)
            putBoolean(BUNDLE_KEY_DIALOG, isAskConvertAudioToVideoDialog)
        }
        serviceSpeakerMuteMessageSend(action, mBundle, mMessenger)
    }

    //get token from TATATU server using interface
    override fun tokenResponse(token: String?) {
        token?.let {
            if (isAudioPermissionGranted(this)) {
                LogUtils.e(CommonTAG, "CN->CallServices FN--> tokenResponse() --> token response permission granted $token")
                //                val testToken = token.substring(1) //only for testing
                //                LogUtils.e(CommonTAG, "CN->CallServices FN--> tokenResponse() --> testToken-->$testToken")
                helper = VoiceVideoCallHelper(this, it, callType, usageType, roomName, isStartCallImmediately)

                helperInitiating = false

                if (isOnResumeCall) {
                    isOnResumeCall = false
                    helper?.onResume()
                }
            }
            return
        }

        ///Stop Service and give a error Message
        if (token.isNullOrEmpty()) {
            val mMessage = Message.obtain(null, HANDLER_HANDLER_ACTION_ERROR, 0, 0)
            mMessage?.obj = getString(R.string.error_calls_video)
            mMessenger?.send(mMessage)
            stopService()
        }
    }

    override fun onDestroy() {
        isDestroy = true
        if (helper != null) {
            helper?.onDestroy()
        } else {
            soundPoolManager?.release()
        }

        isVideoMute = false
        LogUtils.e(CommonTAG, "onDestroy() --> Service")
        super.onDestroy()
    }

    //Handler of incoming messages from clients.
    class IncomingHandler(private val callServices: CallServices) : Handler() {
        override fun handleMessage(msg: Message) {
            LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() msg.what--> ${msg.what}")
            when (msg.what) {
                SERVICE_REGISTER_BIND -> {
                    logMessage("SERVICE_REGISTER_BIND")
                    callServices.mMessenger = msg.replyTo
                    getDataFromActivity(msg)
                    if (callServices.callType == VoiceVideoCallType.VIDEO) {
                        callServices.helper?.createAudioAndVideoTracks()
                    }
                    if (callServices.usageType == VoiceVideoUsageType.OUTGOING) {
                        callServices.startForeground(SERVICE_NOTIFICATION_ID, getStartupNotification(callServices, callServices.callType, callServices.usageType, callServices.notificationIntent, callServices.acceptIntent, callServices.declineIntent, callServices.closeIntent))
                    }

                    LogUtils.e(logTag, "PENDINGMESSAGE: checking list")
                }
                SERVICE_UNREGISTER_UNBIND -> {
                    logMessage("SERVICE_UNREGISTER_UNBIND")
                    callServices.mMessenger = null
                }
                // [START] - CALL MANAGEMENT
                ACTION_CALL_DECLINE -> {

                    callServices.stopService(CALL_ACTION_DECLINED)
                }
                ACTION_CALL_CLOSE -> {
                    logMessage("call Action ->${msg.what}")
                    callServices.stopService(CALL_ACTION_CLOSED)
                }
                ACTION_CALL_ACCEPT -> {
                    //get data form activity
                    logMessage("ACTION_CALL_ACCEPT")
                    getCallAcceptFormActivity(msg)
                }
                // [END] - CALL MANAGEMENT
                ACTION_ACTIVITY_PAUSED -> {
                    logMessage("ACTION_ACTIVITY_PAUSED")
                    if (!callServices.isDestroy) callServices.helper?.onPause()
                }
                ACTION_ACTIVITY_SPEAKER -> {
                    val isSpeaker = msg.obj as Boolean
                    logMessage("ACTION_ACTIVITY_SPEAKER isSpeaker--> $isSpeaker")
                    callServices.isSpeaker = isSpeaker
                    callServices.helper?.handleSpeakersToggle(isSpeaker)
                    callServices.showNotification(NotificationType.ACCEPT_CALL)
                }
                ACTION_ACTIVITY_MUTE -> {
                    val isMute = msg.obj as Boolean
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() --> CALL_MUTE --> $isMute")
                    callServices.isMute = isMute
                    callServices.helper?.toggleMute(isMute)
                    callServices.showNotification(NotificationType.ACCEPT_CALL)
                }
                ACTION_VIDEO_MUTE -> {
                    val isMute = msg.obj as Boolean
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() --> ACTION_VIDEO_MUTE --> $isMute")
                    isVideoMute = !isMute
                    callServices.helper?.toggleVideoMute(isMute)
                }
                ACTION_ACTIVITY_RESUMED -> {
                    logMessage("ACTION_ACTIVITY_RESUMED")
                    if (callServices.helper != null) callServices.helper?.onResume()
                    else {
                        callServices.isOnResumeCall = true
                    }
                }
                ACTION_CANCEL_TIMER -> {
                    logMessage("ACTION_CANCEL_TIMER")
                    callServices.helper?.cancelTimer()
                }
                ACTION_AUDIO_CONVERT_VIDEO -> {
                    logMessage("ACTION_AUDIO_CONVERT_VIDEO msg.obj msg.arg1-->${msg.arg1}")
                    if (msg.arg1 == 1) {
                        callServices.helper?.audioConvertVideo()
                    } else {
                        callServices.helper?.audioConvertVideo(false)
                    }
                }

                ACTION_REQUEST_LOCAL_CAMERA_CAPTURE -> {
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() --> ACTION_REQUEST_LOCAL_CAMERA_CAPTURE ")
                    callServices.sendCameraCapture()
                }
                //get request from fragment for localVideoTrack
                ACTION_GET_LOCAL_VIDEO_TRACK -> {
                    if (msg.obj is String) {
                        if ((msg.obj as String).equals("CONVERT_TO_VIDEO", true)) {
                            callServices.helper?.createAudioAndVideoTracks(true)
                            callServices.helper?.putSpeakerOn()
                        }
                    } else {
                        //send localVideoTrack to fragment
                        val mMessage = Message.obtain(null, ACTION_GET_LOCAL_VIDEO_TRACK, 0, 0)
                        if (callServices.helper != null) {
                            mMessage?.obj = callServices.helper?.localVideoTrack
                            callServices.mMessenger?.send(mMessage)
                        } else {
                            callServices.helper?.createAudioAndVideoTracks(true)
                        }
                    }
                }
                ACTION_VOICE_VIDEO_PERMISSION_ALLOWED -> {  // when user first time allow voice video permission
                    if (callServices.helper == null) {
                        LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() --> initHelper")
                        callServices.isStartCallImmediately = true
                        callServices.initHelper(callServices.userRepo)
                    } else {
                        LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() --> connectToRoom")
                        if (callServices.helper?.room == null || callServices.helper?.room?.state == Room.State.DISCONNECTED) callServices.helper?.connectToRoom(callServices.roomName)
                    }
                }
                ACTION_REQUEST_ROOM_PARTICIPANTS -> {
                    //send localVideoTrack to fragment
                    val mMessage = Message.obtain(null, ACTION_GET_ROOM_PARTICIPANTS, 0, 0)
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() --> ${callServices.helper?.room}")
                    LogUtils.e(CommonTAG, "CN->CallServices FN--> handleMessage() isConnected--> ${callServices.helper?.isConnected}")
                    mMessage?.obj = if (!callServices.helper?.room?.remoteParticipants.isNullOrEmpty()) {
                        if (!callServices.helper?.room?.remoteParticipants?.get(0)?.remoteVideoTracks.isNullOrEmpty()) {
                            callServices.helper?.room?.remoteParticipants?.get(0)?.remoteVideoTracks?.get(0)?.remoteVideoTrack
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                    callServices.mMessenger?.send(mMessage)
                }
                ACTION_REQUEST_CHANGE_DIALOG_STATUS -> {
                    callServices.isAskConvertAudioToVideoDialog = false
                }
                else -> super.handleMessage(msg)
            }
        }

        private fun getCallAcceptFormActivity(msg: Message) {
            LogUtils.e(CommonTAG, "CN->CallServices FN--> getCallAcceptFormActivity() --> Enter")
            if (callServices.helper == null) {
                LogUtils.e(CommonTAG, "CN->CallServices FN--> getCallAcceptFormActivity() --> initHelper")
                callServices.isStartCallImmediately = true
                callServices.initHelper(callServices.userRepo)
            } else {
                LogUtils.e(CommonTAG, "CN->CallServices FN--> getCallAcceptFormActivity() --> connectToRoom")
                if (callServices.helper?.room == null || callServices.helper?.room?.state == Room.State.DISCONNECTED)  callServices.helper?.connectToRoom(callServices.roomName)
            }
            getDataFromActivity(msg)
            LogUtils.e(CommonTAG, "CN->CallServices FN--> getCallAcceptFormActivity() --> callType --> " + "${callServices.callType}, usageType --> ${callServices.usageType}, " + "callStatus --> ${callServices.callStatus}, showedName --> " + "${callServices.showedName}, showedAvatar --> ${callServices.showedAvatar}")
            callServices.showNotification(NotificationType.ACCEPT_CALL)
            // sends the manage call to notify server that all other unanswered devices must end the call
            sendCallManageNotification(callServices.userRepo.fetchCachedMyUserId(), CALL_ACTION_ANSWERED, currentCallID!!, true)
        }

        private fun getDataFromActivity(msg: Message) {
            val mBundle = msg.obj as Bundle
            callServices.usageType = mBundle.getSerializable(BUNDLE_KEY_USAGE_TYPE) as VoiceVideoUsageType
            callServices.callType = mBundle.getSerializable(BUNDLE_KEY_CALL_TYPE) as VoiceVideoCallType
            callServices.callStatus = mBundle.getSerializable(BUNDLE_KEY_CALL_STATUS) as VoiceVideoStatus
            callServices.showedName = mBundle.getString(BUNDLE_KEY_CALL_PERSON_NAME)
            callServices.showedAvatar = mBundle.getString(BUNDLE_KEY_CALL_PERSON_AVATAR)
            callServices.roomName = mBundle.getString(BUNDLE_KEY_CALL_ROOM_NAME)
            callServices.participantId = mBundle.getString(BUNDLE_KEY_CALL_PERSON_ID)
            LogUtils.d(logTag, "testPARTICIPANTID: ${callServices.participantId} in IncomingHandler")
            currentCallID = mBundle.getString(BUNDLE_KEY_CALL_ID)
        }

        private fun logMessage(msg: String) {
            LogUtils.e(CommonTAG, "CN->CallServices FN--> logMessage() --> $msg")
        }
    }

    internal fun sendCameraCapture(cameraCaptureCompat: CameraCapturerCompat? = helper?.cameraCaptureCompat) {
        val msg = Message.obtain(null, ACTION_GET_LOCAL_CAMERA_CAPTURE, 0, 0)
        msg.obj = cameraCaptureCompat
        mMessenger?.send(msg)
    }

    internal fun stopService(action: String? = null) {
        LogUtils.e("Tag", "stopService() --> action --> $action")
        //stop ringing if start
        withOutPermissionRingingAction(false)
        // if action is evaluated -> send the manage call to stop service
        action?.let {
            if (areStringsValid(participantId, currentCallID)) {
                sendCallManageNotification(participantId!!, action, currentCallID!!)
            }
        }
        serviceSpeakerMuteMessageSend(HANDLER_ACTIVITY_REJECT_CLOSE, null, mMessenger)
        helper?.disconnectRoom()
        helper?.soundPoolManager?.stopRinging()
        helper?.soundPoolManager?.release()
        notificationManager.cancel(SERVICE_NOTIFICATION_ID)
        if (action == null) {
            stopForeground(true)
            stopSelf()
        }
    }


    internal fun showNotification(type: NotificationType) {
        LogUtils.e(CommonTAG, "CN->CallServices FN--> showNotification() type--> $type")
        val notification: Notification = when (type) {
            NotificationType.STARTUP -> {
                getStartupNotification(this, callType, usageType, this.notificationIntent, acceptIntent, declineIntent, this.closeIntent)!!
            }
            NotificationType.ONGOING -> {
                val builder = CallsNotificationUtils.getBaseBuilder(this, notificationIntent, callType, usageType)
                with(builder) {
                    this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_hangup), closeIntent).build())
                }
                builder.setOngoing(true).build()
            }
            NotificationType.MISSED_CALL -> {
                val appIntent = Intent(this, if ((this.applicationContext as TTUApp).foregroundManager.isForeground) CallActivity::class.java else SplashActivity::class.java)
                appIntent.action = Intent.ACTION_MAIN
                val appPending = PendingIntent.getService(this, 0, appIntent, 0)
                val builder = CallsNotificationUtils.getBaseBuilder(this, appPending, callType, usageType, autoCancel = true, ongoing = false, body = getString(R.string.calls_missed_from, showedName))
                builder.setAutoCancel(true).build()
            }
            NotificationType.ACCEPT_CALL -> {
                //speaker pending Intent
                val speakerIntent = Intent(this, CallServices::class.java)
                speakerIntent.action = NOTIFICATION_ACTION_CALL_SPEAKER
                val speakerPending = PendingIntent.getService(this, 0, speakerIntent, 0)

                //mute pending Intent
                val muteIntent = Intent(this, CallServices::class.java)
                muteIntent.action = NOTIFICATION_ACTION_CALL_MUTE
                val mutePending = PendingIntent.getService(this, 0, muteIntent, 0)

                val builder = CallsNotificationUtils.getBaseBuilder(this, notificationIntent, callType, usageType)
                with(builder) {

                    // adds usual "Hang up" action
                    this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_hangup), closeIntent).build())

                    if (callType == VoiceVideoCallType.VIDEO && usageType == VoiceVideoUsageType.OUTGOING) {
                        //Log
                        LogUtils.e(CommonTAG, "CN->CallServices FN--> showNotification() --> callType == VoiceVideoCallType.VIDEO && usageType == VoiceVideoUsageType.OUTGOING get true")
                    } else {
                        // adds "Toggle speaker" action
                        if (!isSpeaker) this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_speaker), speakerPending).build())
                        else this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_earpiece), speakerPending).build())

                        // adds "Toggle mute" action
                        if (!isMute) this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_mute), mutePending).build())
                        else this.addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_calls_un_mute), mutePending).build())
                    }
                }
                callStatus = VoiceVideoStatus.ACCEPT
                mNotificationBuilder = builder
                builder.build()
            }
        }
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    fun changeNotificationBody(isReconnecting: Boolean = true, isDisconnected: Boolean = false) {
        val body = when {
            isReconnecting -> {
                resources.getString(R.string.call_connection_lost_reconnecting)
            }
            isDisconnected -> {
                resources.getString(R.string.not_able_to_connect_the_call)
            }
            else -> {
                getString(if (usageType == VoiceVideoUsageType.OUTGOING) {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_out_vc
                    else R.string.call_notif_body_out_vd
                } else {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_inc_vc
                    else R.string.call_notif_body_inc_vd
                })
            }
        }

        mNotificationBuilder?.setContentText(body)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, mNotificationBuilder?.build())
    }


    //send Message to Service
    private fun serviceSpeakerMuteMessageSend(action: Int, bundle: Bundle?, mMessenger: Messenger?, msg: Message? = null) {
        try {
            LogUtils.e(CommonTAG, "onServiceConnected() --> $action")
            var mMessage: Message? = null
            if (msg == null) {
                mMessage = Message.obtain(null, action, 0, 0)
            }
            //Bundle Attach if it's not null
            if (bundle != null && msg != null) msg.obj = bundle
            else mMessage?.obj = bundle
            //finally send message
            if (msg != null) mMessenger?.send(msg)
            else mMessenger?.send(mMessage)
        } catch (e: RemoteException) {
            LogUtils.e(CommonTAG, "onServiceConnected() --> Error")
            e.printStackTrace()
        }
    }

    //send Message to Service
    private fun serviceSpeakerMuteMessageSend(action: Int, isBoolean: Boolean, mMessenger: Messenger?) {
        try {
            LogUtils.e(CommonTAG, "onServiceConnected() --> $action")
            val mMessage: Message? = Message.obtain(null, action, 0, 0)
            mMessage?.obj = isBoolean
            mMessenger?.send(mMessage)
        } catch (e: RemoteException) {
            LogUtils.e(CommonTAG, "onServiceConnected() --> Error")
            e.printStackTrace()
        }
    }


    companion object {
        /**
         * Command to the service to register a client, receiving callbacks
         * from the service.  The Message's replyTo field must be a Messenger of
         * the client where callbacks should be sent.
         */
        const val SERVICE_REGISTER_BIND = 1
        /**
         * Command to the service to unregister a client, ot stop receiving callbacks
         * from the service.  The Message's replyTo field must be a Messenger of
         * the client as previously given with MSG_REGISTER_CLIENT.
         */
        const val SERVICE_UNREGISTER_UNBIND = 2
        // call actions
        const val ACTION_CALL_DECLINE = 3
        const val ACTION_CALL_CLOSE = 4
        const val ACTION_CALL_ACCEPT = 5
        const val ACTION_CALL_MAIN = 6
        //fragment life cycle
        const val ACTION_ACTIVITY_PAUSED = 7
        const val ACTION_ACTIVITY_RESUMED = 8
        const val ACTION_ADD_PARTICIPANT_VIDEO_VIEW = 9
        const val ACTION_REMOVE_PARTICIPANT_AND_MANAGE_DISCONNECT = 10
        const val ACTION_MOVE_LOCAL_VIDEO_PRIMARY_VIEW = 11
        const val ACTION_STREAM_VOICE_CALL = 12
        const val HANDLER_ACTION_MUTE_CONVERSATION_VIEW = 13
        const val HANDLER_HANDLER_ACTION_ERROR = 21
        const val HANDLER_CALL_STATUS = 22
        const val HANDLER_ACTIVITY_REJECT_CLOSE = 23
        const val ACTION_MUTE = 24
        const val ACTION_SPEAKER = 25
        const val ACTION_VOLUME_CONTROL_STREAM = 26
        const val ACTION_ACTIVITY_SPEAKER = 27
        const val ACTION_ACTIVITY_MUTE = 28
        const val ACTION_VIDEO_MUTE = 29
        const val ACTION_GET_LOCAL_VIDEO_TRACK = 30
        const val ACTION_CANCEL_TIMER = 31
        const val ACTION_AUDIO_CONVERT_VIDEO = 32
        const val ACTION_VIDEO_REMOVE_PARTICIPANTS_UNSUBSCRIBE = 34
        const val ACTION_VOICE_VIDEO_PERMISSION_ALLOWED = 35
        const val ACTION_REQUEST_LOCAL_CAMERA_CAPTURE = 36
        const val ACTION_GET_LOCAL_CAMERA_CAPTURE = 37
        const val ACTION_REQUEST_ROOM_PARTICIPANTS = 38
        const val ACTION_GET_ROOM_PARTICIPANTS = 39
        const val ACTION_CALLER_VIDEO_MUTE = 40
        const val ACTION_REQUEST_CHANGE_DIALOG_STATUS = 41

        const val HANDLER_MANAGE_DISCLAIMER = 42


        const val NOTIFICATION_ACTION_CALL_DECLINE = "reject_call"
        const val NOTIFICATION_ACTION_CALL_CLOSE = "close_call"
        const val NOTIFICATION_ACTION_CALL_ACCEPT = "accept_call"
        const val NOTIFICATION_ACTION_CALL_REJECT_START_ACT = "reject_call_startup"
        const val NOTIFICATION_ACTION_CALL_CLOSE_START_ACT = "close_call_startup"
        const val NOTIFICATION_ACTION_CALL_ACCEPT_START_ACT = "accept_call_startup"
        const val NOTIFICATION_ACTION_CALL_MANAGE = "manage_call"

        const val NOTIFICATION_ACTION_CALL_SPEAKER = "call_speaker"
        const val NOTIFICATION_ACTION_CALL_MUTE = "call_mute"
        const val NOTIFICATION_ACTION_CALL_FIRST_START_ACT = "call_start_activity"

        const val ACTION_MAIN = "main"

        const val BUNDLE_KEY_USAGE_TYPE = "usageType"
        const val BUNDLE_KEY_CALL_TYPE = "callType"
        const val BUNDLE_KEY_CALL_STATUS = "callStatus"
        const val BUNDLE_KEY_CALL_PERSON_ID = "callPersonID"
        const val BUNDLE_KEY_CALL_PERSON_NAME = "callPersonName"
        const val BUNDLE_KEY_CALL_PERSON_AVATAR = "callPersonAvatar"
        const val BUNDLE_KEY_CALL_ROOM_NAME = "callRoomName"
        const val BUNDLE_KEY_CALL_ID = "callId"
        const val BUNDLE_KEY_CALL_TIME = "callTime"
        const val BUNDLE_KEY_AUDIO_PERMISSION_REQUIRED = "audioPermissionRequired"
        const val BUNDLE_KEY_VIDEO_ACCEPT = "videoAccept"
        const val BUNDLE_KEY_CALL_MANAGE_ACTION = "callManageAction"
        const val BUNDLE_KEY_CALL_MUTE = "callMute"
        const val BUNDLE_KEY_DIALOG = "isDialogOpen"

        const val SERVICE_NOTIFICATION_ID = 101

        var currentCallID: String? = null
        var isCallActivityForeground: Boolean = false
        var isVideoMute = false
        var isCallFragmentDestoryView = false

        val logTag = CallServices::class.java.simpleName

        /**
         * Launches the service performing an internal check about the app state: if a first
         * [IllegalStateException] is caught, signaling that the service was started when the app
         * was in the background, then:
         *  1. if OS is 26+, the [Context.startForegroundService] method is used, or
         *  2. a new [IllegalStateException] is thrown.
         *
         * @param context The calling [Context].
         * @param intent The wanted [Intent].
         * @throws IllegalStateException when the method is accessed with the app in background.
         */
        fun startService(context: Context, intent: Intent) {
            LogUtils.d(logTag, "CALL service: startService()")

            try {
                if (!isMyServiceRunning(context, CallServices::class.java)) {
                    context.startService(intent)
                }
            } catch (e: IllegalStateException) {
                LogUtils.e(logTag, "Cannot start background service: " + e.message, e)

                if (hasOreo()) {
                    // INFO: 2019-12-18     for Oreo
                    context.startForegroundService(intent)
                } else {
                    throw IllegalStateException("Starting CallServices when app in background with OS < 26")
                }
            }
        }

        fun getStartupIncomingNotification(context: Context, callType: VoiceVideoCallType, mainIntent: PendingIntent, acceptIntent: PendingIntent, declineIntent: PendingIntent): Notification? {
            return getStartupNotification(context, callType, VoiceVideoUsageType.INCOMING, mainIntent, acceptIntent, declineIntent)
        }

        private fun getStartupNotification(context: Context, callType: VoiceVideoCallType, usageType: VoiceVideoUsageType, mainIntent: PendingIntent, acceptIntent: PendingIntent? = null, declineIntent: PendingIntent? = null, closeIntent: PendingIntent? = null): Notification? {
            if (closeIntent == null && !(acceptIntent != null && declineIntent != null)) return null
            lateinit var contentViewCollapsed: RemoteViews
            lateinit var contentViewExpanded: RemoteViews
            when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    contentViewExpanded = RemoteViews((context as CallServices).packageName, R.layout.notification_night_calls)
                    contentViewCollapsed = RemoteViews(context.packageName, R.layout.notification_calls_night_collapsed)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    contentViewExpanded = RemoteViews((context as CallServices).packageName, R.layout.notification_calls)
                    contentViewCollapsed = RemoteViews(context.packageName, R.layout.notification_calls_collapsed)
                }
                else -> {
                    contentViewExpanded = RemoteViews((context as CallServices).packageName, R.layout.notification_calls)
                    contentViewCollapsed = RemoteViews(context.packageName, R.layout.notification_calls_collapsed)
                }
            }

            contentViewCollapsed.also {
                val body = context.getString(if (usageType == VoiceVideoUsageType.OUTGOING) {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_out_vc
                    else R.string.call_notif_body_out_vd
                } else {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_inc_vc
                    else R.string.call_notif_body_inc_vd
                })

                it.setTextViewText(R.id.txt_notification_title, context.showedName)
                it.setTextViewText(R.id.txt_notification_sub_title, body)
            }

            contentViewExpanded.also {
                val body = context.getString(if (usageType == VoiceVideoUsageType.OUTGOING) {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_out_vc
                    else R.string.call_notif_body_out_vd
                } else {
                    if (callType == VoiceVideoCallType.VOICE) R.string.call_notif_body_inc_vc
                    else R.string.call_notif_body_inc_vd
                })
                it.setTextViewText(R.id.txt_notification_title, context.showedName)
                it.setTextViewText(R.id.txt_notification_sub_title, body)
                it.setOnClickPendingIntent(R.id.txt_notification_accept, acceptIntent)
                it.setOnClickPendingIntent(R.id.txt_notification_reject, declineIntent)
            }
            val builder = if (usageType == VoiceVideoUsageType.INCOMING) {
                CallsNotificationUtils.getBaseBuilder(context, mainIntent, callType, usageType, contentViewExpanded = contentViewExpanded/*, contentViewCollapsed*/)
            } else {
                CallsNotificationUtils.getBaseBuilder(context, mainIntent, callType, usageType)
            }
            with(builder) {
                if (usageType == VoiceVideoUsageType.OUTGOING && closeIntent != null) {
                    addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.action_calls_hangup), closeIntent).build())
                } else {
                    contentViewExpanded.setOnClickPendingIntent(R.id.txt_notification_accept, acceptIntent)
                    contentViewExpanded.setOnClickPendingIntent(R.id.txt_notification_reject, declineIntent)
                }
            }
            context.mNotificationBuilder = builder
            return builder.setOngoing(true).build()
        }
    }


    override fun manageCall(bundle: Any?) {
        (bundle as Bundle).getString(BUNDLE_KEY_CALL_MANAGE_ACTION)?.let { action ->
            LogUtils.e("Tag", "manageCall() --> NOTIFICATION_ACTION_CALL_MANAGE --> $action")
            when (action) {
                CALL_ACTION_ANSWERED, CALL_ACTION_CLOSED, CALL_ACTION_DECLINED -> {
                    stopService()
                }
            }
        }
    }

    override fun manageStopService(action: Any?, status: Any?) {
        LogUtils.e(CommonTAG, "CN->CallServices FN--> manageStopService() --> action --> $action , status--> $status")
        when (action) {
            CALL_ACTION_CLOSED, CALL_ACTION_DECLINED -> {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun withOutPermissionRingingAction(isPlay: Boolean = true) {
        LogUtils.e(CommonTAG, "CN->CallServices FN--> withOutPermissionRingingAction() --> $isPlay")
        if (isPlay && !isAudioPermissionGranted(this)) {
            if (soundPoolManager == null) {
                audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val pair = when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> true to true
                    AudioManager.RINGER_MODE_SILENT -> false to false
                    AudioManager.RINGER_MODE_VIBRATE -> false to true
                    else -> true to true
                }
                TTUApp.canPlaySounds = pair.first
                TTUApp.canVibrate = pair.second
                soundPoolManager = SoundPoolManager.getInstance(this, usageType)
                soundPoolManager?.playRinging(this)
            }
        } else {
            soundPoolManager?.stopRinging()
        }
    }
}
