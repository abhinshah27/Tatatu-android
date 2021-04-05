package rs.highlande.app.tatatu.feature.authentication.view.signUp


import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_access_sign_up.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.resolveColorAttribute
import rs.highlande.app.tatatu.databinding.SignUpBinding
import rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels.SignUpViewModel
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewFragment
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewModel


// INFO: 2019-08-26    MOBILE sign up has been statically hidden in XML file

class SignUpFragment : BaseFragment() {

    private val mWebViewModel: WebViewModel by sharedViewModel()
    private val mSignUpViewModel: SignUpViewModel by viewModel()
    private var mSignUpBinding: SignUpBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mSignUpBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_access_sign_up, container, false)
        mSignUpBinding?.signUpViewModel = mSignUpViewModel
        mSignUpBinding?.lifecycleOwner = this
        return mSignUpBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }


    private fun initObserver() {
        mSignUpViewModel.errorFullName.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                txl_sign_up_full_name.error = it
            } else {
                txl_sign_up_full_name.error = null
                txl_sign_up_full_name.isErrorEnabled = false
            }
        })
        mSignUpViewModel.errorEmail.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                txl_sign_up_email.error = it
            } else {
                txl_sign_up_email.error = null
                txl_sign_up_email.isErrorEnabled = false
            }
        })
        mSignUpViewModel.errorMobile.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                txl_mobile.error = it
            } else {
                txl_mobile.error = null
                txl_mobile.isErrorEnabled = false
            }
        })
        mSignUpViewModel.errorPassword.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                txl_sign_up_password.error = it
            } else {
                txl_sign_up_password.error = null
                txl_sign_up_password.isErrorEnabled = false
            }
        })
        mSignUpViewModel.errorConfirmPassword.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                txl_sign_up_confirm_password.error = it
            } else {
                txl_sign_up_confirm_password.error = null
                txl_sign_up_confirm_password.isErrorEnabled = false
            }
        })
        mSignUpViewModel.cursorCount.observe(this, Observer {
            if (it == 4) {
                edt_mobile_number.requestFocus()
            }
        })

    }

    private fun init() {
        changeSelectedTextColor()
        mSignUpViewModel.edt_sign_up_password = edt_sign_up_password
        mSignUpViewModel.edt_sign_up_confirm_password = edt_sign_up_confirm_password
    }


    private fun changeSelectedTextColor() {

        val tc = getString(R.string.msg_tc_terms)
        val privacy = getString(R.string.msg_tc_privacy)

        val terms = SpannableString(context!!.resources.getString(R.string.msg_terms_condition))
        terms.setSpan(object : ClickableSpan() {
            override fun onClick(p0: View) {
                webView(true)
            }

            override fun updateDrawState(textPaint: TextPaint) {
                setTextColor(textPaint)
            }
        }, terms.indexOf(tc), terms.indexOf(tc) + tc.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        terms.setSpan(object : ClickableSpan() {
            override fun onClick(p0: View) {
                webView(false)
            }

            override fun updateDrawState(textPaint: TextPaint) {
                setTextColor(textPaint)
            }
        }, terms.indexOf(privacy), terms.indexOf(privacy) + privacy.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        txt_terms_conditions.movementMethod = LinkMovementMethod.getInstance() //this give the click on highlight text
        txt_terms_conditions.setText(terms, TextView.BufferType.SPANNABLE) // set the above text with Buffer
    }

    //set the color and false underline[underline are default]
    fun setTextColor(textPaint: TextPaint) {
        context?.let {
            textPaint.color = resolveColorAttribute(it, R.attr.textColorPrimary)
            textPaint.isUnderlineText = false
        }
    }

    fun webView(termsConditions: Boolean) {
        val wvType = if (termsConditions) {
            mWebViewModel.mWebUrl = resources.getString(R.string.ttu_tos)
            mWebViewModel.mToolbarName = resources.getString(R.string.tb_terms_of_use)
            WebViewFragment.WVType.TOS
        } else {
            mWebViewModel.mWebUrl = resources.getString(R.string.ttu_privacy)
            mWebViewModel.mToolbarName = resources.getString(R.string.tb_privacy_notice)
            WebViewFragment.WVType.PRIVACY
        }
        val mWebViewFragment = WebViewFragment.newInstance(wvType)
        addReplaceFragment(R.id.fl_login_container, mWebViewFragment, false, true, null)
    }


    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    companion object {

        val logTag = SignUpFragment::class.java.simpleName


        fun newInstance(): SignUpFragment {
            val bundle = Bundle()

            return SignUpFragment().apply { arguments = bundle.apply { } }

        }

    }


}
