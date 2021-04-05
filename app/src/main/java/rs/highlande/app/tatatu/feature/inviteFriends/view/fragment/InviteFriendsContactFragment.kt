package rs.highlande.app.tatatu.feature.inviteFriends.view.fragment


import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_invite_friends_contact.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.KoinComponent
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.PermissionHelper
import rs.highlande.app.tatatu.core.util.getFormattedPhoneNumber
import rs.highlande.app.tatatu.feature.inviteFriends.view.adapter.ContactAdapter
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.ContactViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.InviteFriendsViewModel
import rs.highlande.app.tatatu.model.ContactList


class InviteFriendsContactFragment : BaseFragment(), KoinComponent {

    private val mPermissionRequestCode = 10004
    private var mList = ArrayList<ContactList>()
    private var mAdapter: ContactAdapter? = null

    private val mContactViewModel: ContactViewModel by sharedViewModel()
    private val mInviteFriendsViewModel: InviteFriendsViewModel by sharedViewModel()


    private var permissionHelper: PermissionHelper? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var isPermissionDeniedBySystem: Boolean = false
    private var isPermissionDenied: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        mContactViewModel.mList.observe(this, Observer { showData(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_invite_friends_contact, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initRecyclerView()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(InviteFriendsFragment.logTag, "Contacts")

        if (!isPermissionDeniedBySystem && !isPermissionDenied) initPermissions()
        showData(mContactViewModel.mList.value)
    }

    private fun initPermissions() {
        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.READ_CONTACTS), mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                isPermissionDeniedBySystem = false
                isPermissionDenied = false
                if (mList.isNullOrEmpty()) mContactViewModel.loadContact(context!!, this@InviteFriendsContactFragment)
            }

            override fun onPermissionDenied() {
                isPermissionDenied = true
            }

            override fun onPermissionDeniedBySystem() {
                isPermissionDeniedBySystem = true
            }
        })
    }

    //init RecyclerView
    private fun initRecyclerView() {
        mLayoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
        rv_contact.layoutManager = mLayoutManager!!
        mAdapter = ContactAdapter(mList, object : ContactAdapter.ItemClickListener {
            override fun itemClick(position: Int) {
                openDefaultMessage(mList[position].number)
            }
        })
        rv_contact.adapter = mAdapter
    }

    fun openDefaultMessage(phoneNumber: String = "") {
        if (!mInviteFriendsViewModel.mInvitationLink.value.isNullOrEmpty()) {
            val messageLink =
                resources.getString(R.string.sms_body) + mInviteFriendsViewModel.mInvitationLink.value!![0].invitationLink

            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${getFormattedPhoneNumber(phoneNumber)}")).apply {
                putExtra("sms_body", messageLink)

                if (resolveActivity(activity!!.packageManager) != null)
                    startActivity(this)
                else
                    showError(getString(R.string.error_generic))
            }

        } else {
            showError(resources.getString(R.string.link_not_available))
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}

    //callback for permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionHelper != null) {
            permissionHelper!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showData(contacts: ArrayList<ContactList>?) {
        if (!contacts.isNullOrEmpty()) {
            contacts.sortBy { it.name }
            this.mList.clear()
            this.mList.addAll(contacts)
            mAdapter?.notifyDataSetChanged()
            rv_contact.visibility = View.VISIBLE
            noResult.visibility = View.GONE
        } else {
            rv_contact.visibility = View.GONE
            noResult.visibility = View.VISIBLE
        }
    }

    //    private fun getContactNumberFromId(contactId: Long): String {
    //        var phoneNumber = ""
    //        val phones = activity!!.contentResolver.query(
    //            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    //            null,
    //            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
    //            null,
    //            null
    //        )
    //        while (phones!!.moveToNext()) {
    //            phoneNumber =
    //                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
    //        }
    //        phones.close()
    //        return phoneNumber
    //    }
}
