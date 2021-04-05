package rs.highlande.app.tatatu.connection.webSocket

import android.content.Context
import org.koin.core.KoinComponent
import rs.highlande.app.tatatu.BuildConfig
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.util.LogUtils
import java.util.*
import java.util.concurrent.*

/**
 * Singleton class handling the web socket connection lifecycle.
 *
 * @author mbaldrighi on 2019-07-10.
 */
class WebSocketConnection(private val application: Context) : KoinComponent {

    var client: WebSocketClient? = null
        private set
    var clientChat: WebSocketClient? = null
        private set

    private val checkConnectionRunnable = Runnable {
        LogUtils.v(logTag, "Checking CONNECTION STATUS")
        if (!isConnected(false) && !TTUApp.socketConnecting)
            openConnection(false)
        if (!isConnectedChat(false) && !TTUApp.socketConnectingChat)
            openConnection(true)
    }
    private var connectionScheduler: ScheduledExecutorService? = null
    private var scheduledFuture: ScheduledFuture<*>? = null

    /**
     * This method is used to check if RTCOM socket is connected AND subscribed.
     * @return The connection status of the socket.
     */
    val isConnected: Boolean
        get() = isConnected(true)

    /**
     * This method is used to check if CHAT socket is both connected AND subscribed.
     * @return The connection status of the socket.
     */
    val isConnectedChat: Boolean
        get() = isConnectedChat(true)


    private fun startCheckConnection() {
        if (connectionScheduler == null) {
            connectionScheduler = Executors.newSingleThreadScheduledExecutor()
            (connectionScheduler as? ScheduledThreadPoolExecutor)?.removeOnCancelPolicy = true
        }
        if (scheduledFuture == null)
            scheduledFuture = connectionScheduler!!.scheduleAtFixedRate(
                checkConnectionRunnable,
                500, 5000, TimeUnit.MILLISECONDS
            )
    }

    private fun stopCheckConnection() {
        connectionScheduler = null

        scheduledFuture?.cancel(true)
        scheduledFuture = null
    }


    fun openConnection(isChat: Boolean) {
        if (isChat) clientChat?.close()
        else client?.close()

        try {
            if (isChat) {
                if (clientChat == null) clientChat = WebSocketClient(application, END_POINT_CHAT, true)
            } else {
                if (client == null) client = WebSocketClient(application, END_POINT, false)
            }

            if (TTUApp.deviceConnected) {
                if (isChat) {
                    clientChat!!.connect()
                    LogUtils.d(logTag, "Device connected for CHAT")
                } else {
                    client!!.connect()
                    LogUtils.d(logTag, "Device connected")
                }
            } else {
                LogUtils.e(logTag, "NO CONNECTION AVAILABLE")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(logTag, e.message, e)
        }
        startCheckConnection()
    }

    fun closeConnection() {
        if (client != null) {
            client!!.close()
            client = null
        }
        if (clientChat != null) {
            clientChat!!.close()
            clientChat = null
        }
        stopCheckConnection()
    }

    /**
     * This method sends calls to server for socket /rtcom.
     * @param message the provided message to be sent.
     */
    fun sendMessage(message: String?): RequestStatus {
        LogUtils.d(logTag, "Original message: $message")
        return sendMessage(Crypto.encryptData(message))
    }

    private fun sendMessage(message: ByteArray?): RequestStatus {
        if (message != null && message.isNotEmpty()) {
            if (isConnected(false)) {
                client!!.webSocket?.sendBinary(message)
                LogUtils.d(logTag, "Encrypted Bytes sent: " + Arrays.toString(message))
                return RequestStatus.SENT
            }

            LogUtils.d(logTag, "Socket not connected. Bytes: " + Arrays.toString(message) + "\tNOT SENT")
            return RequestStatus.NO_CONNECTION
        }

        LogUtils.d(logTag, "Original bytes null or empty\tERROR")
        return RequestStatus.ERROR
    }

    /**
     * This method sends calls to server for socket /chat.
     * @param message the provided message to be sent.
     */
    fun sendMessageChat(message: String?): RequestStatus {
        LogUtils.d(logTag, "Original message: $message")
        return sendMessageChat(Crypto.encryptData(message))
    }

    private fun sendMessageChat(message: ByteArray?): RequestStatus {
        if (message != null && message.isNotEmpty()) {
            if (isConnectedChat(false)) {
                clientChat?.webSocket?.sendBinary(message)
                LogUtils.d(logTag, "Encrypted Bytes sent to CHAT: " + Arrays.toString(message))
                return RequestStatus.SENT
            }

            LogUtils.d(logTag, "Socket CHAT not connected. Bytes: " + Arrays.toString(message) + "\tCHAT NOT SENT")
            return RequestStatus.NO_CONNECTION
        }

        LogUtils.d(logTag, "Original bytes null or empty\tCHAT ERROR")
        return RequestStatus.ERROR
    }

    /**
     * This method is used to check if RTCOM socket is both connected or connected AND subscribed.
     * @param forCalls whether the check must be performed also on subscription.
     * @return The connection status of the socket.
     */
    fun isConnected(forCalls: Boolean): Boolean {
        val basicCondition = TTUApp.deviceConnected && (client?.hasOpenConnection() == true)

        return if (forCalls && TTUApp.hasValidCredentials)
            basicCondition && (client?.isSocketSubscribed == true)
        else
            basicCondition
    }

    /**
     * This method is used to check if CHAT socket is just connected or connected AND subscribed.
     * @param forCalls whether the check must be performed also on subscription.
     * @return The connection status of the socket.
     */
    fun isConnectedChat(forCalls: Boolean): Boolean {
        val basicCondition = TTUApp.deviceConnected && (clientChat?.hasOpenConnection() == true)

        return if (forCalls && TTUApp.hasValidCredentials)
            basicCondition && (clientChat?.isSocketSubscribed == true)
        else
            basicCondition
    }

    /**
     * It serves as only entry point for the socket subscription process, other than the [WebSocketAdapter] itself.
     * @param context the activity's/application's [Context]
     */
    fun subscribeSockets(context: Context) {
        client?.adapter?.subscribeSocketRTCom(context)
        clientChat?.adapter?.subscribeSocketChat(context)
    }

    /**
     * It serves as only entry point for the socket connection listeners.
     * @param observer the provided [rs.highlande.app.tatatu.connection.webSocket.WebSocketAdapter.ConnectionObserver]
     */
    fun attachConnectionObservers(observer: WebSocketAdapter.ConnectionObserver) {
        LogUtils.d(logTag, "testCONNECTION:\tattaching CONNECTION observer ($observer)")
        client?.adapter?.attachConnectionObserver(observer)
        clientChat?.adapter?.attachConnectionObserver(observer)
    }

    /**
     * It serves as only entry point for the socket subscription listeners.
     * @param observer the provided [rs.highlande.app.tatatu.connection.webSocket.WebSocketAdapter.SubscriptionObserver]
     */
    fun attachSubscriptionObservers(observer: WebSocketAdapter.SubscriptionObserver) {
        client?.adapter?.attachSubscriptionObserver(observer)
        clientChat?.adapter?.attachSubscriptionObserver(observer)
    }


    /**
     * This method is the common entry point to update the socket header OUTSIDE the socket's creation.
     *
     *
     * IMPORTANT: only disconnecting and reconnecting, headers can be successfully added.
     */
    fun updateSocketHeader() {
        // only disconnecting and reconnecting seem to do the trick with headers
        closeConnection()
        openConnection(false)
        openConnection(true)
    }


    override fun toString(): String {
        return this.hashCode().toString()
    }

    companion object {

        val logTag = WebSocketConnection::class.java.simpleName

        private val END_POINT = if (BuildConfig.USE_PROD_CONNECTION) SERVER_END_POINT_PROD else SERVER_END_POINT_DEV

        // TODO: 2019-12-16    restore normal STAGING endpoint
        private val END_POINT_CHAT =
            if (BuildConfig.USE_PROD_CONNECTION) SERVER_END_POINT_PROD_CHAT else SERVER_END_POINT_DEV_CHAT
    }
}
