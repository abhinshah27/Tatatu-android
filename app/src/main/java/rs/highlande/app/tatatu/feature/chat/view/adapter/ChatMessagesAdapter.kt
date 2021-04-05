/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.view.adapter

import android.media.MediaPlayer
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.ui.PlayerView
import io.realm.OrderedRealmCollection
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecyclerListener
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseVideoPlayerRealmAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.VideoViewHolderManager
import rs.highlande.app.tatatu.feature.chat.view.ChatMessagesFragment
import rs.highlande.app.tatatu.model.chat.ChatMessage
import java.util.concurrent.atomic.AtomicInteger


/**
 * Custom adapter for chat MESSAGES view.
 * @author mbaldrighi on 10/15/2018.
 */
class ChatMessagesAdapter(
    messages: OrderedRealmCollection<ChatMessage>,
    private val fragment: ChatMessagesFragment
): BaseVideoPlayerRealmAdapter<ChatMessage, BaseRecyclerListener, ChatMessageVH>(messages, true) {

    private val TYPE_INCOMING = 0
    private val TYPE_OUTGOING = 1
    private val TYPE_INCOMING_PHOTO = 2
    private val TYPE_OUTGOING_PHOTO = 3
    private val TYPE_INCOMING_VIDEO = 4
    private val TYPE_OUTGOING_VIDEO = 5
    private val TYPE_INCOMING_AUDIO = 6
    private val TYPE_OUTGOING_AUDIO = 7
    private val TYPE_INCOMING_LOCATION = 8
    private val TYPE_OUTGOING_LOCATION = 9
    private val TYPE_INCOMING_DOCUMENT = 10
    private val TYPE_OUTGOING_DOCUMENT = 11
    private val TYPE_INCOMING_WEBLINK = 12
    private val TYPE_OUTGOING_WEBLINK = 13
    private val TYPE_INCOMING_MISSED_VOICE_CALL = 14
    private val TYPE_OUTGOING_MISSED_VOICE_CALL = 15
    private val TYPE_INCOMING_MISSED_VIDEO_CALL = 16
    private val TYPE_OUTGOING_MISSED_VIDEO_CALL = 17
    private val TYPE_SYSTEM = 18

    var playAudioTask: AsyncTask<*,*,*>? = null
    var mediaPlayer: MediaPlayer? = null

    var videoView: PlayerView? = null
//    var videoHelper: ExoPlayerViewHelper? = null
    //var videoHolder: ChatMessageVHMediaVideo? = null

    var manualItem: AtomicInteger = AtomicInteger(-1)
    val dispatcher = DefaultControlDispatcher()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageVH {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            TYPE_INCOMING_PHOTO, TYPE_OUTGOING_PHOTO -> ChatMessageVHMediaPhoto(
                inflater.inflate(R.layout.item_chat_media_photo, parent, false),
                fragment
            )
            TYPE_INCOMING_AUDIO, TYPE_OUTGOING_AUDIO -> ChatMessageVHMediaAudio(
                inflater.inflate(R.layout.item_chat_media_audio, parent, false),
                fragment
            )
            TYPE_INCOMING_VIDEO, TYPE_OUTGOING_VIDEO -> ChatMessageVHMediaVideo(
                inflater.inflate(R.layout.item_chat_media_video, parent, false),
                fragment,
                VideoViewHolderManager(parent.context)
            )
//            TYPE_INCOMING_LOCATION, TYPE_OUTGOING_LOCATION -> ChatMessageVHMediaLocation(
//                inflater.inflate(R.layout.item_chat_media_location, parent, false),
//                fragment
//            )
            TYPE_INCOMING_DOCUMENT -> ChatMessageVHDocument(
                inflater.inflate(R.layout.item_chat_balloon_incoming_document, parent, false),
                fragment
            )
            TYPE_OUTGOING_DOCUMENT -> ChatMessageVHDocument(
                inflater.inflate(R.layout.item_chat_balloon_outgoing_document, parent, false),
                fragment
            )
            TYPE_INCOMING_WEBLINK, TYPE_OUTGOING_WEBLINK -> {
                ChatMessageVHMediaWebLink(
                    inflater.inflate(
                        if (viewType == TYPE_INCOMING_WEBLINK) R.layout.item_chat_media_weblink_incoming
                        else R.layout.item_chat_media_weblink_outgoing,
                        parent,
                        false
                    ),
                    fragment
                )
            }
            TYPE_INCOMING -> ChatMessageVHText(
                inflater.inflate(R.layout.item_chat_balloon_incoming, parent, false), fragment
            )
            TYPE_INCOMING_MISSED_VOICE_CALL, TYPE_INCOMING_MISSED_VIDEO_CALL -> ChatMessageVHMissedCall(
                inflater.inflate(R.layout.item_chat_balloon_incoming_missed_call, parent, false), fragment
            )
            TYPE_OUTGOING_MISSED_VOICE_CALL, TYPE_OUTGOING_MISSED_VIDEO_CALL -> ChatMessageVHMissedCall(
                inflater.inflate(R.layout.item_chat_balloon_outgoing_missed_call, parent, false), fragment
            )
            TYPE_SYSTEM -> ChatMessageVHSystem(
                inflater.inflate(R.layout.item_chat_system, parent, false), fragment
            )
            else -> ChatMessageVHText(
                inflater.inflate(R.layout.item_chat_balloon_outgoing, parent, false), fragment
            )
        }
    }

    override fun onBindViewHolder(holder: ChatMessageVH, position: Int) {

        // TODO: 2019-12-24    REMOVE: toro
//        if (holder is ChatMessageVHMediaVideo) {
//            (holder.playerView as? PlayerView)?.setControlDispatcher(object: ControlDispatcher {
//
//                override fun dispatchSetPlayWhenReady(player: Player, playWhenReady: Boolean): Boolean {
//                    val dispatched = dispatcher.dispatchSetPlayWhenReady(player, playWhenReady)
//                    if (dispatched) {
//                        this@ChatMessagesAdapter.manualItem.set(
//                                if (!playWhenReady) -1 else holder.playerOrder
//                        )
//                    }
//                    return dispatched
//                }
//
//                override fun dispatchSetShuffleModeEnabled(player: Player?, shuffleModeEnabled: Boolean): Boolean {
//                    return dispatcher.dispatchSetShuffleModeEnabled(player, shuffleModeEnabled)
//                }
//
//                override fun dispatchSeekTo(player: Player, windowIndex: Int, positionMs: Long): Boolean {
//                    return dispatcher.dispatchSeekTo(player, windowIndex, positionMs)
//                }
//
//                override fun dispatchSetRepeatMode(player: Player, repeatMode: Int): Boolean {
//                    return dispatcher.dispatchSetRepeatMode(player, repeatMode)
//                }
//
//                override fun dispatchStop(player: Player?, reset: Boolean): Boolean {
//                    return dispatcher.dispatchStop(player, reset)
//                }
//            })
//        }

        if (data != null) {
            if (holder is VideoViewHolder)
                super.onBindViewHolder(holder, position)

            holder.setMessage(data!!, position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = data?.get(position)
        val userId = fragment.viewModel.user?.uid
        if (userId.isNullOrEmpty()) return -1

        return if (message?.isSystem() == true) {
            TYPE_SYSTEM
        } else {
            when(message?.getDirectionType(userId)) {
                ChatMessage.DirectionType.INCOMING ->
                    when {
                        message.hasLocation() -> TYPE_INCOMING_LOCATION
                        message.hasDocument() -> TYPE_INCOMING_DOCUMENT
                        message.hasWebLink() -> TYPE_INCOMING_WEBLINK
                        message.isMissedVoiceCall() -> TYPE_INCOMING_MISSED_VOICE_CALL
                        message.isMissedVideoCall() -> TYPE_INCOMING_MISSED_VIDEO_CALL
                        else -> when {
                            message.hasPicture() -> TYPE_INCOMING_PHOTO
                            message.hasVideo() -> TYPE_INCOMING_VIDEO
                            message.hasAudio() -> TYPE_INCOMING_AUDIO
                            else -> TYPE_INCOMING
                        }
                    }
                ChatMessage.DirectionType.OUTGOING ->
                    when {
                        message.hasLocation() -> TYPE_OUTGOING_LOCATION
                        message.hasDocument() -> TYPE_OUTGOING_DOCUMENT
                        message.hasWebLink() -> TYPE_OUTGOING_WEBLINK
                        message.isMissedVoiceCall() -> TYPE_OUTGOING_MISSED_VOICE_CALL
                        message.isMissedVideoCall() -> TYPE_OUTGOING_MISSED_VIDEO_CALL
                        else -> when {
                            message.hasPicture() -> TYPE_OUTGOING_PHOTO
                            message.hasVideo() -> TYPE_OUTGOING_VIDEO
                            message.hasAudio() -> TYPE_OUTGOING_AUDIO
                            else -> TYPE_OUTGOING
                        }
                    }
                else -> {
                    -1
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        return data?.get(position)?.messageID.hashCode().toLong()
    }


    override fun onViewAttachedToWindow(holder: ChatMessageVH) {
        super.onViewAttachedToWindow(holder)

        when (holder) {
//            is ChatMessageVHMediaAudio -> {
//                // TODO: 10/23/2018     disables even this kind of auto play
////                if (holder.stoppedForScrolling && holder.lastPlayerPosition > 0) {
////                    holder.stoppedForScrolling = false
////                    holder.preparePlayerAndStart()
////                }
//
//                playAudioTask = holder.playTask
//                mediaPlayer = holder.mediaPlayer
//            }
//
//            is ChatMessageVHMediaVideo ->  {
//                if (holder.playEnded) holder.playEnded = false
//                videoHelper = holder.playerHelper
//                videoHolder = holder
//            }

            // INFO: 2020-01-09    Maps is commented
//            is ChatMessageVHMediaLocation -> {
//                holder.initMap()
//            }

            is ChatMessageVHMediaWebLink -> {
                holder.messageOutgoing?.setTextIsSelectable(true)
                holder.messageIncoming?.setTextIsSelectable(true)
            }
            is ChatMessageVHText, is ChatMessageVHDocument -> {
                (holder as? ChatMessageVHText)?.messageNoMedia?.setTextIsSelectable(true)
                (holder as? ChatMessageVHDocument)?.messageNoMedia?.setTextIsSelectable(true)
            }
        }
    }

    // TODO: 2019-12-24    REMOVE: toro
//    override fun getKeyForOrder(order: Int): Any? {
//        if (data?.isNotEmpty() == true && data!!.size > order)
//            return data!![order]
//        return null
//    }
//
//    override fun getOrderForKey(key: Any): Int? {
//        return if (key is ChatMessage && data?.isNotEmpty() == true) {
//            val i = data?.indexOf(key) ?: 0
//            if (i != -1) i else 0
//        } else 0
//    }


    //region == Class custom methods ==

    fun getMessageByID(messageID: String): Pair<Int, ChatMessage?> {
        val mess = data?.filter { chatMessage -> chatMessage.messageID == messageID }

        return if (mess?.size == 1) data!!.indexOf(mess[0]) to mess[0]
        else -1 to null
    }

    fun cleanAdapterMediaControllers() {
        if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        if (playAudioTask?.isCancelled == false)
            playAudioTask?.cancel(true)


        // TODO: 2019-12-27    replace with video holder calls

//        if (videoHelper?.isPlaying == true) videoHelper?.pause()
//        videoHelper?.release()

    }

    //endregion

}