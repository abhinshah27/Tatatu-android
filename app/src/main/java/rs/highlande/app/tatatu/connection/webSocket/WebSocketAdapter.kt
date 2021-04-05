package rs.highlande.app.tatatu.connection.webSocket

import android.content.Context
import android.os.Handler
import android.os.Message
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame
import com.neovisionaries.ws.client.WebSocketState
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.NotificationService
import rs.highlande.app.tatatu.connection.SubscribeToSocketService
import rs.highlande.app.tatatu.connection.SubscribeToSocketServiceChat
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.chat.HandleChatsUpdateService
import java.util.*

/**
 * @author mbaldrighi on 2019-07-10.
 */
class WebSocketAdapter internal constructor(private val context: Context, private val isChat: Boolean) :
    WebSocketAdapter(), Handler.Callback, KoinComponent {

    private val tracker: WebSocketTracker by inject()
    private val connection: WebSocketConnection by inject()

    // TODO: 2019-07-12    consider if still necessary
//    interface OnConnectionChangedListener {
//        fun onConnectionChange()
//    }
//    var mListener: OnConnectionChangedListener? = null

    private var connectionObservers: MutableSet<ConnectionObserver>? = HashSet()
    private var subscriptionObservers: MutableSet<SubscriptionObserver>? = HashSet()

    private var subscribeHandler: Handler? = null
    var isRtComSubscribed: Boolean = false
    var isChatSubscribed: Boolean = false


    init {
        subscribeHandler = Handler(this)
    }


    @Throws(Exception::class)
    override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {
        super.onStateChanged(websocket, newState)
        LogUtils.d(logTag, "WS STATE CHANGED: $newState for WebSocket: $websocket")

//        if (newState != WebSocketState.OPEN)
//            mListener!!.onConnectionChange()
    }

    @Throws(Exception::class)
    override fun onConnected(websocket: WebSocket?, headers: Map<String, List<String>>?) {
        super.onConnected(websocket, headers)
        LogUtils.d(logTag, "WS CONNECTION SUCCESS for WebSocket: $websocket")

        notifyConnection(isChat)

        if (isChat) {
            TTUApp.socketConnectingChat = false
            // automatically re-subscribe client to real-time communication
            subscribeSocketChat(context)
        } else {
            TTUApp.socketConnecting = false
            // automatically re-subscribe client to real-time communication
            subscribeSocketRTCom(context)
        }
    }

    @Throws(Exception::class)
    override fun onDisconnected(
        websocket: WebSocket?, serverCloseFrame: WebSocketFrame?,
        clientCloseFrame: WebSocketFrame?, closedByServer: Boolean
    ) {
        super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
        LogUtils.d(logTag, "WS DISCONNECTION SUCCESS for WebSocket: $websocket")

        if (isChat) TTUApp.subscribedToSocketChat = false
        else TTUApp.subscribedToSocket = false

        if (closedByServer)
            connection.openConnection(isChat)
    }

    @Throws(Exception::class)
    override fun onTextMessage(websocket: WebSocket?, text: String?) {
        super.onTextMessage(websocket, text)
        LogUtils.d(logTag, "WS MESSAGE RECEIVED: $text")

        tracker.onDataReceivedAsync(text)
    }

    @Throws(Exception::class)
    override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
        super.onBinaryMessage(websocket, binary)
        LogUtils.d(logTag, "WS ENCRYPTED BYTE[] MESSAGE RECEIVED")

        tracker.onDataReceivedAsync(binary)
    }

    /*
	 * ERRORS
	 */
    @Throws(Exception::class)
    override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
        super.onError(websocket, cause)
        LogUtils.e(logTag, "WS GENERIC ERROR: ${cause!!.message} for WebSocket: $websocket", cause)
    }

    @Throws(Exception::class)
    override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
        super.onConnectError(websocket, exception)
        LogUtils.e(
            logTag,
            "WS CONNECTION ERROR: ${exception!!.message} for WebSocket: $websocket",
            exception
        )

        if (isChat) {
            TTUApp.socketConnectingChat = false
            TTUApp.subscribedToSocketChat = false
        }
        else {
            TTUApp.socketConnecting = false
            TTUApp.subscribedToSocket = false
        }
    }

    @Throws(Exception::class)
    override fun onTextMessageError(websocket: WebSocket?, cause: WebSocketException?, data: ByteArray?) {
        super.onTextMessageError(websocket, cause, data)
        LogUtils.e(logTag, "WS TEXT MESSAGE ERROR: ${cause!!.message}", cause)
    }

    @Throws(Exception::class)
    override fun onUnexpectedError(websocket: WebSocket?, cause: WebSocketException?) {
        super.onUnexpectedError(websocket, cause)
        LogUtils.e(logTag, "WS UNEXPECTED ERROR: ${cause!!.message}", cause)
    }

    @Throws(Exception::class)
    override fun onSendError(websocket: WebSocket?, cause: WebSocketException?, frame: WebSocketFrame?) {
        super.onSendError(websocket, cause, frame)
        LogUtils.e(logTag, "WS SEND ERROR: ${cause!!.message} for WebSocket: $websocket", cause)
    }

    /*
		* PINGs&PONGs
		*/
    @Throws(Exception::class)
    override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        super.onPingFrame(websocket, frame)
        LogUtils.v(logTag, "WS PING RECEIVED for WebSocket: $websocket")
        websocket?.sendPong()
    }

    @Throws(Exception::class)
    override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        super.onPongFrame(websocket, frame)
        LogUtils.v(logTag, "WS PONG RECEIVED for WebSocket: $websocket")
    }


    private fun notifyConnection(isChat: Boolean) {

        LogUtils.d(logTag, "testCONNECTION:\tobservers.size=${connectionObservers?.size}\tisChat=$isChat")

        if (connectionObservers != null) {
            for (observer in connectionObservers!!) {
                LogUtils.d(logTag, "testCONNECTION:\tNOTIFYING")
                observer.onConnectionEstablished(isChat)
            }

            connectionObservers!!.clear()
        }
    }

    private fun notifySubscription(isChat: Boolean) {
        if (subscriptionObservers != null) {
            for (observer in subscriptionObservers!!) {
                observer.onSubscriptionEstablished(isChat)
            }

            subscriptionObservers!!.clear()
        }
    }

    internal fun attachConnectionObserver(observer: ConnectionObserver?) {

        LogUtils.d(logTag, "testCONNECTION:\tattaching CONNECTION observer ($observer)")

        if (observer == null) return

        if (connectionObservers == null)
            connectionObservers = HashSet()

        connectionObservers!!.add(observer)
    }

    internal fun attachSubscriptionObserver(observer: SubscriptionObserver?) {
        if (observer == null) return

        if (subscriptionObservers == null)
            subscriptionObservers = HashSet()

        subscriptionObservers!!.add(observer)
    }

    internal fun subscribeSocketRTCom(context: Context) {
        SubscribeToSocketService.startService(context, subscribeHandler)
    }

    internal fun subscribeSocketChat(context: Context) {
        SubscribeToSocketServiceChat.startService(context, subscribeHandler)
    }


    interface ConnectionObserver {
        fun onConnectionEstablished(isChat: Boolean)
    }

    interface SubscriptionObserver {
        fun onSubscriptionEstablished(isChat: Boolean)
    }



    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            SubscribeToSocketService.SUCCESS_RTCOM -> {

                isRtComSubscribed = true

                // notification for custom actions
                notifySubscription(false)

                tracker.checkPendingAndRetry(false)

                //Notification Service Call
                NotificationService.startService(context)

                if (connection.isConnectedChat) {
                    tracker.checkPendingAndRetry(true)

                    HandleChatsUpdateService.startService(context)
//                        ChatSetUserOnlineService.startService(context.getHLContext())

                    // notification for custom actions
                    notifySubscription(true)
                }

//                    // automatically re-sends client's FCM notifications token
//                    SendFCMTokenService.startService(context.getHLContext(), null);
//                    // automatically re-fetches user's general configuration data
//                    GetConfigurationDataService.startService(context.getHLContext());
            }

            //reopen chat
            SubscribeToSocketServiceChat.SUCCESS_CHAT -> {

                isChatSubscribed = true

                // INFO: 4/18/19    socket CHAT has to wait that main socket is connected to attempt retry ops
                if (connection.isConnected) {

                    // notification for custom actions
                    notifySubscription(true)

                    tracker.checkPendingAndRetry(true)

                    HandleChatsUpdateService.startService(context)
//                    ChatSetUserOnlineService.startService(context.getHLContext())
                }
            }
        }
        return true
    }

    companion object {

        val logTag = WebSocketAdapter::class.java.simpleName
    }


    //region == Getters and setters ==

//    fun setListener(listener: OnConnectionChangedListener) {
//        this.mListener = listener
//    }

    //endregion

}