package rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.MutableLiveData
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.isEmailValid
import rs.highlande.app.tatatu.core.util.isPasswordValid
import rs.highlande.app.tatatu.core.util.isPhoneValid
import rs.highlande.app.tatatu.core.util.showError
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.authentication.view.login.LoginFragment
import rs.highlande.app.tatatu.feature.authentication.view.signUp.SignUpAddUserNameFragment
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.createPost.view.getParentActivity

/**
 * Created by Abhin.
 */

class SignUpViewModel(var context: Context) : BaseViewModel() {

    private val authManager: AuthManager by inject()

    //    private val mLoginRepository: LoginRepository  by inject()
    val errorEmail = mutableLiveData("")
    val errorPassword = mutableLiveData("")
    val errorMobile = mutableLiveData("")
    val errorFullName = mutableLiveData("")
    val errorConfirmPassword = mutableLiveData("")

    var mobile = mutableLiveData("")
    var mEmail = mutableLiveData("")
    var cursorCount = mutableLiveData(0)
    var mFullName = mutableLiveData("")
    var mPassword = mutableLiveData("")
    var emailVisibility = mutableLiveData(0)
    var mobileVisibility = mutableLiveData(8)
    var countryCode = mutableLiveData("+")
    var mConfirmPassword = mutableLiveData("")

    var checkValidation = true
    var mCheckTerms: Boolean = false
    var edt_sign_up_password: AppCompatEditText? = null
    var edt_sign_up_confirm_password: AppCompatEditText? = null
    var visiblePassword = mutableLiveData(true)
    var visibleConfirmPassword = mutableLiveData(true)

    var radioGroupClick = RadioGroup.OnCheckedChangeListener { _, p1 ->
        if (p1 == R.id.rb_sign_up_email) {
            checkValidation = true
            mobileVisibility.value = 8 // gone
            emailVisibility.value = 0  // visible
            errorPassword.value = ""
            errorMobile.value = ""
        } else {
            checkValidation = false
            emailVisibility.value = 8 // gone
            mobileVisibility.value = 0 // visible
            errorPassword.value = ""
            errorEmail.value = ""
        }
    }

    fun onCheckedChange(button: CompoundButton, check: Boolean) {
        mCheckTerms = check
    }

    var fullNameTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.toString().isNotEmpty()) {
                mFullName.value = p0.toString()
                errorFullName.value = ""
            } else {
                errorFullName.value = context.resources.getString(R.string.valid_name)
            }
        }
    }

    var emailTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.toString().isNotEmpty()) {
                mEmail.value = p0.toString()
                errorEmail.value = ""
            } else {
                errorEmail.value = context.resources.getString(R.string.valid_email)
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
                mPassword.value = p0.toString()
                errorPassword.value = ""
                errorConfirmPassword.value=""
            } else {
                errorPassword.value = context.resources.getString(R.string.msg_validation_password)
            }
        }
    }

    var confirmPasswordTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (p0.toString().isNotEmpty()) {
                mConfirmPassword.value = p0.toString()
                errorConfirmPassword.value = ""
                errorPassword.value=""
            } else {
                errorConfirmPassword.value = context.resources.getString(R.string.msg_confirm_password_match)
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
                countryCode.value = context.resources.getString(R.string.plus)
                cursorCount.value = 0
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
                errorMobile.value = context.resources.getString(R.string.msg_validation_mobile)
            }
        }
    }


    fun onClick(view: View) {
        val activity = view.getParentActivity()!!
        when (view.id) {
            R.id.ll_back_to_login -> {
                if (view.getParentActivity()?.supportFragmentManager?.findFragmentByTag(LoginFragment.logTag) != null)
                    activity.supportFragmentManager.popBackStack()
                else
                    addReplaceFragment(activity, R.id.fl_login_container, LoginFragment(), addFragment = false, addToBackStack = true, animationHolder = null)
            }
            R.id.btn_sign_up -> doSignUp(mFullName.value!!, mEmail.value!!, countryCode.value!!, mobile.value!!, mPassword.value!!, mConfirmPassword.value!!, activity)
            R.id.img_sign_up_password -> {
                visiblePassword.value = !visiblePassword.value!!
                passwordCheck(edt_sign_up_password!!, visiblePassword.value)
            }
            R.id.img_sign_up_confirm_password -> {
                visibleConfirmPassword.value = !visibleConfirmPassword.value!!
                passwordCheck(edt_sign_up_confirm_password!!, visibleConfirmPassword.value)
            }
        }
    }

    private fun doSignUp(fullName: String = "", email: String = "", countryCode: String = "", mobile: String = "", password: String = "", confirmPassword: String = "", activity: AppCompatActivity) {
        when {
            TextUtils.isEmpty(fullName) -> errorFullName.value = context.resources.getString(R.string.valid_name)
            checkValidation && TextUtils.isEmpty(email) -> errorEmail.value = context.resources.getString(R.string.msg_enter_email)
            checkValidation && !isEmailValid(email) -> errorEmail.value = context.resources.getString(R.string.msg_email_validation)
            !checkValidation && TextUtils.isEmpty(mobile) -> errorMobile.value = context.resources.getString(R.string.msg_enter_mobile)
            !checkValidation && !isPhoneValid(countryCode, mobile) -> errorMobile.value = context.resources.getString(R.string.msg_validation_mobile)
            TextUtils.isEmpty(password) -> errorPassword.value = context.resources.getString(R.string.msg_enter_password)
            !isPasswordValid(password) -> errorPassword.value = context.resources.getString(R.string.msg_validation_password)
            TextUtils.isEmpty(confirmPassword) -> errorConfirmPassword.value = context.resources.getString(R.string.msg_validation_confirm_password)
            password != confirmPassword -> errorConfirmPassword.value = context.resources.getString(R.string.msg_confirm_password_match)
            !mCheckTerms -> showError(activity, activity.getString(R.string.msg_checkBox_Terms_and_Condition))
            else -> {
                authManager.latestSigningUser.apply {
                    if (checkValidation) signupUserInfo.email = email
                    else signupUserInfo.email = countryCode + mobile
                    signupUserInfo.password = password
                    signupUserInfo.confirmPassword = confirmPassword
                    signupUserInfo.fullName = fullName
                    permissions.authType = AuthManager.AuthType.CUSTOM
                    permissions.termsSigned = true
                }
                // GOTO username
                goToUsername(activity)
            }
        }

        /* if (checkValidation) {
             if (TextUtils.isEmpty(fullName)) {
                 errorFullName.value = context.resources.getString(R.string.valid_name)
             } else if (TextUtils.isEmpty(email)) {
                 errorEmail.value = context.resources.getString(R.string.msg_email_validation)
             } else if (!isEmailValid(email)) {
                 errorEmail.value = context.resources.getString(R.string.msg_email_validation)
             } else if (TextUtils.isEmpty(password)) {
                 errorPassword.value = context.resources.getString(R.string.msg_validation_password)
             } else if (!isPasswordValid(password)) {
                 errorPassword.value = context.resources.getString(R.string.msg_validation_password)
             } else if (TextUtils.isEmpty(confirmPassword)) {
                 errorConfirmPassword.value = context.resources.getString(R.string.msg_validation_confirm_password)
             } else if (password != confirmPassword) {
                 errorConfirmPassword.value = context.resources.getString(R.string.msg_confirm_password_match)
             } else if (!mCheckTerms) {
                 showError(activity, activity.getString(R.string.msg_checkBox_Terms_and_Condition))
             } else {
                 authManager.latestSigningUser.apply {
                     signupUserInfo.email = email
                     signupUserInfo.password = password
                     signupUserInfo.confirmPassword = confirmPassword
                     signupUserInfo.fullName = fullName
                     permissions.authType = AuthManager.AuthType.CUSTOM
                     permissions.termsSigned = true
                 }
                 // GOTO username
                 goToUsername(activity)
             }
         } else {
             if (TextUtils.isEmpty(fullName)) {
                 errorFullName.value = context.resources.getString(R.string.valid_name)
             } else if (TextUtils.isEmpty(countryCode)) {
                 errorMobile.value = context.resources.getString(R.string.msg_enter_country_code)
             } else if (TextUtils.isEmpty(mobile)) {
                 errorMobile.value = context.resources.getString(R.string.msg_enter_mobile)
             } else if (!isPhoneValid(countryCode, mobile)) {
                 errorMobile.value = context.resources.getString(R.string.msg_validation_mobile)
             } else if (TextUtils.isEmpty(password)) {
                 errorPassword.value = context.resources.getString(R.string.msg_validation_password)
             } else if (!isPasswordValid(password)) {
                 errorPassword.value = context.resources.getString(R.string.msg_validation_password)
             } else if (TextUtils.isEmpty(confirmPassword)) {
                 errorConfirmPassword.value = context.resources.getString(R.string.msg_validation_confirm_password)
             } else if (password != confirmPassword) {
                 errorConfirmPassword.value = context.resources.getString(R.string.msg_confirm_password_match)
             } else if (!mCheckTerms) {
                 showError(activity, activity.getString(R.string.msg_checkBox_Terms_and_Condition))
             } else {
                 authManager.latestSigningUser.apply {
                     signupUserInfo.email = email
                     signupUserInfo.password = password
                     signupUserInfo.confirmPassword = confirmPassword
                     signupUserInfo.fullName = fullName
                     permissions.authType = AuthManager.AuthType.CUSTOM
                     permissions.termsSigned = true
                 }
                 // GOTO username
                 goToUsername(activity)
             }
         }*/
    }

    private fun goToUsername(activity: AppCompatActivity) {
        addReplaceFragment(activity, R.id.fl_login_container, SignUpAddUserNameFragment(), addFragment = false, addToBackStack = true, animationHolder = null)
    }

    private fun passwordCheck(editText: AppCompatEditText, it: Boolean?) {
        if (it != null && it) {
            editText.transformationMethod = PasswordTransformationMethod()
            editText.setSelection(editText.text.toString().length)
        } else {
            editText.transformationMethod = null
            editText.setSelection(editText.text.toString().length)
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(mFullName)
    }

}