package rs.highlande.app.tatatu.connection.webSocket

import android.content.Context
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketExtension
import com.neovisionaries.ws.client.WebSocketFactory
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.getSecureID
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.core.util.TIME_UNIT_SECOND
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import java.io.IOException
import java.util.*

/**
 * Instance of the web socket client.
 *
 * It handles the instantiation of [WebSocket] and [WebSocketAdapter] objects.
 *
 * @author mbaldrighi on 2019-07-10.
 */
class WebSocketClient internal constructor(
    private val application: Context,
    private val host: String,
    private val isChat: Boolean
) : KoinComponent {

    private val authManager: AuthManager by inject()

    private val preferenceHelper: PreferenceHelper by inject()

    internal var webSocket: WebSocket? = null
        private set

    internal var adapter: WebSocketAdapter? = null
        private set

    internal val isSocketSubscribed: Boolean
        get() = adapter != null && (adapter!!.isRtComSubscribed || adapter!!.isChatSubscribed)



    private fun getUserId(): String {
        preferenceHelper.getUserId().let {
            LogUtils.d(LOG_TAG, "Socket header: \"$SOCKET_HEADER_ID\":\"$it\"")
            return it
        }
    }
    private fun getCurrentToken(): String {
        (authManager.latestCredentials?.accessToken ?: "").let {
            LogUtils.d(LOG_TAG, "Socket header: \"$SOCKET_HEADER_TOKEN\":\"$it\"")
            return it
        }
    }
    private fun getTTUId(): String {
        preferenceHelper.getUserTTUId().let {
            LogUtils.d(LOG_TAG, "Socket header: \"$SOCKET_HEADER_TTU_ID\":\"$it\"")
            return it
        }
    }
    private fun getLang(): String {
        Locale.getDefault().language.let {
            LogUtils.d(LOG_TAG, "Socket header: \"$SOCKET_HEADER_LANG\":\"$it\"")
            return it
        }
    }
    private fun getSecureId(): String {
        getSecureID(application).let {
            LogUtils.d(LOG_TAG, "Socket header: \"$SOCKET_HEADER_DEVICE\":\"$it\"")
            return it
        }
    }


    init {
        LogUtils.d(LOG_TAG, "TEST SOCKET Client ID: $this")
    }

    internal fun connect() {
        try {
            if (webSocket == null) {
                webSocket = WebSocketFactory().createSocket(host, TIMEOUT)

                adapter = WebSocketAdapter(application, isChat).apply { webSocket!!.addListener(this) }

                webSocket!!.addHeader(SOCKET_HEADER_ID, getUserId())
                webSocket!!.addHeader(SOCKET_HEADER_TTU_ID, getTTUId())
                webSocket!!.addHeader(SOCKET_HEADER_DEVICE, getSecureId())
                webSocket!!.addHeader(SOCKET_HEADER_TOKEN, getCurrentToken())
                webSocket!!.addHeader(SOCKET_HEADER_LANG, getLang())
                webSocket!!.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                webSocket!!.pingInterval = TIME_UNIT_SECOND * 60
                webSocket!!.connectAsynchronously()

                LogUtils.d(LOG_TAG, "TEST SOCKET WebSocket ID: " + webSocket!!.toString())
            } else
                reconnect()
        } catch (e: IOException) {
            e.printStackTrace()
            LogUtils.e(LOG_TAG, e.message ?: "")
        }

    }

    @Throws(IOException::class)
    private fun reconnect() {
        webSocket = webSocket!!.recreate().connectAsynchronously()

        LogUtils.d(LOG_TAG, "WebSocket ID: " + webSocket!!.toString())
    }

    fun close() {
        webSocket?.disconnect()
    }

    internal fun hasOpenConnection(): Boolean {
        return webSocket != null && webSocket!!.isOpen
    }

    companion object {

        private const val SOCKET_HEADER_ID = "x-id"
        private const val SOCKET_HEADER_TTU_ID = "x-ttu-id"
        private const val SOCKET_HEADER_DEVICE = "x-device-id"
        private const val SOCKET_HEADER_TOKEN = "x-token-id"
        private const val SOCKET_HEADER_LANG = "x-lang"
        val LOG_TAG = WebSocketClient::class.java.simpleName
        private const val TIMEOUT = 3000
    }

}
