package rs.highlande.app.tatatu.feature.authentication.repository

import android.content.Context
import io.reactivex.Observable
import org.json.JSONObject
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.core.util.TTUAuthException
import rs.highlande.app.tatatu.feature.authentication.api.AuthApi
import rs.highlande.app.tatatu.model.User

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-26.
 */
class AuthRepository(private val context: Context) : BaseRepository() {

    private val api by inject<AuthApi>()
    private val preferences by inject<PreferenceHelper>()

    fun checkUserName(username: String): Observable<Pair<Boolean, TTUAuthException?>> = api.checkUsername(username)

    fun doLoginOrSignup(payload: JSONObject, authType: AuthManager.AuthType): Observable<Pair<User?, TTUAuthException?>> = api.doLoginOrSignup(payload, authType)

    fun doConfirmSocialSignup(payload: JSONObject): Observable<Pair<User?, TTUAuthException?>> = api.doConfirmSocial(payload)

    fun sendPasswordRecoveryEmail(email: String) = api.sendPasswordRecoveryEmail(email)

    fun logout(caller: OnServerMessageReceivedListener): Observable<RequestStatus> {
        return preferences.getDeviceToken()?.let {
            api.logout(it, caller)
        } ?: Observable.just(RequestStatus.ERROR)
    }

}