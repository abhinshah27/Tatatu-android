package rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.repository.ContactHelper
import rs.highlande.app.tatatu.model.ContactList

/**
 * Created by Abhin.
 */

class ContactViewModel : BaseViewModel() {

    var mList = MutableLiveData<ArrayList<ContactList>>()
    var mNameList = MutableLiveData<ArrayList<String>>()

    fun loadContact(mContext: Context, mFragment: Fragment) {
        ContactHelper(mContext).getAllContacts(mFragment, object : ContactHelper.ContactLoadComplete {
            override fun getAllContact(mContactArrayList: ArrayList<ContactList>, mNameArrayList: ArrayList<String>) {
                mList.value = mContactArrayList
                mNameList.value = mNameArrayList
            }
        })
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(mList, mNameList)
    }
}