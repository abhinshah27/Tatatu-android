package rs.highlande.app.tatatu.feature.wallet.repository

import android.os.Bundle
import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.feature.wallet.api.WalletApi

/**
 * Created by Abhin.
 */
class WalletRepository : BaseRepository() {
    private val mWalletApi: WalletApi by inject()

    //get Currency Conversion Rate
    fun getCurrencyConversionRate(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return mWalletApi.getCurrencyConversionRate(caller, bundle)
    }
}