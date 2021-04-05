package rs.highlande.app.tatatu.feature.commonView

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.account.profile.view.fragment.MyProfileFragment
import rs.highlande.app.tatatu.feature.commonView.MainScreen.Companion.BUNDLE_KEY_MAIN_SCREEN
import rs.highlande.app.tatatu.feature.home.view.HomePageFragment
import rs.highlande.app.tatatu.feature.main.view.MainActivity
import rs.highlande.app.tatatu.feature.notification.view.fragment.NotificationFragment
import rs.highlande.app.tatatu.feature.search.view.fragment.SearchFragment
import rs.highlande.app.tatatu.model.NotificationSimpleResponse
import rs.highlande.app.tatatu.model.event.NotificationEvent

/**
 * Activity holding the properties and methods to handle a bottom navigation
 * @author mbaldrighi on 2019-06-24.
 */
abstract class  BottomNavigationActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    protected var bottomNav: BottomNavigationView? = null
    private var viewPager: ViewPager? = null
    private val pagerAdapter by lazy {
        PagerAdapter(supportFragmentManager)
    }
    private var currentItem = 0
    private var currentItemId = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        manageIntent()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        manageIntent(intent)
    }


    override fun onResume() {
        super.onResume()

        if (isMainActivity()) scrollToScreen(MainScreen.getForMenuItem(currentItemId) ?: MainScreen.HOME) else {
            // for every other activity the bottom bar is "off"
            bottomNav?.menu?.setGroupCheckable(0, false, true)
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationEvent(event: NotificationEvent?) {

        if (event != null) {
            if (event.mNotificationList != null) {
                mNotificationList.clear()
                mNotificationList.addAll(event.mNotificationList!!)
                LogUtils.d(logTag, "onNotificationEvent: ListSize --> ${mNotificationList.size}")
            }
            if (event.mToReadCount != null && event.mToReadCount!! > 0) {
                setBadge(event.mToReadCount!!)
            } else {
                removeBadge()
            }
        } else {
            LogUtils.d(logTag, "onNotificationEvent: Null")
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(bundleNavItem, currentItem)
        outState.putInt(bundleNavItemId, currentItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        currentItem = savedInstanceState.getInt(bundleNavItem)
        currentItemId = savedInstanceState.getInt(bundleNavItemId)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        currentItemId = item.itemId

        return MainScreen.getForMenuItem(item.itemId)?.let {
            if (it == MainScreen.STORE) {
                // setup custom action for STORE item
                fireOpenBrowserIntent(this, getString(R.string.url_redeem))
                false
            } else {
                if (isMainActivity()) scrollToScreen(it)
                else {
                    goToMain(item.itemId)
                }
                true
            }
        } ?: false
    }


    override fun manageIntent() {}

    protected open fun manageIntent(newIntent: Intent?) {
        val int = newIntent ?: intent

        currentItemId = (int.getSerializableExtra(BUNDLE_KEY_MAIN_SCREEN) as? MainScreen)?.menuItemId ?: 0
    }


    protected open fun setupBottomNavigation(view: BottomNavigationView, viewPager: ViewPager? = null) {
        this.viewPager = viewPager?.also {
            it.adapter = pagerAdapter
            it.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    // STORE position is still handled but only four fragments appear in PagerAdapter

                    @IdRes
                    val id = when (position) {
                        MainScreen.HOME.ordinal -> R.id.bottomnav_home
                        MainScreen.ACCOUNT.ordinal -> R.id.bottomnav_account
                        MainScreen.SEARCH.ordinal -> R.id.bottomnav_search
                        MainScreen.NOTIFICATION.ordinal -> R.id.bottomnav_notifications
                        MainScreen.STORE.ordinal -> R.id.bottomnav_store

                        else -> R.id.bottomnav_home
                    }

                    selectBottomNavigationViewMenuItem(id)
                }
            })
        }

        scrollToScreen(MainScreen.HOME)

        bottomNav = view.also {
            it.setOnNavigationItemSelectedListener(this)
        }

    }

    /**
     * Scrolls ViewPager to show the provided screen.
     */
    private fun scrollToScreen(screen: MainScreen) {
        if (screen != MainScreen.STORE && screen.ordinal != viewPager?.currentItem) {
            currentItem = screen.ordinal
            viewPager?.currentItem = screen.ordinal
        }
    }

    /**
     * Selects the specified item in the bottom navigation view.
     */
    private fun selectBottomNavigationViewMenuItem(@IdRes menuItemId: Int) {
        bottomNav?.setOnNavigationItemSelectedListener(null)
        bottomNav?.selectedItemId = menuItemId
        bottomNav?.setOnNavigationItemSelectedListener(this)
    }

    /**
     * Function that check if the current instance is a valid [MainActivity] instance.
     */
    private fun isMainActivity(): Boolean {
        return this is MainActivity
    }

    private fun goToMain(@IdRes menuItemId: Int) {

        var hasToReturn = false

        val onScreen = when (menuItemId) {
            R.id.bottomnav_account -> MainScreen.ACCOUNT
            R.id.bottomnav_search -> MainScreen.SEARCH
            R.id.bottomnav_store -> {
                hasToReturn = true
                MainScreen.STORE
            }
            R.id.bottomnav_notifications -> MainScreen.NOTIFICATION
            else -> MainScreen.HOME

        }

        if (hasToReturn) {
            // STORE item was selected: MainActivity is not called again
            return
        }

        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BUNDLE_KEY_MAIN_SCREEN, onScreen)
        })
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_to_right)
    }


    /**
     * Subclass of [FragmentStatePagerAdapter] handling screen changes for MainActivity
     */
    private class PagerAdapter(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                NAV_PAGE_HOME -> HomePageFragment.newInstance()
                NAV_PAGE_ACCOUNT -> MyProfileFragment.newInstance()
                NAV_PAGE_SEARCH -> SearchFragment.newInstance()
                NAV_PAGE_NOTIF -> NotificationFragment.newInstance()
                else -> HomePageFragment.newInstance()
            }
        }

        /**
         * Only four fragments are handled by ViewPager. [MainScreen.STORE] item is handled with a
         * custom click action.
         */
        override fun getCount(): Int {
            return 4
        }
    }

    companion object {

        val logTag = BottomNavigationActivity::class.java.simpleName

        const val bundleNavItem = "navItem"
        const val bundleNavItemId = "navItemId"
        var mNotificationList = ArrayList<NotificationSimpleResponse>()
    }

    fun setBadge(count: Int) {
        bottomNav?.getOrCreateBadge(R.id.bottomnav_notifications) // Show badge
        val badge = bottomNav?.getBadge(R.id.bottomnav_notifications) // Get badge
        badge?.badgeGravity = BadgeDrawable.TOP_END
        badge?.number = count
    }

    fun removeBadge() {
        bottomNav?.removeBadge(R.id.bottomnav_notifications) // Remove badge
    }
}

/**
 * Screens available for display in the main screen, with their respective titles,
 * icons, and menu item IDs and fragments.
 */
enum class MainScreen(@IdRes val menuItemId: Int) {
    HOME(R.id.bottomnav_home), ACCOUNT(R.id.bottomnav_account), SEARCH(R.id.bottomnav_search), NOTIFICATION(R.id.bottomnav_notifications), STORE(R.id.bottomnav_store);

    companion object {
        const val BUNDLE_KEY_MAIN_SCREEN = "MAIN_SCREEN"

        fun getForMenuItem(menuItemId: Int): MainScreen? {
            for (mainScreen in values()) {
                if (mainScreen.menuItemId == menuItemId) {
                    return mainScreen
                }
            }
            return null
        }
    }
}

