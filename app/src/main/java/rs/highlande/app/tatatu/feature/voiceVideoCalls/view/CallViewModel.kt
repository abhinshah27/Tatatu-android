package rs.highlande.app.tatatu.feature.voiceVideoCalls.view

import android.app.Application
import android.os.CountDownTimer
import android.os.Handler
import android.view.View
import androidx.lifecycle.MutableLiveData
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.isVideoPermissionGranted
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceTOVideoDialogUserClick
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceVideoCallType
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceVideoStatus
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceVideoUsageType

/**
 * Created by Abhin.
 */

class CallViewModel(private val context: Application) : BaseAndroidViewModel(context) {

    // TODO: 2019-12-09    NECESSARY LD for all properties?
    var status = mutableLiveData(context.resources.getString(R.string.calls_status_incoming))
    var profileName = mutableLiveData("")
    var profileAvatar = mutableLiveData("")
    var roomName = mutableLiveData("")
    var callId = mutableLiveData("")
    var calledId = mutableLiveData("")
    var callTime = mutableLiveData("")

    var viewVoiceGreyBackground = mutableLiveData(View.VISIBLE)
    var viewVideoGreyBackground = mutableLiveData(View.GONE)
    var viewVideoTransparentBackground = mutableLiveData(View.GONE)
    var participantsName = mutableLiveData(View.VISIBLE)
    var callStatusRinging = mutableLiveData(View.VISIBLE)
    var callChronometerStatus = mutableLiveData(View.GONE)
    var callEncryptionDisclaimer = mutableLiveData(View.GONE)

    //Call Type and Usage
    var usageType = mutableLiveData(VoiceVideoUsageType.INCOMING)
    var callType = mutableLiveData(VoiceVideoCallType.VIDEO)

    //update UI based on CallStatus
    var callStatus = mutableLiveData(VoiceVideoStatus.DEFAULT)

    //Video Mute by opponent
    var mVideoMuteObserve= MutableLiveData<Boolean>()
    var mThumbnailBlurBackground = mutableLiveData(View.GONE)
    var mImageThumbnailBackground = mutableLiveData(View.GONE)

    //Call Incoming
    var incomingAcceptCallImageButton = mutableLiveData(View.VISIBLE)
    var incomingDeclineCallImageButton = mutableLiveData(View.VISIBLE)
    var callIncomingDeclineText = mutableLiveData(View.VISIBLE)
    var callIncomingAcceptText = mutableLiveData(View.VISIBLE)

    //Profile Image
    var incomingParticipantProfile = mutableLiveData(View.VISIBLE)
    var incomingStartedParticipantProfile = mutableLiveData(View.GONE)
    var animationRinging = mutableLiveData(View.VISIBLE)

    //Buttons
    var startedDeclineCall = mutableLiveData(View.GONE)

    //below 3 button
    //Camera and Speaker
    var speakerCameraVisible = mutableLiveData(View.GONE)
    var speaker = mutableLiveData(false)
    var speakerClick = mutableLiveData(false)
    var cameraSwitch = mutableLiveData(false)
    var cameraSwitchIcon = mutableLiveData(false)

    //Video
    var videoClick = mutableLiveData(true)
    var videoVisibility = mutableLiveData(View.GONE)
    var videoFrameLayoutVisibility = mutableLiveData(View.GONE)

    //Mice
    var isMute = mutableLiveData(false)
    var isMuteClick = mutableLiveData(false)
    var isConvertVideo  = mutableLiveData(false)
    var miceVisible = mutableLiveData(View.GONE)

    //Animation
    var buttonHide = mutableLiveData(false)
    var isClickVoiceIncomingCall = false
    var isClickVideoIncomingCall = false
    var show = false
    var isAnimationStart = false
    var mTimer: CountDownTimer? = null
    var isThumbnailsBottom = true
    var isVoiceToVideoConvertDialogCancel = false
    var isOpenDialog: Boolean?=false

    var mVoiceTOVideoDialogUserClick = VoiceTOVideoDialogUserClick.DEFAULT

    fun onClick(view: View) {
        when (view.id) {
            R.id.img_speaker_camera_call -> {
                if (callType.value == VoiceVideoCallType.VOICE) {
                    speaker.value = !speaker.value!!
                    speakerClick.value = true
                } else {
                    cameraSwitch.value = !cameraSwitch.value!!
                }
                if (callType.value == VoiceVideoCallType.VIDEO) autoAnimation()

            }
            R.id.img_call_started_mute_call -> {
                isMute.value = !isMute.value!!
                isMuteClick.value = true
                if (callType.value == VoiceVideoCallType.VIDEO) autoAnimation()
            }
            R.id.img_call_started_video_call -> {
                disableVideoButtonClick()
            }

            R.id.img_incoming_accept_call -> {
                isClickVoiceIncomingCall = true
                callStatus.value = VoiceVideoStatus.ACCEPT
            }

            R.id.img_call_started_decline_call -> {
                callStatus.value = VoiceVideoStatus.CLOSE
            }

            R.id.img_incoming_decline_call -> {
                callStatus.value = VoiceVideoStatus.DECLINE
            }

            R.id.fl_video_container -> {
                onClickAnimation()
            }

            R.id.view_video_grey_background -> {
                onClickAnimation()
            }
        }
    }

    fun disableVideoButtonClick() {
        isClickVideoIncomingCall = true
        videoClick.value = !videoClick.value!!
        if (callType.value == VoiceVideoCallType.VOICE || !isVideoPermissionGranted(context)) {
            isConvertVideo.value = true
        }
        var click = true
        if (callType.value == VoiceVideoCallType.VIDEO && !isVideoPermissionGranted(context)) click = false
        onCalledVideoAction(videoClick.value!!, click)
        if (callType.value == VoiceVideoCallType.VIDEO) {
            autoAnimation()
        }
    }

    //when caller user video enable and disable
    fun onCallerVideoActions(it: Boolean) {
        LogUtils.e(CommonTAG,"CN->CallViewModel FN--> onCallerVideoActions() --> enter $it")
        if (it) {
            incomingStartedParticipantProfile.value = View.GONE
            viewVideoGreyBackground.value = View.GONE
        } else {
            incomingStartedParticipantProfile.value = View.VISIBLE
            viewVideoGreyBackground.value = View.VISIBLE
        }
    }

    //when called user video enable and disable
    fun onCalledVideoAction(it: Boolean, isClick: Boolean=false) {
        if (it) {
            if (isClick)mVideoMuteObserve.value=false
            mThumbnailBlurBackground.value = View.VISIBLE
            mImageThumbnailBackground.value = View.VISIBLE
        } else {
            if (isClick)mVideoMuteObserve.value=true
            mThumbnailBlurBackground.value = View.GONE
            mImageThumbnailBackground.value = View.GONE
        }
    }

    //incoming call ui for - voice,video
    fun incomingCallUI() {
        LogUtils.e(CommonTAG, "CN->CallViewModel FN--> incomingCallUI() --> ")
        videoFrameLayoutVisibility.value = View.GONE

        //Incoming Button
        incomingAcceptCallImageButton.value = View.VISIBLE
        incomingDeclineCallImageButton.value = View.VISIBLE
        callIncomingDeclineText.value = View.VISIBLE
        callIncomingAcceptText.value = View.VISIBLE

        //Name and Status and Background
        viewVoiceGreyBackground.value = View.VISIBLE
        participantsName.value = View.VISIBLE
        callStatusRinging.value = View.VISIBLE
        status.value = context.resources.getString(R.string.calls_status_incoming)
        callChronometerStatus.value = View.GONE
        callEncryptionDisclaimer.value = View.VISIBLE

        //Image
        animationRinging.value = View.VISIBLE
        incomingParticipantProfile.value = View.VISIBLE
        incomingStartedParticipantProfile.value = View.GONE

        //below 3 buttons
        speakerCameraVisible.value = View.GONE
        miceVisible.value = View.GONE
        videoVisibility.value = View.GONE

        //decline button
        startedDeclineCall.value = View.GONE
    }

    //outgoing voice call ui - when call is ringing [only for voice]
    fun outgoingVoiceCallUI() {
        LogUtils.e(CommonTAG, "CN->CallViewModel FN--> outgoingVoiceCallUI() --> ")
        videoFrameLayoutVisibility.value = View.GONE

        //Incoming Button
        incomingAcceptCallImageButton.value = View.GONE
        incomingDeclineCallImageButton.value = View.GONE
        callIncomingDeclineText.value = View.GONE
        callIncomingAcceptText.value = View.GONE

        //Name and Status and Background
        viewVoiceGreyBackground.value = View.VISIBLE
        participantsName.value = View.VISIBLE
        callStatusRinging.value = View.VISIBLE
        status.value = context.resources.getString(R.string.calls_status_Ringing)
        callChronometerStatus.value = View.GONE
        callEncryptionDisclaimer.value = View.VISIBLE

        //Image
        animationRinging.value = View.VISIBLE
        incomingParticipantProfile.value = View.VISIBLE
        incomingStartedParticipantProfile.value = View.GONE

        //below 3 buttons
        speakerCameraVisible.value = View.VISIBLE
        miceVisible.value = View.VISIBLE
        videoVisibility.value = View.VISIBLE

        //decline button
        startedDeclineCall.value = View.VISIBLE
    }

    //voice call started ui - when call is active
    fun voiceCallStartedUI() {
        LogUtils.e(CommonTAG, "CN->CallViewModel FN--> voiceCallStartedUI() --> ")
        videoFrameLayoutVisibility.value = View.GONE

        //Incoming Button
        incomingAcceptCallImageButton.value = View.GONE
        incomingDeclineCallImageButton.value = View.GONE
        callIncomingDeclineText.value = View.GONE
        callIncomingAcceptText.value = View.GONE

        //Image
        animationRinging.value = View.GONE
        incomingParticipantProfile.value = View.GONE
        incomingStartedParticipantProfile.value = View.VISIBLE

        //Name and Status and Background
        viewVoiceGreyBackground.value = View.VISIBLE
        participantsName.value = View.VISIBLE
        callStatusRinging.value = View.GONE
        callChronometerStatus.value = View.VISIBLE
        callEncryptionDisclaimer.value = View.VISIBLE

        //below 3 buttons
        speakerCameraVisible.value = View.VISIBLE
        miceVisible.value = View.VISIBLE
        videoVisibility.value = View.VISIBLE

        //decline button
        startedDeclineCall.value = View.VISIBLE
    }

    //video call ui - outgoing video call, when video call active
    fun videoCallUI() {
        LogUtils.e(CommonTAG, "CN->CallViewModel FN--> videoCallUI() --> ")
        videoFrameLayoutVisibility.value = View.VISIBLE
        callChronometerStatus.value = View.GONE
        videoClick.value = false

        //Incoming Button
        incomingAcceptCallImageButton.value = View.GONE
        incomingDeclineCallImageButton.value = View.GONE
        callIncomingDeclineText.value = View.GONE
        callIncomingAcceptText.value = View.GONE

        //Image
        animationRinging.value = View.GONE
        incomingParticipantProfile.value = View.GONE
        incomingStartedParticipantProfile.value = View.GONE


        //Name and Status and Background
        viewVoiceGreyBackground.value = View.GONE
        participantsName.value = View.GONE
        callStatusRinging.value = View.GONE
        callEncryptionDisclaimer.value = View.VISIBLE

        //below 3 buttons
        speakerCameraVisible.value = View.VISIBLE
        miceVisible.value = View.VISIBLE
        videoVisibility.value = View.VISIBLE

        //decline button
        startedDeclineCall.value = View.VISIBLE
        setAutoHideButton()
    }

    fun autoAnimation() {
//        LogUtils.e(CommonTAG, "CN->CallViewModel FN--> autoAnimation() --> ")
        if (callType.value == VoiceVideoCallType.VIDEO) {
            if (mTimer != null) {
                mTimer?.cancel()
                mTimer = null
            }
            mTimer = object : CountDownTimer(4000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    show = false
                    buttonHide.value = true
                    cancel()
                }
            }.start()
        }
    }

    private fun onClickAnimation() {
        if (callType.value == VoiceVideoCallType.VIDEO) {
            show = !show
            if (show) autoAnimation()
            buttonHide.value = true
        }
    }


    private fun setAutoHideButton() {
        if (callType.value == VoiceVideoCallType.VIDEO && startedDeclineCall.value == View.VISIBLE) {
            Handler().postDelayed({
                show = false
                buttonHide.value = true
            }, 500)
        }
    }
    override fun getAllObservedData(): Array<MutableLiveData<*>> = arrayOf()
}