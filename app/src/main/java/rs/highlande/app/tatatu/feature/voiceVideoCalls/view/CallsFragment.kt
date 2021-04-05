package rs.highlande.app.tatatu.feature.voiceVideoCalls.view

import android.Manifest
import android.content.*
import android.media.AudioManager
import android.os.*
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.twilio.video.*
import kotlinx.android.synthetic.main.common_toolbar_calls.*
import kotlinx.android.synthetic.main.fragment_calls.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.showDialogGeneric
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.CallViewModelBinding
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.*
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceVideoCallHelper.Companion.isVideoEnabled
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_CALL_ACCEPT
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_CALL_DECLINE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_REQUEST_LOCAL_CAMERA_CAPTURE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_ID
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_PERSON_AVATAR
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_PERSON_ID
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_PERSON_NAME
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_ROOM_NAME
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_STATUS
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_CALL_TYPE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.BUNDLE_KEY_USAGE_TYPE
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.HANDLER_ACTION_MUTE_CONVERSATION_VIEW
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.SERVICE_REGISTER_BIND
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.SERVICE_UNREGISTER_UNBIND


/**
 * Created by Abhin.
 */
class CallsFragment : BaseFragment() {

    private val mViewModel: CallViewModel by viewModel()
    private var mBinding: CallViewModelBinding? = null
    // messenger for communicating with service
    private var mService: Messenger? = null
    // flag to check service bouded or not
    private var mIsBound = false
    // messenger for communicating with activity
    private var mMessenger: Messenger? = null
    // flag to check 'mMessenger' registered with service or not
    private var mRegisterMessenger = false
    private val mPermissionRequestCode = 10044
    private var localVideoView: VideoRenderer? = null
    private var permissionHelper: PermissionHelper? = null
    // propertied for dragger view
    private var widgetXFirst: Float = 0F
    private var widgetDX: Float = 0F
    private var widgetYFirst: Float = 0F
    private var widgetDY: Float = 0F
    private var isVideoAccept: Boolean? = null
    private var isOnResumeCall = false
    private var isStopService = false
    private var isConvertAudioToVideoThumbnails = false
    private var localVideoTrack: LocalVideoTrack? = null
    private var remoteStoreVideoTrack: VideoTrack? = null
    private var cameraCapturesCompat: CameraCapturerCompat? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_calls, container, false)
        mBinding?.lifecycleOwner = this
        mBinding?.data = mViewModel
        return mBinding!!.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        initObserver()
    }

    private fun initObserver() {
        // observer for camera
        mViewModel.cameraSwitch.observe(this, Observer {
            switchCamera()
        })
        // observer for auto hide button
        mViewModel.buttonHide.observe(this, Observer {
            if (it) {
                animateActionButton()
            }
        })
        // observer for auto hide button
        mViewModel.isConvertVideo.observe(this, Observer {
            if (it) {
                if (isVideoPermissionGranted(context!!)) {
                    mViewModel.callType.value = VoiceVideoCallType.VIDEO
                    mViewModel.callStatus.value = VoiceVideoStatus.ACCEPT
                    requestToConvertVoiceToVideoCall()
                    mViewModel.isConvertVideo.value = false
                } else {
                    if (!mViewModel.isOpenDialog!!) initVideoPermissionCall(PermissionAllow.CONVERT)
                }
            }
        })

        // observer for speaker and update notifcation
        mViewModel.speakerClick.observe(this, Observer {
            if (it) {
                mViewModel.speakerClick.value = false
                val msg: Message = Message.obtain(null, CallServices.ACTION_ACTIVITY_SPEAKER, 0, 0)
                msg.obj = mViewModel.speaker.value
                mService?.send(msg)
            }
        })

        // observer for mute and update notification
        mViewModel.isMuteClick.observe(this, Observer {
            if (it) {
                mViewModel.isMuteClick.value = false
                val msg: Message = Message.obtain(null, CallServices.ACTION_ACTIVITY_MUTE, 0, 0)
                msg.obj = mViewModel.isMute.value
                mService?.send(msg)
            }
        })
        // observer for mute and update notification
        mViewModel.mVideoMuteObserve.observe(this, Observer {
            if (it != null) {
                if (mViewModel.isVoiceToVideoConvertDialogCancel && it) {
                    mViewModel.isVoiceToVideoConvertDialogCancel = false
                    requestToConvertVoiceToVideoCall()
                }
                val msg: Message = Message.obtain(null, CallServices.ACTION_VIDEO_MUTE, 0, 0)
                msg.obj = it
                mService?.send(msg)
            }
        })
        //update UI based on CallStatus
        mViewModel.callStatus.observe(this, Observer {
            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> mViewModel.callStatus Observer() --> $it")
            if (mViewModel.callType.value == VoiceVideoCallType.VOICE) txt_tb_title.text = context?.resources?.getString(R.string.call_tatatu_voice)
            else txt_tb_title.text = ""
            when (it) {
                VoiceVideoStatus.ACCEPT -> {
                    when (mViewModel.callType.value!!) {
                        VoiceVideoCallType.VOICE -> {
                            if (isAudioPermissionGranted(context!!)) {
                                onCallAcceptSendDataService()
                                mViewModel.voiceCallStartedUI()
                            } else initVoicePermissionCall(PermissionAllow.ACCEPT)
                        }
                        VoiceVideoCallType.VIDEO -> {
                            videoCallAcceptAction()
                        }
                    }
                }
                VoiceVideoStatus.DECLINE -> callDecline()
                VoiceVideoStatus.CLOSE -> callClose()
                else -> setDefaultView()
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onActivityCreated() --> ")
        setUI()
    }

    //after Accept from notification and UI button then perform action
    private fun videoCallAcceptAction() {
        if (isVideoPermissionGranted(context!!)) {
            onCallAcceptSendDataService()
            updateVideoUI()
        } else {
            if (!mViewModel.isOpenDialog!!) initVideoPermissionCall(PermissionAllow.ACCEPT)
        }
    }

    private fun setUI() {
        localVideoView = thumbnailVideoView as VideoRenderer
        mMessenger = Messenger(IncomingHandler(this))
        primaryVideoView?.applyZOrder(false)
        thumbnailVideoView?.applyZOrder(true)
        videoThumbnailDraggableWall()

        mBinding?.elapsedTime?.typeface = ResourcesCompat.getFont(context!!, R.font.lato)

    }

    private fun init() {
        if (arguments != null) {
            val callType = arguments?.getSerializable(BUNDLE_KEY_CALL_TYPE)
            val usageType = arguments?.getSerializable(BUNDLE_KEY_USAGE_TYPE)
            val callStatus = arguments?.getSerializable(BUNDLE_KEY_CALL_STATUS)
            val callName = arguments?.getString(BUNDLE_KEY_CALL_PERSON_NAME)
            val callAvatar = arguments?.getString(BUNDLE_KEY_CALL_PERSON_AVATAR)
            val roomName = arguments?.getString(BUNDLE_KEY_CALL_ROOM_NAME)
            val callID = arguments?.getString(BUNDLE_KEY_CALL_ID)
            val calledID = arguments?.getString(BUNDLE_KEY_CALL_PERSON_ID)
            val callTime = arguments?.getString(CallServices.BUNDLE_KEY_CALL_TIME)
            val callMute = arguments?.getBoolean(CallServices.BUNDLE_KEY_CALL_MUTE)

            mViewModel.isOpenDialog = arguments?.getBoolean(CallServices.BUNDLE_KEY_DIALOG)


            isVideoAccept = arguments?.getBoolean(CallServices.BUNDLE_KEY_VIDEO_ACCEPT)
            val isAudioPermissionRequired = arguments?.getBoolean(CallServices.BUNDLE_KEY_AUDIO_PERMISSION_REQUIRED)

            if (callType != null && usageType != null && callStatus != null && areStringsValid(callName, callAvatar, callID, roomName)) {
                mViewModel.usageType.value = usageType as VoiceVideoUsageType
                mViewModel.callType.value = callType as VoiceVideoCallType
                mViewModel.profileName.value = callName
                mViewModel.profileAvatar.value = callAvatar
                mViewModel.roomName.value = roomName
                mViewModel.callId.value = callID
                mViewModel.callTime.value = callTime
                mViewModel.calledId.value = calledID
                mViewModel.callStatus.value = callStatus as VoiceVideoStatus
                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> init() --> timer->$callTime")
                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> init() --> ${mViewModel.usageType.value},${mViewModel.callType.value},${mViewModel.callStatus.value},${mViewModel.profileName.value},${mViewModel.profileAvatar.value},${mViewModel.roomName.value},${mViewModel.callId.value}")
                if (isAudioPermissionRequired != null && isAudioPermissionRequired && hasMarshmallow()) {
                    if (mViewModel.callType.value == VoiceVideoCallType.VOICE) {
                        initVoicePermissionCall(PermissionAllow.ACCEPT_TIME_RESET)
                    }
                }
                if (callMute != null) {
                    mViewModel.isMute.value = callMute
                }
                if (mViewModel.isOpenDialog!!) {
                    isVideoAccept = false
                    CallServices.isVideoMute = true
                    openSwitchToVideoDialog()
                }
            }
        } else {
            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> init() --> get Arguments null ")
        }
    }

    // show - hide action buttons
    private fun animateActionButton() {
        val animationDuration: Long = 300
        val yEnd = -dpToPx(134f, resources).toFloat()

        val animateUp = TranslateAnimation(0f, 0f, 0f, yEnd)
        animateUp.duration = animationDuration
        animateUp.fillAfter = true
        animateUp.interpolator = AccelerateInterpolator()

        val animateDown = TranslateAnimation(0f, 0f, yEnd, 0f)
        animateDown.duration = animationDuration
        animateDown.fillAfter = true
        animateDown.interpolator = DecelerateInterpolator()

        val animateAlphaShow = AlphaAnimation(0f, 1f)
        animateAlphaShow.duration = animationDuration
        animateAlphaShow.fillAfter = true

        val animateAlphaHide = AlphaAnimation(1f, 0f)
        animateAlphaHide.duration = animationDuration
        animateAlphaHide.fillAfter = true

        val transition: Transition = Slide(Gravity.BOTTOM)
        transition.duration = animationDuration

        transition.addTarget(R.id.img_call_started_decline_call)
        transition.addTarget(R.id.img_call_started_video_call)
        transition.addTarget(R.id.img_speaker_camera_call)
        transition.addTarget(R.id.img_call_started_mute_call)

        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition) {
                //                LogUtils.e("Tag", "onTransitionEnd() --> ")
                mViewModel.isAnimationStart = false
            }

            override fun onTransitionResume(transition: Transition) {
                //                LogUtils.e("Tag", "onTransitionResume() --> ")
            }

            override fun onTransitionPause(transition: Transition) {
                //                LogUtils.e("Tag", "onTransitionPause() --> ")
            }

            override fun onTransitionCancel(transition: Transition) {
                //                LogUtils.e("Tag", "onTransitionCancel() --> ")
            }

            override fun onTransitionStart(transition: Transition) {
                //                LogUtils.e("Tag", "onTransitionStart() --> ")
                mViewModel.isAnimationStart = true
            }
        })

        if (mViewModel.show && mViewModel.isThumbnailsBottom) {
            view_video_transparent_background.startAnimation(animateAlphaShow)
            cl_thumbnail_video_view.startAnimation(animateUp)
        } else if (!mViewModel.show && mViewModel.isThumbnailsBottom && img_call_started_video_call.visibility == View.VISIBLE) {
            cl_thumbnail_video_view.startAnimation(animateDown)
            view_video_transparent_background.startAnimation(animateAlphaHide)
        }

        if (view != null && !mViewModel.isAnimationStart) {
            TransitionManager.beginDelayedTransition(cl_calls_main, transition)
            img_call_started_decline_call.visibility = if (mViewModel.show) View.VISIBLE else View.GONE
            img_speaker_camera_call.visibility = if (mViewModel.show) View.VISIBLE else View.GONE
            img_call_started_mute_call.visibility = if (mViewModel.show) View.VISIBLE else View.GONE
            img_call_started_video_call.visibility = if (mViewModel.show) View.VISIBLE else View.GONE
            view_video_transparent_background.visibility = if (mViewModel.show) View.VISIBLE else View.GONE
            if (mViewModel.show) mViewModel.autoAnimation()
        }
    }

    // video thumbnail dagger
    private fun videoThumbnailDraggableWall() {
        val mNormalGap = dpToPx(16f, resources).toFloat()
        val mDuration: Long = 250
        val stickyAxis: STICKY = STICKY.AXIS_XY
        val listener = View.OnTouchListener(function = { v, event ->
            val viewParent: View = (v.parent as View)
            val mParentHeight = viewParent.height
            val mParentWidth = viewParent.width
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    this.widgetDX = v.x - event.rawX
                    this.widgetDY = v.y - event.rawY
                    this.widgetXFirst = v.x
                    this.widgetYFirst = v.y
                }
                MotionEvent.ACTION_MOVE -> {
                    // Screen border Collision
                    var newX = event.rawX + this.widgetDX
                    newX = 0F.coerceAtLeast(newX)
                    newX = (mParentWidth - v.width).toFloat().coerceAtMost(newX)
                    v.x = newX
                    var newY = event.rawY + this.widgetDY
                    newY = 0F.coerceAtLeast(newY)
                    newY = (mParentHeight - v.height).toFloat().coerceAtMost(newY)
                    v.y = newY
                }
                MotionEvent.ACTION_UP -> {
                    when (stickyAxis) {
                        STICKY.AXIS_X -> {
                            if (event.rawX >= mParentWidth / 2) {
                                v.animate().x((mParentWidth) - (v.width).toFloat()).setDuration(mDuration).start()

                            } else {
                                v.animate().x(0F).setDuration(mDuration).start()
                            }
                        }
                        STICKY.AXIS_Y -> {
                            if (event.rawY >= mParentHeight / 2) {
                                v.animate().y((mParentHeight) - (v.height).toFloat()).setDuration(mDuration).start()
                            } else {
                                v.animate().y(0F).setDuration(mDuration).start()
                            }
                        }
                        STICKY.AXIS_XY -> {
                            if (event.rawX >= mParentWidth / 2) {
                                LogUtils.i(CommonTAG, "CN->CallsFragment FN--> videoThumbnailDraggableWall() --> x_right")
                                v.animate().x((mParentWidth) - ((v.width).toFloat() + mNormalGap)).setDuration(mDuration).start()
                            } else {
                                LogUtils.i(CommonTAG, "CN->CallsFragment FN--> videoThumbnailDraggableWall() --> x_left")
                                v.animate().x(mNormalGap).setDuration(mDuration).start()
                            }

                            if (event.rawY >= mParentHeight / 2) {
                                LogUtils.i(CommonTAG, "CN->CallsFragment FN--> videoThumbnailDraggableWall() --> y_right")
                                mViewModel.isThumbnailsBottom = true
                                v.animate().y((mParentHeight) - ((v.height).toFloat() + mNormalGap)).setDuration(mDuration).start()
                            } else {
                                LogUtils.i(CommonTAG, "CN->CallsFragment FN--> videoThumbnailDraggableWall() --> y_left")
                                mViewModel.isThumbnailsBottom = false
                                v.animate().y(mNormalGap).setDuration(mDuration).start()
                            }
                        }
                    }
                }
                else -> false
            }
            true
        })
        cl_thumbnail_video_view.setOnTouchListener(listener)
    }

    // check voice permission
    private fun initVoicePermissionCall(mPermissionAllow: PermissionAllow = PermissionAllow.DEFAULT) {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> initVoicePermissionCall() --> ")
        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE), mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                when (mPermissionAllow) {
                    PermissionAllow.ACCEPT -> {
                        mViewModel.voiceCallStartedUI()
                        requestForInitHelper()
                    }
                    PermissionAllow.ACCEPT_TIME_RESET -> {
                        mViewModel.callTime.value = SystemClock.elapsedRealtime().toString()
                        mViewModel.voiceCallStartedUI()
                        requestForInitHelper()
                    }
                    else -> {
                        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onPermissionGranted() --> Default")
                    }
                }
            }

            override fun onPermissionDenied() {
            }

            override fun onPermissionDeniedBySystem() {
            }
        })
    }

    // check video permissions
    private fun initVideoPermissionCall(permissionAllow: PermissionAllow = PermissionAllow.DEFAULT) {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> initVideoPermissionCall() --> ")
        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE), mPermissionRequestCode, true)
        permissionHelper!!.requestManually(object : PermissionHelper.PermissionCallbackManually {
            override fun onPermissionGranted() {
                cameraAudioPermissionGranted(permissionAllow)
            }

            override fun onCheckPermissionManually(requestCode: Int, permissions: Array<String>, grantResults: IntArray, checkPermissionsManuallyGranted: ArrayList<String>, checkPermissionsManuallyPending: ArrayList<String>) {
                /*LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> onCheckPermissionManually() checkPermissionsManuallyGranted--> ${Gson().toJson(checkPermissionsManuallyGranted)}")
                LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> onCheckPermissionManually() checkPermissionsManuallyPending--> ${Gson().toJson(checkPermissionsManuallyPending)}")*/
                if (checkPermissionsManuallyGranted.containsAll(arrayListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))) {
                    cameraAudioPermissionGranted(permissionAllow)
                } else if (checkPermissionsManuallyGranted.containsAll(arrayListOf(Manifest.permission.RECORD_AUDIO))) {
                    if (checkPermissionsManuallyPending.contains(Manifest.permission.CAMERA) && mViewModel.isConvertVideo.value!!) {
                        mViewModel.videoClick.value = false
                    }
                    onCallAcceptSendDataService()
                    updateVideoUI(permissionAllow)
                    if (remoteStoreVideoTrack != null) {
                        mViewModel.onCallerVideoActions(remoteStoreVideoTrack?.isEnabled!!)
                        remoteStoreVideoTrack?.addRenderer(primaryVideoView)
                    }
                }
            }
        })
    }

    private fun cameraAudioPermissionGranted(permissionAllow: PermissionAllow) {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> cameraAudioPermissionGranted() --> permissionAllow --> $permissionAllow")
        when (permissionAllow) {
            PermissionAllow.ACCEPT -> {
                if (!mViewModel.isConvertVideo.value!!) {
//                    isVideoEnabled = true
                    requestToConvertVoiceToVideoCall()
                    onCallAcceptSendDataService()
                    updateVideoUI()
//                    requestForInitHelper()
                }
            }
            PermissionAllow.CONVERT, PermissionAllow.CONVERT_FORM_VOICE_TO_VIDEO_POPUP -> {
                isVideoEnabled = true

                CallServices.isVideoMute = false
                mViewModel.callType.value = VoiceVideoCallType.VIDEO
                updateVideoUI()
                requestToConvertVoiceToVideoCall()
                mViewModel.callStatus.value = VoiceVideoStatus.ACCEPT
                mViewModel.isConvertVideo.value = false
                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> cameraAudioPermissionGranted() --> ${(permissionAllow == PermissionAllow.CONVERT_FORM_VOICE_TO_VIDEO_POPUP)}")
                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> cameraAudioPermissionGranted() UserClick.OK --> ${mViewModel.mVoiceTOVideoDialogUserClick}  --> (mViewModel.mUserClick==UserClick.OK) --> ${(mViewModel.mVoiceTOVideoDialogUserClick == VoiceTOVideoDialogUserClick.OK)}")
                if (permissionAllow == PermissionAllow.CONVERT_FORM_VOICE_TO_VIDEO_POPUP) {
                    if (mViewModel.mVoiceTOVideoDialogUserClick == VoiceTOVideoDialogUserClick.OK) {
                        //                        isVideoEnabled = true
                        mViewModel.onCalledVideoAction(false, true)
                    }
                }
            }
            else -> {
                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onPermissionGranted() --> onStartPermission()")
            }
        }
    }


    // show video UI
    private fun updateVideoUI(permissionAllow: PermissionAllow = PermissionAllow.DEFAULT) {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> updateVideoUI() --> ")
        if (permissionAllow != PermissionAllow.CONVERT) {
            txt_tb_title.text = ""
            primaryVideoView?.visibility = View.VISIBLE
            mViewModel.videoCallUI()
            primaryVideoView?.applyZOrder(false)
            thumbnailVideoView?.applyZOrder(true)
            mViewModel.show = false
            mViewModel.autoAnimation()
            isVideoPermissionGranted(context!!).let {
                mViewModel.onCallerVideoActions(it)
                mViewModel.onCalledVideoAction(!it, false)
            }

            mViewModel.callEncryptionDisclaimer.value = View.GONE

        } else {
            mViewModel.mVideoMuteObserve.value.let {
                if (it == null) mViewModel.onCalledVideoAction(true, false)
                else mViewModel.onCalledVideoAction(it, false)
            }
        }
    }

    //when user want to convert Audio call to Video Call
    private fun requestToConvertVoiceToVideoCall(convertToVideo: Boolean = true) {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> requestToConvertVoiceToVideoCall() --> ")
        val msg: Message = Message.obtain(null, CallServices.ACTION_GET_LOCAL_VIDEO_TRACK, 0, 0)
        if (convertToVideo) msg.obj = "CONVERT_TO_VIDEO"
        mService?.send(msg)

        //send for Camera Capture
        serviceMessageSend(ACTION_REQUEST_LOCAL_CAMERA_CAPTURE, null)
    }

    fun requestForInitHelper() {
        val msg: Message = Message.obtain(null, CallServices.ACTION_VOICE_VIDEO_PERMISSION_ALLOWED, 0, 0)
        mService?.send(msg)
    }

    //bind the service
    override fun onStart() {
        super.onStart()
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onStart() --> ")

        AnalyticsUtils.trackScreen(logTag)

        if (!mRegisterMessenger) doBindService()
    }

    override fun onResume() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onResume() --> ")
        super.onResume()
        if (mRegisterMessenger) serviceMessageSend(CallServices.ACTION_ACTIVITY_RESUMED, null)
        else {
            isOnResumeCall = true
        }
    }

    override fun onPause() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onPause() --> ")
        mViewModel.mVideoMuteObserve.value = null //prevent the old value  override
        serviceMessageSend(CallServices.ACTION_ACTIVITY_PAUSED, null)
        super.onPause()
    }

    override fun onStop() {
        doUnbindService(isStopService)
        super.onStop()
    }


    override fun onDestroyView() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onDestroyView() --> ")
        mCallsFragment = null
        mViewModel.callStatus.value = VoiceVideoStatus.DEFAULT
        thumbnailVideoView.visibility = View.VISIBLE
        isVideoAccept = null
        isStopService = false
        super.onDestroyView()
    }

    // bind service with fragment
    private fun doBindService() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> doBindService() --> ")
        //Create Intent of service
        val intent = Intent(activity, CallServices::class.java).apply {
            replaceExtras(arguments)
        }

        //Start Service if it's not already started
        if (!isMyServiceRunning(context!!, CallServices::class.java)) {
            activity?.startService(intent)
        }

        //Bind the Service
        if (!mIsBound) {
            activity?.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            mIsBound = true
        }
    }

    // unbind service from fragment
    private fun doUnbindService(isStop: Boolean = false) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> doUnbindService() --> MSG_UNREGISTER_CLIENT mMessenger")
                    val msg: Message = Message.obtain(null, SERVICE_UNREGISTER_UNBIND)
                    msg.replyTo = mMessenger
                    mService!!.send(msg)
                    mRegisterMessenger = false
                } catch (e: RemoteException) {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> doUnbindService() --> There is nothing special we need to do if the service has crashed")
                    e.printStackTrace()
                }
            }
            activity?.unbindService(mConnection)
            mIsBound = false
        }
        if (isStop) {
            val intent = Intent(activity, CallServices::class.java)
            activity?.stopService(intent)
        }
    }

    // listener for service connection
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            /**
             * This is called when the connection with the service has been established, giving us the service object we can use to
             * interact with the service.  We are communicating with our service through an IDL interface, so get a client-side
             * representation of that from the raw service object.
             */
            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onServiceConnected() --> ")
            mService = Messenger(service)
            try {
                val msg: Message = Message.obtain(null, SERVICE_REGISTER_BIND)
                msg.replyTo = mMessenger
                msg.obj = createBundleForService()
                mService!!.send(msg)
                mRegisterMessenger = true

                if (mViewModel.callType.value == VoiceVideoCallType.VIDEO && mViewModel.usageType.value == VoiceVideoUsageType.INCOMING && isVideoAccept != null && isVideoAccept!!) {
                    videoCallAcceptAction()
                }

                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onServiceConnected() --> isOnResumeCall -->$isOnResumeCall")
                if (isOnResumeCall) {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onServiceConnected() --> onresume call")
                    isOnResumeCall = false
                    serviceMessageSend(CallServices.ACTION_ACTIVITY_RESUMED, null)
                }
            } catch (e: RemoteException) {
                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onServiceConnected() --> Error ${e.printStackTrace()}")
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mService = null
            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onServiceDisconnected() --> Service Disconnected")
        }
    }

    // handler incoming messages from service
    class IncomingHandler(var callsFragment: CallsFragment) : Handler() {
        override fun handleMessage(msg: Message) {
            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> msg.what ${msg.what}")
            when (msg.what) {
                ACTION_CALL_ACCEPT, CallServices.ACTION_CALL_MAIN -> {
                    getDataFromService(msg)
                }
                CallServices.ACTION_ADD_PARTICIPANT_VIDEO_VIEW -> {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> ")
                    msg.obj?.let {
                        callsFragment.addRemoteParticipantVideo(it as VideoTrack)
                    }
                }
                CallServices.ACTION_REMOVE_PARTICIPANT_AND_MANAGE_DISCONNECT -> {
                    msg.obj?.let {
                        callsFragment.removeRemoteParticipantAndManageDisconnect(it as RemoteParticipant)
                    }
                }
                CallServices.ACTION_VIDEO_REMOVE_PARTICIPANTS_UNSUBSCRIBE -> {
                    (msg.obj as? RemoteVideoTrack)?.let {
                        if (it.isEnabled) it.removeRenderer(callsFragment.primaryVideoView)
                    }
                }
                CallServices.ACTION_GET_LOCAL_CAMERA_CAPTURE -> {
                    msg.obj?.let {
                        callsFragment.cameraCapturesCompat = (it as CameraCapturerCompat)
                    }
                }
                CallServices.ACTION_MOVE_LOCAL_VIDEO_PRIMARY_VIEW -> {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> ${msg.obj}")
                }
                CallServices.ACTION_GET_ROOM_PARTICIPANTS -> {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() ACTION_GET_ROOM_PARTICIPANTS--> ${msg.obj}")
                    msg.obj?.let {
                        callsFragment.moveLocalVideoToThumbnailView()
                        val remoteVideoTrack = it as RemoteVideoTrack
                        remoteVideoTrack.addRenderer(callsFragment.primaryVideoView)
                        if (!isVideoPermissionGranted(callsFragment.context!!)) callsFragment.remoteStoreVideoTrack = msg.obj as RemoteVideoTrack
                        else callsFragment.remoteStoreVideoTrack = null
                        //set video disable if user are caller the video
                        if (callsFragment.mViewModel.callStatus.value == VoiceVideoStatus.ACCEPT) {
                            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> remoteVideoTrack.isEnabled ${remoteVideoTrack.isEnabled}")
                            callsFragment.mViewModel.onCallerVideoActions(remoteVideoTrack.isEnabled)
                        }
                    }
                }
                CallServices.ACTION_GET_LOCAL_VIDEO_TRACK -> {
                    if (msg.obj == null && msg.arg1 == 1 && msg.arg2 == 1) {
                        callsFragment.mService?.send(Message.obtain(null, CallServices.ACTION_REQUEST_ROOM_PARTICIPANTS, 0, 0))
                    }
                    msg.obj?.let {
                        if (callsFragment.localVideoTrack == null) {
                            callsFragment.localVideoTrack = it as LocalVideoTrack
                            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() msg.arg1--> ${msg.arg1}")
                            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() msg.arg2--> ${msg.arg2}")
                            if (msg.arg1 == 1) { // opponent connected room
                                if (msg.arg2 == 0) { // opponent video track not enable
                                    callsFragment.mViewModel.onCallerVideoActions(false)
                                }
                                callsFragment.moveLocalVideoToThumbnailView()
                            } else {
                                callsFragment.localVideoTrack?.addRenderer(callsFragment.primaryVideoView)
                            }
                            //request participants
                            callsFragment.mService?.send(Message.obtain(null, CallServices.ACTION_REQUEST_ROOM_PARTICIPANTS, 0, 0))
                        } else {
                            callsFragment.localVideoTrack = it as LocalVideoTrack
                            if (msg.arg2 == 0) { // opponent video track not enable
                                callsFragment.mViewModel.onCallerVideoActions(false)
                                callsFragment.moveLocalVideoToThumbnailView()
                            } else {
                                callsFragment.localVideoTrack?.addRenderer(callsFragment.thumbnailVideoView)
                                callsFragment.checkLocalVideoTrackEnable()

                                //request participants
                                callsFragment.mService?.send(Message.obtain(null, CallServices.ACTION_REQUEST_ROOM_PARTICIPANTS, 0, 0))
                            }
                        }
                        var videoEnable = 1
                        if (CallServices.isVideoMute) videoEnable = 0
                        callsFragment.mService?.send(Message.obtain(null, CallServices.ACTION_AUDIO_CONVERT_VIDEO, videoEnable, 0))
                    }
                }
                //use for is converted video
                CallServices.ACTION_AUDIO_CONVERT_VIDEO -> {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> ")
                    msg.obj?.let {
                        callsFragment.localVideoTrack = it as LocalVideoTrack
                        if (msg.arg1 == 1) {
                            callsFragment.localVideoTrack?.addRenderer(callsFragment.thumbnailVideoView)
                            if (msg.arg2 == 0) callsFragment.mViewModel.onCallerVideoActions(false)
                            callsFragment.checkLocalVideoTrackEnable()
                        } else {
                            callsFragment.localVideoTrack?.addRenderer(callsFragment.primaryVideoView)
                        }
                        var videoEnable = 1
                        if (CallServices.isVideoMute) videoEnable = 0
                        callsFragment.mService?.send(Message.obtain(null, CallServices.ACTION_AUDIO_CONVERT_VIDEO, videoEnable, 0))
                    }
                }
                HANDLER_ACTION_MUTE_CONVERSATION_VIEW -> {
                    msg.obj?.let {
                        callsFragment.manageMicroPhoneMuteParticipants(it as Boolean)
                    }
                }
                CallServices.HANDLER_HANDLER_ACTION_ERROR -> {
                    msg.obj?.let {
                        callsFragment.showError(it as String)
                    }
                }
                CallServices.ACTION_SPEAKER -> {
                    msg.obj?.let {
                        callsFragment.setActionSpeaker(it as Boolean)
                    }
                }
                CallServices.ACTION_MUTE -> {
                    msg.obj?.let {
                        callsFragment.setActionMute(it as Boolean)
                    }
                }
                CallServices.ACTION_CALLER_VIDEO_MUTE -> {
                    msg.obj?.let {
                        callsFragment.mViewModel.onCallerVideoActions(it as Boolean)
                    }
                }
                CallServices.ACTION_STREAM_VOICE_CALL -> {
                    callsFragment.activity?.volumeControlStream = AudioManager.STREAM_VOICE_CALL
                }
                CallServices.HANDLER_CALL_STATUS -> {
                    msg.obj?.let {
                        callsFragment.txt_call_status_ringing.text = (it as String)
                        when (callsFragment.txt_call_status_ringing.text) {
                            callsFragment.resources.getString(R.string.call_connected) -> {
                                if (msg.arg1 == 0) {
                                    if (callsFragment.mViewModel.callType.value == VoiceVideoCallType.VOICE) {
                                        callsFragment.mViewModel.callTime.value = SystemClock.elapsedRealtime().toString()
                                        callsFragment.mViewModel.callStatus.value = VoiceVideoStatus.ACCEPT
                                    } else {
                                        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> calltype==VIDEO")
                                    }
                                } else {
                                    Handler().postDelayed(Runnable {
                                        // handler for user visibility
                                        callsFragment.txt_call_status_ringing.visibility = View.GONE
                                    }, 500)
                                }
                            }
                            callsFragment.resources.getString(R.string.calls_timer_disconnecting_num, 3) -> {
                                callsFragment.txt_call_status_ringing.visibility = View.VISIBLE
                            }
                            callsFragment.resources.getString(R.string.calls_timer_disconnecting_num, 1) -> {
                                callsFragment.callClose()
                            }
                            callsFragment.resources.getString(R.string.call_connection_lost_reconnecting) -> {
                                callsFragment.txt_call_status_ringing.visibility = View.VISIBLE
                            }
                            callsFragment.resources.getString(R.string.not_able_to_connect_the_call) -> {
                                Handler().postDelayed(Runnable {
                                    // handler for user visibility
                                    callsFragment.callClose()
                                }, 1000)
                            }
                            else -> {
                                LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> null or empty")
                            }
                        }
                        /*if (callsFragment.txt_call_status_ringing.text == callsFragment.resources.getString(R.string.call_connected) && callsFragment.mViewModel.callType.value == VoiceVideoCallType.VOICE) {
                            callsFragment.mViewModel.callTime.value = SystemClock.elapsedRealtime().toString()
                            callsFragment.mViewModel.callStatus.value = VoiceVideoStatus.ACCEPT
                        } else if (callsFragment.txt_call_status_ringing.text == callsFragment.resources.getString(R.string.calls_timer_disconnecting_num, 3)) {
                            callsFragment.txt_call_status_ringing.visibility = View.VISIBLE
                        } else if (callsFragment.txt_call_status_ringing.text == callsFragment.resources.getString(R.string.calls_timer_disconnecting_num, 1)) {
                            callsFragment.callClose()
                        }*/
                    }
                }
                CallServices.HANDLER_ACTIVITY_REJECT_CLOSE -> {
                    LogUtils.e(CommonTAG, "CN->CallsFragment FN--> handleMessage() --> HANDLER_ACTIVITY_CLOSE")
                    callsFragment.isStopService = true
                    callsFragment.activity?.finishAndRemoveTask()
                }
                CallServices.ACTION_VOLUME_CONTROL_STREAM -> {
                    callsFragment.activity?.volumeControlStream = AudioManager.STREAM_VOICE_CALL
                }
                CallServices.HANDLER_MANAGE_DISCLAIMER -> {
                    callsFragment.mViewModel.callEncryptionDisclaimer.value = msg.obj as Int
                }
                else -> super.handleMessage(msg)
            }
        }


        // extract data from service
        private fun getDataFromService(msg: Message) {
            val mBundle = msg.obj as Bundle
            val usageType = mBundle.getSerializable(BUNDLE_KEY_USAGE_TYPE) as VoiceVideoUsageType
            val callType = mBundle.getSerializable(BUNDLE_KEY_CALL_TYPE) as VoiceVideoCallType
            val callStatus = mBundle.getSerializable(BUNDLE_KEY_CALL_STATUS) as VoiceVideoStatus
            val callName = mBundle.getString(BUNDLE_KEY_CALL_PERSON_NAME)
            val callAvatar = mBundle.getString(BUNDLE_KEY_CALL_PERSON_AVATAR)
            callsFragment.mViewModel.isOpenDialog = mBundle.getBoolean(CallServices.BUNDLE_KEY_DIALOG)
            callsFragment.mViewModel.callType.value = callType
            callsFragment.mViewModel.usageType.value = usageType
            callsFragment.mViewModel.profileName.value = callName
            callsFragment.mViewModel.profileAvatar.value = callAvatar
            callsFragment.mViewModel.callStatus.value = callStatus
            if (callsFragment.mViewModel.isOpenDialog!!) {
                CallServices.isVideoMute = true
                callsFragment.openSwitchToVideoDialog()
            }
        }
    }


    // update speaker value
    fun setActionSpeaker(isSpeaker: Boolean) {
        mViewModel.speaker.value = isSpeaker
    }

    // update mute value
    fun setActionMute(isMute: Boolean) {
        mViewModel.isMute.value = isMute
    }

    // twilio - get microphone actions
    private fun manageMicroPhoneMuteParticipants(isMute: Boolean?) {
        val mMessage = getString(if (isMute!!) R.string.calls_user_action_unmute else R.string.calls_user_action_mute, mViewModel.profileName.value)
        showError(mMessage)
    }

    //twilio - get primary view actions
    private fun moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView != null && localVideoTrack != null) {
            localVideoTrack?.let {
                if (it.isEnabled) {
                    it.removeRenderer(thumbnailVideoView)
                    it.addRenderer(primaryVideoView)
                }
            }
            if (primaryVideoView != null) {
                localVideoView = primaryVideoView
                primaryVideoView?.mirror = cameraCapturesCompat?.cameraSource == CameraCapturer.CameraSource.FRONT_CAMERA
            }
        }
    }

    private fun moveLocalVideoToThumbnailView() {
        if (primaryVideoView != null && localVideoTrack != null) {
            localVideoTrack?.let {
                if (it.isEnabled) {
                    it.removeRenderer(primaryVideoView)
                    it.addRenderer(thumbnailVideoView)
                }
            }
            localVideoView = thumbnailVideoView
            thumbnailVideoView.mirror = cameraCapturesCompat?.cameraSource == CameraCapturer.CameraSource.FRONT_CAMERA
            checkLocalVideoTrackEnable()
        }
    }

    fun checkLocalVideoTrackEnable() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> checkLocalVideoTrackEnable() --> ")
        if (localVideoTrack != null && !localVideoTrack?.isEnabled!!) {
            mViewModel.videoClick.value = true
            mViewModel.onCalledVideoAction(true)
        }
    }

    private fun removeRemoteParticipantAndManageDisconnect(remoteParticipant: RemoteParticipant) {
        remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
            if (remoteVideoTrackPublication.isTrackSubscribed) {
                remoteVideoTrackPublication.remoteVideoTrack?.let {
                    if (it.isEnabled) it.removeRenderer(primaryVideoView)
                }
            }
        }
        moveLocalVideoToPrimaryView()
    }

    private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
        if (!videoTrack.isEnabled) mCallsFragment?.mViewModel?.onCallerVideoActions(false)
        else mCallsFragment?.mViewModel?.onCallerVideoActions(true)
        txt_tb_title.text = ""

        moveLocalVideoToThumbnailView()
        primaryVideoView.mirror = false
        videoTrack.addRenderer(primaryVideoView)

        if (mViewModel.callType.value == VoiceVideoCallType.VOICE) {
            mViewModel.callType.value = VoiceVideoCallType.VIDEO
            mViewModel.videoCallUI()

            openSwitchToVideoDialog()

            isConvertAudioToVideoThumbnails = true
        }

        if (primaryVideoView.visibility == View.GONE || primaryVideoView.visibility == View.INVISIBLE) {
            primaryVideoView.visibility = View.VISIBLE
            primaryVideoView.applyZOrder(false)
            thumbnailVideoView.applyZOrder(true)
        }
    }


    private fun openSwitchToVideoDialog() {
        mViewModel.mVoiceTOVideoDialogUserClick = VoiceTOVideoDialogUserClick.DEFAULT
        if (context?.isValid() == true) {
            showDialogGeneric(context!!, getString(R.string.calls_dialog_switch_message, mViewModel.profileName.value), getString(R.string.calls_dialog_switch_title), getString(R.string.btn_ok), getString(R.string.action_cancel), DialogInterface.OnClickListener { dialog, _ ->
                if (isVideoPermissionGranted(context!!)) {
                    requestToConvertVoiceToVideoCall()
                    mViewModel.onCalledVideoAction(false, true)
                } else {
                    initVideoPermissionCall(PermissionAllow.CONVERT_FORM_VOICE_TO_VIDEO_POPUP)
                }
                mViewModel.isVoiceToVideoConvertDialogCancel = false
                mViewModel.mVoiceTOVideoDialogUserClick = VoiceTOVideoDialogUserClick.OK
                dialog.dismiss()
                mViewModel.videoClick.value = false
            }, DialogInterface.OnClickListener { dialog, _ ->
               /* if (!mViewModel.isOpenDialog!! && isVideoPermissionGranted(context!!)) {
                    mViewModel.disableVideoButtonClick()
                }*/

                dialog.dismiss()
                mViewModel.isVoiceToVideoConvertDialogCancel = true
                mViewModel.mVoiceTOVideoDialogUserClick = VoiceTOVideoDialogUserClick.CANCEL
            })?.apply {
                setOnDismissListener {
                    if (mViewModel.mVoiceTOVideoDialogUserClick == VoiceTOVideoDialogUserClick.DEFAULT) {
//                        if ((!mViewModel.isOpenDialog!! && isVideoPermissionGranted(context))) mViewModel.disableVideoButtonClick()
                        mViewModel.isVoiceToVideoConvertDialogCancel = true
                    }
                    if (!isVideoPermissionGranted(context)) {
                        if (mViewModel.videoVisibility.value==View.GONE) {
                            Handler(activity!!.mainLooper).postDelayed(Runnable {
                                onCallAcceptSendDataService()
                                updateVideoUI(PermissionAllow.ACCEPT)
                                if (remoteStoreVideoTrack != null /*&& primaryVideoView!=null*/) {
                                    mViewModel.onCallerVideoActions(remoteStoreVideoTrack?.isEnabled!!)
                                    remoteStoreVideoTrack?.addRenderer(activity!!.primaryVideoView)
                                }
                            },100)
                        }
                        mViewModel.videoClick.value = true
                        mViewModel.onCalledVideoAction(true, true)
                    } else {
                        if ((mViewModel.mVoiceTOVideoDialogUserClick == VoiceTOVideoDialogUserClick.DEFAULT || mViewModel.mVoiceTOVideoDialogUserClick == VoiceTOVideoDialogUserClick.CANCEL) && localVideoTrack == null) {
                            mViewModel.disableVideoButtonClick()
                        }
                    }

                    setFullScreen()
                    onAskConvertAudioToVideoDialogFalse()
                    mViewModel.isOpenDialog = false
                    mViewModel.callEncryptionDisclaimer.value = View.GONE
                }
            }
        }
    }


    // send message to service
    private fun serviceMessageSend(action: Int, bundle: Bundle?) {
        try {
            LogUtils.e(CommonTAG, "CN->CallsFragment FN--> serviceMessageSend() --> $action")
            val msg: Message = Message.obtain(null, action, 0, 0)
            //Bundle Attach if it's not null
            if (bundle != null) msg.obj = bundle
            mService?.send(msg)
        } catch (e: RemoteException) {
            LogUtils.e(CommonTAG, "onServiceConnected() --> Error")
            e.printStackTrace()
        }
    }


    // permission callbacks
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionHelper != null) {
            permissionHelper!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // switch camera function
    private fun switchCamera() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> switchCamera() cameraCapturesCompat--> ${cameraCapturesCompat?.cameraSource}")
        if (cameraCapturesCompat != null) {
            val cameraSource = cameraCapturesCompat?.cameraSource
            cameraCapturesCompat?.switchCamera()
            mViewModel.cameraSwitchIcon.value = cameraSource != CameraCapturer.CameraSource.BACK_CAMERA
            if (thumbnailVideoView.visibility == View.VISIBLE) {
                thumbnailVideoView.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
            } else {
                primaryVideoView.mirror = cameraSource == CameraCapturer.CameraSource.BACK_CAMERA
            }
        } else {
            serviceMessageSend(ACTION_REQUEST_LOCAL_CAMERA_CAPTURE, null)
        }
    }

    // send data to service
    private fun onCallAcceptSendDataService() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onCallAcceptSendDataService() --> ")
        val mBundle = createBundleForService()
        serviceMessageSend(ACTION_CALL_ACCEPT, mBundle)
    }

    // send data to service
    private fun onAskConvertAudioToVideoDialogFalse() {
        LogUtils.e(CommonTAG, "CN->CallsFragment FN--> onCallAcceptSendDataService() --> ")
        serviceMessageSend(CallServices.ACTION_REQUEST_CHANGE_DIALOG_STATUS, null)
    }

    private fun createBundleForService(): Bundle {
        val mBundle = Bundle()
        mBundle.putString(BUNDLE_KEY_CALL_ID, mViewModel.callId.value)
        mBundle.putString(BUNDLE_KEY_CALL_PERSON_ID, mViewModel.calledId.value)
        mBundle.putSerializable(BUNDLE_KEY_CALL_TYPE, mViewModel.callType.value)
        mBundle.putSerializable(BUNDLE_KEY_USAGE_TYPE, mViewModel.usageType.value)
        mBundle.putSerializable(BUNDLE_KEY_CALL_STATUS, mViewModel.callStatus.value)
        mBundle.putString(BUNDLE_KEY_CALL_PERSON_NAME, mViewModel.profileName.value)
        mBundle.putString(BUNDLE_KEY_CALL_ROOM_NAME, mViewModel.roomName.value)
        mBundle.putString(BUNDLE_KEY_CALL_PERSON_AVATAR, mViewModel.profileAvatar.value)
        return mBundle
    }

    // call reject
    private fun callDecline() {
        LogUtils.e("Tag", "callReject() --> ")
        mService?.send(Message.obtain(null, ACTION_CALL_DECLINE, 0, 0))
    }

    // call Close
    private fun callClose() {
        LogUtils.e("Tag", "callClose() --> ")
        if (mService != null) {
            serviceMessageSend(CallServices.ACTION_CALL_CLOSE, null)
        } else activity?.finishAndRemoveTask()
    }

    private fun setDefaultView() {
        when (mViewModel.usageType.value) {
            VoiceVideoUsageType.INCOMING -> {
                mViewModel.incomingCallUI()
            }
            VoiceVideoUsageType.OUTGOING -> {
                if (mViewModel.callType.value == VoiceVideoCallType.VOICE) {
                    mViewModel.outgoingVoiceCallUI()
                } else {
                    updateVideoUI()
                }
            }
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}

    companion object {

        val logTag = CallsFragment::class.java.simpleName

        private var mCallsFragment: CallsFragment? = null
        fun newInstance(): CallsFragment {
            mCallsFragment = CallsFragment()
            val args = Bundle().apply { }
            return mCallsFragment!!.apply { arguments = args }
        }
    }

    private fun setFullScreen() {
        activity!!.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}