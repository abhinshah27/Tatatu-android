package rs.highlande.app.tatatu.feature.account.profile.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DELETE_PROFILE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_EDIT_PROFILE
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi

/**
 * Created by Abhin.
 */
class ProfileApi : BaseApi() {

    //call the Edit Profile
    fun getEditProfile(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_EDIT_PROFILE, logTag = "Edit Profile", caller = caller))
    }

    //call the Delete Profile
    fun getDeleteProfile(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_DELETE_PROFILE, logTag = "DELETE PROFILE", caller = caller))
    }

}