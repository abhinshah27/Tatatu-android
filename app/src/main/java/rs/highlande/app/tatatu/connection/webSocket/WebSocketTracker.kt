package rs.highlande.app.tatatu.connection.webSocket

import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import io.reactivex.Observable
import org.json.JSONException
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.realTime.RTCommHelper
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.areStringsValid
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Class holder of all the methods and logic behind the web socket server calls storing, retrying,
 * and dispatching the related [OnServerMessageReceivedListener] caller instance.
 *
 * // INFO: 2019-09-02    The logic for the handling of the auth/earning calls is present in the form of comments or unused methods.
 *
 * @author mbaldrighi on 2019-07-11.
 */
class WebSocketTracker : KoinComponent {

    private val rtCommHelper: RTCommHelper by inject()

    private val socketConnection: WebSocketConnection by inject()

    private val authManager: AuthManager by inject()

    private val requests = ConcurrentHashMap<String, SocketRequest>()
    private val requestsChat = ConcurrentHashMap<String, SocketRequest>()
    private val requestsAuth = ConcurrentHashMap<String, SocketRequest>()


    @Synchronized
    fun callServer(request: SocketRequest, isChat: Boolean = false): Observable<RequestStatus> {
        val status =
            if (isChat) socketConnection.sendMessageChat(request.body)
            else socketConnection.sendMessage(request.body)

        // ERROR status will just repeat error if retried
        if (status != RequestStatus.ERROR) storeId(request)

        return Observable.just(status)
    }

    @Synchronized
    fun callServerChat(request: SocketRequest): Observable<RequestStatus> {
        return callServer(request, true)

    }


    @Synchronized
    private fun storeId(request: SocketRequest) {
        if (!areStringsValid(request.id, request.body))
            return

        val map = if (request.isChat) requestsChat else requests
        map[request.id] = request

        LogUtils.d("TEST SOCKET", "map size = ${map.size}")
    }


    @Synchronized
    fun removeAfterCheck(id: String): OnServerMessageReceivedListener? {
        if (requests.isEmpty() && requestsChat.isEmpty())
            return null

        var caller: OnServerMessageReceivedListener? = null

        try {
            if (requests.containsKey(id)) {
                caller = requests[id]?.getCaller()
                requests.remove(id)
                LogUtils.d("TEST SOCKET", "map size = ${requests.size}")
            }

            if (requestsChat.containsKey(id)) {
                caller = requestsChat[id]?.getCaller()
                requestsChat.remove(id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(LOG_TAG, e.message, e)
        }


        return caller
    }


    @Synchronized
    private fun moveAuthRequest(id: String) {
        if (requests.isEmpty() && requestsChat.isEmpty()) return

        try {
            if (requests.containsKey(id)) {
                requests[id]?.let {
                    requestsAuth[id]
                    requests.remove(id)
                }
                LogUtils.d("TEST SOCKET", "map size = ${requests.size}")
            }

            if (requestsChat.containsKey(id)) {
                requests[id]?.let {
                    requestsAuth[id]
                    requestsChat.remove(id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(LOG_TAG, e.message, e)
        }
    }

    @Synchronized
    fun checkPendingAndRetry(isChat: Boolean) {
        LogUtils.d(LOG_TAG, "Inside checkPending... method")

//        retryAuthRequests(isChat)
//
//        LogUtils.d(LOG_TAG, "Retry non-Auth requests")

        val map: Map<String, SocketRequest> = if (isChat) requestsChat else requests
        try {
            if (map.isNotEmpty()) {
                for (it in map.entries) {
                    val r = it.value
                    if (r.isValid) {
                        if (r.isChat)
                            socketConnection.sendMessageChat(r.body)
                        else
                            socketConnection.sendMessage(r.body)
                    }
                    else (map as ConcurrentHashMap).remove(r.id)
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(LOG_TAG, e.message, e)
        }
    }


    @Synchronized
    private fun retryAuthRequests(isChat: Boolean) {
        LogUtils.d(LOG_TAG, "Retry Auth request (isChat = $isChat)")

        requestsAuth.forEach {
            val r = it.value
            if (r.isValid) {
                if (isChat && r.isChat)
                    socketConnection.sendMessageChat(r.body)
                else
                    socketConnection.sendMessage(r.body)
            }
            else (requestsAuth).remove(r.id)
        }
    }


    internal fun onDataReceivedAsync(data: ByteArray?) {
        val ht = HandlerThread("decryptAndProcessData")
        ht.start()
        Handler(ht.looper).post {
            try {
                onDataReceived(data)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            ht.quitSafely()
        }
    }

    @Throws(JSONException::class)
    private fun onDataReceived(data: ByteArray?) {
        val newData = Crypto.decryptData(data)
        onDataReceived(newData)
    }

    internal fun onDataReceivedAsync(data: String?) {
        val ht = HandlerThread("decryptAndProcessData")
        ht.start()
        Handler(ht.looper).post {
            try {
                onDataReceived(data)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            ht.quitSafely()
        }
    }


    @Throws(JSONException::class)
    private fun onDataReceived(data: String?) {
        if (TextUtils.isEmpty(data))
            return

        LogUtils.d(LOG_TAG, "Binary data converted to: " + data!!)

        val response = SocketResponse(data)

//        if (response.is401Error) {
//
//            // move request from common map to AuthReqs map
//            moveAuthRequest(response.id)
//
//            // need to get new credentials
//            authManager.checkAndFetchCredentials()
//
//        } else {
            if (response.isValid) {
                val caller = removeAfterCheck(response.id)

                if (response.isError) {
                    val error = response.error
                    (caller ?: rtCommHelper).handleErrorResponse(
                        response.id,
                        response.callCode!!,
                        error.first,
                        error.second
                    )
                } else {
                    (caller ?: rtCommHelper).handleSuccessResponse(
                        response.id,
                        response.callCode!!,
                        response.jsonResponse
                    )
                }
            }
//        }
    }

    private fun getRequest(id: String): SocketRequest? {
        var req: SocketRequest? = null
        if (!TextUtils.isEmpty(id)) {
            if (requests.containsKey(id)) {
                req = requests[id]
                requests.remove(id)
            } else if (requestsChat.containsKey(id)) {
                req = requestsChat[id]
                requestsChat.remove(id)
            }
        }

        return req
    }

    companion object {
        val LOG_TAG = WebSocketTracker::class.java.simpleName
    }

}
