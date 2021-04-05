package rs.highlande.app.tatatu.feature.authentication.repository

import android.app.Activity
import android.app.Dialog
import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.AuthenticationCallback
import com.auth0.android.callback.BaseCallback
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_LOGOUT
import rs.highlande.app.tatatu.connection.webSocket.WebSocketAdapter
import rs.highlande.app.tatatu.connection.webSocket.WebSocketConnection
import rs.highlande.app.tatatu.connection.webSocket.realTime.RTCommHelper
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.core.util.TTUAuthException
import rs.highlande.app.tatatu.model.User

/**
 * Class
 * @author mbaldrighi on 2019-07-23.
 */
class AuthManager(val context: Context) : KoinComponent, OnServerMessageReceivedListener {

    private val repository: AuthRepository by inject()
    private val preferences: PreferenceHelper by inject()
    private val socketConnection: WebSocketConnection by inject()

    private val realTimeHelper: RTCommHelper by inject()


    enum class AuthType(val value: String) {
        CUSTOM("TaTaTuConnection"),
        FACEBOOK("facebook"),
        GOOGLE("google-oauth2")
    }
    enum class AuthAction { LOGIN, SIGNUP }


    private val authScheme = context.getString(R.string.com_auth0_scheme)
    private val authAudience = context.getString(R.string.com_auth0_audience)


    private val auth0 = Auth0(context).apply { isOIDCConformant = true }
    private val authClient = AuthenticationAPIClient(auth0)

    private val secureManager by lazy {
        SecureCredentialsManager(
            context,
            authClient,
            SharedPreferencesStorage(context)
        )
    }

    private val nonSecureManager by lazy {
        CredentialsManager(
            authClient,
            SharedPreferencesStorage(context)
        )
    }


    var latestSigningUser = AccountingData()
    var latestCredentials: Credentials? = null
    val currentAuthType = { latestSigningUser.permissions.authType }


    val userAuthenticatedLogin = AuthPairObservable()
    val userAuthenticatedSignup = AuthPairObservable()
    val userAuthenticationError = ErrorObservable()


    private var useNonSecureManager = false

    var rememberUser = preferences.isRememberUser()
        set(value) {
            field = value
            preferences.rememberUser(value)
        }
    var rememberedAuth = preferences.getAuthString()
        private set(value) {
            field = value
            preferences.storeAuthString(value)
        }

    /**
     * Property needed because refreshToken is not returned every time BUT IT DOES NOT EXPIRE
     */
    private var refreshToken = preferences.getRefreshToken()

    private val credentialsObservers = mutableSetOf<CredentialListener?>()


    var onPauseForSocial = false


    private fun getCurrentAuthType() = latestSigningUser.permissions.authType


    //region = AUTH =

    private fun doAuthAction(activity: Activity, actionType: AuthAction) {

        if (getCurrentAuthType() == AuthType.CUSTOM) {

            if (actionType == AuthAction.LOGIN) {
                authClient
                    .login(
                        latestSigningUser.signupUserInfo.email,
                        latestSigningUser.signupUserInfo.password,
                        latestSigningUser.permissions.authType.value
                    )
                    .addAuthenticationParameters(latestSigningUser.registrationDataForAuth0.invoke())
            } else {
                authClient
                    .signUp(
                        latestSigningUser.signupUserInfo.email,
                        latestSigningUser.signupUserInfo.password,
                        latestSigningUser.userName,
                        latestSigningUser.permissions.authType.value
                    )
                    .addSignUpParameters(latestSigningUser.registrationDataForAuth0.invoke())
            }.apply {
                setAudience(authAudience)
                setScope(AUTH_SCOPE)
                start(object : AuthenticationCallback<Credentials> {

                    override fun onSuccess(payload: Credentials?) {

                        // Needs the call to print credentials and store them in local object to
                        // complete the signup process, but actual cred are stored in CredentialManager
                        // only if it's LOGIN
                        storeCredentials(payload, actionType == AuthAction.LOGIN)

                        if (actionType == AuthAction.LOGIN) {
                            rememberedAuth = if (rememberUser) latestSigningUser.signupUserInfo.email else ""
                            userAuthenticatedLogin.value = true to AuthType.CUSTOM
                        }
                        else userAuthenticatedSignup.value = true to AuthType.CUSTOM
                    }

                    override fun onFailure(error: AuthenticationException?) {
                        userAuthenticationError.e = error
                        LogUtils.e(logTag, error?.message ?: "Authentication CUSTOM FAILED")
                    }
                })
            }
        } else {

            // is Social auth
            WebAuthProvider.login(auth0)
                .withScheme(authScheme)
                .withConnection(getCurrentAuthType().value)
                .withAudience(authAudience)
                .withScope(AUTH_SCOPE)
                .withParameters(latestSigningUser.registrationDataForAuth0.invoke()).apply {

                    // start authentication
                    start(activity, object : AuthCallback {

                        override fun onSuccess(credentials: Credentials) {

                            // INFO: 2019-08-26    behavior changed: with SOCIAL cannot be true
                            rememberUser = false

                            storeCredentials(credentials)

                            if (actionType == AuthAction.LOGIN)
                                userAuthenticatedLogin.value = true to latestSigningUser.permissions.authType
                            else {
                                userAuthenticatedSignup.value = true to latestSigningUser.permissions.authType
                            }

                            onPauseForSocial = false
                        }

                        override fun onFailure(dialog: Dialog) {}

                        override fun onFailure(exception: AuthenticationException?) {
                            userAuthenticationError.e = exception
                            LogUtils.e(
                                logTag,
                                exception?.message ?: "Authentication SOCIAL(${getCurrentAuthType().value}) FAILED"
                            )

                            onPauseForSocial = false
                        }
                    })
                }

            onPauseForSocial = true
        }

    }

    fun loginAuth0(activity: Activity) = doAuthAction(
        activity,
        AuthAction.LOGIN
    )

    fun signupAuth0(activity: Activity) = doAuthAction(
        activity,
        AuthAction.SIGNUP
    )


    private fun loginOrSignup(isLogin: Boolean): Observable<Pair<User?, TTUAuthException?>> {

        val authType = latestSigningUser.permissions.authType

        return if (isLogin || authType == AuthType.CUSTOM) {

            LogUtils.d(logTag, "testNEWAUTH: isLogin=$isLogin  authType=$authType  >>>  repository.doLoginOrSignup(...)")

            // if login or signup custom use "doLoginOrSignup" method with internal conditioning
            repository.doLoginOrSignup(
                latestSigningUser.getDataForTatatuSignup(
                    isLogin,
                    latestCredentials?.accessToken ?: ""
                ),
                authType
            )
        } else {

            LogUtils.d(logTag, "testNEWAUTH: isLogin=$isLogin  authType=$authType  >>>  repository.doConfirmSocialSignup(...)")

            // if signup social use "doConfirmSocialSignup" method
            repository.doConfirmSocialSignup(
                latestSigningUser.getDataForTatatuSignup(
                    isLogin,
                    latestCredentials?.accessToken ?: ""
                )
            )
        }
    }

    fun completeLogin(): Observable<Pair<User?, TTUAuthException?>> {
        return loginOrSignup(true)
    }

    fun completeSignup(): Observable<Pair<User?, TTUAuthException?>> {
        return loginOrSignup(false)
    }


    fun logout() {

        callLogout()

        latestSigningUser = AccountingData()

        clearCredentials()

        preferences.clear()
        TTUApp.hasValidCredentials = false

        RealmUtils.doRealmLogout()

        // INFO: 2019-10-03    closeConnection() invocation moved after logout call.
//        socketConnection.closeConnection()
    }


    fun storeFieldsForSignup(invitationId: String, inviterToken: String) {
        latestSigningUser.permissions.invitationId = invitationId
        latestSigningUser.permissions.inviterToken = inviterToken
    }

    fun resetInvitation() {
        latestSigningUser.permissions.invitationId = ""
        latestSigningUser.permissions.inviterToken = ""
    }

    fun hasPendingInvitation() = latestSigningUser.permissions.hasPendingInvitation()


    //endregion


    //region = Server communication =

    private fun callLogout() {
        val disposable = repository.logout(this@AuthManager)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribeWith(object : DisposableObserver<RequestStatus>() {
                override fun onComplete() {
                    dispose()
                }

                override fun onNext(t: RequestStatus) {
                    if (t != RequestStatus.SENT) socketConnection.closeConnection()
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    socketConnection.closeConnection()
                    dispose()
                }
            })
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        if (callCode == SERVER_OP_LOGOUT) {
            LogUtils.d(logTag, "LOGOUT call: SUCCESS")

            socketConnection.closeConnection()
        }
    }

    override fun handleErrorResponse(
        idOp: String,
        callCode: Int,
        errorCode: Int,
        description: String?
    ) {
        if (callCode == SERVER_OP_LOGOUT) {
            LogUtils.e(logTag, "LOGOUT call: ERROR")

            socketConnection.closeConnection()
        }
    }

    //endregion


    //region = CREDENTIALS =

    private fun storeCredentials(credentials: Credentials?, saveToCredManager: Boolean = true) {

        if (credentials == null) {
            LogUtils.e(logTag, "Authentication FAILED with null CREDENTIALS")
            return
        }

        LogUtils.d(
            logTag, "(0) Credentials =\n" +
                    "\taccessToken : ${credentials.accessToken}" +
                    "\ttokenType : ${credentials.type}" +
                    "\trefreshToken : ${credentials.refreshToken}" +
                    "\texpiresIn : ${credentials.expiresIn}"
        )

        if (!credentials.refreshToken.isNullOrBlank()) {
            preferences.storeRefreshToken(credentials.refreshToken!!)
            refreshToken = credentials.refreshToken!!
        }

        latestCredentials = credentials


        // INFO: 2019-08-22    assigning here really makes sure that the credentials are valid
        TTUApp.hasValidCredentials = true


        if (saveToCredManager) {
            // INFO: 2019-09-30    reverts fix to get again analytics about the crash
//            try {
            if (useNonSecureManager)
                nonSecureManager.saveCredentials(credentials)
            else
                secureManager.saveCredentials(credentials)
//            }
//            catch (e: CredentialsManagerException) {
//                if (e.cause is CryptoException) {
//                    if (useNonSecureManager)
//                        nonSecureManager.saveCredentials(credentials)
//                    else
//                        secureManager.saveCredentials(credentials)
//                }
//            }
        }
    }

    private fun clearCredentials() {
        if (useNonSecureManager)
            nonSecureManager.clearCredentials()
        else
            secureManager.clearCredentials()
    }


    fun checkAndFetchCredentials() {

        secureManager.getCredentials(
            object : BaseCallback<Credentials, CredentialsManagerException> {
                override fun onSuccess(payload: Credentials?) {
                    if (payload == null) {
                        throw IllegalArgumentException("Credential management FAILED: NULL")
                    }

                    storeCredentials(payload)

                    notifyCredentialsObservers(CredentialsResult.SUCCESS)

                    if (!onPauseForSocial) {
                        openSockets(object : WebSocketAdapter.ConnectionObserver {
                            override fun onConnectionEstablished(isChat: Boolean) {
                                LogUtils.d(
                                    logTag,
                                    "Socket ${if (isChat) "CHAT" else "RTCOM"} connected after TOKEN REFRESH in SECURE"
                                )
                            }
                        })
                    }
                }

                override fun onFailure(error: CredentialsManagerException?) {
                    LogUtils.e(logTag, error?.message ?: "Credential management FAILED")

                    if (error?.isDeviceIncompatible == true) {
                        useNonSecureManager = true
                        checkAndFetchNonSecureCredentials()
                    } else {
                        notifyCredentialsObservers(CredentialsResult.FAILURE)
                    }
                }
            }
        )
    }

    private fun checkAndFetchNonSecureCredentials(): Boolean {

        val hasValid = nonSecureManager.hasValidCredentials()

        nonSecureManager.getCredentials(
            object : BaseCallback<Credentials, CredentialsManagerException> {
                override fun onSuccess(payload: Credentials?) {
                    if (payload == null) {
                        throw IllegalArgumentException("Credential management FAILED: NULL")
                    }

                    storeCredentials(payload)

                    notifyCredentialsObservers(CredentialsResult.SUCCESS)

                    openSockets(object : WebSocketAdapter.ConnectionObserver {
                        override fun onConnectionEstablished(isChat: Boolean) {
                            LogUtils.d(logTag, "Socket ${if (isChat) "CHAT" else "RTCOM"} connected after TOKEN REFRESH in SECURE")
                        }
                    })
                }

                override fun onFailure(error: CredentialsManagerException?) {
                    LogUtils.e(logTag, error?.message ?: "Credential management FAILED")
                    notifyCredentialsObservers(CredentialsResult.FAILURE)
                }
            }
        )

        return hasValid
    }


    // TODO: 2019-08-30    RETURN HERE
    fun refreshCredentials() {
        if (refreshToken.isNotBlank()) {
            authClient
                .renewAuth(refreshToken)
                .addParameter("scope", "openid offline_access")
                .start(
                    object : BaseCallback<Credentials, AuthenticationException> {
                        override fun onSuccess(payload: Credentials?) {
                            if (payload == null) {
                                throw IllegalArgumentException("Credential management FAILED: NULL")
                            }

                            storeCredentials(payload)

                            notifyCredentialsObservers(CredentialsResult.SUCCESS)

                            openSockets(object : WebSocketAdapter.ConnectionObserver {
                                override fun onConnectionEstablished(isChat: Boolean) {
                                    LogUtils.d(logTag, "Socket ${if (isChat) "CHAT" else "RTCOM"} connected after TOKEN REFRESH in SECURE")
                                }
                            })
                        }
                        override fun onFailure(error: AuthenticationException?) {
                            LogUtils.e(logTag, error?.message ?: "Credential management FAILED")
                            notifyCredentialsObservers(CredentialsResult.FAILURE)
                        }
                    }
                )
        } else notifyCredentialsObservers(CredentialsResult.FAILURE)
    }

    enum class CredentialsResult { SUCCESS, FAILURE }
    private fun notifyCredentialsObservers(result: CredentialsResult) {
        if (result == CredentialsResult.SUCCESS)
            LogUtils.d(logTag, "(2) CREDENTIALS: notifying listeners with validCredentials=${TTUApp.hasValidCredentials}")
        else
            LogUtils.d(logTag, "CREDENTIALS: notifying listeners with FAILURE")

        val disposable = Observable
            .fromIterable(credentialsObservers)
            .subscribeOn(Schedulers.trampoline())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<CredentialListener?>() {
                override fun onComplete() {
                    dispose()
                }

                override fun onNext(t: CredentialListener) {
                    t.onCredentialsFetched(result)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    dispose()
                }
            })
    }

    fun addCredentialObserver(observer: CredentialListener?) = credentialsObservers.add(observer)
    fun removeCredentialObserver(observer: CredentialListener?) = credentialsObservers.remove(observer)


    fun doesNeedTokenRefresh(): Boolean {
        return /*latestCredentials?.expiresIn?.let {
            it < 600
        } ?:*/ true
    }

    //endregion


    internal fun openSockets(observer: WebSocketAdapter.ConnectionObserver) {

        if (!TTUApp.socketConnecting && !TTUApp.socketConnectingChat) {

            // needs to close connection to allow socket rebuild with new headers
            socketConnection.closeConnection()

            TTUApp.socketConnecting = true
            TTUApp.socketConnectingChat = true

            val disposable = Observable
                .just(true)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Boolean>() {
                    override fun onComplete() {
                        dispose()
                    }

                    override fun onNext(t: Boolean) {

                        LogUtils.d(logTag, "Connecting socket from AuthManager.checkCredentials")

                        socketConnection.openConnection(false)
                        socketConnection.openConnection(true)
                        socketConnection.attachConnectionObservers(observer)
                    }

                    override fun onError(e: Throwable) {
                        dispose()
                    }
                })
        }
    }


    companion object {

        val logTag = AuthManager::class.java.simpleName

        const val AUTH_SCOPE = "offline_access"

    }


    class BooleanObservable : java.util.Observable() {
        var value = false
            set(value) {
                field = value
                setChanged()
                notifyObservers(value)
            }
    }

    class AuthPairObservable : java.util.Observable() {
        var value = false to AuthType.CUSTOM
            set(value) {
                field = value
                setChanged()
                notifyObservers(value)
            }
    }

    class ErrorObservable : java.util.Observable() {
        var e: Exception? = null
            set(value) {
                field = value
                setChanged()
                notifyObservers(value)
            }

    }


    interface CredentialListener {
        fun onCredentialsFetched(result: CredentialsResult)
    }

}