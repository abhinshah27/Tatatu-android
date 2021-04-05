package rs.highlande.app.tatatu.feature.account.settings.view

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.SettingsUpgradeAccountVerifyNowEmailViewModelBinding
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.SettingsUpgradeAccountVerifyNowEmailViewModel
import rs.highlande.app.tatatu.model.User

/**
 * Created by Abhin.
 */
class SettingsEmailSentActivity : BaseActivity() {

    private val mSettings: SettingsUpgradeAccountVerifyNowEmailViewModel by viewModel()
    private lateinit var mBinding: SettingsUpgradeAccountVerifyNowEmailViewModelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.fragment_upgrade_account_verify_now_email_sent)
        mBinding.lifecycleOwner = this // view model attach with lifecycle
        mBinding.mViewModel = mSettings //setting up view model
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    override fun configureLayout() {}
    override fun bindLayout() {}
    override fun manageIntent() {}

    override fun observeOnMyUserAction(user: User) {
        super.observeOnMyUserAction(user)
        mBinding.txtVerifyEmailSentMsg.text = getString(R.string.msg_we_have_sent, user.privateInfo.email)
    }

    companion object {
        val logTag = SettingsEmailSentActivity::class.java.simpleName
    }
}