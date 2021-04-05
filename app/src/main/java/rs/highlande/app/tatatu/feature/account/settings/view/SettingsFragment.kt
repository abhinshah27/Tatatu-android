package rs.highlande.app.tatatu.feature.account.settings.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.toolbar_settings.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_USER_ID
import rs.highlande.app.tatatu.core.util.hasQ
import rs.highlande.app.tatatu.feature.account.settings.view.blockAccounts.SettingsBlockAccountsFragment
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewFragment
import rs.highlande.app.tatatu.feature.commonView.webView.WebViewModel
import rs.highlande.app.tatatu.model.SettingsList

/**
 * Created by Abhin.
 */
class SettingsFragment : BaseFragment() {

    private val webViewModel by sharedViewModel<WebViewModel>()

    private var mAdapter: SettingViewModelAdapter? = null
    private var mArrayList = ArrayList<SettingsList>()
    private var mUserId = ""

    companion object {

        val logTag = SettingsFragment::class.java.simpleName

        fun newInstance(userId: String): SettingsFragment {
            val bundle = Bundle().apply { putString(BUNDLE_KEY_USER_ID, userId) }
            val instance = SettingsFragment()
            instance.arguments = bundle
            return instance
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    private fun init() {
        if (arguments != null) mUserId = arguments!!.getString(BUNDLE_KEY_USER_ID, "")

        mArrayList.clear()
        mArrayList.addAll(initPrePareList(context!!))
        txt_tb_title.text = resources.getString(R.string.tb_settings)
        img_tb_back.setOnClickListener {
            activity?.onBackPressed()
        }
        initRecyclerView()
    }

    private fun initRecyclerView() {
        rv_settings.layoutManager = LinearLayoutManager(context!!)
        mAdapter = SettingViewModelAdapter(context!!, mArrayList, object : SettingViewModelAdapter.ItemClickListener {
            override fun itemClick(position: Int) {

                when (mArrayList[position].mSettingName) {
                    resources.getString(R.string.settings_select_language) -> {

                    }
                    resources.getString(R.string.settings_color_theme) -> {
                        addReplaceFragment(R.id.container, SettingsThemePrivacyFragment.newInstance(SettingsThemePrivacyFragment.UIType.COLOR), addFragment = false, addToBackStack = true, animationHolder = NavigationAnimationHolder())
                    }
                    resources.getString(R.string.settings_upgrade) -> {
                        addReplaceFragment(R.id.container, SettingsUpgradeAccountFragment(), addFragment = false, addToBackStack = true, animationHolder = NavigationAnimationHolder())
                    }
                    resources.getString(R.string.settings_verify_account) -> {
                        addReplaceFragment(R.id.container, SettingsUpgradeAccountVerifyNowFragment(), addFragment = false, addToBackStack = true, animationHolder = NavigationAnimationHolder())
                    }
                    resources.getString(R.string.settings_notification_settings) -> {
                        addReplaceFragment(R.id.container, SettingsNotificationsFragment(), addFragment = false, addToBackStack = true, animationHolder = NavigationAnimationHolder())
                    }
                    resources.getString(R.string.settings_profile_privacy) -> {
                        addReplaceFragment(R.id.container, SettingsThemePrivacyFragment.newInstance(SettingsThemePrivacyFragment.UIType.PRIVATE_PROFILE), addFragment = false, addToBackStack = true, animationHolder = NavigationAnimationHolder())
                    }
                    resources.getString(R.string.settings_block_profile) -> {
                        addReplaceFragment(R.id.container, SettingsBlockAccountsFragment(), addFragment = false, addToBackStack = true, animationHolder = NavigationAnimationHolder())
                    }
                    resources.getString(R.string.settings_privacy_notice) -> {
                        webViewModel.mWebUrl = resources.getString(R.string.ttu_privacy)
                        webViewModel.mToolbarName = resources.getString(R.string.tb_privacy_notice)
                        addReplaceFragment(
                            R.id.container,
                            WebViewFragment.newInstance(WebViewFragment.WVType.PRIVACY),
                            addFragment = false,
                            addToBackStack = true,
                            animationHolder = NavigationAnimationHolder()
                        )
                    }
                    resources.getString(R.string.settings_terms_of_use) -> {
                        webViewModel.mWebUrl = resources.getString(R.string.ttu_tos)
                        webViewModel.mToolbarName = resources.getString(R.string.tb_terms_of_use)
                        addReplaceFragment(
                            R.id.container,
                            WebViewFragment.newInstance(WebViewFragment.WVType.TOS),
                            addFragment = false,
                            addToBackStack = true,
                            animationHolder = NavigationAnimationHolder()
                        )
                    }
                    resources.getString(R.string.settings_help_center) -> {
                        // TODO: 2019-08-07    implement Help Center
                        Toast.makeText(context, "GOTO Help Center", Toast.LENGTH_SHORT).show()

                    }
                }
            }
        })
        rv_settings.adapter = mAdapter
        mAdapter!!.notifyDataSetChanged()
    }


    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    private fun initPrePareList(context: Context): java.util.ArrayList<SettingsList> {
        val mArrayList = java.util.ArrayList<SettingsList>()
        val settingsNames = arrayListOf(context.resources.getString(R.string.settings_select_language), context.resources.getString(R.string.settings_color_theme), context.resources.getString(R.string.settings_upgrade), context.resources.getString(R.string.settings_profile_privacy), context.resources.getString(R.string.settings_privacy_notice), context.resources.getString(R.string.settings_terms_of_use))
        val settingsImages = arrayListOf(ContextCompat.getDrawable(context, R.drawable.ic_select_language), ContextCompat.getDrawable(context, R.drawable.ic_color_theme), ContextCompat.getDrawable(context, R.drawable.ic_upgrade_account), ContextCompat.getDrawable(context, R.drawable.ic_verify_account), ContextCompat.getDrawable(context, R.drawable.ic_settings_notifications), ContextCompat.getDrawable(context, R.drawable.ic_profile_privacy), ContextCompat.getDrawable(context, R.drawable.ic_blocked_profiles), ContextCompat.getDrawable(context, R.drawable.ic_privacy_settings), ContextCompat.getDrawable(context, R.drawable.ic_settings_terms_of_use), ContextCompat.getDrawable(context, R.drawable.ic_help_center))
        mArrayList.clear()
        for ((i, e) in settingsNames.withIndex()) {

            if (e.equals(context.resources.getString(R.string.settings_notification_settings), true)) {
                mArrayList.add(SettingsList(settingsImages[i], e, context.resources.getString(R.string.settings_choose_which)))
            } else {
                if (i == 0) { // not added the first and second position
                    continue
                } else if (i == 1) {
                    if (hasQ()) continue
                    else mArrayList.add(SettingsList(settingsImages[i], e))
                } else {
                    mArrayList.add(SettingsList(settingsImages[i], e))
                }
            }

        }
        return mArrayList
    }
}

//Full Settings List with Hide options
// arrayListOf(context.resources.getString(R.string.settings_select_language),
// context.resources.getString( R.string.settings_color_theme),
// context.resources.getString(R.string.settings_upgrade),
// context.resources.getString( R.string.settings_verify_account),
// context.resources.getString(R.string.settings_notification_settings),
// context.resources.getString( R.string.settings_profile_privacy),
// context.resources.getString(R.string.settings_block_profile),
// context.resources.getString( R.string.settings_privacy_notice),
// context.resources.getString(R.string.settings_terms_of_use),
// context.resources.getString(R.string.settings_help_center)
// )