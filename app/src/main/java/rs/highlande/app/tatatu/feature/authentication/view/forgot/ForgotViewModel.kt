package rs.highlande.app.tatatu.feature.authentication.view.forgot

import android.app.Application
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import io.reactivex.schedulers.Schedulers
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.isEmailValid
import rs.highlande.app.tatatu.feature.authentication.repository.AuthRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.createPost.view.getParentActivity


/**
 * Created by Abhin.
 */
class ForgotViewModel(var context: Application) : BaseAndroidViewModel(context), KoinComponent {

    private val authRepository: AuthRepository  by inject()
    var errorEmail = mutableLiveData("")
    var email = mutableLiveData("")

    val callSuccess = mutableLiveData(false)
    val callError = mutableLiveData(false)


    /* var emailTextWatcher = object : TextWatcher {
         override fun afterTextChanged(p0: Editable?) {
         }

         override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
         }

         override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
             if (p0.toString().isNotEmpty()) {
                 email.value = p0.toString()
                 errorEmail.value = ""
             } else {
                 errorEmail.value = context.resources.getString(R.string.msg_enter_your_email)
             }
         }
     }*/

    fun test()
    {
        R.id.txt_tb_next
    }

    private fun forgotEmail(email: String, activity: AppCompatActivity) {
        if (TextUtils.isEmpty(email)) {
            errorEmail.value = context.resources.getString(R.string.msg_enter_email)
        } else if (!isEmailValid(email)) {
            errorEmail.value = context.resources.getString(R.string.msg_email_validation)
        } else {

            showProgress.value = true

            addDisposable(
                authRepository.sendPasswordRecoveryEmail(email)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                        {

                            showProgress.postValue(false)

                            it.second?.let {
                                callError.postValue(true)
                            } ?: callSuccess.postValue(true)
                        },
                        { thr ->
                            thr.printStackTrace()
                            showProgress.postValue(false)
                            callError.postValue(true)
                        }
                    )
            )

        }
    }

    fun onClick(view: View) {
        val activity = view.getParentActivity()!!
        when (view.id) {
            R.id.btn_send -> forgotEmail(email.value!!, activity)
            R.id.txt_back_to_sign_in -> activity.supportFragmentManager.popBackStack()
        }
    }

    fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.toString().isNotEmpty()) {
            email.value = s.toString()
            errorEmail.value = ""
        } else {
            errorEmail.value = context.resources.getString(R.string.msg_enter_email)
        }
    }

    fun onEditText(view: View) {
        LogUtils.e("Tag", "-->$view")
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(callError, callSuccess, errorEmail, email)
    }


    companion object {
        val logTag = ForgotViewModel::class.java.simpleName
    }


}