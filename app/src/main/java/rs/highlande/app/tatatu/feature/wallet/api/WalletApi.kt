package rs.highlande.app.tatatu.feature.wallet.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi

/**
 * Created by Abhin.
 */
class WalletApi : BaseApi() {

    //Call the GetCurrencyConversionRate
    fun getCurrencyConversionRate(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return tracker.callServer(SocketRequest(bundle, callCode = SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE, logTag = "Get Currency Conversion Rate", caller = caller))
    }
}