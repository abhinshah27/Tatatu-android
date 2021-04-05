package rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import org.koin.core.inject
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager

/**
 * Created by mbaldrighi on 2019-07-25.
 */
class SignUpWelcomeViewModel : BaseViewModel() {

    private val authManager: AuthManager by inject()

    /**
     * Event generated for next in button
     */
    fun onDone(activity: Activity, permissionComm: Boolean, permissionShare: Boolean) {

        showProgress.value = true

        authManager.latestSigningUser.apply {
            permissions.termsSigned = true
            permissions.is18Years = true
            permissions.marketingCommunication = permissionComm
            permissions.sharePersonalData = permissionShare
        }

        authManager.signupAuth0(activity)
    }


    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }
}