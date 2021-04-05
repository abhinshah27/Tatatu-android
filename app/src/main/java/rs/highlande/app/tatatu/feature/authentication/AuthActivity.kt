package rs.highlande.app.tatatu.feature.authentication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.FragmentManager
import com.auth0.android.authentication.AuthenticationException
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.authentication.view.login.LoginFragment
import rs.highlande.app.tatatu.feature.authentication.view.signUp.SignUpFragment
import rs.highlande.app.tatatu.feature.authentication.view.signUp.SignUpWelcomeFragment
import rs.highlande.app.tatatu.feature.main.view.MainActivity


/**
 * Created by Abhin.
 */

class AuthActivity : BaseActivity() {

    private val authViewModel: AuthViewModel by viewModel()
    private val preferenceHelper: PreferenceHelper by inject()

    private var doubleBackToExitPressedOnce: Boolean = false

    var openVerificationDialog = false


    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        manageIntent()

        setUpObservers()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        this.intent = intent

        manageIntent()
    }


    //    @SuppressLint("NewApi")
    //    override fun onBackPressed() {
    //        if (supportFragmentManager.backStackEntryCount > 0) {
    //            supportFragmentManager.popBackStack()
    //        } else
    //            if (!doubleBackToExitPressedOnce) {
    //            doubleBackToExitPressedOnce = true
    //            showError(resources.getString(R.string.msg_press_back_to_exit))
    //            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    //        } else {
    //            super.onBackPressed()
    //            finishAffinity()
    //        }
    //    }

    override fun configureLayout() {}

    override fun bindLayout() {}


    override fun manageIntent() {
        val showFragment = intent.getIntExtra(FRAGMENT_KEY_CODE, FRAGMENT_INVALID)
        val extras = intent.extras
        showFragment(showFragment, extras)
    }

    private fun showFragment(showFragment: Int, extras: Bundle?) {
        when (showFragment) {

            FRAGMENT_LOGIN -> {
                val fragment = supportFragmentManager.findFragmentByTag(LoginFragment.logTag)
                if (fragment == null) {
                    addReplaceFragment(
                        R.id.fl_login_container,
                        LoginFragment.newInstance(),
                        false,
                        true,
                        null
                    )
                }
            }

            FRAGMENT_SIGNUP -> {
                val fragment = supportFragmentManager.findFragmentByTag(SignUpFragment.logTag)
                if (fragment == null) {
                    addReplaceFragment(
                        R.id.fl_login_container,
                        SignUpFragment.newInstance(),
                        false,
                        true,
                        null
                    )
                }
            }
        }
    }


    private fun setUpObservers() {

        LogUtils.d(logTag, "testOBSERVERS: SETTING UP")

        authViewModel.userReceived.observe(this, androidx.lifecycle.Observer {
            LogUtils.d(logTag, "testOBSERVERS: USER RECEIVED with value = ${it.first}")

            hideLoader()

            if (it.first) {
                if (it.second != AuthManager.AuthType.CUSTOM) {
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                    finish()
                    overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out)
                } else {

                    LogUtils.d(logTag, "TESTSIGNUPCUSTOM: checking fragment")

                    // INFO: 2020-02-18    Fixes bug where popBackStack wasn't working: coming from
                    //  invite, LoginFragment does not exist in backStack
                    Handler().postDelayed(
                        {
                            if (supportFragmentManager.findFragmentByTag(LoginFragment.logTag) != null)
                                supportFragmentManager.popBackStack(LoginFragment.logTag, 0)
                            else {
                                LogUtils.d(logTag, "TESTSIGNUPCUSTOM: fragment is null")
                                LogUtils.d(logTag, "TESTSIGNUPCUSTOM: clearing backstack, BS entries: ${supportFragmentManager.backStackEntryCount}")
                                supportFragmentManager.popBackStackImmediate(SignUpFragment.logTag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                                LogUtils.d(logTag, "TESTSIGNUPCUSTOM: backstack cleared, BS entries: ${supportFragmentManager.backStackEntryCount}")
                                LogUtils.d(logTag, "TESTSIGNUPCUSTOM: replacing")
                                showFragment(FRAGMENT_LOGIN, null)
                            }

                            openVerificationDialog = true
                        },
                        500
                    )
                }
            }
        })

        authViewModel.errorOnAuth.observe(this, androidx.lifecycle.Observer {
            if (it.first) {

                hideLoader()

                supportFragmentManager.popBackStack(LoginFragment.logTag, 0)

                val message = when (it.second) {
                    is AuthenticationException -> {
                        val authExc = it.second as AuthenticationException

                        LogUtils.e(logTag, "Exception with code: ${authExc.code} and description: ${authExc.description}")

                        when {
                            authExc.code == "invalid_grant" -> getString(R.string.error_auth_login_custom)

                            // INFO: 2019-10-02    new Auht0 management: following 2 errors no longer received
//                            authExc.code == "access_denied" && authExc.description.contains("lang=en") -> {
//                                getString(R.string.error_auth_signup_needed)
//                            }
//                            authExc.code == "access_denied" && authExc.description.contains("object Object") -> {
//                                showDialogGeneric(
//                                    this@AuthActivity,
//                                    R.string.dialog_auth_10_minutes_message,
//                                    R.string.dialog_auth_10_minutes_title,
//                                    R.string.btn_ok,
//                                    onPositive = DialogInterface.OnClickListener { dialogInterface, _ ->
//                                        dialogInterface.dismiss()
//                                    })
//                                return@Observera
//                            }

                            authExc.code == "unauthorized" && authExc.description.contains("NotVerified") -> {
                                getString(R.string.error_auth_email_verification)
                            }
                            authExc.code == "user_exists" -> getString(R.string.error_auth_existing_user)
                            else -> getString(R.string.error_auth_generic)
                        }
                    }
                    is TTUAuthException -> {
                        LogUtils.e(logTag, it.second?.message)

                        if ((it.second as TTUAuthException).isSocialToConfirmException()) {

                            addReplaceFragment(
                                R.id.fl_login_container,
                                SignUpWelcomeFragment.newInstance(true),
                                false,
                                true,
                                NavigationAnimationHolder()
                            )

                            return@Observer
                        }
                        else getString(R.string.error_auth_generic)
                    }
                    else -> {
                        LogUtils.e(logTag, it.second?.message)
                        getString(R.string.error_auth_generic)
                    }


                }

                showError(message)
            }
        })

        authViewModel.showProgressSocial.observe(this, androidx.lifecycle.Observer {
            if (it.first) {
                showLoader(
                    if (it.second == AuthManager.AuthAction.LOGIN) R.string.progress_login
                    else R.string.progress_signup
                )
            } else hideLoader()
        })

    }


    companion object {

        val logTag = AuthActivity::class.java.simpleName

        fun openLoginFragment(context: Context) {
            openFragment<AuthActivity>(
                context,
                FRAGMENT_LOGIN,
                finish = true,
                animations = ActivityAnimationHolder(R.anim.alpha_in, R.anim.alpha_out)
            )
        }

        fun openSignupFragment(context: Context) {
            openFragment<AuthActivity>(
                context,
                FRAGMENT_SIGNUP,
                finish = true,
                animations = ActivityAnimationHolder(R.anim.alpha_in, R.anim.alpha_out)
            )
        }

        fun openAfterLogoutOrDelete(context: Context) {
            openFragment<AuthActivity>(
                context,
                FRAGMENT_LOGIN,
                finish = true,
                animations = ActivityAnimationHolder(R.anim.alpha_in, R.anim.alpha_out),
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

    }

}