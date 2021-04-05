package rs.highlande.app.tatatu.feature.account.settings.view

/**
 * Created by Abhin.
 */

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_upgrade_account_verify_now.*
import kotlinx.android.synthetic.main.toolbar_settings.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.databinding.SettingsUpgradeAccountVerifyNowViewModelBinding
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.SettingsUpgradeAccountVerifyNowViewModel


class SettingsUpgradeAccountVerifyNowFragment : BaseFragment() {

    private val mSettings: SettingsUpgradeAccountVerifyNowViewModel by viewModel()
    private var mBinding: SettingsUpgradeAccountVerifyNowViewModelBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, rs.highlande.app.tatatu.R.layout.fragment_upgrade_account_verify_now, container, false)
        mBinding?.lifecycleOwner = this // view model attach with lifecycle
        mBinding?.mViewModel = mSettings //setting up view model
        return mBinding!!.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        mSettings.openEmail.observe(this, Observer {
            if (it != null && it) {
                activity!!.startActivity(Intent(activity!!, SettingsEmailSentActivity::class.java))
                fragmentManager?.popBackStack()
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        mSettings.openEmail.value = false
        txt_tb_title.text = resources.getString(rs.highlande.app.tatatu.R.string.tb_verify_account)
        img_tb_back.setOnClickListener {
            fragmentManager!!.popBackStack()
        }

        btn_verify_account_verify_now.setOnClickListener {
            mSettings.verifyAccount()
        }
    }


    override fun configureLayout(view: View) {}
    override fun bindLayout() {}
}