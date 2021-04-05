package rs.highlande.app.tatatu.feature.authentication.view.login

/**
 * Created by Abhin.
 */
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_access_login.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.showDialogGeneric
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.LoginViewModelBinding
import rs.highlande.app.tatatu.feature.authentication.AuthActivity
import rs.highlande.app.tatatu.feature.authentication.InvisibleGmailActivity


// INFO: 2019-08-26    MOBILE log in has been statically hidden in XML file

class LoginFragment : BaseFragment() {


    private var mLoginViewModelBinding: LoginViewModelBinding? = null
    private val mLoginViewModel: LoginViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mLoginViewModelBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_access_login, container, false)
        return mLoginViewModelBinding?.let {
            it.loginViewModel = mLoginViewModel
            it.lifecycleOwner = this
            it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureLayout(view)
    }

    private fun initObserver() {
        mLoginViewModel.errorEmail.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                txl_login_email.error = it
            } else {
                txl_login_email.error=null
                txl_login_email.isErrorEnabled = false
            }
        })
        mLoginViewModel.errorPassword.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                txl_login_password.error = it
            } else {
                txl_login_password.error=null
                txl_login_password.isErrorEnabled = false
            }
        })
        mLoginViewModel.errorMobile.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                txl_mobile.error = it
            } else {
                txl_mobile.error=null
                txl_mobile.isErrorEnabled = false
            }
        })
        mLoginViewModel.cursorCount.observe(viewLifecycleOwner, Observer {
            if (it==4) {
                edt_mobile_number.requestFocus()
            }
        })
        mLoginViewModel.cachedAuthString.observe(viewLifecycleOwner, Observer {
            if (it.isNotBlank()) {
                // TODO: 2019-08-18    supposes that only possible login is email
                edt_login_email.setText(it)
                checkbox_Remember.isChecked = true
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initObserver()
        init()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    override fun onResume() {
        super.onResume()

        if ((activity as? AuthActivity)?.openVerificationDialog == true) {
            showDialogGeneric(
                activity!!,
                R.string.dialog_signup_email_message,
                R.string.dialog_signup_email_title,
                R.string.dialog_signup_email_pos,
                onPositive = DialogInterface.OnClickListener { dialogInterface, _ ->

                    // INFO: 2020-01-10    adds middle activity as support for Gmail redirection
//                            fireOpenEmailIntent(this@AuthActivity)
                    startActivity(Intent(activity!!, InvisibleGmailActivity::class.java))

                    dialogInterface.dismiss()
                },
                cancelable = false
            )

            (activity as AuthActivity).openVerificationDialog = false
        }
    }


    private fun init() {
        mLoginViewModel.mobileCountry = edt_mobile_country
        mLoginViewModel.edt_login_password = edt_login_password
//        txl_login_email.error="test test test test test test"
        mLoginViewModel.showProgress.observe(viewLifecycleOwner, Observer {
            if (it) showLoader(R.string.progress_login)
            else hideLoader()
        })
    }

    override fun configureLayout(view: View) {
        mLoginViewModel.getCachedAuthString()
    }

    override fun bindLayout() {}


    companion object {

        val logTag = LoginFragment::class.java.simpleName

        fun newInstance(): LoginFragment {

            return LoginFragment().apply {
                arguments = Bundle().apply {

                    // TODO: 2019-07-23    put arguments

                }
            }

        }
    }

}