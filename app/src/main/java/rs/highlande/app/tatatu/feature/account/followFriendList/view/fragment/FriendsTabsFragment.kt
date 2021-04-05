package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.databinding.FragmentFollowTabsBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.account.settings.view.SettingsFragment
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.model.User

class FriendsTabsFragment : BaseFragment() {

    companion object {

        val logTag = FriendsTabsFragment::class.java.simpleName
        private const val ANALYTICS_TAG = "FriendsInviteFragment"
        private const val CONTEXT_FRIENDS = "Friends"
        private const val CONTEXT_INVITED = "Invited"

        private const val STARTING_TAB = "STARTING_TAB"
        const val TAB_FRIENDS = 0
        const val TAB_INVITES = 1

        fun newInstance(startTab: Int = 0): FriendsTabsFragment {
            val bundle = Bundle()
            bundle.putInt(STARTING_TAB, startTab)
            val instance = FriendsTabsFragment()
            instance.arguments = bundle
            return instance
        }

    }

    private lateinit var followTabsBinding: FragmentFollowTabsBinding
    private var tabsReady = false

    private var currentTab: Int? = null

    private var lastInvitedCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        followTabsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_follow_tabs, container, false)
        configureLayout(followTabsBinding.root)
        return followTabsBinding.root
    }

    override fun onStart() {
        super.onStart()

        when(arguments?.getInt(
            STARTING_TAB,
            TAB_FRIENDS
        )) {
            TAB_FRIENDS -> AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_FRIENDS)
            TAB_INVITES -> AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_INVITED)
        }
    }

    fun setupTabs(startingTab: Int) {
        if (tabsReady)
            return

        currentTab = startingTab

        getUser()?.let { user ->
            tabsReady = true
            val fragmentTransaction = childFragmentManager.beginTransaction()
            when(startingTab) {
                TAB_FRIENDS -> {
                    fragmentTransaction.replace(R.id.tabContainer,
                        if (user.balanceInfo.friendsCount == 0) {
                            NoFriendsFragment.newInstance(user.uid)
                        } else {
                            FriendsFragment.newInstance(user.uid)
                        }
                    )
                }
                TAB_INVITES -> {
                    followTabsBinding.followTablayout.getTabAt(TAB_INVITES)!!.select()
                    fragmentTransaction.replace(R.id.tabContainer,
                        InvitesFragment.newInstance(user.uid)
                    )
                }
            }
            fragmentTransaction.commit()

            followTabsBinding.followTablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {

                        currentTab = it.position

                        val fragmentTransaction = childFragmentManager.beginTransaction()
                        when(it.position) {

                            0 -> {
                                AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_FRIENDS)

                                fragmentTransaction.replace(
                                    R.id.tabContainer,
                                    if (user.balanceInfo.friendsCount == 0) {
                                        NoFriendsFragment.newInstance(user.uid)
                                    } else {
                                        FriendsFragment.newInstance(user.uid)
                                    }
                                )
                            }
                            1 -> {
                                AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_INVITED)

                                fragmentTransaction.replace(R.id.tabContainer,
                                    InvitesFragment.newInstance(user.uid)
                                )
                            }
                        }
                        fragmentTransaction.commit()
                    }
                }
            })
        }
    }

    override fun onStop() {
        arguments?.putInt(STARTING_TAB, currentTab ?: TAB_FRIENDS)
        tabsReady = false
        super.onStop()
    }

    override fun configureLayout(view: View) {
        (activity as? AccountActivity)?.apply {
            setSupportActionBar(followTabsBinding.followToolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            followTabsBinding.followToolbar.backArrow.setOnClickListener {
                activity!!.onBackPressed()
            }
            setHasOptionsMenu(true)
        }
    }

    override fun bindLayout() {}

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_my_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionNewCall -> ChatActivity.openCallsCreationFragment(context!!)
            R.id.actionSettings -> {
                addReplaceFragment(R.id.container, SettingsFragment.newInstance(getUser()?.uid ?: ""), false, true, NavigationAnimationHolder())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun expandActionBar(expand: Boolean) {
        followTabsBinding.appBar.setExpanded(expand)
    }

    override fun observeOnMyUserAction(user: User) {
        arguments?.let {
            setupTabs(it.getInt(
                STARTING_TAB,
                TAB_FRIENDS
            ))
        }

        followTabsBinding.followTablayout.getTabAt(0)?.let {
            it.text = context!!.resources.getString(R.string.profile_friends).plus(" ").plus(user.balanceInfo.friendsCount)
        }

        // count comes only from server call: initially set to 0 and then to lastInvitedCount
        followTabsBinding.followTablayout.getTabAt(1)?.let {
            it.text = context!!.resources.getString(R.string.profile_invites).plus(" ").plus(lastInvitedCount)
        }

        followTabsBinding.followToolbar.title.text = user.username
    }


    fun setFriendCount(count: Int) {
        followTabsBinding.followTablayout.getTabAt(0)?.let {
            it.text = context!!.resources.getString(R.string.profile_friends).plus(" ").plus(count)
        }
    }

    fun setInvitedCount(count: Int) {
        lastInvitedCount = count
        followTabsBinding.followTablayout.getTabAt(1)?.let {
            LogUtils.d(logTag, "testCOUNT: Setting Invited count to $lastInvitedCount")
            it.text = context!!.resources.getString(R.string.profile_invites).plus(" ").plus(lastInvitedCount)
        }
    }
}