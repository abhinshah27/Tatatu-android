package rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels

import android.app.Application
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.authentication.repository.AuthRepository
import rs.highlande.app.tatatu.feature.authentication.view.signUp.SignUpWelcomeFragment
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.createPost.view.getParentActivity
import java.util.concurrent.TimeUnit

/**
 * Created by Abhin.
 */
class SignUpAddUserViewModel(context: Application) : BaseAndroidViewModel(context) {

    private val authManager: AuthManager by inject()
    private val repository: AuthRepository by inject()

    val errorUserName = mutableLiveData("")
    var verifyUserName = mutableLiveData(View.GONE)
    var buttonEnabled = mutableLiveData(false)
    var userName = mutableLiveData("")

    var userNameValid = false



    fun createTextChangeObservable(editText: EditText?) {
        editText?.let {
            addDisposable(
                Observable
                    .create<String> { emitter ->

                        val textWatcher = object: TextWatcher {

                            override fun afterTextChanged(s: Editable?) = Unit

                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                s?.toString()?.let {
                                    LogUtils.d(logTag, "TESTUSERNAME: current string in ET: $s")
                                    emitter.onNext(it)
                                }
                            }
                        }
                        editText.addTextChangedListener(textWatcher)
                        emitter.setCancellable { editText.removeTextChangedListener(textWatcher) }
                    }
                    .debounce(300, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .toFlowable(BackpressureStrategy.BUFFER)
                    .observeOn(Schedulers.io())
                    .subscribe(
                        { username ->

                            LogUtils.d(logTag, "TESTUSERNAME: emitted value: $username")

                                addDisposable(
                                    repository.checkUserName(username)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(
                                            {
                                                if (it.second != null) {
                                                    errorUserName.value =
                                                        getApplication<TTUApp>().resources.getString(
                                                            R.string.error_existing_username,
                                                            username
                                                        )
                                                    userNameValid = false
                                                    verifyUserName.value = View.GONE
                                                    buttonEnabled.value = false
                                                } else {
                                                    userNameValid = it.first
                                                    this@SignUpAddUserViewModel.userName.value =
                                                        username
                                                    errorUserName.value = ""
                                                    verifyUserName.value = View.VISIBLE
                                                    buttonEnabled.value = true
                                                }
                                            },
                                            {
                                                it.printStackTrace()
                                                errorUserName.value = ""
                                                userNameValid = false
                                                verifyUserName.value = View.GONE
                                                buttonEnabled.value = false
                                            }
                                        )
                                )
                        },
                        {
                            it.printStackTrace()
                            errorUserName.value = ""
                            userNameValid = false
                            verifyUserName.value = View.GONE
                            buttonEnabled.value = false}
                    )
            )
        }

    }


    /**
     * Event generated for next in button
     */
    fun onNext(v: View) {
        if (!userName.value.isNullOrBlank()) {
            if (userNameValid) {
                authManager.latestSigningUser.userName = userName.value!!
                addReplaceFragment(
                    v.getParentActivity()!!,
                    R.id.fl_login_container,
                    SignUpWelcomeFragment.newInstance(false),
                    addFragment = false,
                    addToBackStack = true,
                    animationHolder = null
                )
            } else {
                errorUserName.value = getApplication<TTUApp>().resources.getString(R.string.error_existing_username, userName.value!!)
                buttonEnabled.value = false
                verifyUserName.value = View.GONE
            }
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }


    companion object {
        val logTag = SignUpAddUserViewModel::class.java.simpleName
    }
}