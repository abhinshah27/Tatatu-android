package rs.highlande.app.tatatu.connection.webSocket.realTime

import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.chat.ChatMessage
import java.io.Serializable

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-12.
 */


/**
 * Listener interface for actions on Real-Time communication for SOCIAL purposes.
 * @author mbaldrighi on 2019-07-12.
 */
interface RealTimeCommunicationListener : Serializable {

    fun onPostAdded(post: Post?, position: Int)
    fun onPostUpdated(postId: String, position: Int)
    fun onPostDeleted(position: Int)
    fun onHeartsUpdated(position: Int)
    fun onSharesUpdated(position: Int)
    fun onTagsUpdated(position: Int)
    fun onCommentsUpdated(position: Int)

    fun onNewDataPushed(hasInsert: Boolean)

}


/**
 * Listener interface for actions on Real-Time communication for CHAT purposes.
 * @author mbaldrighi on 2019-07-12.
 */
interface RealTimeChatListener {

    fun onNewMessage(newMessage: ChatMessage)
    fun onStatusUpdated(userId: String, status: Int, date: String)
    fun onActivityUpdated(userId: String, chatId: String, activity: String)
    fun onMessageDelivered(chatId: String, userId: String, date: String)
    fun onMessageRead(chatId: String, userId: String, date: String)
    fun onMessageOpened(chatId: String, userId: String, date: String, messageID: String)

}