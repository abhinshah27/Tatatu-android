package rs.highlande.app.tatatu.feature.authentication.view.login

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.MutableLiveData
import com.google.android.material.textfield.TextInputEditText
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.isEmailValid
import rs.highlande.app.tatatu.core.util.isPhoneValid
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.authentication.view.forgot.ForgotPasswordFragment
import rs.highlande.app.tatatu.feature.authentication.view.signUp.SignUpFragment
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.createPost.view.getParentActivity


/**
 * Created by Abhin.
 */
class LoginViewModel(var context: Context) : BaseViewModel() {

    private val authManager: AuthManager by inject()

    //    private val mLoginRepository: LoginRepository  by inject()
    private var isVisibility: Boolean = true

    val errorPassword = mutableLiveData("")
    val errorEmail = mutableLiveData("")
    val errorMobile = mutableLiveData("")

    var email = mutableLiveData("")
    var password = mutableLiveData("")
    var countryCode = mutableLiveData("")
    var mobile = mutableLiveData("")

    var remember = mutableLiveData(false)
    var visiblePassword = mutableLiveData(true)
    var mobileVisibility = mutableLiveData(8)
    var emailVisibility = mutableLiveData(0)
    var cursorCount = mutableLiveData(0)


    var cachedAuthString = mutableLiveData("")


    var mobileCountry: TextInputEditText? = null
    var edt_login_password: AppCompatEditText? = null

    var radioGroupClick = RadioGroup.OnCheckedChangeListener { _, p1 ->
        if (p1 == R.id.rb_email) {
            isVisibility = true
            mobileVisibility.value = 8 // gone
            emailVisibility.value = 0  // visible
            errorPassword.value = ""
            errorMobile.value = ""
        } else {
            isVisibility = false
            emailVisibility.value = 8 // gone
            mobileVisibility.value = 0 // visible
            errorPassword.value = ""
            errorEmail.value = ""
        }
    }

    fun onCheckedChange(button: CompoundButton, check: Boolean) {
        remember.value = check
        authManager.rememberUser = check
    }

    var emailTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.toString().isNotEmpty()) {
                email.value = p0.toString()
                errorEmail.value = ""
            } else {
                errorEmail.value = context.resources.getString(R.string.setting_msg_error_email)
            }
        }
    }

    var passwordTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.toString().isNotEmpty()) {
                password.value = p0.toString()
                errorPassword.value = ""
            } else {
                errorPassword.value = context.resources.getString(R.string.msg_enter_password)
            }

        }
    }

    var countryCodeTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.isNullOrEmpty()) {
                mobileCountry!!.setText(context.resources.getString(R.string.plus))
                cursorCount.value = 0
                if (mobileCountry != null) mobileCountry!!.setSelection(mobileCountry!!.text!!.length)
            } else {
                cursorCount.value = p0.length
                countryCode.value = p0.toString()
            }
        }
    }

    var mobileTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.toString().isNotEmpty()) {
                mobile.value = p0.toString()
                errorMobile.value = ""
            } else {
                errorMobile.value = context.resources.getString(R.string.msg_enter_mobile)
            }
        }
    }


    fun getCachedAuthString() {
        cachedAuthString.value = authManager.rememberedAuth
    }


    //Click Handling in ViewModel
    fun onClick(view: View) {
        val activity = view.getParentActivity()!!
        when (view.id) {
            R.id.ll_sign_up -> {
                if (view.getParentActivity()?.supportFragmentManager?.findFragmentByTag(SignUpFragment.logTag) != null)
                    activity.supportFragmentManager.popBackStack()
                else
                    addReplaceFragment(activity, R.id.fl_login_container, SignUpFragment(), addFragment = false, addToBackStack = true, animationHolder = null)
            }
            R.id.txt_forgot_password -> addReplaceFragment(activity, R.id.fl_login_container, ForgotPasswordFragment(), addFragment = false, addToBackStack = true, animationHolder = NavigationAnimationHolder())
            R.id.img_facebook, R.id.txt_facebook -> doLoginSocial(activity, AuthManager.AuthType.FACEBOOK)
            R.id.img_google, R.id.txt_google -> doLoginSocial(activity, AuthManager.AuthType.GOOGLE)
            R.id.btn_login -> doLogin(activity, email.value!!, password.value!!, countryCode.value!!, mobile.value!!)
            R.id.img_password -> {
                visiblePassword.value = !visiblePassword.value!!
                if (visiblePassword.value!!) {
                    edt_login_password!!.transformationMethod = PasswordTransformationMethod()
                    edt_login_password!!.setSelection(edt_login_password!!.text.toString().length)
                } else {
                    edt_login_password!!.transformationMethod = null
                    edt_login_password!!.setSelection(edt_login_password!!.text.toString().length)
                }
            }
        }
    }


    private fun doLogin(context: Context, email: String = "", password: String = "", countryCode: String, mobile: String) {
        when {
            isVisibility && TextUtils.isEmpty(email) -> errorEmail.value = context.resources.getString(R.string.setting_msg_error_email)
            isVisibility && !isEmailValid(email) -> errorEmail.value = context.resources.getString(R.string.setting_msg_error_valid_email)
            !isVisibility && TextUtils.isEmpty(countryCode) -> errorMobile.value = context.resources.getString(R.string.msg_enter_country_code)
            !isVisibility && TextUtils.isEmpty(mobile) -> errorMobile.value = context.resources.getString(R.string.msg_enter_mobile)
            !isVisibility && !isPhoneValid(countryCode, mobile) -> errorMobile.value = context.resources.getString(R.string.msg_validation_mobile)
            TextUtils.isEmpty(password) -> errorPassword.value = (context.resources.getString(R.string.msg_enter_password))
//            !isPasswordValid(password) -> errorPassword.value = context.resources.getString(R.string.msg_validation_password)
            else -> {
                showProgress.value = true
                // forms the signing user for Auth0
                authManager.latestSigningUser.apply {
                    if (!isVisibility) signupUserInfo.email= mobile
                    else signupUserInfo.email = email
                    signupUserInfo.password = password
                    permissions.authType = AuthManager.AuthType.CUSTOM
                }
                // perform loginAuth0 action
                authManager.loginAuth0(context as Activity)
            }
        }

        /* if (isVisibility) {
             if (TextUtils.isEmpty(email)) {
                 errorEmail.value = context.resources.getString(R.string.msg_enter_email)
             } else if (!isEmailValid(email)) {
                 errorEmail.value = context.resources.getString(R.string.msg_email_validation)
             } else if (TextUtils.isEmpty(password)) {
                 errorPassword.value = (context.resources.getString(R.string.msg_enter_password))
             } else if (!isPasswordValid(password)) {
                 errorPassword.value = context.resources.getString(R.string.msg_validation_password)
             } else {


             }
         } else {
             if (TextUtils.isEmpty(countryCode)) {
                 errorMobile.value = context.resources.getString(R.string.msg_enter_mobile)
             } else if (TextUtils.isEmpty(mobile)) {
                 errorMobile.value = context.resources.getString(R.string.msg_enter_mobile)
             } else if (!isPhoneValid(countryCode, mobile)) {
                 errorMobile.value = context.resources.getString(R.string.msg_validation_mobile)
             } else if (TextUtils.isEmpty(password)) {
                 errorPassword.value = (context.resources.getString(R.string.msg_enter_password))
             } else if (!isPasswordValid(password)) {
                 errorPassword.value = context.resources.getString(R.string.msg_validation_password)
             } else {

                 // TODO: 2019-07-24    MOBILE????
                 // forms the signing user for Auth0
                 authManager.latestSigningUser.apply {
                     signupUserInfo.email = email
                     signupUserInfo.password = password

                     permissions.authType = AuthManager.AuthType.CUSTOM
                 }

                 // perform loginAuth0 action
                 authManager.loginAuth0(context as Activity)

             }
         }*/

    }


    fun doLoginSocial(activity: Activity, type: AuthManager.AuthType) {
        authManager.latestSigningUser.permissions.authType = type
        // perform loginAuth0 action
        authManager.loginAuth0(activity)
    }



    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(email, password, mobile)
    }
}