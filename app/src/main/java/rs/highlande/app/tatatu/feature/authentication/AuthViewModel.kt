package rs.highlande.app.tatatu.feature.authentication

import androidx.lifecycle.MutableLiveData
import io.reactivex.schedulers.Schedulers
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.WebSocketAdapter
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.core.util.TTUAuthException
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.authentication.repository.AuthRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import java.util.*

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-25.
 */
class AuthViewModel : BaseViewModel() {

    private val authManager by inject<AuthManager>()
    private val authRepo by inject<AuthRepository>()
    private val userRepo by inject<UsersRepository>()
    private val preferences by inject<PreferenceHelper>()

    val userReceived: MutableLiveData<Pair<Boolean, AuthManager.AuthType?>> = mutableLiveData(false to null)
    val errorOnAuth = mutableLiveData<Triple<Boolean, Exception?, AuthManager.AuthType>>(Triple(false, null, AuthManager.AuthType.CUSTOM))

    val showProgressSocial: MutableLiveData<Pair<Boolean, AuthManager.AuthAction?>> = mutableLiveData(false to null)


    private val loginObserver = { p0: Observable, _: Any ->
        val value = (p0 as AuthManager.AuthPairObservable).value

        LogUtils.d(logTag, "userAuthenticatedLogin = $value")

        if (value.second != AuthManager.AuthType.CUSTOM)
            showProgressSocial.postValue(true to AuthManager.AuthAction.LOGIN)

        if (value.first) {
            completeAuth(true)
        }
    }

    private val signupObserver = { p0: Observable, _: Any ->

        val value = (p0 as AuthManager.AuthPairObservable).value

        LogUtils.d(logTag, "userAuthenticatedSignup = $value")

        if (value.second != AuthManager.AuthType.CUSTOM)
            showProgressSocial.postValue(true to AuthManager.AuthAction.SIGNUP)

        if (value.first) {
            completeAuth(false, value.second)
        }
    }

    private val errorObserver = { p0: Observable, _: Any ->

        val value = (p0 as AuthManager.ErrorObservable)

        LogUtils.e(logTag, "userAuthenticationError = ${value.e?.message}")

        errorOnAuth.postValue(Triple(true, value.e, authManager.currentAuthType()))

    }


    init {
        authManager.userAuthenticatedLogin.addObserver(loginObserver)
        authManager.userAuthenticatedSignup.addObserver(signupObserver)
        authManager.userAuthenticationError.addObserver(errorObserver)
    }


    private fun completeAuth(isLogin: Boolean, authType: AuthManager.AuthType? = null) {
        val obs = if (isLogin)
            authManager.completeLogin()
        else
            authManager.completeSignup()

        addDisposable(
            obs.observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(
                    {
                        if (it.second != null) {
                            if (!(it.second as TTUAuthException).isSocialToConfirmException()) authManager.logout()
                            errorOnAuth.postValue(Triple(true, it.second, authManager.currentAuthType()))
                        }
                        else {
                            it.first?.let { user ->
                                userRepo.cacheUser(user)

                                if (isLogin) {
                                    // user is cached -> open sockets
                                    authManager.openSockets(object : WebSocketAdapter.ConnectionObserver {
                                        override fun onConnectionEstablished(isChat: Boolean) {
                                            if (!isChat) {
                                                LogUtils.d(logTag, "Socket Connection RECEIVED for LOGIN")
                                                userReceived.postValue(true to authType)
                                            }
                                        }
                                    })
                                }
                                else {
                                    if (authType != AuthManager.AuthType.CUSTOM) {
                                        // user is cached -> open sockets
                                        authManager.openSockets(object : WebSocketAdapter.ConnectionObserver {
                                            override fun onConnectionEstablished(isChat: Boolean) {
                                                if (!isChat) {
                                                    LogUtils.d(logTag, "Socket Connection RECEIVED for SOCIAL SIGNUP")
                                                    userReceived.postValue(true to authType)
                                                }
                                            }
                                        })
                                    } else userReceived.postValue(true to authType)
                                }

                                return@subscribe
                            }

                            authManager.logout()
                            errorOnAuth.postValue(Triple(true, it.second, authManager.currentAuthType()))
                        }
                    },
                    {
                        authManager.logout()
                        errorOnAuth.postValue(Triple(true, null, authManager.currentAuthType()))
                    }
                )
        )
    }


    override fun getAllObservedData(): Array<MutableLiveData<*>> = arrayOf(userReceived, errorOnAuth, showProgressSocial)


    override fun onCleared() {

        authManager.userAuthenticatedLogin.deleteObserver(loginObserver)
        authManager.userAuthenticatedSignup.deleteObserver(signupObserver)
        authManager.userAuthenticationError.deleteObserver(errorObserver)

        super.onCleared()
    }


    companion object {
        val logTag = AuthViewModel::class.java.simpleName
    }

}