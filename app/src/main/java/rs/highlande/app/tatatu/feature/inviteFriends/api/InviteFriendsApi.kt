package rs.highlande.app.tatatu.feature.inviteFriends.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DO_SEND_INVITATION
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_INVITATION_LINK
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi

/**
 * Created by Abhin.
 */
class InviteFriendsApi : BaseApi() {

    //Call the Get Invitation Link
    fun getInvitationLink(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_GET_INVITATION_LINK, logTag = "Get Invitation Link", caller = caller))
    }

    //Call Do Send Invitation
    fun doSendInvitation(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_DO_SEND_INVITATION, logTag = "Do Send Invitation", caller = caller))
    }
}