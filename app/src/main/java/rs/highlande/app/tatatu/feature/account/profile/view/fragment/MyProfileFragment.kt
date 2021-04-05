package rs.highlande.app.tatatu.feature.account.profile.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.getReadableCount
import rs.highlande.app.tatatu.databinding.MyProfileInsightsBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.FollowTabsFragment
import rs.highlande.app.tatatu.feature.account.settings.view.SettingsFragment
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.event.UserFollowEvent


class MyProfileFragment : BaseProfileFragment() {

    private lateinit var insightsBinding: MyProfileInsightsBinding

    companion object {

        val logTag = MyProfileFragment::class.java.simpleName

        private var BUNDLE_FROM_BOTTOM_VIEW = "BUNDLE_FROM_BOTTOM_VIEW"

        fun newInstance(fromBottomView: Boolean = true): MyProfileFragment {
            val bundle = Bundle().apply {
                putBoolean(BUNDLE_FROM_BOTTOM_VIEW, fromBottomView)
            }
            val instance = MyProfileFragment()
            instance.arguments = bundle
            return instance
        }
    }

    private var fromBottomView = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        (profileBinding.profileToolbar as Toolbar).inflateMenu(R.menu.menu_my_profile)
        (profileBinding.profileToolbar as Toolbar).setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionNewCall -> ChatActivity.openCallsCreationFragment(context!!)
                R.id.actionSettings -> {
                    if (!getUser()?.uid.isNullOrBlank()) {
                        if (fromBottomView)
                            AccountActivity.openSettingsFragments(context!!, getUser()!!.uid)
                        else {
                            addReplaceFragment(
                                R.id.container,
                                SettingsFragment.newInstance(getUser()!!.uid),
                                false,
                                true,
                                NavigationAnimationHolder()
                            )
                        }
                    }
                }
            }
            true
        }
        setHasOptionsMenu(true)
        return profileBinding.root
    }

    override fun onStart() {
        super.onStart()

        loadProfileData()
        EventBus.getDefault().register(this)

    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(logTag)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }


    override fun loadProfileData() {
        getUser()?.let {
            updateProfileUI(it)
            arguments?.let {
                fromBottomView = it.getBoolean(BUNDLE_FROM_BOTTOM_VIEW, true)
                if (fromBottomView) {
                    profileBinding.profileToolbar.backArrow.visibility = View.INVISIBLE
                } else {
                    profileBinding.profileToolbar.backArrow.setOnClickListener {
                        activity?.onBackPressed()
                    }
                }
            }
            profileBinding.profileToolbar.title.text = it.username
        }
    }

    override fun configureLayout(view: View) {
        getUser()?.let {
            viewModel.setUser(it, false)
        }
    }
    override fun bindLayout() {
        profileBinding.profileactionViewstub.setOnInflateListener { _, view ->
            DataBindingUtil.bind<MyProfileInsightsBinding>(view)?.let {
                insightsBinding = it
                insightsBinding.profileEditButton.setOnClickListener { view ->
                    if (fromBottomView)
                        AccountActivity.openEditProfileFragment(view.context)
                    else {
                        addReplaceFragment(
                            R.id.container,
                            ProfileEditFragment.newInstance(),
                            false,
                            true,
                            NavigationAnimationHolder()
                        )
                    }
                }
            }
        }
        profileBinding.profileactionViewstub.viewStub?.let {
            it.layoutResource = R.layout.my_profile_insights
            it.inflate()
        }
        setupProfileTabs(true)
    }

    override fun observeOnMyUserAction(user: User) {
        super.observeOnMyUserAction(user)
        viewModel.setUser(user, false)
        profileBinding.sectionFollowers.setOnClickListener {
            if (fromBottomView)
                AccountActivity.openFollowersFragment(context!!, user.uid)
            else {
                addReplaceFragment(
                    R.id.container,
                    FollowTabsFragment.newInstance(FollowTabsFragment.TAB_FOLLOWERS, user.uid),
                    false,
                    true,
                    NavigationAnimationHolder()
                )
            }
        }

        profileBinding.sectionFollowing.setOnClickListener {
            if (fromBottomView)
                AccountActivity.openFollowingFragment(context!!, user.uid)
            else {
                addReplaceFragment(
                    R.id.container,
                    FollowTabsFragment.newInstance(FollowTabsFragment.TAB_FOLLOWING, user.uid),
                    false,
                    true,
                    NavigationAnimationHolder()
                )
            }
        }

        loadProfileData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserFollowEvent(event: UserFollowEvent) {
        viewModel.profileUserLiveData.value?.let {
            when(event.type) {
                0 -> {
                    if (event.newFollow) {
                        it.detailsInfo.followersCount ++
                    } else {
                        it.detailsInfo.followersCount --
                    }
                }
                else -> {
                    if (event.newFollow) {
                        it.detailsInfo.followingCount ++
                    } else {
                        it.detailsInfo.followingCount --
                    }
                }
            }
            profileBinding.userFollowingNumberTextview.text = getReadableCount(resources, it.detailsInfo.followingCount)
            profileBinding.userFollowersNumberTextview.text = getReadableCount(resources, it.detailsInfo.followersCount)
        }
    }

}
