package rs.highlande.app.tatatu.feature.account.settings.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.api.BaseApi

/**
 * Created by Abhin.
 */
class SettingsApi : BaseApi() {

    //call the verify Account
    fun getVerifyAccount(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_ST_VERIFY_ACCOUNT, logTag = "Settings Verify Account", caller = caller))
    }

    //Change Account
    fun getChangeAccountType(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_ST_CHANGE_ACCOUNT_TYPE, logTag = "Settings Upgrade Account", caller = caller))
    }

    //Block Account List
    fun getBlockAccountList(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_ST_GET_BLACKLIST, logTag = "Settings Block Account List", caller = caller))
    }

    //Remove Block Account
    fun getRemoveBlockAccount(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_ST_REMOVE_BLACKLIST, logTag = "Settings Remove Block Account", caller = caller))
    }

    //Change Account Visibility
    fun getAccountVisibility(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_ST_CHANGE_ACCOUNT_VISIBILITY, logTag = "Settings Change Account Visibility", caller = caller))
    }
}