package rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper

import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.telephony.TelephonyManager
import android.view.View
import com.google.gson.Gson
import com.twilio.video.*
import com.twilio.video.CameraCapturer
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_ADD_PARTICIPANT_VIDEO_VIEW
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_AUDIO_CONVERT_VIDEO
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_CALLER_VIDEO_MUTE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_GET_LOCAL_VIDEO_TRACK
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_MOVE_LOCAL_VIDEO_PRIMARY_VIEW
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_MUTE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_REMOVE_PARTICIPANT_AND_MANAGE_DISCONNECT
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_STREAM_VOICE_CALL
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_VIDEO_REMOVE_PARTICIPANTS_UNSUBSCRIBE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_VOLUME_CONTROL_STREAM
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.HANDLER_ACTION_MUTE_CONVERSATION_VIEW
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.HANDLER_CALL_STATUS
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.HANDLER_HANDLER_ACTION_ERROR
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.HANDLER_MANAGE_DISCLAIMER
import kotlin.math.roundToLong

/**
 * Created by Abhin.
 */
class VoiceVideoCallHelper(private val context: Context? = null, private var accessToken: String? = "", var callType: VoiceVideoCallType = VoiceVideoCallType.VOICE, var usageType: VoiceVideoUsageType = VoiceVideoUsageType.OUTGOING, private var roomName: String? = "", val isStartCallImmediately: Boolean = false) : KoinComponent, CallsSDKTokenUtils.CallsSDKTokenListener {

    private val usersRepository by inject<UsersRepository>()
    private val foregroundManager by inject<ForegroundManager>()

    private var mCallActionsReceiverRegistered: Boolean = false
    // A Room represents communication between a local participant and one or more participants.
    var room: Room? = null
    var connectionFailure: Boolean = false
    private var localParticipant: LocalParticipant? = null

    //Encoding parameters represent the sender side bandwidth constraints.
    private val encodingParameters = EncodingParameters(0, 0)

    private lateinit var audioManager: AudioManager
    internal var soundPoolManager: SoundPoolManager? = null
    private var timeoutTimer: CallTimeoutTimer? = null

    // Create an audio track
    var localAudioTrack: LocalAudioTrack? = null

    // Create a video track
    var localVideoTrack: LocalVideoTrack? = null

    private var isReceiverRegistered: Boolean = false
    private var localBroadcastReceiver: CallActionsReceiver? = null

    private var participantIdentity: String? = ""


    private var previousAudioMode = -10 //default value [changed from 0 to -10]
    private var previousMicrophoneMute = false

    private var disconnectedFromOnDestroy = false
    private var areSpeakersOn: Boolean? = null

    internal var cameraCaptureCompat: CameraCapturerCompat? = null


    private var isAudioEnabled: Boolean? = null

    private var canConnectToRoom: Boolean = false

    private var isPhoneCallReceiverRegistered: Boolean = false
    private var phoneCallReceiver: IncomingPhoneCallReceiver? = null

    //broadcast Receiver
    private var hasIncomingCall: Boolean = false

    var isConnected: Boolean = false
    var isReConnecting: Boolean = false

    //    val pendingMessages = mutableListOf<Message?>()


    //Create the Room Listener
    private val roomListener = object : Room.Listener {

        override fun onParticipantConnected(room: Room, remoteParticipant: RemoteParticipant) {
            LogUtils.e(CommonTAG, "onParticipantConnected() remoteParticipant --> $remoteParticipant")
            val connection = "onParticipantConnected()"
            printRoomValues(connection, room)
            manageParticipantConnected(remoteParticipant)
        }

        override fun onConnected(room: Room) {
            printRoomValues("onConnected()", room)
            manageRoomConnect()
        }

        override fun onParticipantDisconnected(room: Room, remoteParticipant: RemoteParticipant) {
            LogUtils.e(CommonTAG, "onParticipantDisconnected() remoteParticipant --> $remoteParticipant")
            printRoomValues("onParticipantDisconnected()", room)
            manageParticipantDisconnected(remoteParticipant)

            if (context?.isValid() == true) {
                Handler().postDelayed({ (context as? CallServices)?.stopService() }, 1500)
            }
        }

        override fun onConnectFailure(room: Room, twilioException: TwilioException) {
            LogUtils.e(CommonTAG, "onConnectFailure() twilioException--> ${Gson().toJson(twilioException)}")
            printRoomValues("onConnectFailure()", room)
            isConnected = false
            when (twilioException.code) {
                TwilioException.ACCESS_TOKEN_EXPIRED_EXCEPTION, TwilioException.ACCESS_TOKEN_INVALID_EXCEPTION, TwilioException.ACCESS_TOKEN_NOT_YET_VALID_EXCEPTION -> {
                    connectionFailure = true
                    CallsSDKTokenUtils.init(usersRepository.fetchCachedMyUserId(), this@VoiceVideoCallHelper)
                }
                else -> {
                    manageConnectionFailure()
                }
            }
        }

        override fun onReconnected(room: Room) {
            val connection = "onReconnected()"
            printRoomValues(connection, room)
            if (isReConnecting) {
                isReConnecting = false
                val msg = Message.obtain(null, HANDLER_CALL_STATUS, 1, 0)
                updateCallStatus(msg, context!!.resources.getString(R.string.call_connected), (context as CallServices).mMessenger)
                context.changeNotificationBody(false)
            }
        }

        override fun onDisconnected(room: Room, twilioException: TwilioException?) {
            LogUtils.e(CommonTAG, "onDisconnected() twilioException--> ${Gson().toJson(twilioException)}")
            LogUtils.e(CommonTAG, "onDisconnected() Exception--> ${twilioException.toString()}")
            isConnected = false
            isReConnecting = false
            val connection = "onDisconnected()"
            printRoomValues(connection, room)
            manageDisconnected()

            //after a long time local participant not responding or go in airplane mode than
            when (twilioException?.code) {
                TwilioException.MEDIA_CONNECTION_ERROR_EXCEPTION, TwilioException.SIGNALING_CONNECTION_TIMEOUT_EXCEPTION -> {
                    val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
                    updateCallStatus(msg, context!!.resources.getString(R.string.not_able_to_connect_the_call), (context as CallServices).mMessenger)
                    context.changeNotificationBody(isReconnecting = false, isDisconnected = true)

                    Handler().postDelayed({
                        if (!foregroundManager.isForeground || !CallServices.isCallActivityForeground) context.stopService(CallsNotificationUtils.CALL_ACTION_CLOSED)
                    }, 2000)
                }
            }
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            LogUtils.e(CommonTAG, "onReconnecting() Exception--> $twilioException with code ${twilioException.code}")
            val connection = "onReconnecting()"
            printRoomValues(connection, room)
            isReConnecting = true
            val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
            updateCallStatus(msg, context!!.resources.getString(R.string.call_connection_lost_reconnecting), (context as CallServices).mMessenger)
            context.changeNotificationBody()
        }

        override fun onRecordingStarted(room: Room) {
            /**
             * Indicates when media shared to a Room is being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            val connection = "onRecordingStarted()"
            printRoomValues(connection, room)
        }

        override fun onRecordingStopped(room: Room) {
            /**
             * Indicates when media shared to a Room is no longer being recorded. Note that
             * recording is only available in our Group Rooms developer preview.
             */
            val connection = "onRecordingStopped()"
            printRoomValues(connection, room)
        }
    }

    init {
        initVariable()
        LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> init() isVideoEnabled--> $isVideoEnabled")
    }


    private fun initVariable() {

        //  LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> initVariable() --> ")
        //  INFO: 2019-12-11     If you look at line 127, part of the condition to create localVideoTrack
        //  is that the call MUST BE of type VIDEO, otherwise why allocate memory for something we
        //  don't need?
        //  IN CASE THE CAMERA IS TURNED ON, after checking the permissions, we instantiate these
        //  3 objects.
        if (context != null) {
            if (isVideoPermissionGranted(context) && callType == VoiceVideoCallType.VIDEO) {
                if (cameraCaptureCompat == null) cameraCaptureCompat = CameraCapturerCompat(context, getAvailableCameraSource())
                createAudioAndVideoTracks()
            }

            if (isAudioPermissionGranted(context)) {
                if (soundPoolManager == null) soundPoolManager = SoundPoolManager.getInstance(context, usageType)
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                localAudioTrack = LocalAudioTrack.create(context, true)

                //Enable changing the volume using the up/down keys during a conversation
                serviceMessageSend(ACTION_STREAM_VOICE_CALL, null, (context as CallServices).mMessenger)

                /* LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> initVariable() audioManager.mode--> ${audioManager.mode}")
                 LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> initVariable() previousAudioMode--> $previousAudioMode")*/
                //Needed for being able to use speakers.
                if (usageType == VoiceVideoUsageType.OUTGOING) {
                    if (previousAudioMode == -10) {
                        previousAudioMode = audioManager.mode
                    }
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                }

                // Needed for setting/abandoning audio focus during call
                areSpeakersOn = (callType == VoiceVideoCallType.VIDEO || usageType == VoiceVideoUsageType.INCOMING)
                audioManager.isSpeakerphoneOn = areSpeakersOn!!

                val msg = Message.obtain(null, ACTION_VOLUME_CONTROL_STREAM, 0, 0)
                context.mMessenger?.send(msg)
            }

            //            val pair = when (audioManager.mode) {
            //                AudioManager.RINGER_MODE_NORMAL -> true to true
            //                AudioManager.RINGER_MODE_SILENT -> false to false
            //                AudioManager.RINGER_MODE_VIBRATE -> false to true
            //                else -> true to true
            //            }
            //
            //            TTUApp.canPlaySounds = pair.first
            //            TTUApp.canVibrate = pair.second

            //Setup the broadcast receiver to be notified of video notification messages
            localBroadcastReceiver = CallActionsReceiver()

            handleCallStartup()
        }
    }


    fun onResume() {
        LogUtils.e("Tag", "helper --> ACTION_ACTIVITY_RESUMED")
        registerReceiver()

        val fromBackground = localAudioTrack == null && localVideoTrack == null

        if (isVideoPermissionGranted(context!!) && callType == VoiceVideoCallType.VIDEO) {
            if (cameraCaptureCompat == null) cameraCaptureCompat = CameraCapturerCompat(context, getAvailableCameraSource())

            //If the local video track was released when the app was put in the background, recreate.
            localVideoTrack = if (localVideoTrack == null && callType == VoiceVideoCallType.VIDEO) {
                LocalVideoTrack.create(context, true, cameraCaptureCompat?.videoCapturer!!)
            } else {
                localVideoTrack
            }
            // If connected to a Room then share the local video and audio track, m

            localVideoTrack?.let {
                LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> onResume() --> isVideoEnabled $isVideoEnabled")
                if (isVideoEnabled != null) it.enable(isVideoEnabled!!)
                localParticipant?.publishTrack(it)
            }
            //            if (!room?.remoteParticipants.isNullOrEmpty()) startVideoLocalVideoTracker()
        }

        if (isAudioPermissionGranted(context)) {
            localAudioTrack = if (localAudioTrack == null) {
                LocalAudioTrack.create(context, true)
            } else {
                localAudioTrack
            }
            localAudioTrack?.let {
                if (isAudioEnabled != null) it.enable(isAudioEnabled!!)
                localParticipant?.publishTrack(it)
            }
        }

        bluetoothDevice()

        if (room == null || room!!.state == Room.State.DISCONNECTED) {
            if ((fromBackground || usageType == VoiceVideoUsageType.OUTGOING || canConnectToRoom)) {
                LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> onResume() --> connectToRoom")
                connectToRoom()
            }
        }

        //manage after destroy the UI
        if (isReConnecting) {
            val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
            updateCallStatus(msg, context.resources.getString(R.string.call_connection_lost_reconnecting), (context as CallServices).mMessenger)
            context.changeNotificationBody()
        }
    }

    private fun bluetoothDevice() {
        val pair = audioManager.areThereConnectedDevicesOut()
        val devicePresent = pair.first
        val bluetooth = pair.second
        handleSpeakersToggle(!devicePresent && areSpeakersOn!!)
        if (bluetooth) {
            audioManager.isBluetoothScoOn = true
            audioManager.startBluetoothSco()
        }
        //Update encoding parameters if they have changed.
        localParticipant?.setEncodingParameters(encodingParameters)
    }


    fun onPause() {
        LogUtils.e("Tag", "Helper --> onPause() --> ")
        /**
         * If this local video track is being shared in a Room, remove from local
         * participant before releasing the video track. Participants will be notified that
         * the track has been removed.
         */
        if (localVideoTrack != null && localAudioTrack != null && localParticipant != null) { //Only if an incoming call is detected.

            localVideoTrack?.let {
                LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> onPause() --> isEnable ${it.isEnabled}")
                isVideoEnabled = it.isEnabled
                localParticipant?.unpublishTrack(it)
            }
            /**
             * Release the local video track before going in the background. This ensures that the
             * camera can be used by other applications while this app is in the background.
             */
            localVideoTrack?.release()
            localVideoTrack = null


            if (hasIncomingCall) {

                localAudioTrack?.let {
                    isAudioEnabled = it.isEnabled
                    localParticipant?.unpublishTrack(it)
                }
                soundPoolManager?.stopRinging()
                configureAudio()
                localAudioTrack?.release()
                localAudioTrack = null
            }

        }
    }

    fun onDestroy() {
        LogUtils.e("Tag", "helper onDestroy() --> ")
        unregisterReceiver()
        disconnectedFromOnDestroy = true

        timeoutTimer?.cancel()
        timeoutTimer = null

        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        room?.disconnect()
        room = null

        configureAudio()

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        localAudioTrack?.release()
        localVideoTrack?.release()

        soundPoolManager?.release()
    }


    private fun manageParticipantDisconnected(remoteParticipant: RemoteParticipant) {
        removeRemoteParticipant(remoteParticipant)
    }

    private fun removeRemoteParticipant(remoteParticipant: RemoteParticipant) {
        if (remoteParticipant.identity != participantIdentity) {
            return
        }
        removeParticipantVideo(remoteParticipant)
        LogUtils.e(CommonTAG, "Multiple participants are not currently supported in this UI")
    }

    private fun removeParticipantVideo(it: RemoteParticipant) {
        val msg = Message.obtain(null, ACTION_REMOVE_PARTICIPANT_AND_MANAGE_DISCONNECT, 0, 0)
        msg.obj = it
        (context as CallServices).mMessenger?.send(msg)
    }

    private fun removeParticipantVideoUnsubscribe(remoteVideoTrack: RemoteVideoTrack) {
        val msg = Message.obtain(null, ACTION_VIDEO_REMOVE_PARTICIPANTS_UNSUBSCRIBE, 0, 0)
        msg.obj = remoteVideoTrack
        (context as CallServices).mMessenger?.send(msg)

        handleUIEncryptionDisclaimer(View.VISIBLE)
    }

    fun createAudioAndVideoTracks(isServices: Boolean = false) {
        if (localAudioTrack == null && isAudioPermissionGranted(context!!)) localAudioTrack = LocalAudioTrack.create(context, true)
        if (localVideoTrack == null && isVideoPermissionGranted(context!!)) {
            if (cameraCaptureCompat == null) cameraCaptureCompat = CameraCapturerCompat(context, getAvailableCameraSource())
            if (cameraCaptureCompat != null) localVideoTrack = LocalVideoTrack.create(context, true, cameraCaptureCompat!!.videoCapturer)
        }
        LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> createAudioAndVideoTracks() isVideoEnabled--> $isVideoEnabled")
        startVideoLocalVideoTracker(isServices)
        //cameraCaptureCompat send to Fragment
        (context as CallServices).sendCameraCapture(cameraCaptureCompat)
    }

    internal fun putSpeakerOn() {
        if (areSpeakersOn != true) {
            areSpeakersOn = true
            if (::audioManager.isInitialized) {
                audioManager.isSpeakerphoneOn = areSpeakersOn!!
            }
        }
    }


    private fun manageParticipantConnected(remoteParticipant: RemoteParticipant) {
        val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
        updateCallStatus(msg, context!!.resources.getString(R.string.call_connecting), (context as CallServices).mMessenger)
        participantIdentity = remoteParticipant.identity
        timeoutTimer?.run {
            this.cancel()
            timeoutTimer = null
        }
        addRemoteParticipant(remoteParticipant)
    }

    private fun manageDisconnected() {
        localParticipant = null

        // Only reinitialize the UI if disconnect was not called from onDestroy()
        if (!disconnectedFromOnDestroy) {
            configureAudio()
            moveLocalVideoToPrimaryView()
        }
    }

    internal fun toggleMute(isMute: Boolean) {
        /*
         * Enable/disable the local audio track. The results of this operation are
         * signaled to other Participants in the same Room. When an audio track is
         * disabled, the audio is muted.
         */
        localAudioTrack?.let {
            val enable = !isMute
            it.enable(enable)
            val msg = Message.obtain(null, ACTION_MUTE, 0, 0)
            msg.obj = isMute
            (context as CallServices).mMessenger?.send(msg)
        }
    }

    internal fun toggleVideoMute(isMute: Boolean) {
        /*
         * Enable/disable the local Video track. The results of this operation are
         * signaled to other Participants in the same Room. When an video track is
         * disabled, the video is muted.
         */
        localVideoTrack?.let {
            LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> toggleVideoMute() --> isMute $isMute")
            //            isVideoEnabled=isMute
            it.enable(isMute)
        }
    }

    private fun manageConnectionFailure() {
        soundPoolManager?.stopRinging()
        soundPoolManager?.playError()
        configureAudio()
        (context as CallServices).stopService()
    }


    private fun handleUIEncryptionDisclaimer(visibility: Int) {
        (context as? CallServices)?.mMessenger?.send(Message.obtain().apply {
            what = HANDLER_MANAGE_DISCLAIMER
            obj = visibility
        })
    }


    override fun tokenResponse(token: String?) {
        token?.let {
            accessToken = it
            LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> tokenResponse() --> connectToRoom")
            connectToRoom(roomName)
        }
    }

    private fun configureAudio(enable: Boolean = false) {
        with(audioManager) {
            if (enable) {
                /*LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> configureAudio() audioManager.mode--> ${audioManager.mode}")
                LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> configureAudio() previousAudioMode--> $previousAudioMode")*/
                if (usageType == VoiceVideoUsageType.INCOMING && previousAudioMode == -10) {
                    previousAudioMode = audioManager.mode
                }
                //Request audio focus before making any device switch
                requestAudioFocus()
                /**
                 * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
                 * to be in this mode when playout and/or recording starts for the best
                 * possible VoIP performance. Some devices have difficulties with
                 * speaker mode if this is not set.
                 */
                mode = AudioManager.MODE_IN_COMMUNICATION
                //Always disable microphone mute during a WebRTC call.
                previousMicrophoneMute = isMicrophoneMute
                isMicrophoneMute = false
                if (usageType == VoiceVideoUsageType.INCOMING && callType == VoiceVideoCallType.VOICE) isSpeakerphoneOn = false
            } else {
                /* LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> configureAudio() else part audioManager.mode--> ${audioManager.mode}")
                 LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> configureAudio() else part previousAudioMode--> $previousAudioMode")*/
                mode = previousAudioMode
                abandonAudioFocus()
                isMicrophoneMute = previousMicrophoneMute
                isSpeakerphoneOn = true
            }
        }
    }

    private fun manageRoomConnect() {
        localParticipant = room?.localParticipant
        soundPoolManager?.playRinging(context!!)
        //call_ringing
        if (usageType == VoiceVideoUsageType.OUTGOING) {
            val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
            if (callType == VoiceVideoCallType.VOICE) updateCallStatus(msg, context!!.resources.getString(R.string.calls_status_Ringing), (context as CallServices).mMessenger)
            timeoutTimer = CallTimeoutTimer(30000, 1000)
            timeoutTimer?.start()
        } else {
            timeoutTimer?.run {
                this.cancel()
                timeoutTimer = null
            }
            val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
            updateCallStatus(msg, context!!.resources.getString(R.string.calls_timer_disconnecting_num), (context as CallServices).mMessenger)
        }
        isConnected = true
        // Only one participant is supported
        room?.remoteParticipants?.firstOrNull()?.let { addRemoteParticipant(it) }
    }

    fun cancelTimer() {
        timeoutTimer?.run {
            this.cancel()
            timeoutTimer = null
        }
    }

    //when user convert the audio to video
    fun audioConvertVideo(isEnabled: Boolean = true) {
        LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> audioConvertVideo() --> $isEnabled")
        if (localParticipant != null) {
            localVideoTrack?.let {
                isVideoEnabled = isEnabled
                it.enable(isEnabled)
                localParticipant?.publishTrack(it)
            }
        }
    }

    private val participantListener = object : RemoteParticipant.Listener {
        override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.e(CommonTAG, "onAudioTrackPublished: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " + "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " + "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " + "name=${remoteAudioTrackPublication.trackName}]")
        }

        override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.e(CommonTAG, "onAudioTrackUnpublished: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " + "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " + "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " + "name=${remoteAudioTrackPublication.trackName}]")
        }

        override fun onDataTrackPublished(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication) {
            LogUtils.e(CommonTAG, "onDataTrackPublished: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " + "enabled=${remoteDataTrackPublication.isTrackEnabled}, " + "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " + "name=${remoteDataTrackPublication.trackName}]")
        }

        override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication) {
            LogUtils.e(CommonTAG, "onDataTrackUnpublished: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " + "enabled=${remoteDataTrackPublication.isTrackEnabled}, " + "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " + "name=${remoteDataTrackPublication.trackName}]")
        }

        override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.e(CommonTAG, "onVideoTrackPublished: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " + "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " + "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " + "name=${remoteVideoTrackPublication.trackName}]")
            //if audio call in background than it's set the video
            if (callType == VoiceVideoCallType.VOICE) {
                (context as CallServices).callType = VoiceVideoCallType.VIDEO   //Services callType
                context.isAskConvertAudioToVideoDialog = true
                callType = VoiceVideoCallType.VIDEO //Helper call type
                isVideoEnabled = false
                //                context.sendDataToActivity(CallServices.ACTION_CALL_MAIN)
            }
        }

        override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.e(CommonTAG, "onVideoTrackUnpublished: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " + "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " + "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " + "name=${remoteVideoTrackPublication.trackName}]")
        }

        override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication, remoteAudioTrack: RemoteAudioTrack) {
            LogUtils.e(CommonTAG, "onAudioTrackSubscribed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " + "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " + "name=${remoteAudioTrack.name}]")
        }

        override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication, remoteAudioTrack: RemoteAudioTrack) {
            LogUtils.e(CommonTAG, "onAudioTrackUnsubscribed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " + "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " + "name=${remoteAudioTrack.name}]")
        }

        override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication, twilioException: TwilioException) {
            LogUtils.e(CommonTAG, "onAudioTrackSubscriptionFailed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " + "name=${remoteAudioTrackPublication.trackName}]" + "[TwilioException: code=${twilioException.code}, " + "message=${twilioException.message}]")
        }

        override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication, remoteDataTrack: RemoteDataTrack) {
            LogUtils.e(CommonTAG, "onDataTrackSubscribed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " + "name=${remoteDataTrack.name}]")
        }

        override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication, remoteDataTrack: RemoteDataTrack) {
            LogUtils.e(CommonTAG, "onDataTrackUnsubscribed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " + "name=${remoteDataTrack.name}]")
        }

        override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication, twilioException: TwilioException) {
            LogUtils.e(CommonTAG, "onDataTrackSubscriptionFailed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " + "name=${remoteDataTrackPublication.trackName}]" + "[TwilioException: code=${twilioException.code}, " + "message=${twilioException.message}]")
        }

        override fun onVideoTrackSubscribed(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication, remoteVideoTrack: RemoteVideoTrack) {
            LogUtils.e(CommonTAG, "onVideoTrackSubscribed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " + "name=${remoteVideoTrack.name}]")
            addRemoteParticipantVideo(remoteVideoTrack)
        }

        override fun onVideoTrackUnsubscribed(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication, remoteVideoTrack: RemoteVideoTrack) {
            LogUtils.e(CommonTAG, "onVideoTrackUnsubscribed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " + "name=${remoteVideoTrack.name}]")
            removeParticipantVideoUnsubscribe(remoteVideoTrack)
        }

        override fun onVideoTrackSubscriptionFailed(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication, twilioException: TwilioException) {
            LogUtils.e(CommonTAG, "onVideoTrackSubscriptionFailed: " + "[RemoteParticipant: identity=${remoteParticipant.identity}], " + "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " + "name=${remoteVideoTrackPublication.trackName}]" + "[TwilioException: code=${twilioException.code}, " + "message=${twilioException.message}]")
            val mBundle = Bundle()
            mBundle.putString("video", context!!.getString(R.string.error_calls_video))
            serviceMessageSend(HANDLER_HANDLER_ACTION_ERROR, mBundle, (context as CallServices).mMessenger)
        }

        override fun onAudioTrackEnabled(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.e(CommonTAG, "AUDIO track ENABLED")
            handleMutedConversationView(true)
        }

        override fun onAudioTrackDisabled(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication) {
            LogUtils.e(CommonTAG, "AUDIO track DISABLED")
            handleMutedConversationView(false)
        }

        override fun onVideoTrackEnabled(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.e(CommonTAG, "VIDEO track ENABLED")
            handleVideoMutedConversationView(true)
            //            (context as CallServices).isParticipantVideoMute = false
        }

        override fun onVideoTrackDisabled(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication) {
            LogUtils.e(CommonTAG, "VIDEO track DISABLED")
            //            (context as CallServices).isParticipantVideoMute = true
            handleVideoMutedConversationView(false)
        }
    }

    private fun handleMutedConversationView(audio: Boolean) {
        val msg = Message.obtain().apply {
            this.what = HANDLER_ACTION_MUTE_CONVERSATION_VIEW
            this.obj = audio
        }
        (context as CallServices).mMessenger?.send(msg)
    }

    private fun handleVideoMutedConversationView(video: Boolean) {
        val msg = Message.obtain().apply {
            this.what = ACTION_CALLER_VIDEO_MUTE
            this.obj = video
        }
        (context as CallServices).mMessenger?.send(msg)

        handleUIEncryptionDisclaimer(if (video) View.GONE else View.VISIBLE)
    }

    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        soundPoolManager?.stopRinging()
        cancelTimer()

        if (callType == VoiceVideoCallType.VOICE && usageType == VoiceVideoUsageType.INCOMING) handleSpeakersToggle(false)

        if (!remoteParticipant.remoteVideoTracks.isNullOrEmpty()) {
            // Add participant renderer
            remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
                if (remoteVideoTrackPublication.isTrackSubscribed) {
                    remoteVideoTrackPublication.remoteVideoTrack?.let { addRemoteParticipantVideo(it) }
                }
            }
        } else {
            val msg = Message.obtain(null, ACTION_GET_LOCAL_VIDEO_TRACK, 1, 0)
            msg.obj = localVideoTrack
            (context as CallServices).mMessenger?.send(msg)
        }
        //Start listening for participant events
        remoteParticipant.setListener(participantListener)

        val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
        updateCallStatus(msg, context!!.resources.getString(R.string.call_connected), (context as CallServices).mMessenger)
        context.callStartedTime = SystemClock.elapsedRealtime()
    }

    //Set primary view as renderer for participant video track
    private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
        cancelTimer()
        val msg = Message.obtain(null, ACTION_ADD_PARTICIPANT_VIDEO_VIEW, 0, 0)
        msg.obj = videoTrack
        (context as CallServices).mMessenger?.send(msg)

        handleUIEncryptionDisclaimer(View.GONE)
    }


    private fun startVideoLocalVideoTracker(isServices: Boolean = false) {
        var isRemoteParticipantsAdded = 0
        var isRemoteVideoTrackEnable = 0
        if (!room?.remoteParticipants.isNullOrEmpty()) {
            isRemoteParticipantsAdded = 1
            room?.remoteParticipants?.get(0)?.remoteVideoTracks?.firstOrNull()?.let { remoteVideoTrackPublication ->
                isRemoteVideoTrackEnable = if (remoteVideoTrackPublication.isTrackSubscribed) {
                    if (remoteVideoTrackPublication.isTrackEnabled) 1
                    else 0
                } else {
                    0
                }
            }
        }
        /*LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> startVideoLocalVideoTracker() isRemoteParticipantsAdded--> $isRemoteParticipantsAdded")
        LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> startVideoLocalVideoTracker() isRemoteVideoTrackEnable--> $isRemoteVideoTrackEnable")*/
        val msg = if (isServices) {
            Message.obtain(null, ACTION_AUDIO_CONVERT_VIDEO, isRemoteParticipantsAdded, isRemoteVideoTrackEnable)
        } else {
            Message.obtain(null, ACTION_GET_LOCAL_VIDEO_TRACK, isRemoteParticipantsAdded, isRemoteVideoTrackEnable)
        }
        msg.obj = localVideoTrack
        (context as CallServices).mMessenger?.send(msg)
    }

    private fun moveLocalVideoToPrimaryView() {
        val msg = Message.obtain(null, ACTION_MOVE_LOCAL_VIDEO_PRIMARY_VIEW, 0, 0)
        msg.obj = localVideoTrack
        (context as CallServices).mMessenger?.send(msg)
    }

    fun disconnectRoom() {
        room?.disconnect()

    }

    //Get the camera source
    private fun getAvailableCameraSource(): CameraCapturer.CameraSource {
        return if (CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA)) CameraCapturer.CameraSource.FRONT_CAMERA
        else CameraCapturer.CameraSource.BACK_CAMERA
    }

    fun connectToRoom(roomName: String? = null) {
        LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> connectToRoom() --> if(!roomName.isNullOrEmpty() && !accessToken.isNullOrBlank()) -> ${(!roomName.isNullOrEmpty() && !accessToken.isNullOrBlank())}")
        LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> connectToRoom() --> (room == null || connectionFailure)  -> ${(room == null || connectionFailure)}")

        if ((room == null || room?.state == Room.State.DISCONNECTED) && !roomName.isNullOrEmpty() && !accessToken.isNullOrBlank()) {
            configureAudio(true)
            val connectOptionsBuilder = ConnectOptions.Builder(accessToken!!).roomName(roomName)

            //Add local audio track to connect options to share with participants.
            localAudioTrack?.let { connectOptionsBuilder.audioTracks(listOf(it)) }

            LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> connectToRoom() localVideoTrack $localVideoTrack")

            //Add local video track to connect options to share with participants.
            localVideoTrack?.let { connectOptionsBuilder.videoTracks(listOf(it)) }

            connectOptionsBuilder.encodingParameters(encodingParameters)
            if (room == null || connectionFailure) {
                connectionFailure = false
                room = Video.connect(context!!, connectOptionsBuilder.build(), roomListener)
            }
            bluetoothDevice()
        } else {
            LogUtils.e(CommonTAG, "Unexpected call token error")
            (context as CallServices).mMessenger?.send(Message.obtain().apply {
                this.what = HANDLER_HANDLER_ACTION_ERROR
                this.obj = context.getString(R.string.error_calls_connection)
            })
        }
    }

    private fun printRoomValues(connection: String, room: Room) {
        LogUtils.e(CommonTAG, "$connection --> ${room.sid}")
        LogUtils.e(CommonTAG, "$connection --> ${room.name}")
        LogUtils.e(CommonTAG, "$connection --> ${room.state}")
        LogUtils.e(CommonTAG, "$connection --> ${room.mediaRegion}")
        LogUtils.e(CommonTAG, "$connection --> ${room.dominantSpeaker}")
        LogUtils.e(CommonTAG, "$connection --> ${room.remoteParticipants}")
    }


    fun handleSpeakersToggle(forced: Boolean? = null) {
        val enable = forced ?: !audioManager.isSpeakerphoneOn
        audioManager.isSpeakerphoneOn = enable
        areSpeakersOn = enable
    }

    //region == Handles notification ==

    private fun handleCallStartup() {
        //when we on background than show the notification
        if (!((context as CallServices).applicationContext as TTUApp).foregroundManager.isForeground) {
            context.showNotification(CallServices.NotificationType.STARTUP)
        }

        if (soundPoolManager == null) soundPoolManager = SoundPoolManager.getInstance(context, usageType)

        //Only handle the notification if not already connected to a Video Room
        LogUtils.d(CommonTAG, "Call: $callType")
        if (room == null && usageType == VoiceVideoUsageType.INCOMING && !isStartCallImmediately) soundPoolManager?.playRinging(context)
        //LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> handleCallStartup() usageType--> $usageType")
        if (isStartCallImmediately || (usageType == VoiceVideoUsageType.OUTGOING && !roomName.isNullOrEmpty())) {
            LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> handleCallStartup() --> connectToRoom")
            connectToRoom(roomName)
        }
    }

    private fun registerReceiver() {
        LogUtils.i(CommonTAG, "CN->VideoVoiceCallHelper FN--> registerReceiver() --> ")
        if (!isReceiverRegistered) {
            LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> registerReceiver() --> Enter")
            val intentFilter = IntentFilter()

            // add all the needed IntentFilters
            intentFilter.addAction(if (hasLollipop()) AudioManager.ACTION_HEADSET_PLUG
            else Intent.ACTION_HEADSET_PLUG)
            intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)

            registerPhoneCallReceiver()

            if (isReceiverRegistered) {
                unregisterReceiver()
            }

            if (!mCallActionsReceiverRegistered) {
                mCallActionsReceiverRegistered = true
                (context as CallServices).registerReceiver(localBroadcastReceiver!!, intentFilter)
                isReceiverRegistered = true
            }
        }
    }


    private fun registerPhoneCallReceiver() {
        LogUtils.i(CommonTAG, "CN->VideoVoiceCallHelper FN--> registerPhoneCallReceiver() --> ")
        if (!isPhoneCallReceiverRegistered) {
            if (phoneCallReceiver == null) phoneCallReceiver = IncomingPhoneCallReceiver()
            (context as CallServices).applicationContext.registerReceiver(phoneCallReceiver!!, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
            isPhoneCallReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        LogUtils.i(CommonTAG, "CN->VideoVoiceCallHelper FN--> unregisterReceiver() --> ")
        if (isReceiverRegistered) {
            try {
                val applicationContext = (context as CallServices).application
                applicationContext.apply {
                    if (mCallActionsReceiverRegistered) if (localBroadcastReceiver != null) {
                        context.unregisterReceiver(localBroadcastReceiver)
                        mCallActionsReceiverRegistered = false
                    }
                    if (phoneCallReceiver != null) unregisterReceiver(phoneCallReceiver)
                    isReceiverRegistered = false
                    isPhoneCallReceiverRegistered = false
                }
            } catch (e: IllegalArgumentException) {
                LogUtils.e(CommonTAG, e.message, e)
            }
        }
    }
    //endregion


    //region == Audio Manager section ==

    private var focusRequest: AudioFocusRequest? = null
    private fun AudioManager.requestAudioFocus() {
        if (hasOreo()) {
            LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> requestAudioFocus() --> hasOreo")
            val playbackAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(playbackAttributes).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener { }.build()
            if (focusRequest != null) this.requestAudioFocus(focusRequest!!)
        } else {
            this.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun AudioManager.abandonAudioFocus() {

        if (hasOreo()) {
            /*LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> abandonAudioFocus() --> oreo and oreo above")*/
            if (focusRequest != null) {
                /*  LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> abandonAudioFocus() --> oreo and oreo above Enter")*/
                abandonAudioFocusRequest(focusRequest!!)
            }
        } else {
            /*LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> abandonAudioFocus() --> oreo below")*/
            abandonAudioFocus(null)
        }
    }

    private fun AudioManager.areThereConnectedDevicesOut(): Pair<Boolean, Boolean> {
        var res = false
        var deviceInfo = false

        if (hasMarshmallow()) {
            val arr = this.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (it in arr) {
                if (it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    res = true
                    deviceInfo = false
                    break
                } else if (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    res = true
                    deviceInfo = true
                    break
                }
            }
        } else {
            res = this.isWiredHeadsetOn || this.isBluetoothA2dpOn || this.isBluetoothScoOn
            deviceInfo = this.isBluetoothScoOn || this.isBluetoothA2dpOn
        }

        return res to deviceInfo
    }
    //endregion

    //BoardCast Receiver for HEADSET and BLUETOOTH Profile
    private inner class CallActionsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            LogUtils.e(CommonTAG, "CN->VideoVoiceCallHelper FN--> onReceive--> $action")
            when (action) {
                // all the cases
                AudioManager.ACTION_HEADSET_PLUG -> {
                    if (!isInitialStickyBroadcast) {
                        val state = intent.extras?.getInt("state") ?: 0
                        handleSpeakersToggle(state != 1)
                    }
                }
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    when (intent.extras?.getInt(BluetoothProfile.EXTRA_STATE)) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            handleSpeakersToggle(false)
                            audioManager.isBluetoothScoOn = true
                            audioManager.startBluetoothSco()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            handleSpeakersToggle(true)
                            audioManager.isBluetoothScoOn = false
                            audioManager.stopBluetoothSco()
                        }
                        else -> {
                        }
                    }
                }
                else -> {
                    if (intent != null) handleCallStartup()
                }
            }
        }
    }

    fun updateCallStatus(msg: Message, message: String, mMessenger: Messenger?) {
        msg.obj = message
        mMessenger?.send(msg)
    }

    //send Message to Service
    private fun serviceMessageSend(action: Int, bundle: Bundle?, mMessenger: Messenger?, msg: Message? = null) {
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
            (context as CallServices).mMessenger?.send(msg ?: mMessage)

        } catch (e: RemoteException) {
            LogUtils.e(CommonTAG, "onServiceConnected() --> Error")
            e.printStackTrace()
        }
    }

    private inner class CallTimeoutTimer(millis: Long, interval: Long) : CountDownTimer(millis, interval) {
        override fun onFinish() {}
        override fun onTick(millisUntilFinished: Long) {
            val roundedMillis = (millisUntilFinished.toDouble() / 1000).roundToLong() * 1000
            val mMessenger = (context as CallServices).mMessenger
            if (roundedMillis <= 4000L) {
                val msg = Message.obtain(null, HANDLER_CALL_STATUS, 0, 0)
                updateCallStatus(msg, context.resources.getString(R.string.calls_timer_disconnecting_num, (roundedMillis / 1000) - 1), mMessenger)
            }
        }
    }

    private inner class IncomingPhoneCallReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
            val string = when (action) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    hasIncomingCall = true
                    "RINGING"
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    "OFF HOOK"
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    hasIncomingCall = false
                    "IDLE"
                }
                else -> ""
            }

            LogUtils.d(CommonTAG, "Incoming phone call: $string")
        }
    }


    companion object {
        var isVideoEnabled: Boolean? = null
    }
}

enum class STICKY { AXIS_X, AXIS_Y, AXIS_XY }
enum class VoiceVideoCallType { VOICE, VIDEO }
enum class VoiceVideoUsageType { INCOMING, OUTGOING }
enum class VoiceVideoStatus { DEFAULT, ACCEPT, DECLINE, CLOSE }
enum class PermissionAllow { DEFAULT, ACCEPT, CONVERT, CONVERT_FORM_VOICE_TO_VIDEO_POPUP, ACCEPT_TIME_RESET }
enum class VoiceTOVideoDialogUserClick { DEFAULT, OK, CANCEL }
