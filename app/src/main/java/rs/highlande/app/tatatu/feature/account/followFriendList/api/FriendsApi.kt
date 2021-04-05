package rs.highlande.app.tatatu.feature.account.followFriendList.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_USERS_INVITED
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi

/**
 * Created by Abhin.
 */
class FriendsApi : BaseApi() {

    //Call the Get Users Invited
    fun getUsersInvited(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_GET_USERS_INVITED, logTag = "Get Users Invited", caller = caller))
    }
}