package rs.highlande.app.tatatu.feature.chat.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.vincent.videocompressor.LogUtils
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.model.chat.ChatMessage


/**
 * [ConstraintLayout] child class with the function of holding the logic to perform image resource
 * changes on the "iconState"/"iconStateInner"-tagged [ImageView] to display the
 * [ChatMessage.Status.SENT], [ChatMessage.Status.DELIVERED], and [ChatMessage.Status.READ] statuses.
 * @author mbaldrighi on 2019-12-23.
 */
class MessageOutgoingStateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var messageSent = false
    private var messageDelivered = false
    private var messageRead = false


    override fun onCreateDrawableState(extraSpace: Int): IntArray? {

        // If the drawable state is not recognized we merge our custom state into
        // the existing drawable state before returning it.

        return when {
            messageSent -> { // We are going to add 1 extra state.
                val drawableState = super.onCreateDrawableState(extraSpace + 1)
                View.mergeDrawableStates(drawableState, stateMessageSent)
                drawableState
            }
            messageDelivered -> {
                val drawableState = super.onCreateDrawableState(extraSpace + 1)
                View.mergeDrawableStates(drawableState, stateMessageDelivered)
                drawableState
            }
            messageRead -> {
                val drawableState = super.onCreateDrawableState(extraSpace + 1)
                View.mergeDrawableStates(drawableState, stateMessageRead)
                drawableState
            }
            else -> {
                super.onCreateDrawableState(extraSpace)
            }
        }
    }


    fun setState(state: ChatMessage.Status?) {
        val refresh = when (state) {
            ChatMessage.Status.SENT -> {
                if (!messageSent && !messageDelivered && !messageRead) {
                    messageSent = true
                    messageDelivered = false
                    messageRead = false
                    true
                } else false
            }
            ChatMessage.Status.DELIVERED -> {
                if (!messageDelivered && !messageRead) {
                    messageSent = false
                    messageDelivered = true
                    messageRead = false
                    true
                } else false
            }
            ChatMessage.Status.READ -> {
                if (!messageRead) {
                    messageSent = false
                    messageDelivered = false
                    messageRead = true
                    true
                } else false
            }
            else -> {
                if (!messageSent && !messageDelivered && !messageRead) {
                    messageSent = false
                    messageDelivered = false
                    messageRead = false
                    true
                } else false
            }
        }

        if (refresh)
            refreshDrawableState()

    }


    fun setStateIcon(state: ChatMessage.Status?) {
        findViewWithTag<ImageView?>(STATE_VIEW_TAG)
            ?.let { it ->
                applyState(it, state)
            }
        findViewWithTag<ImageView?>(STATE_VIEW_TAG_INNER)
            ?.let { it ->
                applyState(it, state)
            }

    }

    private fun applyState(view: ImageView, state: ChatMessage.Status?) {
        LogUtils.d(logTag, "TESTSTATE: View: $view with State $state")

        when (state) {
            ChatMessage.Status.SENT -> {
                if (!messageSent && !messageDelivered && !messageRead) {
                    LogUtils.d(logTag, "TESTSTATE: Processsing $state")
                    view.setImageResource(R.drawable.layer_chat_sent)
                    view.visibility = View.VISIBLE
                }
            }
            ChatMessage.Status.DELIVERED -> {
                if (!messageDelivered && !messageRead) {
                    LogUtils.d(logTag, "TESTSTATE: Processsing $state")
                    view.setImageResource(R.drawable.layer_chat_delivered)
                    view.visibility = View.VISIBLE
                }
            }
            ChatMessage.Status.READ -> {
                if (!messageRead) {
                    LogUtils.d(logTag, "TESTSTATE: Processsing $state")
                    view.setImageResource(R.drawable.layer_chat_read)
                    view.visibility = View.VISIBLE
                }
            }
            else -> {
                if (!messageSent && !messageDelivered && !messageRead) {
                    LogUtils.d(logTag, "TESTSTATE: No valid state: hiding $view")
                    view.visibility = View.INVISIBLE
                }
            }
        }
    }


    companion object {

        val logTag = MessageOutgoingStateLayout::class.java.simpleName

        val stateMessageSent = intArrayOf(R.attr.message_state_sent)
        val stateMessageDelivered = intArrayOf(R.attr.message_state_delivered)
        val stateMessageRead = intArrayOf(R.attr.message_state_read)

        private const val STATE_VIEW_TAG = "iconState"
        private const val STATE_VIEW_TAG_INNER = "iconStateInner"       // needed for weblink messages

    }

}