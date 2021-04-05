package rs.highlande.app.tatatu.feature.account.settings.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_upgrade_account.*
import kotlinx.android.synthetic.main.toolbar_settings.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_POST_ID
import rs.highlande.app.tatatu.core.util.resolveColorAttribute
import rs.highlande.app.tatatu.databinding.SettingsUpgradeAccountViewModelBinding
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.SettingsUpgradeAccountViewModel
import rs.highlande.app.tatatu.model.AccountType
import rs.highlande.app.tatatu.model.User

/**
 * Created by Abhin.
 */

class SettingsUpgradeAccountFragment : BaseFragment() {

    private val mSettings: SettingsUpgradeAccountViewModel by viewModel()
    private lateinit var mBinding: SettingsUpgradeAccountViewModelBinding

    private var changeFromCache = true

    companion object {

        val logTag = SettingsUpgradeAccountFragment::class.java.simpleName

        fun newInstance(userId: String): SettingsUpgradeAccountFragment {
            val bundle = Bundle().apply {
                putString(BUNDLE_KEY_POST_ID, userId)
            }
            val instance = SettingsUpgradeAccountFragment()
            instance.arguments = bundle
            return instance
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_upgrade_account, container, false)
        mBinding.lifecycleOwner = this // view model attach with lifecycle
        mBinding.mViewModel = mSettings //setting up view model
        return mBinding.root
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
        mSettings.openEmail.observe(this, Observer {
            if (it != null && it) {
                activity!!.startActivity(Intent(activity!!, SettingsEmailSentActivity::class.java))
                fragmentManager?.popBackStack()
            }
        })
        mSettings.showVerification.observe(this, Observer {
            if (it != null && it) {
                fetchUser(true)
                showEmailVerification()
            }
        })
        mSettings.mPublicFigure.observe(this, Observer {
            setColor(it!!, txt_public_figure, img_public_right)
            checkVisibility()
        })
        mSettings.mCelebrity.observe(this, Observer {
            setColor(it!!, txt_celebrity, img_celebrity_right)
            checkVisibility()
        })
        mSettings.mCharity.observe(this, Observer {
            setColor(it!!, txt_charity, img_charity_right)
            checkVisibility()
        })
    }


    private fun showEmailVerification() {
        btn_upgrade_account_verify_now.visibility = View.VISIBLE
        txt_tb_next.visibility = View.INVISIBLE
        txt_upgrade_message.visibility = View.VISIBLE
    }

    private fun setColor(mValue: Boolean, textView: AppCompatTextView, imageView: AppCompatImageView) {
        if (mValue) {
            textView.setTextColor(resolveColorAttribute(context!!, R.attr.textColorPrimary))
            imageView.visibility = View.VISIBLE
        } else {
            textView.setTextColor(resolveColorAttribute(context!!, R.attr.textColorSecondary))
            imageView.visibility = View.GONE
        }
    }

    private fun checkVisibility() {
        txt_tb_next.visibility = when {
            changeFromCache && getUser()?.verified == false -> View.VISIBLE
            !changeFromCache && (mSettings.mPublicFigure.value!! || mSettings.mCelebrity.value!! || mSettings.mCharity.value!!) -> View.VISIBLE
            else -> View.INVISIBLE
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        mSettings.openEmail.value = false
        txt_tb_title.text = resources.getString(R.string.tb_upgrade_account)
        txt_tb_next.text = resources.getString(R.string.tb_upgrade_account_save)

        img_tb_back.setOnClickListener {
            fragmentManager!!.popBackStack()
        }

        txt_tb_next.setOnClickListener {
            mSettings.changeAccountType()
        }

        btn_upgrade_account_verify_now.setOnClickListener {
            mSettings.verifyAccount()
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    override fun observeOnMyUserAction(user: User) {
        super.observeOnMyUserAction(user)

        if (mSettings.savedType == null || user.accountType == mSettings.savedType) {
            val views = when (user.accountType) {
                AccountType.PUBLIC_FIGURE -> mBinding.txtPublicFigure to mBinding.imgPublicRight
                AccountType.CELEBRITY -> mBinding.txtCelebrity to mBinding.imgCelebrityRight
                AccountType.CHARITY -> mBinding.txtCharity to mBinding.imgCharityRight
                else -> null to null
            }

            if (views.first != null && views.second != null) {
                changeFromCache = true
                setColor(true, views.first!!, views.second!!)
                checkVisibility()
            }
        }

        changeFromCache = false
    }

}