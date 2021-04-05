package rs.highlande.app.tatatu.feature.account.settings.repository

import android.os.Bundle
import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.feature.account.settings.api.SettingsApi

/**
 * Created by Abhin.
 */
class SettingsRepository : BaseRepository() {
    private val settingsApi: SettingsApi by inject()

    //Verify Account
    fun getVerifyAccount(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return settingsApi.getVerifyAccount(caller, bundle)
    }

    //Change Account Type
    fun getChangeAccountType(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return settingsApi.getChangeAccountType(caller, bundle)
    }

    //Block Account List
    fun getBlockAccountList(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return settingsApi.getBlockAccountList(caller, bundle)
    }

    //Remove Block Account List
    fun getRemoveBlockAccount(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return settingsApi.getRemoveBlockAccount(caller, bundle)
    }

    //Change Account Visibility public or private
    fun getAccountVisibility(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return settingsApi.getAccountVisibility(caller, bundle)
    }
}