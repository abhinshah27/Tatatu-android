package rs.highlande.app.tatatu.feature.home.api

import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_HOME
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-15.
 */
class HomeApi : BaseApi() {

    fun getHomeContent(caller: OnServerMessageReceivedListener): Observable<RequestStatus> {
        return tracker.callServer(
            SocketRequest(
                null,
                callCode = SERVER_OP_GET_HOME,
                logTag = "GET HOME CONTENT call",
                caller = caller
            )
        )
    }

}