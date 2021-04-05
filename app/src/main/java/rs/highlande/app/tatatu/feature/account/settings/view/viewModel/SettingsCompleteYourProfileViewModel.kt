package rs.highlande.app.tatatu.feature.account.settings.view.viewModel

import android.view.View
import androidx.lifecycle.MutableLiveData
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.createPost.view.getParentActivity

/**
 * Created by Abhin.
 */
class SettingsCompleteYourProfileViewModel : BaseViewModel() {

    val mErrorShow = mutableLiveData(-1)

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    fun onClick(view: View) {
        val activity = view.getParentActivity()!!
        when (view.id) {
            R.id.img_complete_your_profile_close -> {
                activity.supportFragmentManager.popBackStack()
            }
            R.id.btn_complete_profile_update_profile -> {
                mErrorShow.value = 101
            }
            R.id.btn_complete_profile_update_later -> {
                mErrorShow.value = 103
            }
        }
    }
}