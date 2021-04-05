package rs.highlande.app.tatatu.feature.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.common_activity_container.view.*
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.ActivityAccountBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.FollowRequestsFragment
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.FollowTabsFragment
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.FriendsTabsFragment
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.SentFollowRequestsFragment
import rs.highlande.app.tatatu.feature.account.profile.view.fragment.MyProfileFragment
import rs.highlande.app.tatatu.feature.account.profile.view.fragment.ProfileEditFragment
import rs.highlande.app.tatatu.feature.account.profile.view.fragment.ProfileFragment
import rs.highlande.app.tatatu.feature.account.settings.view.SettingsFragment
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonView.BottomNavigationActivity
import rs.highlande.app.tatatu.feature.main.view.MainActivity
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsSDKTokenUtils
import rs.highlande.app.tatatu.feature.wallet.view.WalletFragment

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-07.
 */
class AccountActivity : BottomNavigationActivity() {

    private lateinit var binding: ActivityAccountBinding

    private val preferenceHelper: PreferenceHelper by inject()
    private val usersRepository by inject<UsersRepository>()

    private var startedFromNotification: Boolean = false
    private var appForeground: Boolean = true
    private var ignoreIntent: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        ignoreIntent = savedInstanceState != null
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account)
        binding.lifecycleOwner = this

        setupBottomNavigation(binding.activityContainer.bottomBar as BottomNavigationView)

        if (CallsSDKTokenUtils.callToken.isNullOrBlank()) {
            //call for token generate
            CallsSDKTokenUtils.init(usersRepository.fetchCachedMyUserId())
        }
    }


    override fun configureLayout() {}

    override fun bindLayout() {}

    override fun manageIntent() {
        if (ignoreIntent) return

        val intent = intent ?: return
        manageIntent(intent)
    }


    override fun manageIntent(newIntent: Intent?) {
        val intent = newIntent ?: return

        val showFragment = intent.getIntExtra(FRAGMENT_KEY_CODE, FRAGMENT_INVALID)
        val extras = intent.extras
        showFragment(showFragment, extras)
    }

    private fun showFragment(showFragment: Int, extras: Bundle?) {
        when (showFragment) {

            FRAGMENT_PROFILE -> {
                val fromNotification = extras?.getBoolean(BUNDLE_KEY_FROM_NOTIFICATION)
                val userId = extras?.getString(BUNDLE_KEY_USER_ID) ?: ""

                if (fromNotification == null || !fromNotification && preferenceHelper.getUserId() == userId) {
                    addReplaceFragment(
                        R.id.container,
                        MyProfileFragment.newInstance(false),
                        false,
                        true,
                        null
                    )
                } else {
                    val appForeground = extras.getBoolean(BUNDLE_NOTIFICATION_APP_FOREGROUND)
                    fromNotification.let {
                        startedFromNotification = it
                    }
                    appForeground.let {
                        this.appForeground = it
                    }
                    addReplaceFragment(R.id.container, ProfileFragment.newInstance(userId), false, !startedFromNotification, null)
                }

            }

            FRAGMENT_ACCOUNT_FOLLOWERS -> {
                val userId = extras?.getString(BUNDLE_KEY_USER_ID) ?: ""
                addReplaceFragment(R.id.container, FollowTabsFragment.newInstance(FollowTabsFragment.TAB_FOLLOWERS, userId), false, true, null)
            }

            FRAGMENT_ACCOUNT_FOLLOWING -> {
                val userId = extras?.getString(BUNDLE_KEY_USER_ID) ?: ""
                addReplaceFragment(R.id.container, FollowTabsFragment.newInstance(FollowTabsFragment.TAB_FOLLOWING, userId), false, true, null)
            }

            FRAGMENT_FOLLOW_REQUESTS -> {
                addReplaceFragment(R.id.container, FollowRequestsFragment.newInstance(), false, true, null)
            }

            FRAGMENT_SENT_FOLLOW_REQUESTS -> {
                addReplaceFragment(R.id.container, SentFollowRequestsFragment.newInstance(), false, true, null)
            }

            FRAGMENT_FRIENDS -> {
                addReplaceFragment(R.id.container, FriendsTabsFragment.newInstance(FriendsTabsFragment.TAB_FRIENDS), false, true, null)
            }

            FRAGMENT_INVITES -> {
                addReplaceFragment(R.id.container, FriendsTabsFragment.newInstance(FriendsTabsFragment.TAB_INVITES), false, true, null)
            }

            FRAGMENT_SETTINGS -> {
                val userId = extras?.getString(BUNDLE_KEY_USER_ID) ?: ""
                addReplaceFragment(R.id.container, SettingsFragment.newInstance(userId), false, true, null)
            }

            FRAGMENT_EDIT_PROFILE-> {
                addReplaceFragment(R.id.container, ProfileEditFragment.newInstance(), false, true, null)
            }

            FRAGMENT_WALLET-> {
                addReplaceFragment(R.id.container, WalletFragment.newInstance(), false, true, null)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.no_animation, R.anim.slide_out_to_right)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            if (startedFromNotification) {
                if (!appForeground)
                    startActivity(Intent(this, MainActivity::class.java))

                setResult(Activity.RESULT_OK)
                finish()

                if (!appForeground)
                    overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out)
            } else {
                finish()

                if (supportFragmentManager.fragments.isNotEmpty() &&
                    supportFragmentManager.fragments[0] is ProfileFragment) {
                    overridePendingTransition(R.anim.no_animation, R.anim.slide_out_to_right)
                }

            }
        } else {
            supportFragmentManager.popBackStack()
        }
    }


    companion object {

        fun openProfileFragment(
            context: Context,
            userId: String,
            fromNotification: Boolean? = null,
            appForeground: Boolean = true,
            flags: Int? = null
        ) {
            val bundle = Bundle().apply {
                putString(BUNDLE_KEY_USER_ID, userId)
                if (fromNotification != null) {
                    putBoolean(BUNDLE_KEY_FROM_NOTIFICATION, fromNotification)
                    putBoolean(BUNDLE_NOTIFICATION_APP_FOREGROUND, appForeground)
                }

            }
            openFragment<AccountActivity>(
                context,
                FRAGMENT_PROFILE,
                bundle,
                requestCode = if (fromNotification == true) REQUEST_RESULT_NOTIFICATION else null,
                animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation),
                flags = flags
            )
        }

        fun openFollowersFragment(context: Context, userId: String) {
            val bundle = Bundle().apply { putString(BUNDLE_KEY_USER_ID, userId) }
            openFragment<AccountActivity>(context, FRAGMENT_ACCOUNT_FOLLOWERS, bundle, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left))
        }

        fun openSettingsFragments(context: Context,userId: String) {
            val bundle = Bundle().apply { putString(BUNDLE_KEY_USER_ID, userId) }
            openFragment<AccountActivity>(context, FRAGMENT_SETTINGS, bundle, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left))
        }

        fun openWalletFragments(context: Context) {
            openFragment<AccountActivity>(context, FRAGMENT_WALLET, null, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left))
        }

        fun openFollowingFragment(context: Context, userId: String) {
            val bundle = Bundle().apply { putString(BUNDLE_KEY_USER_ID, userId) }
            openFragment<AccountActivity>(context, FRAGMENT_ACCOUNT_FOLLOWING, bundle, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left))
        }

        fun openFollowerRequestsFragment(context: Context) {
            openFragment<AccountActivity>(context, FRAGMENT_FOLLOW_REQUESTS, null, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left))
        }

        fun openSentFollowerRequestsFragment(context: Context) {
            openFragment<AccountActivity>(context, FRAGMENT_SENT_FOLLOW_REQUESTS, null, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left))
        }

        fun openFriendsFragment(context: Context, userId: String) {
            val bundle = Bundle().apply { putString(BUNDLE_KEY_USER_ID, userId) }
            openFragment<AccountActivity>(context, FRAGMENT_FRIENDS, bundle, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left))
        }

        fun openEditProfileFragment(context: Context) {
            openFragment<AccountActivity>(
                context,
                FRAGMENT_EDIT_PROFILE,
                null,
                animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            )
        }
    }
}
