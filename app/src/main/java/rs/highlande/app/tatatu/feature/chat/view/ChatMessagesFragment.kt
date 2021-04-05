/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.view

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.chat_new_message.*
import kotlinx.android.synthetic.main.chat_new_message.profilePicture
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import kotlinx.android.synthetic.main.common_toolbar_back_picture.*
import kotlinx.android.synthetic.main.common_toolbar_back_picture.view.*
import kotlinx.android.synthetic.main.fragment_chat_messages.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.webSocket.realTime.RTCommHelper
import rs.highlande.app.tatatu.connection.webSocket.realTime.RealTimeChatListener
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.hideKeyboard
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.ChatNewMessageBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.chat.ActionType
import rs.highlande.app.tatatu.feature.chat.AudioRecordingHelper
import rs.highlande.app.tatatu.feature.chat.RemoveFocusClickListener
import rs.highlande.app.tatatu.feature.chat.api.ChatApi
import rs.highlande.app.tatatu.feature.chat.view.adapter.ChatMessagesAdapter
import rs.highlande.app.tatatu.feature.chat.view.viewModel.ChatMessagesViewModel
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewModel
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostFragment
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializer
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializerDelegate
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceVideoCallType
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatRecipientStatus
import rs.highlande.app.tatatu.model.event.ImageBottomEvent
import java.util.*

/**
 * @author mbaldrighi on 10/15/2018.
 */
class ChatMessagesFragment : BaseFragment(),
    RealTimeChatListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    OnCompleteListener<Location>,
    DefaultCallInitializer by DefaultCallInitializerDelegate() {

    companion object {

        val logTag = ChatMessagesFragment::class.java.simpleName

        fun newInstance(chatRoomId: String):
                ChatMessagesFragment {
            val fragment = ChatMessagesFragment()
            fragment.arguments = Bundle().apply {
                this.putString(EXTRA_PARAM_1, chatRoomId)
            }

            return fragment
        }
    }

    val viewModel: ChatMessagesViewModel by viewModel()
    val mWebViewModel: WebViewModel by sharedViewModel()

    private val rtCommHelper: RTCommHelper by inject()


    private lateinit var adapter: ChatMessagesAdapter
    private var llm: LinearLayoutManager? = null

    private var canFetchMessages: Boolean = false

    private var isTyping: Boolean = false


    private val toolbarHeight by lazy { resources.getDimensionPixelSize(R.dimen.toolbar_height) }
    private var stopAnimating = false

    var lastScrollPosition: Int? = null

    // INFO: 2020-01-09    Maps is commented
    private val googleApiClient by lazy {
        /*GoogleApiClient.Builder(context!!)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()*/
    }

    // INFO: 2020-01-09    Maps is commented
    private val fusedLocationClient by lazy {
        /*if (checkPlayServices(context!!))
            LocationServices.getFusedLocationProviderClient(context!! as Activity)
        else*/ null
    }

    private var micAnimation: ViewPropertyAnimator? = null

    private var permissionHelper: PermissionHelper? = null
    private val mPermissionRequestCode = 10005
    val mPermissionRequestCodeCalls = 10006


    private lateinit var menu: Menu


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as? BaseActivity)?.let { act ->
            act.setSupportActionBar(containerParticipant as Toolbar)
            act.supportActionBar?.let { bar ->
                bar.setDefaultDisplayHomeAsUpEnabled(false)
                bar.setDisplayShowHomeEnabled(false)
                bar.setHomeButtonEnabled(false)
                bar.setDisplayShowTitleEnabled(false)
            }
        }

        subscribeToLiveData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_chat_messages, container, false).apply {
            DataBindingUtil.bind<ChatNewMessageBinding>(findViewById<View>(R.id.bottomActions))
                ?.let {
                    it.viewModel = viewModel
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //chatHelper = ChatMessagesInteractionsHelper(view, this)
        configureLayout(view)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
        with(menu) {

            this@ChatMessagesFragment.menu = this


            findItem(R.id.actionVideoCall).isVisible =
                context?.let {
                    hasDeviceCamera(it)
                } ?: false
            findItem(R.id.actionVideoCall).isEnabled =
                viewModel.getValidRoomMainThread()?.canVideoCallParticipant() == true
            findItem(R.id.actionCall).isEnabled =
                viewModel.getValidRoomMainThread()?.canVoiceCallParticipant() == true
            findItem(R.id.actionOverflow).isVisible = false

            super.onCreateOptionsMenu(this, inflater)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.actionVideoCall -> {
                callParticipant(VoiceVideoCallType.VIDEO)
            }
            R.id.actionCall -> {
                callParticipant(VoiceVideoCallType.VOICE)
            }
        }

        return true
    }


    fun subscribeToLiveData() {
        viewModel.chatRoomUpdateLiveData.observe(viewLifecycleOwner, Observer {
            setStatusIndicatorAndString(it.first, it.second)
        })
//        viewModel.chatsToReadUpdateLiveData.observe(viewLifecycleOwner, Observer {
//            toolbar.toReadCount.text = it.toString()
//            toolbar.toReadCount.visibility = if (it > 0) View.VISIBLE else View.GONE
//            LogUtils.d(LOG_TAG, "All messages read for ChatRoom (id: ${viewModel.chatRoomId}")
//        })
        viewModel.updateListErrorLiveData.observe(viewLifecycleOwner, Observer {
            showError(getString(R.string.error_generic))
            activity!!.finish()
        })

        /*
         * Code block related to AUDIO recording
         */
        viewModel.chatRecordingAudioLiveData.observe(viewLifecycleOwner, Observer {
            when (it.audioRecordingStatus) {
                ChatMessagesViewModel.AudioRecordingStatus.AudioRecordingEnum.START_RECORDING -> {
//                    btnActionCamera.visibility = View.INVISIBLE
//                    recordOverlay.baseAnimateAlpha(1000, true)
//                    micAnimation = recordingMic.baseAnimateAlpha(750, false, true)
//
//                    recordElapsedTime?.start()
//                    recordElapsedTime?.base = SystemClock.elapsedRealtime()

                }
                ChatMessagesViewModel.AudioRecordingStatus.AudioRecordingEnum.STOP_RECORDING -> {
//                    recordElapsedTime?.stop()
//                    micAnimation?.cancel()
//                    btnActionCamera?.visibility = View.VISIBLE
//                    if (!it.exceptionCaught && it.fileCreated) {
//
//                        recordOverlay.baseAnimateAlpha(200, false)
//                        if (it.isCancel) {
//                            LogUtils.d(LOG_TAG, "Audio recording successfully canceled")
//                            Toast.makeText(context, R.string.chat_rec_audio_cancel, Toast.LENGTH_SHORT).show()
//                        } else showError(getString(R.string.error_upload_media))
//                    } else {
//                        recordOverlay.visibility = View.GONE
//                        recordOverlay.alpha = 0f
//                    }

                }
                ChatMessagesViewModel.AudioRecordingStatus.AudioRecordingEnum.SLIDE_TO_CANCEL -> {
//                    if (it.motionEvent == null) return@Observer
//
//                    val view = recordOverlay
//                    val viewWidth = view.width
//
//                    val offsetViewBounds = Rect()
//                    //returns the visible bounds
//                    btnActionMic.getDrawingRect(offsetViewBounds)
//                    // calculates the relative coordinates to the parent
//                    frame.offsetDescendantRectToMyCoords(btnActionMic, offsetViewBounds)
//
//                    val buttonLeftPlus = offsetViewBounds.left - 10
//
//                    val size = Point()
//                    activity?.windowManager!!.defaultDisplay.getSize(size)
//                    val screenWidth = size.x
//
//                    val canAnimate = (it.motionEvent.rawX < buttonLeftPlus) || !it.movingLeft
//                    val offScreen = it.motionEvent.rawX - screenWidth                  // should always be negative Int
//                    val visibleViewWidth = viewWidth - offScreen.absoluteValue
//                    val alphaOffset = visibleViewWidth / viewWidth
//
//                    if (canAnimate) {
//                        view.animate()
//                            .alpha(alphaOffset)
//                            .translationX(offScreen)
//                            .setDuration(0)
//                            .start()
//                    }
//
//                    // when slider view is visible for half of the screen width, stop recording
//                    if (it.movingLeft && visibleViewWidth <= (screenWidth*.35)) {
//                        view.baseAnimateAlpha(500, false)
//                        view.post {
//                            // reset original view position (??)
//                            view.translationX = 0f
//                        }
//
//                        viewModel.isCancelingAudio = true
//                    }
                }
            }
        })

        // var used t
        var count = 0L

        viewModel.newMessageReadyLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                hideLoader(); count = 0
                addNewMessage(it)
            }
        })

        viewModel.uploadMediaProgressUpdateLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                if (it.first && it.second != null) {
                    if (it.second == ActionType.UPLOAD && (count == 0L || it.third > count)) {
                        count = it.third

                        showLoader(
                            when (it.second) {
                                ActionType.COMPRESS -> getString(
                                    R.string.progress_video_compress,
                                    it.third
                                )
                                ActionType.UPLOAD -> getString(
                                    R.string.progress_media_upload,
                                    it.third
                                )
                                ActionType.CREATE -> getString(R.string.progress_post_create)
                                else -> getString(R.string.progress_post_edit)
                            }
                        )
                    }
                } else {
                    hideLoader()
                    count = 0
                }
            }
        })
        viewModel.uploadMediaFinishLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {

                LogUtils.d(logTag, "TESTCHATPROGRESS: FINISH with action: ${it.second}")

                hideLoader()
                count = 0
                when (it.second) {
                    true -> LogUtils.d(tag, "UPLOADSUCCESS")
                    false -> showError(getString(R.string.error_generic))
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        EventBus.getDefault().register(this)
        rtCommHelper.listenerChat = this
        (activity as ChatActivity).apply {
            hideMainBottomNavigation()
        }
    }

    override fun onResume() {
        super.onResume()

        bindLayout()

        viewModel.updateRoomsList()
        viewModel.setMessageRead()

        setMessages()

    }

    override fun onPause() {

//        chatHelper?.onPause()

//        lastScrollPosition = llm?.findLastVisibleItemPosition()

        adapter.apply {
            cachePositions()
            viewModel.playingVideos = playingVideos
            stopVideos(false)
        }

        if (!hasNougat()) adapter.cleanAdapterMediaControllers()
        messageBox.hideKeyboard()

        // INFO: 2020-01-09    Maps is commented
//        if (googleApiClient.isConnected || googleApiClient.isConnecting)
//            googleApiClient.disconnect()

        super.onPause()
    }

    override fun onStop() {
        if (hasNougat()) adapter.cleanAdapterMediaControllers()

        (activity as ChatActivity).apply {
            restoreMainBottomNavigation()
        }

        rtCommHelper.listenerChat = null
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroyView() {
        adapter.stopVideos(true)
        super.onDestroyView()
    }

    /**
     * getting result and set the arrayList and call adapter
     */
    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            Activity.RESULT_OK -> {
                when (requestCode) {
                    mRequestCamera -> {
                        if (mCameraImageUri != null) {
                            LogUtils.d("RESULTDATA", data?.data)
                            PathUtil.getPath(context!!, mCameraImageUri!!)?.let {
                                viewModel.uploadMessageMedia(it)
                            }
                        }
                    }
                    mVideoCapture -> {
                        PathUtil.getPath(context!!, data!!.data!!)?.let {
                            viewModel.uploadMessageMedia(it, true)
                        }
                    }
                    mRequestGallery -> {
                        PathUtil.getPath(context!!, data!!.data!!)?.let {
                            viewModel.uploadMessageMedia(it, PathUtil.isVideoFormat(it))
                            return
                        }

                        showMessage(R.string.error_upload_media)
                    }
                }
//                mAdapter?.deSelected()
//                mAdapter?.oldPosition = -1
            }
            Activity.RESULT_CANCELED -> {
                when (requestCode) {
                    mVideoCapture -> showError(resources.getString(R.string.videoCancel))
                }
            }
            else -> {
                when (requestCode) {
                    mVideoCapture -> showError(resources.getString(R.string.videoFailed))
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            mPermissionRequestCode -> {
                permissionHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
            mPermissionRequestCodeCalls -> {
                forwardRequestPermissionResult(requestCode, permissions, grantResults)
            }
        }

    }

    private var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null

    override fun configureLayout(view: View) {

        arguments?.let {
            it.getString(EXTRA_PARAM_1)?.let { chatRoomId ->
                viewModel.chatRoomId = chatRoomId
            }
        }
        adapter = ChatMessagesAdapter(
            viewModel.fetchMessages(
                fetchDirection = ChatApi.FetchDirection.AFTER,
                fromServer = false
            )!!,
            this
        ).apply {
            playingVideos = viewModel.playingVideos
        }

        viewModel.audioHelper = AudioRecordingHelper(view.context, viewModel)

        /*
         * Bottom sheet behavior
         */
//        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetChat).apply {
//            this.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
//                override fun onSlide(p0: View, p1: Float) {}
//
//                override fun onStateChanged(p0: View, p1: Int) {
//                    LogUtils.d(LOG_TAG, "New State: $p1")
//                }
//            })
//        }
//        btnGalleryPicture.setOnClickListener(galleryClickPicture)
//        btnGalleryVideo.setOnClickListener(galleryClickVideo)
//        btnDocument.setOnClickListener(documentsClick)
//        btnLocation.setOnClickListener(locationClick)
//        closeArrow.setOnClickListener { /*closeSheet()*/ }

        /* ACTION */
        btnPlus.setOnClickListener(actionClickPlus)
        btnActionCamera.setOnClickListener(actionClickCamera)
        btnSend.isEnabled = false

        containerParticipant.backArrow.setOnClickListener { activity!!.onBackPressed() }

//        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
//            }
//        })

        /*
         * Code block related to AUDIO recording
         */
//        btnActionMic.setOnTouchListener(TapAndHoldGestureListener(viewModel.audioHelper, this))
//        actionsBar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                val lp = recordOverlay.layoutParams
//                lp.height = actionsBar.height
//                recordOverlay.layoutParams = lp
//
//                val lp1 = disabledOverlay.layoutParams
//                lp1.height = actionsBar.height
//                disabledOverlay.layoutParams = lp1
//
//                actionsBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
//            }
//        })

        /*
         * Toolbar animation depending whether keyboard is open or closed
         */
//        actionsBar.viewTreeObserver.addOnGlobalLayoutListener {
//            val coords = intArrayOf(0, 0)
//            actionsBar.getLocationOnScreen(coords)
//
//            if (coords[1] < actionsBarY) {
//                stopAnimating = true
//                if (toolbarHeight > 0 && isScrollIdle)
//                    hideToolbar()
//            }
//            else if (coords[1] > actionsBarY) stopAnimating = false
//
//            actionsBarY = coords[1]
//        }

        /*
         * Toolbar animation depending whether RecView is scrolling
         */
//        if (scrollAnimationListener == null)
//            scrollAnimationListener = OnToolbarScrollListener(toolbar, isScrollIdle)
//        containerChat.addOnScrollListener(scrollAnimationListener as OnToolbarScrollListener)

        with(messageBox) {
            this.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 0) {
                        this@ChatMessagesFragment.btnSend.isEnabled = false

                        // INFO: 2019-12-22    SEND_BTN is now always visible
//                        this@ChatMessagesFragment.containerActionsButtons.visibility = View.VISIBLE

                        if (isTyping) {
                            isTyping = false
                            viewModel.setUserActivity()
                        }
                    } else {
                        // INFO: 2019-12-22    SEND_BTN is now always visible
//                        this@ChatMessagesFragment.containerActionsButtons.visibility = View.GONE

                        this@ChatMessagesFragment.btnSend.isEnabled = true

                        if (!isTyping) {
                            isTyping = true
                            viewModel.setUserActivity(context.getString(R.string.chat_activity_typing))
                        }
                    }
                }
            })
            this.setOnClickListener {
                val first = llm?.findFirstVisibleItemPosition()
                if (first == 0) {
                    restoreScrollPosition(delay = 200)
                }
            }
        }
        btnSend.setOnClickListener {
            if (!viewModel.participantId.isNullOrBlank()) {
                (activity as ChatActivity).playSendTone()
                if (TTUApp.canVibrate) vibrateForChat(context)
                viewModel.createOutgoingMessage()?.apply {
                    viewModel.updateChatRoom(this)
                }
            }
        }

        toBottomBtn.setOnClickListener { restoreScrollPosition() }
        toBottomBtn.hide()

//        containerChat.addOnScrollListener(viewModel.scrollListener)
        containerChat.addOnScrollListener(adapter.videoVisibilityScrollListener)
        containerChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val firstVisibleItem = llm?.findFirstVisibleItemPosition()

                when {
                    dy < 0 && firstVisibleItem != null && firstVisibleItem >= 1 -> {
                        if (this@ChatMessagesFragment.toBottomBtn.isOrWillBeHidden)
                            this@ChatMessagesFragment.toBottomBtn.show()
                    }
                    dy > 0 && firstVisibleItem == 0 -> {
                        if (this@ChatMessagesFragment.toBottomBtn.isOrWillBeShown)
                            this@ChatMessagesFragment.toBottomBtn.hide()
                    }
                }
            }
        })
//        containerChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
//            if (bottom < oldBottom) restoreScrollPosition()
//        }

        // TODO: 2019-12-22    REMOVE: toro library
//        if (containerChat is Container) {
//            containerChat.cacheManager = adapter
//            containerChat.setPlayerDispatcher { 250 }
//            containerChat.playerSelector = if (isAutoPlayOn()) PlayerSelector.DEFAULT else {
//                object: PlayerSelector {
//                    val manualPlay = Container.Filter {
//                        it.wantsToPlay() && it.playerOrder == adapter.manualItem.get()
//                    }
//
//                    override fun reverse(): PlayerSelector {
//                        return this
//                    }
//
//                    override fun select(container: Container, items: MutableList<ToroPlayer>): MutableCollection<ToroPlayer> {
//                        return container.filterBy(manualPlay)
//                    }
//                }
//            }
//        }


//        disabledOverlay.setOnClickListener {
//            //do nothing because if visible it is a click catcher
//        }

//        setMessages()

        profilePicture.setOnClickListener {
            viewModel.participantId?.let { id ->
                if (id.isNotBlank())
                    AccountActivity.openProfileFragment(it.context, id)

            }
        }

        llm =
            null                                                                      // resets LinearLayoutManager
        llm = LinearLayoutManager(context, RecyclerView.VERTICAL, true)    // new instance
        llm!!.stackFromEnd = true                                                       //
        if (containerChat.layoutManager == null) containerChat.layoutManager =
            llm      // assigns new instance

        restoreScrollPosition()
    }


    override fun bindLayout() {
//        this.putString(EXTRA_PARAM_2, participantName)
//        this.putString(EXTRA_PARAM_3, participantAvatar)
//        this.putBoolean(EXTRA_PARAM_4, canFetchMessages)
//        this.putBoolean(EXTRA_PARAM_5, finishActivity)


        val chatRoom = viewModel.getValidRoomMainThread()

        // TODO: 2019-12-22    RESTORE?
//        disabledOverlay.visibility = if (chatRoom?.getParticipant()?.canChat == true) { View.GONE } else View.VISIBLE

        bottomActions.requestFocus()

        restoreScrollPosition(false)

        handleToReadValue()

        containerChat.adapter = adapter

        (profilePicture.picture as ImageView).setProfilePicture(viewModel.participantAvatar)
        //MediaHelper.loadProfilePictureWithPlaceholder(context, participantAvatar, profilePicture as ImageView)
        userName.text = viewModel.participantName
        setStatusIndicatorAndString(
            chatRoom?.recipientStatus ?: ChatRecipientStatus.OFFLINE.value,
            dateObj = chatRoom?.getLastSeenDate(viewModel.participantId)
        )

        if (::menu.isInitialized) {
            menu.findItem(R.id.actionCall).isEnabled = chatRoom?.canVoiceCallParticipant() == true
            menu.findItem(R.id.actionVideoCall).isEnabled =
                chatRoom?.canVideoCallParticipant() == true
        }

    }


    //region == Class custom methods ==

    private fun restoreScrollPosition(forceToBottom: Boolean = true, delay: Long = 0) {
        val runnable = Runnable {
            if (adapter.itemCount > 0) {
                this@ChatMessagesFragment.containerChat?.scrollToPosition(
                    when {
                        forceToBottom -> 0
                        lastScrollPosition != null -> lastScrollPosition!!
                        else -> 0
                    }
                )
            }

            this@ChatMessagesFragment.toBottomBtn?.hide()
        }

        if (delay > 0) containerChat?.postDelayed(runnable, delay)
        else containerChat?.post(runnable)
    }

    fun addNewMessage(newMessage: ChatMessage) {
        restoreScrollPosition()
        restoreScrollPosition(delay = 100)
        messageBox.setText("")

        adapter.notifyItemChanged(1)

        viewModel.sendMessage(newMessage)
    }


    fun isAutoPlayOn(): Boolean {
        return true
        //TODO: 03/12 uncomment
        //return mUser.feedAutoplayAllowed(activity)
    }


    private fun setMessages() {
        if (adapter.itemCount == 0 && canFetchMessages) {
            viewModel.chatRoomId?.let {
                viewModel.fetchMessages(fetchDirection = ChatApi.FetchDirection.AFTER)
            }
        }
    }

    private fun setStatusIndicatorAndString(
        status: Int,
        date: String? = null,
        dateObj: Date? = null
    ) {
        with(status) {
            // INFO: 2019-12-22    as per mockup no info showed about availability and status string
            availabilityIndicator?.visibility = View.GONE
            participantLastSeen?.visibility = View.GONE

//            availabilityIndicator?.visibility = if (this == ChatRecipientStatus.ONLINE.value) View.VISIBLE else View.GONE
//            participantLastSeen?.text =
//                if (this == ChatRecipientStatus.ONLINE.value)  {
//                    participantLastSeen.isSelected = false
//                    context?.getString(R.string.chat_status_online)
//                }
//                else {
//                    participantLastSeen.isSelected = true
//                    val dateToFormat =
//                        if (!date.isNullOrBlank()) getDateFromDB(date)
//                        else dateObj ?: Date()
//
//                    if (dateToFormat != null)
//                        context?.getString(
//                            R.string.chat_status_last_seen,
//                            formatDateWithTime(context, dateToFormat, "EEE, dd MMM", true)
//                        )
//                    else
//                        context?.getString(R.string.chat_status_offline)
//                }
        }
    }

    private fun handleToReadValue() {

    }

    private fun callParticipant(callType: VoiceVideoCallType) {
        val chatRoom = viewModel.getValidRoomMainThread()
        if (activity is BaseActivity && chatRoom?.getParticipant() != null && getUser() != null) {
            initPermissionsForCall(
                this,
                mPermissionRequestCodeCalls,
                logTag,
                getUser()!!,
                chatRoom.getParticipant()!!.convertToMainUser(),
                callType == VoiceVideoCallType.VIDEO
            )
        }
    }

    //endregion


    //region == Real time callbacks ==

    override fun onNewMessage(newMessage: ChatMessage) {
        if (llm?.findFirstVisibleItemPosition() == 0)
            restoreScrollPosition()
        viewModel.setMessageRead(true)
        containerChat?.post {
            adapter.notifyItemChanged(1)
        }
    }

    override fun onStatusUpdated(userId: String, status: Int, date: String) {
        if (userId == viewModel.participantId) setStatusIndicatorAndString(status, date)
    }

    var previousActivity: String? = null
    override fun onActivityUpdated(userId: String, chatId: String, activity: String) {

        // INFO: 2019-12-22    as per mockup no info showed about availability and status string
        participantLastSeen?.visibility = View.GONE


//        if (chatId == viewModel.chatRoomId) {
//            if (!activity.isBlank()) {
//                previousActivity = participantLastSeen?.text.toString()
//                participantLastSeen?.text = activity
//            }
//            else if (!previousActivity.isNullOrBlank())
//                participantLastSeen?.text = previousActivity!!
//        }
    }

    override fun onMessageDelivered(chatId: String, userId: String, date: String) {}

    override fun onMessageRead(chatId: String, userId: String, date: String) {}

    override fun onMessageOpened(chatId: String, userId: String, date: String, messageID: String) {}

    //endregion


    // TODO: 2019-12-22    REMOVE because no toolbar motion is required
    inner class OnToolbarScrollListener(
        private val toolbar: View,
        private var isScrollIdle: Boolean
    ) : RecyclerView.OnScrollListener() {

        private var animating = false

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            when {
                dy < 0 -> animateToolbar(false)
                dy > 0 -> if (!stopAnimating) animateToolbar(true)
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            isScrollIdle = newState == RecyclerView.SCROLL_STATE_IDLE
        }

        private fun animateToolbar(show: Boolean) {

            val anim = toolbar.baseAnimateHeight(
                200,
                show,
                toolbarHeight,
                customListener = object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}
                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                        animating = false
                    }

                    override fun onAnimationStart(animation: Animator?) {
                        animating = true
                    }
                },
                start = false
            )

            if (!animating && ((show && toolbar.height == 0) || (!show && toolbar.height > 0)))
                anim?.start()
        }
    }

//    private fun hideToolbar() {
//        if (toolbar.height > 0) {
//            toolbar.baseAnimateHeight(
//                200,
//                false,
//                toolbarHeight
//            )
//        }
//    }

    private val galleryClickPicture = View.OnClickListener {
        // would need refactoring for using PostTypeEnum
//        MediaHelper.checkPermissionForGallery(activity, HLMediaType.PHOTO, fragment)
//        uploadHelper.mediaType = HLMediaType.PHOTO
//        closeSheet()
    }
    private val galleryClickVideo = View.OnClickListener {
        // would need refactoring for using PostTypeEnum
//        MediaHelper.checkPermissionForGallery(activity, HLMediaType.VIDEO, fragment)
//        uploadHelper.mediaType = HLMediaType.VIDEO
//        closeSheet()
    }
    private val documentsClick = View.OnClickListener {
        // would need refactoring for using PostTypeEnum
//        MediaHelper.checkPermissionForDocuments(activity, fragment, false)
//        uploadHelper.mediaType = HLMediaType.DOCUMENT
//        closeSheet()
    }

    // INFO: 2020-01-09    Maps is commented
    private val locationClick = View.OnClickListener {
        if (!checkPlayServices(context!!)) return@OnClickListener

//        if (!googleApiClient.isConnected)
//            googleApiClient.connect()
//        else if (googleApiClient.isConnected) {
//            initPermissionLocation(
//                {
//                    viewModel.setUserActivity(getString(R.string.chat_activity_location))
//                    //closeSheet()
//                }
//            )
//        }
    }

    private val actionClickPlus by lazy {
        object : RemoveFocusClickListener(messageBox) {
        }
    }

    private val actionClickCamera by lazy {
        object : RemoveFocusClickListener(messageBox) {
            override fun onClick(v: View?) {
                super.onClick(v)
                ImageOrVideoBottomSheetDialog().show(
                    childFragmentManager,
                    ImageOrVideoBottomSheetDialog::javaClass.name
                )
            }
        }
    }

    private var mCameraImageUri: Uri? = null
    private val mRequestCamera = 0
    private val mVideoCapture = 1
    private val mRequestGallery = 2

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onImageBottomSheet(event: ImageBottomEvent) {
        when {
            event.mImageClick -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) initPermissionCamera() else cameraIntent()
            event.mGalleryClick -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) initPermissionGallery() else galleryIntent()
            event.mVideoClick -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) initPermissionVideo() else videoIntent()
        }
    }

    private fun galleryIntent() {
        val intent = Intent()
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))

        intent.action = Intent.ACTION_GET_CONTENT //
        startActivityForResult(Intent.createChooser(intent, "Select File"), mRequestGallery)
    }

    //video capture open intent
    private fun videoIntent() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, mVideoCapture)
    }

    //open the Camera
    private fun cameraIntent() {
        val values = ContentValues()
        values.put(
            CreatePostFragment.CAMERA_INTENT_TITLE,
            System.currentTimeMillis().toString() + resources.getString(R.string.app_name) + CreatePostFragment.CAMERA_INTENT_TYPE
        )
        mCameraImageUri =
            activity!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intentCamera = Intent("android.media.action.IMAGE_CAPTURE")
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, mCameraImageUri)
        startActivityForResult(intentCamera, mRequestCamera)
    }

    //get the permission for Video
    private fun initPermissionVideo() {
        val permissions: Array<String> = if (isWriteStoragePermissionGranted()) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        permissionHelper = PermissionHelper(this, permissions, mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                videoIntent()
            }

            override fun onPermissionDenied() {

            }

            override fun onPermissionDeniedBySystem() {

            }
        })
    }

    //get the permission for Video
    private fun initPermissionGallery() {

        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                galleryIntent()
            }

            override fun onPermissionDenied() {

            }

            override fun onPermissionDeniedBySystem() {

            }
        })
    }

    //get the permission for Camera
    private fun initPermissionCamera() {
        val permissions: Array<String> = if (isWriteStoragePermissionGranted()) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionHelper = PermissionHelper(this, permissions, mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                cameraIntent()
            }

            override fun onPermissionDenied() {

            }

            override fun onPermissionDeniedBySystem() {

            }
        })
    }

    // INFO: 2020-01-09    Maps is commented
    private fun initPermissionLocation(ifOps: () -> Unit = {}, elseOps: () -> Unit = {}) {

        permissionHelper = PermissionHelper(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            mPermissionRequestCode
        )
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
//                fusedLocationClient?.lastLocation?.addOnCompleteListener(this@ChatMessagesFragment)
                ifOps()
            }

            override fun onPermissionDenied() {
            }

            override fun onPermissionDeniedBySystem() {
            }
        })
    }

    private fun isWriteStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isReadStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onConnected(p0: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onComplete(p0: Task<Location>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}