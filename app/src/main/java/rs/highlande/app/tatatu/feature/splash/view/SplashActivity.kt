package rs.highlande.app.tatatu.feature.splash.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import io.branch.referral.Branch
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.feature.authentication.AuthActivity
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.main.view.MainActivity
import rs.highlande.app.tatatu.feature.onBoarding.view.OnBoardingActivity

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-25.
 */
class SplashActivity : BaseActivity() {

    private val preferenceHelper: PreferenceHelper by inject()
    private val authManager by inject<AuthManager>()

    private lateinit var branch: Branch


    private val credentialsObserver = object : AuthManager.CredentialListener {
        override fun onCredentialsFetched(result: AuthManager.CredentialsResult) {
            Handler().postDelayed(
                {
                    if (result == AuthManager.CredentialsResult.SUCCESS)
                        LogUtils.d(logTag, "(3) CREDENTIALS: listener notified and validCredentials=${TTUApp.hasValidCredentials}")
                    else
                        LogUtils.d(logTag, "CREDENTIALS: listener notified with FAILURE")

                    if (TTUApp.hasValidCredentials) {
                        authManager.resetInvitation()
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        finish()
                        overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out)
                    } else {
                        if (preferenceHelper.getOnBoard()) {
                            authManager.let {
                                if (it.hasPendingInvitation())
                                    AuthActivity.openSignupFragment(this@SplashActivity)
                                else
                                    AuthActivity.openLoginFragment(this@SplashActivity)
                            }
                        } else {
                            startActivity(Intent(this@SplashActivity, OnBoardingActivity::class.java))
                            finish()
                            overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out)
                        }
                    }
                },
                2500
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }


    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        branch = Branch.getInstance()


        branch.initSession(
            { referringParams, error ->
                if (error == null) {
                    // params are the deep linked params associated with the link that the user clicked -> was re-directed to this app
                    // params will be empty if no data found
                    val obj = referringParams?.optJSONObject("custom_object")
                    if (obj != null) {
                        val uid = obj.optString("uid")
                        val tid = obj.optString("tid")
                        if (!uid.isNullOrBlank() || !tid.isNullOrBlank()) {
                            authManager.storeFieldsForSignup(uid, tid)
                        }
                    }

                    LogUtils.i("BRANCH SDK", referringParams.toString())
                } else {
                    LogUtils.i("BRANCH SDK", error.message)
                }
            },
            this.intent.data,
            this
        )

        authManager.addCredentialObserver(credentialsObserver)

    }

    override fun onStop() {
        authManager.removeCredentialObserver(credentialsObserver)

        super.onStop()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        this.intent = intent
    }


    override fun configureLayout() {}

    override fun bindLayout() {}

    override fun manageIntent() {}


    companion object {
        val logTag = SplashActivity::class.java.simpleName
    }

}