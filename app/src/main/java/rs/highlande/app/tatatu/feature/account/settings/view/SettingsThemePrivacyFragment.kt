package rs.highlande.app.tatatu.feature.account.settings.view

import android.app.UiModeManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_settings_theme_privacy.*
import kotlinx.android.synthetic.main.toolbar_settings.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.databinding.SettingsColorPrivacyViewModelBinding
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.SettingsThemePrivacyViewModel

/**
 * As Color (i.e. Dark Theme) fragment it is available only from Android 10+ (or Q+).
 * Created by Abhin.
 */
class SettingsThemePrivacyFragment : BaseFragment() {

    private val mSettings: SettingsThemePrivacyViewModel by viewModel()
    private var mBinding: SettingsColorPrivacyViewModelBinding? = null
    private val preferenceHelper: PreferenceHelper by inject()

    private var currentUIType = UIType.PRIVATE_PROFILE

    enum class UIType {
        COLOR, PRIVATE_PROFILE;

        @StringRes
        fun toolbarTitle(): Int = if (this == COLOR) R.string.tb_color_theme else R.string.tb_profile_privacy
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        mSettings.isProgress.observe(this, Observer {
            if (it != null && it) {
                //show Loader
            } else {
                //hide Loader
            }
        })

        mSettings.valueChanged.observe(this, Observer {
            if (currentUIType == UIType.PRIVATE_PROFILE)
                fetchUser(true)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_theme_privacy, container, false)
        mBinding?.lifecycleOwner = this // view model attach with lifecycle
        mBinding?.mViewModel = mSettings //setting up view model
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        currentUIType = arguments!!.getSerializable(BUNDLE_KEY_COLOR_PRIVACY) as UIType
        setUI()
        img_tb_back.setOnClickListener {
            fragmentManager!!.popBackStack()
        }
        init()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(
            if (currentUIType == UIType.PRIVATE_PROFILE) "SettingsProfilePrivacyFragment"
            else "SettingsThemeFragment"
        )
    }

    private fun setUI() {
        currentUIType

        txt_tb_title.setText(currentUIType.toolbarTitle())
        when (currentUIType) {
            UIType.COLOR -> {
                txt_color_privacy_title.text = resources.getString(R.string.msg_you_can)
                txt_color_privacy_switch_title.text = resources.getString(R.string.switch_btn_dark_theme)
                switch_color_privacy_title.isChecked = when (preferenceHelper.getTheme()) {
                    UiModeManager.MODE_NIGHT_YES -> true
                    else -> false
                }
            }
            UIType.PRIVATE_PROFILE -> {
                txt_color_privacy_title.text = resources.getString(R.string.msg_settings_your)
                txt_color_privacy_switch_title.text = resources.getString(R.string.switch_btn_private_profile)
                if (getUser() != null) {
                    switch_color_privacy_title.isChecked = !getUser()!!.isPublic
                } else {
                    switch_color_privacy_title.isChecked = false
                }
            }
        }
    }

    private fun init() {
        //set switch Listener after setUI otherwise it's call when status change
        switch_color_privacy_title.setOnCheckedChangeListener { _, checked ->

            when (currentUIType) {
                UIType.PRIVATE_PROFILE -> mSettings.getAccountVisibility(checked)
                UIType.COLOR -> {
                    val theme = when(checked) {
                        true -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            UiModeManager.MODE_NIGHT_YES
                        }
                        false -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            UiModeManager.MODE_NIGHT_NO
                        }
                    }
                    preferenceHelper.setTheme(theme)
                }
            }

        }

    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}

    companion object {
        const val BUNDLE_KEY_COLOR_PRIVACY = "color_privacy"

        val logTag = SettingsThemePrivacyFragment::class.java.simpleName

        fun newInstance(uiType: UIType): SettingsThemePrivacyFragment {
            val frg = SettingsThemePrivacyFragment()
            return frg.apply {
                arguments = Bundle().apply {
                    putSerializable(BUNDLE_KEY_COLOR_PRIVACY, uiType)
                }
            }
        }
    }
}