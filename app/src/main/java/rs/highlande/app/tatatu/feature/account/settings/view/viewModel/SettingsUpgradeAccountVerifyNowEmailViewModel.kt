package rs.highlande.app.tatatu.feature.account.settings.view.viewModel

import android.view.View
import androidx.lifecycle.MutableLiveData
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.feature.createPost.view.getParentActivity

/**
 * Created by Abhin.
 */
class SettingsUpgradeAccountVerifyNowEmailViewModel : BaseViewModel() {
    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    fun onClick(view: View) {
        val activity = view.getParentActivity()!!
        when (view.id) {
            R.id.img_verify_account_email_close -> {
                activity.finish()
            }
            R.id.btn_verify_account_email -> {
                activity.finish()
            }
        }
    }
}