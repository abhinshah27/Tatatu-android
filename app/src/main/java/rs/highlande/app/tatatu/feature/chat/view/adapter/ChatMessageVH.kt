/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.view.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.AsyncTask
import android.text.Spannable
import android.text.SpannableString
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.webkit.MimeTypeMap.getFileExtensionFromUrl
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityOptionsCompat
import androidx.core.text.PrecomputedTextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.cache.PicturesCache
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.ui.RoundedCornersBackgroundSpan
import rs.highlande.app.tatatu.core.ui.recyclerView.PLAYABLE_THRESHOLD
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoObject
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoViewHolderManager
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import rs.highlande.app.tatatu.feature.chat.PlayerViewNoController
import rs.highlande.app.tatatu.feature.chat.WebLinkRecognizer
import rs.highlande.app.tatatu.feature.chat.getAudioTask
import rs.highlande.app.tatatu.feature.chat.onHold.VideoViewActivity
import rs.highlande.app.tatatu.feature.chat.view.ChatMessagesFragment
import rs.highlande.app.tatatu.feature.chat.view.MessageOutgoingStateLayout
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewFragment
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializer
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializerDelegate
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.ChatMessageType
import rs.highlande.app.tatatu.model.chat.HLWebLink
import java.util.*

/**
 * Base class for chat items' view holders.
 * @author mbaldrighi on 10/22/2018.
 */
open class ChatMessageVH(
    itemView: View,
    protected val fragment: ChatMessagesFragment
) : RecyclerView.ViewHolder(itemView), View.OnClickListener, KoinComponent {

    private var dateHeader: TextView? = null

    protected var messageStateIcon: View? = null
    protected var messageStateIconInner: View? = null
    protected var messageStateView: MessageOutgoingStateLayout? = null
//    protected var messageStateViewInner: MessageOutgoingStateLayout? = null
    protected var incomingProfileContainer: View? = null
    protected var incomingProfile: ImageView? = null
    private var incomingEarningText: View? = null
    private var incomingEarningToken: View? = null
    private var incomingEarningValue: TextView? = null
    protected var infoFooterNoMedia: TextView? = null
    protected var chatBackgroundHolder: View? = null

    var messageNoMedia: TextView? = null

    private var prevMessage: ChatMessage? = null
    var nextMessage: ChatMessage? = null
    var currentMessage: ChatMessage? = null
    var isIncoming: Boolean = false

    val context: Context by lazy { itemView.context }

    var callingSendMessageInError = false

    var currentPosition: Int = 0

    init {
        with(itemView) {
            this.setOnClickListener(this@ChatMessageVH)
            dateHeader = this.findViewById(R.id.dateHeader)
            messageStateView = this.findViewById(R.id.messageStateContainer)
            messageStateIcon = this.findViewById(R.id.iconState)
            messageNoMedia = this.findViewById(R.id.message)
            infoFooterNoMedia = this.findViewById(R.id.infoFooter)
            incomingEarningToken = this.findViewById(R.id.earningToken)
            incomingEarningText = this.findViewById(R.id.earningText)
            incomingEarningValue = this.findViewById(R.id.earningValue)
        }
    }

    open fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        val userId = fragment.viewModel.user?.uid
        if (userId.isNullOrEmpty()) return

        currentMessage = messages[position]
        currentPosition = position

        isIncoming = currentMessage?.getDirectionType(userId) == ChatMessage.DirectionType.INCOMING

        if (callingSendMessageInError && (currentMessage?.isError == false))
            callingSendMessageInError = false

        prevMessage = if (messages.size > position + 1) messages[position + 1] else null
        nextMessage = if (position > 0) messages[position - 1] else null


        val marginTop =
            if (position == messages.size - 1 || !hasPreviousMessageSameAuthor())
                context.resources.getDimensionPixelSize(R.dimen.activity_margin)
            else if (hasPreviousMessageSameAuthor())
                context.resources.getDimensionPixelSize(R.dimen.activity_margin_md)
            else 0

        val marginBottom =
            if (position == 0) context.resources.getDimensionPixelSize(R.dimen.activity_margin)
            else 0

        (itemView.layoutParams as? RecyclerView.LayoutParams)?.setMargins(0, marginTop, 0, marginBottom)



        // INFO: 2019-12-24    apparently dateHeader becomes always hidden
//        dateHeader?.text = formatDateWithTime(fragment.context, currentMessage?.creationDateObj, "EEE, dd MMM", true)
        dateHeader?.visibility = View.GONE/*if (currentMessage?.isSameDateAs(prevMessage) == true) {
            when {
                this@ChatMessageVH is ChatMessageVHText -> View.GONE
                this@ChatMessageVH is ChatMessageVHDocument -> View.GONE
//                this@ChatMessageVH is ChatMessageVHMedia -> View.INVISIBLE
                else -> View.VISIBLE
            }
        } else View.VISIBLE*/

        // if message is "incoming" && incomingProfile is evaluated, put participant profile picture
        if (isIncoming)
            incomingProfile?.setProfilePicture(fragment.viewModel.participantAvatar)

        handleMessageState()

    }

    protected fun getActivity(): Activity? {
        return context as? Activity
    }

    protected fun getInfoText(messageItem: ChatMessage?): String {
        return if (!isIncoming) {
            when (messageItem?.getStatusEnum()) {
                ChatMessage.Status.ERROR -> context.getString(R.string.chat_message_status_error)
                ChatMessage.Status.SENDING -> context.getString(R.string.chat_message_status_sending)

                // INFO: 2019-12-24    currently displays only CREATION_DATE
//                ChatMessage.Status.SENT -> context.getString(R.string.chat_message_status_sent)
//                ChatMessage.Status.DELIVERED -> context.getString(R.string.chat_message_status_delivered)
//                ChatMessage.Status.READ -> {
//                    context.getString(
//                            R.string.chat_message_status_read,
//                            ", ${getFormattedDateForMessages(messageItem.readDateObj)}"
//                    )
//                }

                // INFO: 2019-12-24    no DOCS for now
//                ChatMessage.Status.OPENED -> {
//                    context.getString(
//                            R.string.chat_message_status_opened,
//                            ", ${getFormattedDateForMessages(messageItem.openedDateObj)}"
//                    )
//                }
                else -> getFormattedDateForMessages(messageItem?.creationDateObj)
            }
        } else getFormattedDateForMessages(messageItem?.creationDateObj)
    }

    // for now we're allowed to use always the "arrowed" background
    protected fun handleBalloonBackground() {
        val userId = fragment.viewModel.user?.uid
        if (userId.isNullOrEmpty()) return
        with(chatBackgroundHolder) {
            when (currentMessage?.getDirectionType(userId)) {
                ChatMessage.DirectionType.INCOMING -> {
                    if (nextMessage?.getDirectionType(userId) == ChatMessage.DirectionType.INCOMING &&
                        nextMessage?.hasMedia() == false) {
                        this?.setBackgroundResource(R.drawable.background_chat_incoming)
                        handleIncomingFooter(false)
                    } else {
                        this?.setBackgroundResource(R.drawable.background_chat_incoming_last)
                        handleIncomingFooter(true)
                    }
                }

                ChatMessage.DirectionType.OUTGOING -> {
                    if (nextMessage?.getDirectionType(userId) == ChatMessage.DirectionType.OUTGOING &&
                        nextMessage?.hasMedia() == false) {
                        this?.setBackgroundResource(R.drawable.background_chat_outgoing)
                        infoFooterNoMedia?.visibility = View.GONE
                    } else {
                        this?.setBackgroundResource(R.drawable.background_chat_outgoing_last)
                        infoFooterNoMedia?.visibility = View.VISIBLE
                    }
                }
                else -> {
                    LogUtils.d(logTag, "Error in choosing right background")
                }
            }
        }
    }

    private fun handleIncomingFooter(show: Boolean) {
        incomingProfileContainer?.visibility = if (show) View.VISIBLE else View.INVISIBLE
        handleEarnings(show)
        infoFooterNoMedia?.visibility = if (show) View.VISIBLE else View.GONE
    }


    override fun onClick(v: View?) {
        fragment.lastScrollPosition = currentPosition

        if (currentMessage?.isError == true) {

            fragment.viewModel.sendMessage(currentMessage!!)
            callingSendMessageInError = true
        }
    }


    protected fun handleMessageState() {
        if (!isIncoming) {

            LogUtils.d(logTag, "TESTSTATE: Handling state for message:${currentMessage?.messageID} with state: ${currentMessage?.getStatusEnum()}")

            when (currentMessage?.getStatusEnum()) {
                ChatMessage.Status.SENT -> {
                    messageStateView?.setStateIcon(ChatMessage.Status.SENT)
                }
                ChatMessage.Status.DELIVERED -> {
                    messageStateView?.setStateIcon(ChatMessage.Status.DELIVERED)
                }
                ChatMessage.Status.READ -> {
                    messageStateView?.setStateIcon(ChatMessage.Status.READ)
                }

                // INFO: 2019-12-24    no DOCS for now
//                ChatMessage.Status.OPENED -> {
//                    context.getString(
//                            R.string.chat_message_status_opened,
//                            ", ${getFormattedDateForMessages(messageItem.openedDateObj)}"
//                    )
//                }
                else -> {
                    messageStateView?.setStateIcon(null)
//                    messageStateViewInner?.setStateIcon(null)
                }
            }
        } else {
            messageStateIcon?.visibility = View.INVISIBLE
            messageStateIconInner?.visibility = View.INVISIBLE
            messageStateView?.setState(null)
//            messageStateViewInner?.setStateIcon(null)
        }
    }

    protected fun handleEarnings(show: Boolean, negativeValue: Int = View.GONE) {
        incomingEarningText?.visibility = if (show && HANDLE_EARNINGS) View.VISIBLE else negativeValue
        incomingEarningToken?.visibility = if (show && HANDLE_EARNINGS) View.VISIBLE else negativeValue
        incomingEarningValue?.let {
            it.visibility = if (show && HANDLE_EARNINGS) View.VISIBLE else negativeValue
            (it as? TextView)?.text = calculateEarning()
        }

    }

    private fun calculateEarning(): String {

        // TODO: 2019-12-30    CALCULATE EARNINGS

        var multiplier = 1      // it will be deduced from the amount of consecutive msgs

        return formatTTUTokens(0.01 * multiplier)
    }


    private fun getFormattedDateForMessages(date: Date?): String {
        return if (date?.isSameDateAsNow() == true)
            formatTime(fragment.context, date)
        else formatDateWithTime(
            fragment.context,
            date,
            if (date?.isSameYear() == true) "MMMMdd" else "MMMMddyyyy",
            true)
    }

    private fun hasPreviousMessageSameAuthor() = currentMessage?.senderID == prevMessage?.senderID

    private fun hasNextMessageSameAuthor() = currentMessage?.senderID == nextMessage?.senderID


    companion object {
        val logTag = ChatMessageVH::class.java.simpleName

        // INFO: 2019-12-30    Earnings for chat are to be implemented in future releases
        const val HANDLE_EARNINGS = false
    }

}

/**
 * Specific VH for text messages, handling an own "single" footer with message status.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHText(
    itemView: View,
    fragment: ChatMessagesFragment
): ChatMessageVH(itemView, fragment) {

    private val webLinkRecognizer: WebLinkRecognizer by inject()

    init {
        chatBackgroundHolder = messageNoMedia
        incomingProfileContainer = itemView.findViewById(R.id.profilePicture)
        incomingProfile = incomingProfileContainer?.findViewById(R.id.picture)
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        handleBalloonBackground()
        infoFooterNoMedia?.text = getInfoText(currentMessage)

        messageNoMedia?.text = currentMessage?.text

        // start web link recognition
        if (isContextValid(fragment.context)) {
            webLinkRecognizer.recognize(currentMessage?.text, currentMessage?.messageID)
        }
    }

}

// INFO: 2019-12-24    DOCUMENTS are in stand-by for now
/**
 * Specific VH for [ChatMessageType.DOCUMENT] messages, handling an own "single" footer with message
 * status, and the icon for the document extension.
 * @author mbaldrighi on 12/14/2018.
 */
class ChatMessageVHDocument(itemView: View, fragment: ChatMessagesFragment): ChatMessageVH(itemView, fragment) {
    private var icon: ImageView? = null

    init {
        with(itemView) {
            chatBackgroundHolder =
                if (isIncoming) findViewById(R.id.messageContainer)
                else findViewById(R.id.background)

            icon = this.findViewById(R.id.documentIcon)

            incomingProfileContainer = findViewById(R.id.profilePicture)
            incomingProfile = incomingProfileContainer?.findViewById(R.id.picture)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        handleBalloonBackground()
        infoFooterNoMedia?.text = getInfoText(currentMessage)

        with (currentMessage?.sharedDocumentFileName) {
            messageNoMedia?.text = this; handleDocumentIcon(this)
        }
    }


    private fun handleDocumentIcon(fileName: String?) {
        val ext = getFileExtensionFromUrl(fileName)
        if (!ext.isNullOrBlank()) {
            val icon = when(ext) {
                "docx", "doc" -> if (isIncoming) R.drawable.ic_doc_black else R.drawable.ic_doc_white
                "xlsx", "xls" -> if (isIncoming) R.drawable.ic_xls_black else R.drawable.ic_xls_white
                "pptx", "ppt" -> if (isIncoming) R.drawable.ic_ppt_black else R.drawable.ic_ppt_white
                "pdf" -> if (isIncoming) R.drawable.ic_pdf_black else R.drawable.ic_pdf_white
//                "rtf" -> 0
//                "txt" -> 0
                else -> 0
            }

            if (icon != 0)
                this@ChatMessageVHDocument.icon?.setImageResource(icon)
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (!currentMessage?.sharedDocumentFileName.isNullOrBlank()) {
            fireOpenBrowserIntent(fragment.activity!!, currentMessage!!.mediaURL)

//            fireOpenBrowserIntent(
//                    context,
//                    currentMessage!!.mediaURL,
//                    currentMessage!!.sharedDocumentFileName,
//                    if (isIncoming && currentMessage?.isOpened() == false) currentMessage?.messageID else null
//            )
        } else fragment.showError(fragment.getString(R.string.error_generic))
    }

}

/**
 * Class handling messages whose [ChatMessage.isMissedVideoCall] method returns TRUE. We could not use
 * [ChatMessageVH] class because this type of view holders has to manage "both" profile pictures and
 * footers.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMissedCall(
    itemView: View,
    fragment: ChatMessagesFragment
): ChatMessageVH(itemView, fragment), DefaultCallInitializer by DefaultCallInitializerDelegate() {
    private var callIcon: ImageView? = null

    init {
        chatBackgroundHolder = if (isIncoming) messageNoMedia else messageStateView
        with(itemView) {
            findViewById<View?>(R.id.callBack)?.setOnClickListener {

                with(fragment.viewModel) {
                    val room = getValidRoomMainThread()
                    if (room?.getParticipant() != null && user != null) {
                        initPermissionsForCall(
                            fragment,
                            fragment.mPermissionRequestCodeCalls,
                            ChatMessagesFragment.logTag,
                            user!!,
                            room.getParticipant()!!.convertToMainUser(),
                            currentMessage?.isMissedVideoCall() == true
                        )
                    }
                }
            }
            callIcon = findViewById(R.id.callIcon)
        }

        incomingProfileContainer = itemView.findViewById(R.id.profilePicture)
        incomingProfile = incomingProfileContainer?.findViewById(R.id.picture)
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        handleBalloonBackground()
        infoFooterNoMedia?.text = getInfoText(currentMessage)

        if (currentMessage?.getMessageType() == ChatMessageType.VIDEO) {
            callIcon?.setImageResource(R.drawable.ic_chat_missed_video)
            messageNoMedia?.text =
                if (isIncoming) fragment.getString(R.string.chat_text_incoming_missed_video)
                else fragment.getString(R.string.chat_text_outgoing_missed_video)
        } else {
            callIcon?.setImageResource(R.drawable.ic_chat_missed_voice)
            messageNoMedia?.text =
                if (isIncoming) fragment.getString(R.string.chat_text_incoming_missed_voice)
                else fragment.getString(R.string.chat_text_outgoing_missed_voice)
        }
    }

}

/**
 * Class handling messages whose [ChatMessage.isMissedVideoCall] method returns TRUE. We could not use
 * [ChatMessageVH] class because this type of view holders has to manage "both" profile pictures and
 * footers.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHSystem(
    itemView: View,
    fragment: ChatMessagesFragment
) : ChatMessageVH(itemView, fragment) {

    private var systemText: TextView? = null

    init {
        with(itemView) {
            systemText = findViewById(R.id.systemText)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        systemText?.text = currentMessage?.text
    }

}


/**
 * Class handling messages whose [ChatMessage.hasMedia] method returns TRUE. We could not use
 * [ChatMessageVH] class because this type of view holders has to manage "both" profile pictures and
 * footers.
 * @author mbaldrighi on 10/22/2018.
 */
open class ChatMessageVHMedia(
    itemView: View,
    fragment: ChatMessagesFragment,
    protected val videoManager: VideoViewHolderManager? = null
): ChatMessageVH(itemView, fragment), View.OnClickListener {

    protected val picturesCache: PicturesCache by inject()
    protected val audioVideoCache: AudioVideoCache by inject()

    private var groupOutgoing: Group? = null
    private var infoIncoming: TextView? = null
    protected var infoOutgoing: TextView? = null
    private var profileOutgoing: ImageView? = null

    init {
        with(itemView) {
            //            this.setOnClickListener(this@ChatMessageVHMedia)
            groupOutgoing = this.findViewById(R.id.groupOutgoing)
            infoIncoming = this.findViewById(R.id.infoFooterIncoming)
            infoOutgoing = this.findViewById(R.id.infoFooterOutgoing)
            profileOutgoing = this.findViewById<View>(R.id.profileOutgoing)?.findViewById(R.id.picture)

            incomingProfileContainer = this.findViewById(R.id.profileIncoming)
            incomingProfile = incomingProfileContainer?.findViewById(R.id.picture)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        fragment.viewModel.user?.let {
            if (isIncoming) {
                handleGroupIncoming(true)
                groupOutgoing?.visibility = View.INVISIBLE
            } else {
                groupOutgoing?.visibility = View.VISIBLE
                handleGroupIncoming(false)
            }

            val triple: Triple<String?, ImageView?, TextView?> =
                if (isIncoming)
                    Triple(fragment.viewModel.participantAvatar, incomingProfile, infoIncoming)
                else
                    Triple(it.picture, profileOutgoing, infoOutgoing)

            triple.second?.setProfilePicture(triple.first)
            triple.third?.text = getInfoText(currentMessage)
        }


    }

    private fun handleGroupIncoming(visible: Boolean) {
        incomingProfileContainer?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        infoIncoming?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        handleEarnings(visible, View.INVISIBLE)
    }

    protected open fun setLastPlayerPosition() {}
    protected open fun preparePlayerAndStart(resetMedia: Boolean = false) {}

    override fun onClick(v: View?) {
        super.onClick(v)
    }
}

/**
 * Class handling [ChatMessageType.PICTURE] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaPhoto(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment) {
    private var imageView: ImageView? = null

    init {
        with(itemView) {
            imageView = this.findViewById(R.id.imageView)
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        val media = picturesCache.getMedia(currentMessage?.mediaURL, HLMediaType.PHOTO)
        object : CustomViewTarget<ImageView, Drawable>(imageView!!) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                getView().setPicture(currentMessage?.mediaURL)
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                getView().setImageDrawable(resource)
            }

            override fun onResourceCleared(placeholder: Drawable?) {}
        }.setPicture(media ?: currentMessage?.mediaURL)
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (callingSendMessageInError) return

        // TODO: 2020-01-02    open full screen activity??
//        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
//            context as Activity,
//            imageView!!,
//            ViewCompat.getTransitionName(imageView!!)!!
//        )
//
//        context.startActivity(
//            Intent(context, PhotoViewActivity::class.java).let {
//                it.putExtra(EXTRA_PARAM_1, currentMessage?.mediaURL)
//                it.putExtra(EXTRA_PARAM_2, ViewCompat.getTransitionName(imageView!!))
//                it.putExtra(EXTRA_PARAM_3, currentMessage?.messageID)
//                it.putExtra(EXTRA_PARAM_4, true)
//            },
//            options.toBundle()
//        )
    }
}

// INFO: 2019-12-24    AUDIO are in stand-by for now
/**
 * Class handling [ChatMessageType.AUDIO] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaAudio(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment) {
    private var audioView: View? = null
    private var playBtn: View? = null
    private var pauseBtn: View? = null
    private var progressView: View? = null
    private var pauseLayout: View? = null
    private var pauseFrame = 0

    // INFO: 2020-01-09    Lottie is commented
//    private var siriComposition: LottieComposition? = null
//    var lottieView: LottieAnimationView? = null

    var playTask: AsyncTask<*, *, *>? = null
    var mediaPlayer: MediaPlayer? = null
    var playing = false
        get() { return if (isMediaPlayerInError) false else (mediaPlayer?.isPlaying ?: false) }
    var lastPlayerPosition: Int = 0

    var stoppedForScrolling = false

    var isMediaPlayerInError = false


    init {
        with(itemView) {

            audioView = this.findViewById(R.id.audioView)

//            lottieView = this.findViewById(R.id.wave3)
            playBtn = this.findViewById(R.id.play_btn)
            playBtn!!.setOnClickListener {
                if (isStringValid(currentMessage?.mediaURL)) preparePlayerAndStart()
            }
            pauseBtn = this.findViewById(R.id.pause_btn)
            progressView = this.findViewById(R.id.progress_layout)
            pauseLayout = this.findViewById(R.id.pause_layout)
            pauseLayout?.setOnClickListener {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
                lastPlayerPosition = mediaPlayer?.currentPosition ?: 0
                pauseBtn?.visibility = View.GONE
                playing = false
                playBtn?.visibility = View.VISIBLE
//                lottieView?.pauseAnimation()
//                pauseFrame = lottieView?.frame ?: 0
            }

            (this.findViewById(R.id.shareBtn) as View).setOnClickListener {
                fragment.lastScrollPosition = currentPosition

                if (!currentMessage?.mediaURL.isNullOrBlank()) {
                    fragment.viewModel.currentMessageId = currentMessage?.messageID
                    //fragment.shareHelper.initOps(true)
                }
            }
        }

//        siriComposition = TTUApp.siriComposition
//        if (siriComposition != null)
//            lottieView?.setComposition(siriComposition!!)
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        audioView?.setBackgroundColor(Color.BLACK)

        // TODO: 5/6/2018    DISABLES AUTOPLAY AUDIO
//		if (getActivity().getUser().feedAutoplayAllowed(getActivity()) &&
//				super.fragment != null && super.fragment.isVisibleToUser()) {
//
//			if (mediaPlayer == null)
//				mediaPlayer = new MediaPlayer();
//
//			if (!mediaPlayer.isPlaying()) {
//				playBtn.setVisibility(View.GONE);
//				pauseBtn.setVisibility(View.GONE);
//				progressView.setVisibility(View.GONE);
//
//				playTask = new PlayAudioTask(mediaPlayer, playBtn, pauseBtn, progressView, playing,
//						lastPlayerPosition, animationView, pauseFrame)
//						.execute(mItem.getContent());
//			}
//		}
//		else {
        playBtn?.visibility = View.VISIBLE
        pauseBtn?.visibility = View.GONE
        progressView?.visibility = View.GONE
//		}


        //TO MAKE SURE WAVE IS SET TO 0 WHEN VIEW IS ATTACHED.
//        lottieView?.frame = pauseFrame
    }

    fun resetViews() {
//        lottieView?.frame = pauseFrame

        playBtn?.visibility = View.VISIBLE
        pauseBtn?.visibility = View.GONE
    }

    override fun preparePlayerAndStart(resetMedia: Boolean) {

        var media = audioVideoCache.getMedia(currentMessage?.mediaURL,
            HLMediaType.AUDIO
        )
        if (media == null)
            media = currentMessage?.mediaURL

        if (mediaPlayer == null) mediaPlayer = MediaPlayer()
        isMediaPlayerInError = false
        (mediaPlayer as MediaPlayer).setOnErrorListener { mp, _, _ ->
            isMediaPlayerInError = true
            mp.reset()
            true
        }
        playTask = getAudioTask(if (mediaPlayer != null) mediaPlayer!! else MediaPlayer(), playBtn!!, pauseBtn!!,
            progressView!!, playing, lastPlayerPosition, /*lottieView!!,*/ pauseFrame)
            .execute(media)
    }

    override fun setLastPlayerPosition() {
        lastPlayerPosition = mediaPlayer?.currentPosition ?: 0
//        pauseFrame = lottieView?.frame ?: 0
    }
}


/**
 * Class handling [ChatMessageType.VIDEO] message type.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaVideo(
    itemView: View,
    fragment: ChatMessagesFragment,
    videoManager: VideoViewHolderManager
): ChatMessageVHMedia(itemView, fragment, videoManager), VideoViewHolder by videoManager {

    private var videoView: PlayerViewNoController? = null
    private var thumbnailView: ImageView? = null
    private var thumbnailViewLayout: View? = null

    private var playBtn: View? = null
    private var progressView: View? = null
    private var progressBar: View? = null
    private var progressMessage: TextView? = null

    private var lastPlayerPosition = 0L

    private var playerInError: Boolean = false

    private var wantsLandscape: Boolean = false


    init {
        with(itemView) {
            videoView = (this.findViewById(R.id.video_view) as PlayerViewNoController).apply {
                this.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER)
                this.setBackgroundColor(Color.BLACK)

                val playerGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                        onClick()
                        return true
                    }

                    override fun onDoubleTap(e: MotionEvent?): Boolean {
                        return false
                    }
                })

                setOnTouchListener { _, event ->
                    playerGestureDetector.onTouchEvent(event)
                    true
                }
            }
            thumbnailView = this.findViewById(R.id.video_view_thumbnail)
            thumbnailViewLayout = this.findViewById(R.id.video_view_thumbnail_layout)

            playBtn = (this.findViewById(R.id.postPlay) as View)
//                .apply {
//                    this.setOnClickListener {
//                        if (fragment.isAutoPlayOn()) {
//                            videoView?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
//                            preparePlayerAndStart(playEnded)
//                        }
//                        else if (!playerInError) openFullScreenActivity()
//                    }
//                }
            progressView = this.findViewById(R.id.progress_layout)
            progressBar = progressView!!.findViewById(R.id.progress)
            progressMessage = progressView!!.findViewById(R.id.progress_message)
            progressMessage!!.setText(R.string.buffering_video)
        }
    }

    override fun onBindVideoItem(item: VideoObject, hasVideoPlaying: Boolean) {

        videoObject = item

        LogUtils.d(logTag, "VIDEO_TEST: onBind() child: $this")

        videoManager?.apply {
            setPlayIcon(playBtn as AppCompatImageView)
            setVideoView(videoView!!)
        }

        videoPlay(!hasVideoPlaying && (getVideoView().getVisiblePercentage() > PLAYABLE_THRESHOLD))

    }


    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        videoView?.setBackgroundColor(Color.BLACK)

        if (videoManager?.isPlaying() == false) {
            playBtn?.visibility = View.VISIBLE
            progressView?.visibility = View.GONE
        }

        if (hasLollipop())
            videoView?.transitionName = currentMessage?.mediaURL

//        applyThumbnail(true)
    }

    override fun setLastPlayerPosition() {
        lastPlayerPosition = videoManager?.getCurrentPosition() ?: 0
    }

//    override fun preparePlayerAndStart(resetMedia: Boolean) {
//        if (resetMedia) videoView?.player?.seekTo(0)
//        playEnded = false
//        play()
//    }

    fun resetViews(recycled: Boolean) {
        playBtn?.visibility = View.VISIBLE
        progressMessage?.setText(R.string.buffering_video)
        if (recycled)
            thumbnailViewLayout?.visibility = View.VISIBLE
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (callingSendMessageInError || playerInError) return

        // TODO: 2020-01-02    open full screen activity??
//        openFullScreenActivity()

        /*
         * Due to bug for some situations dispatchMediaKeyEvent is triggered but almost ignored by another change in player state with playWhenReady==true
         */
//        when {
//            isPlaying ->  {
//                videoView?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
//                playBtn?.visibility = View.VISIBLE
//                setLastPlayerPosition()
//                pause()
//            }
//            else -> openFullScreenActivity()
//        }
    }

    private fun applyThumbnail(show: Boolean) {
        when (videoManager?.getCurrentPosition()) {
            null, 0L -> {
                object: CustomViewTarget<ImageView, Drawable>(thumbnailView as ImageView) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        thumbnailView?.visibility = View.GONE
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        thumbnailView?.setImageDrawable(resource)
                        thumbnailView?.visibility = View.VISIBLE
                    }
                }.setPicture(extractThumbnailFromUrl(), RequestOptions.fitCenterTransform())

                thumbnailViewLayout?.visibility = if (show) View.VISIBLE else View.GONE
            }
            else -> thumbnailViewLayout?.visibility = View.GONE
        }
    }

    private fun extractThumbnailFromUrl(): String {
        return if (!currentMessage?.videoThumbnail.isNullOrBlank()) currentMessage!!.videoThumbnail ?: ""
        else if (!currentMessage?.mediaURL.isNullOrBlank()) {
            val path = currentMessage!!.mediaURL.substring(0, currentMessage!!.mediaURL.lastIndexOf("/") + 1)
            val fileName = currentMessage!!.mediaURL.substring(currentMessage!!.mediaURL.lastIndexOf("/") + 1)
            val thumbParts = fileName.split(".")
            if (thumbParts.size == 2) {
                val thumb = path.plus(thumbParts[0]).plus("_thumb.jpg")
                val id = currentMessage?.messageID
                fragment.viewModel.setMessageThumbnail(thumb)
                thumb
            } else ""
        } else ""
    }

    private fun openFullScreenActivity() {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            context as Activity,
            videoView!!,
            ViewCompat.getTransitionName(videoView!!)!!)

        // bundle deactivated because of ugly lag  during activity start.

        fragment.startActivityForResult(
            Intent(context, VideoViewActivity::class.java).let {
                it.putExtra(EXTRA_PARAM_1, currentMessage?.mediaURL)
                it.putExtra(EXTRA_PARAM_2, currentMessage?.videoThumbnail)
                it.putExtra(EXTRA_PARAM_3, ViewCompat.getTransitionName(videoView!!))
                it.putExtra(EXTRA_PARAM_4, lastPlayerPosition)
                it.putExtra(EXTRA_PARAM_5, wantsLandscape)
                it.putExtra(EXTRA_PARAM_6, currentMessage?.messageID)
                it.putExtra(EXTRA_PARAM_7, true)
            },
            RESULT_FULL_VIEW_VIDEO
//                ,options.toBundle()
        )
    }


    companion object {
        val logTag = ChatMessageVHMediaVideo::class.java.simpleName
    }

}

// INFO: 2019-12-24    LOCATION are in stand-by for now - Maps is commented
/**
 * Class handling [ChatMessageType.LOCATION] message type.
 * @author mbaldrighi on 10/22/2018.
 */
//class ChatMessageVHMediaLocation(itemView: View, fragment: ChatMessagesFragment): ChatMessageVHMedia(itemView, fragment),
//    OnMapReadyCallback {
//
//    private var mapView: MapView? = null
//    private var map: GoogleMap? = null
//
//    private var location: LatLng = LatLng(0.0, 0.0)
//
//    init {
//        mapView = itemView.findViewById(R.id.map)
//        mapView?.onCreate(null)
//        mapView?.getMapAsync(this@ChatMessageVHMediaLocation)
//    }
//
//    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
//        super.setMessage(messages, position)
//
//        mapView?.onResume()
//
//        val split = currentMessage?.location?.split(";")
//        try {
//            if (split?.size == 2)
//                location = LatLng(split[0].toDouble(), split[1].toDouble())
//        } catch (e: NumberFormatException) {
//            LogUtils.e("ChatMessageLocationVH", "Corrupted location: ${currentMessage?.location}, ${split.toString()}")
//        }
//    }
//
//    override fun onMapReady(p0: GoogleMap?) {
//        MapsInitializer.initialize(fragment.context)
//        map = p0
//        initMap()
//    }
//
//    override fun onClick(v: View?) {
//        super.onClick(v)
//
//        if (callingSendMessageInError) return
//
//        val gmmIntentUri = Uri.parse("geo:${location.latitude},${location.longitude}?z=17")
//        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
//        mapIntent.setPackage("com.google.android.apps.maps")
//
//        if (mapIntent.resolveActivity(context.packageManager) != null)
//            context.startActivity(mapIntent)
//        else
//            fragment.showError(fragment.getString(R.string.error_generic))
//    }
//
//    fun initMap() {
//
//        map?.mapType = GoogleMap.MAP_TYPE_NORMAL
//
//        map?.uiSettings?.isIndoorLevelPickerEnabled = false
//        map?.uiSettings?.isMyLocationButtonEnabled = false
//        map?.setOnMapClickListener { onClick(null) }
//        map?.setOnMarkerClickListener { onClick(null);true }
//
//        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))
//        map?.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker()).position(location))
//    }
//
//    fun removeMap() {
//        map?.clear()
//        map?.mapType = GoogleMap.MAP_TYPE_NONE
//
//        mapView?.onPause()
//    }
//
//}


/**
 * Class handling [ChatMessageType.TEXT] message type as WebLink.
 * @author mbaldrighi on 10/22/2018.
 */
class ChatMessageVHMediaWebLink(
    itemView: View,
    fragment: ChatMessagesFragment
): ChatMessageVHMedia(itemView, fragment) {

    companion object {
        private var backgrSpan: RoundedCornersBackgroundSpan? = null
    }

    private var messageStateContainerOuter = itemView as? MessageOutgoingStateLayout?

    private var imageView: ImageView? = null
    private var containerIncoming: View? = null
    private var containerOutgoing: View? = null
    private var footerOutgoing: TextView? = null
    var messageIncoming: TextView? = null
    var messageOutgoing: TextView? = null
    private var webLinkTitle: AppCompatTextView? = null
    private var webLinkSource: TextView? = null
    private var placeHolderIcon: View? = null

    private var webLinkObj: HLWebLink? = null

    private var stateIcon: View? = null
    private var stateIconMessage: View? = null


    init {
        with(itemView) {
            imageView = this.findViewById(R.id.imageView)
            containerIncoming = this.findViewById(R.id.messageContainerIncoming)

            containerOutgoing = this.findViewById<MessageOutgoingStateLayout?>(R.id.messageStateContainerInner)
//                .also {
//                    messageStateView = it
//                }

            footerOutgoing = this.findViewById(R.id.infoFooter)
            messageIncoming = this.findViewById(R.id.messageIncoming)
            messageOutgoing = this.findViewById(R.id.messageOutgoing)
            webLinkTitle = this.findViewById(R.id.weblinkTitle)
            webLinkSource = this.findViewById(R.id.weblinkSource)
            placeHolderIcon = this.findViewById(R.id.placeHolder)

            stateIcon = this.findViewById(R.id.iconState)
            stateIconMessage = this.findViewById(R.id.iconStateInner)

            if (backgrSpan == null) {
                backgrSpan =
                    RoundedCornersBackgroundSpan(
                        getColor(this.context.resources, R.color.black_70),
                        10,
                        0f
                    )
            }
        }
    }

    override fun setMessage(messages: MutableList<ChatMessage>, position: Int) {
        super.setMessage(messages, position)

        webLinkObj = currentMessage?.webLink

        if (!webLinkObj?.title.isNullOrBlank()) {

            webLinkTitle?.setShadowLayer(10f, 0f, 0f, Color.TRANSPARENT)
            webLinkTitle?.setPadding(10, 10, 10, 10)
            webLinkTitle?.setLineSpacing(2f, 1f)

            val spanned = SpannableString(webLinkObj!!.title)
            spanned.setSpan(
                backgrSpan,
                0,
                webLinkObj!!.title!!.length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )

            // Pass text computation future to AppCompatTextView,
            // which awaits result before measuring.
            webLinkTitle?.setTextFuture(
                PrecomputedTextCompat.getTextFuture(
                    spanned,
                    TextViewCompat.getTextMetricsParams(webLinkTitle!!),
                    /*optional custom executor*/ null
                )
            )
        } else
            webLinkTitle?.text = ""

        webLinkSource?.text = webLinkObj?.source

        currentMessage?.text?.let {
            // while handling the text change constraints / handle view visibility
            val result = WebLinkRecognizer.extractAppendedText(it, webLinkObj?.link)
            val message = result?.first ?: ""

            if (result != null) {
                if (isIncoming) {

                    messageStateView = null

                    if (!message.isBlank()) {
                        messageIncoming?.apply {
                            text = message
                            visibility = View.VISIBLE
                        }
                        containerIncoming?.visibility = View.VISIBLE
                    } else {
                        messageIncoming?.visibility = View.GONE
                        containerIncoming?.visibility = View.GONE
                    }
                } else {
                    if (!message.isBlank()) {

                        messageStateView = containerOutgoing as? MessageOutgoingStateLayout

                        messageOutgoing?.apply {
                            text = message
                            visibility = View.VISIBLE
                        }
                        footerOutgoing?.apply {
                            text = getInfoText(currentMessage)
                            visibility = View.VISIBLE
                        }
                        containerOutgoing?.visibility = View.VISIBLE
                        stateIconMessage?.visibility = View.VISIBLE
                        infoOutgoing?.visibility = View.GONE
                        stateIcon?.visibility = View.GONE
                    } else {

                        messageStateView = messageStateContainerOuter

                        with(View.GONE) {
                            messageOutgoing?.visibility = this
                            containerOutgoing?.visibility = this
                            footerOutgoing?.visibility = this
                            stateIconMessage?.visibility = this
                        }
                        infoOutgoing?.visibility = View.VISIBLE
                        stateIcon?.visibility = View.VISIBLE
                    }
                }
            } else {
                messageStateView = null

                if (isIncoming) {
                    containerIncoming?.visibility = View.GONE
                } else {
                    containerOutgoing?.visibility = View.GONE
                    infoOutgoing?.visibility = View.VISIBLE
                }
            }

            handleMessageState()

            if (isIncoming) {
                ConstraintSet().apply {
                    // get the ConstraintSet
                    clone(itemView as? ConstraintLayout)
                    if (result != null)  {
                        if (message.isBlank()) {
                            this@apply.connect(
                                R.id.earningValue,
                                ConstraintSet.END,
                                R.id.messageContainerIncoming,
                                ConstraintSet.END
                            )
                        } else {
                            this@apply.connect(
                                R.id.earningValue,
                                ConstraintSet.END,
                                ConstraintSet.PARENT_ID,
                                ConstraintSet.END
                            )
                        }
                    } else {
                        this@apply.connect(
                            R.id.earningValue,
                            ConstraintSet.END,
                            ConstraintSet.PARENT_ID,
                            ConstraintSet.END
                        )
                    }
                    applyTo(itemView as ConstraintLayout)
                }
            }
        }

        if (imageView != null) {
            val string = if (webLinkObj?.mediaURL.isNullOrBlank()) WEB_LINK_PLACEHOLDER_URL else webLinkObj?.mediaURL
            object : CustomViewTarget<ImageView, Drawable>(imageView!!) {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    getView().context?.let {
                        if (it.isValid())
                            getView().setPicture(WEB_LINK_PLACEHOLDER_URL)
                    }
                }

                override fun onResourceCleared(placeholder: Drawable?) {}

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    if (webLinkObj?.mediaURL?.contains("png", true) == true)
                        placeHolderIcon?.visibility = View.GONE

                    getView().context?.let {
                        if (it.isValid())
                            getView().setImageDrawable(resource)
                    }
                }
            }.setPicture(string)
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)

        if (callingSendMessageInError) return

        webLinkObj?.link?.let {
            // go to WebView with link
            fragment.addReplaceFragment(
                R.id.container,
                WebViewFragment.newInstance(WebViewFragment.WVType.CHAT)
                    .apply {
                        fragment.mWebViewModel.mWebUrl = it
                        fragment.mWebViewModel.mToolbarName = webLinkObj?.title ?: it
                    },
                true,       // just adds the fragment because of a crash related to the options menu
                true,
                NavigationAnimationHolder()
            )
        }

    }

}