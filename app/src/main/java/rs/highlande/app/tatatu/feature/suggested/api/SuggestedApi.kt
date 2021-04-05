package rs.highlande.app.tatatu.feature.suggested.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_SUGGESTED
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_SUGGESTED_V2
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-16.
 */
class SuggestedApi : BaseApi() {

    @Deprecated("Use getSuggestedV2(caller: OnServerMessageReceivedListener, skip: Int?, limit: Int?) instead.")
    fun getSuggested(caller: OnServerMessageReceivedListener, skip: Int? = null, limit: Int? = null): Observable<RequestStatus> {
        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putInt("skip", skip ?: 0)
                    if (limit != null) putInt("limit", limit)
                },
                callCode = SERVER_OP_GET_SUGGESTED,
                logTag = "GET SUGGESTED call",
                caller = caller
            )
        )
    }

    fun getSuggestedV2(caller: OnServerMessageReceivedListener, skip: Int? = null, limit: Int? = null): Observable<RequestStatus> {
        return tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putInt("skip", skip ?: 0)
                    if (limit != null) putInt("limit", limit)
                },
                callCode = SERVER_OP_GET_SUGGESTED_V2,
                logTag = "GET SUGGESTED call",
                caller = caller
            )
        )
    }

}


const val LIMIT_SUGGESTED_AFTER_FOLLOW = 5