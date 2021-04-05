package rs.highlande.app.tatatu.feature.inviteFriends.view.fragment


import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_invite_friends_email.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.isEmailValid
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.EmailBottomSheetViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.InviteFriendsViewModel

class InviteFriendsEmailFragment : BaseFragment() {

    private val emailBottomSheetViewModel: EmailBottomSheetViewModel by sharedViewModel()
    private val mInviteFriendsViewModel: InviteFriendsViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity!!.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return inflater.inflate(R.layout.fragment_invite_friends_email, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(InviteFriendsFragment.logTag, "Email")
    }

    private fun initObserver() {
        mInviteFriendsViewModel.mSendInvitation.observe(this, Observer {
            if (!it.isNullOrEmpty() && !it[0].id.isNullOrEmpty()) {
                edt_email_address.setText("")
                edt_email_full_name.setText("")
                edt_email_full_name.requestFocus()
                showError(resources.getString(R.string.msg_email))
            }
        })
        mInviteFriendsViewModel.isEmailProgress.observe(this, Observer {
            if (it) {
                mBaseActivity?.setProgressBarNull()
                showLoader(resources.getString(R.string.loader_email))
            } else {
                hideLoader()
            }
        })
        mInviteFriendsViewModel.mErrorInvitation.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                showError(mInviteFriendsViewModel.mErrorInvitation.value!!)
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        edt_email_address.apply {
            setOnEditorActionListener { textView, _, _ ->
                if (textView.id == edt_email_address.id) {
                    checkAndShowBottomSheet()
                    true
                } else false
            }
        }
        btn_create_email.setOnClickListener {
            hideKeyboard(activity!!)
            checkAndShowBottomSheet()
        }
    }

    private fun checkAndShowBottomSheet() {
        val fullName = edt_email_full_name.text.toString()
        val email = edt_email_address.text.toString()
        when {
            TextUtils.isEmpty(fullName) -> {
                Snackbar.make(ll_Main, resources.getString(R.string.valid_name), Snackbar.LENGTH_SHORT).show()
            }
            !isEmailValid(email) || TextUtils.isEmpty(email) -> {
                Snackbar.make(ll_Main, resources.getString(R.string.valid_email), Snackbar.LENGTH_SHORT).show()
            }
            else -> {
                if (getUser()?.isValid() == true) mInviteFriendsViewModel.getSendInvitation(fullName, email, getUser()!!.name)
                //                emailBottomSheetViewModel.mEmail = edt_email_address.text.toString()
                //                EmailBottomSheetDialog().show(childFragmentManager, EmailBottomSheetDialog::javaClass.name)
            }
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}
}
