package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_USER_ID
import rs.highlande.app.tatatu.databinding.FragmentFollowTabsBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.FollowTabsViewModel
import rs.highlande.app.tatatu.feature.account.settings.view.SettingsFragment
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.model.event.UserFollowEvent


class FollowTabsFragment : BaseFragment() {

    companion object {

        private const val STARTING_TAB = "STARTING_TAB"
        const val TAB_FOLLOWERS = 0
        const val TAB_FOLLOWING = 1

        val logTag = FollowTabsFragment::class.java.simpleName
        private const val ANALYTICS_TAG = "FollowersFollowingFragment"
        private const val CONTEXT_FOLLOWERS = "Followers"
        private const val CONTEXT_FOLLOWING = "Following"

        fun newInstance(startTab: Int = 0, userID: String): FollowTabsFragment {
            val bundle = Bundle()
            bundle.putInt(STARTING_TAB, startTab)
            bundle.putString(BUNDLE_KEY_USER_ID, userID)
            val instance = FollowTabsFragment()
            instance.arguments = bundle
            return instance
        }

    }

    private val viewModel by viewModel<FollowTabsViewModel>()

    private lateinit var followTabsBinding: FragmentFollowTabsBinding
    private var tabsReady = false

    private var currentTab: Int? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeToLiveData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        followTabsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_follow_tabs, container, false)
        configureLayout(followTabsBinding.root)
        return followTabsBinding.root
    }

    fun setupTabs(startingTab: Int) {
        if (tabsReady)
            return

        currentTab = startingTab

        viewModel.profileUserLiveData.value?.let { user ->
            tabsReady = true
            val fragmentTransaction = childFragmentManager.beginTransaction()
            when(startingTab) {
                TAB_FOLLOWERS -> {
                    fragmentTransaction.replace(R.id.tabContainer,
                        FollowersFragment.newInstance(user.uid)
                    )
                }
                TAB_FOLLOWING -> {
                    followTabsBinding.followTablayout.getTabAt(TAB_FOLLOWING)!!.select()
                    fragmentTransaction.replace(R.id.tabContainer,
                        FollowingFragment.newInstance(user.uid)
                    )
                }
            }
            fragmentTransaction.commit()
            hideLoader()
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
                                AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_FOLLOWERS)

                                fragmentTransaction.replace(R.id.tabContainer,
                                    FollowersFragment.newInstance(user.uid)
                                )
                            }
                            1 -> {
                                AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_FOLLOWING)

                                fragmentTransaction.replace(
                                    R.id.tabContainer,
                                    FollowingFragment.newInstance(user.uid)
                                )
                            }
                        }
                        fragmentTransaction.commit()

                    }
                }
            })
        }
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
        arguments?.let {
            it.getString(BUNDLE_KEY_USER_ID)?.let {userID ->
                viewModel.fetchUser(userID)
                viewModel.profileErrorResponseLiveData.observe(viewLifecycleOwner, Observer {
                    hideLoader()
                    showError(getString(R.string.error_generic))
                    viewModel.profileErrorResponseLiveData.removeObservers(viewLifecycleOwner)
                })
                showLoader(getString(R.string.loader_fetching_follow))
            }
        }
    }

    fun subscribeToLiveData() {
        viewModel.profileUserLiveData.observe(viewLifecycleOwner, Observer {user ->
            user?.let { _ ->
                arguments?.let {
                    setupTabs(
                        it.getInt(
                            STARTING_TAB,
                            TAB_FOLLOWERS
                        )
                    )
                }
                followTabsBinding.followTablayout.getTabAt(0)?.let {
                    it.text = context!!.resources.getString(R.string.profile_followers).plus(" ")
                        .plus(user.detailsInfo.followersCount)
                }
                followTabsBinding.followTablayout.getTabAt(1)?.let {
                    it.text = context!!.resources.getString(R.string.profile_following).plus(" ")
                        .plus(user.detailsInfo.followingCount)
                }

                followTabsBinding.followToolbar.title.text = user.username
            }
        })
    }

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

    override fun onStart() {
        super.onStart()

        when(arguments?.getInt(
            STARTING_TAB,
            TAB_FOLLOWERS
        )) {
            TAB_FOLLOWERS -> AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_FOLLOWERS)
            TAB_FOLLOWING -> AnalyticsUtils.trackScreen(ANALYTICS_TAG, CONTEXT_FOLLOWING)
        }

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        arguments?.putInt(STARTING_TAB, currentTab ?: TAB_FOLLOWERS)
        tabsReady = false
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun bindLayout() {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserFollowEvent(event: UserFollowEvent) {
        viewModel.profileUserLiveData.value?.let { user->

            when(event.type) {
                0 -> {
                    if (event.newFollow) {
                        user.detailsInfo.followersCount ++
                    } else {
                        user.detailsInfo.followersCount --
                    }
                }
                else -> {
                    if (event.newFollow) {
                        user.detailsInfo.followingCount ++
                    } else {
                        user.detailsInfo.followingCount --
                    }
                }
            }
            followTabsBinding.followTablayout.getTabAt(0)?.let {
                it.text = context!!.resources.getString(R.string.profile_followers).plus(" ")
                    .plus(user.detailsInfo.followersCount)
            }
            followTabsBinding.followTablayout.getTabAt(1)?.let {
                it.text = context!!.resources.getString(R.string.profile_following).plus(" ")
                    .plus(user.detailsInfo.followingCount)
            }

        }
    }

}