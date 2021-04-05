package rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel

import androidx.lifecycle.MutableLiveData
import rs.highlande.app.tatatu.core.ui.BaseViewModel

/**
 * Created by Abhin.
 */

class EmailBottomSheetViewModel : BaseViewModel() {
    var mEmail: String = ""

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }
}