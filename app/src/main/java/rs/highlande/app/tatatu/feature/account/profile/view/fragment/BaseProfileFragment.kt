package rs.highlande.app.tatatu.feature.account.profile.view.fragment

import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import kotlinx.android.synthetic.main.fragment_profile.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.recyclerView.NestedItemVisibilityListener
import rs.highlande.app.tatatu.core.util.getReadableCount
import rs.highlande.app.tatatu.core.util.removeUnderlines
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.FragmentProfileBinding
import rs.highlande.app.tatatu.feature.account.profile.view.ProfileViewModel
import rs.highlande.app.tatatu.model.User

abstract class BaseProfileFragment: BaseFragment() {

    protected lateinit var profileBinding: FragmentProfileBinding
    private var tabsReady = false

    protected val viewModel by sharedViewModel<ProfileViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        profileBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)

        configureLayout(profileBinding.root)

        return profileBinding.root
    }

    protected fun setupProfileTabs(showCreateItem: Boolean) {
        if (tabsReady)
            return

        tabsReady = true
        addReplaceFragment(
            R.id.tabContainer,
            ProfilePostsFragment.newInstance(showCreateItem),
            addFragment = false, addToBackStack = false, animationHolder = null, nested = true
        )

        //TODO: Uncomment when moments and tags are available
        /*profileBinding.profileTablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val fragmentTransaction = childFragmentManager.beginTransaction()
                    when(it.position) {
                        0 -> fragmentTransaction.replace(R.uid.tab_container, ProfilePostsFragment.newInstance(showCreateItem))
                        1 -> fragmentTransaction.replace(R.uid.tab_container, ProfileTagsFragment())
                        2 -> fragmentTransaction.replace(R.uid.tab_container, ProfileMomentsFragment.newInstance(showCreateItem))
                    }
                    fragmentTransaction.commit()
                }
            }
        })*/
    }

    protected open fun updateProfileUI(user: User) {
        profileBinding.apply {

            userFullnameTextview.text = user.name
            userDescriptionTextview.text = user.detailsInfo.details

            followGroup.visibility = View.VISIBLE

            user.detailsInfo.followingCount.let {
                userFollowingNumberTextview.text = getReadableCount(resources, it)
            }

            user.detailsInfo.followersCount.let {
                userFollowersNumberTextview.text = getReadableCount(resources, it)
            }

            user.detailsInfo.website.let {
                userWebSiteTextView.text = it
                userWebSiteTextView.visibility = View.VISIBLE
                removeUnderlines(SpannableString.valueOf(userWebSiteTextView.text))
            }

            userTitleTextView.apply {
                val string = user.accountType.toString(context)
                text = string
                visibility = if (!string.isBlank()) View.VISIBLE else View.GONE
            }

            userPicture.apply {
                picture.setProfilePicture(user.picture)
                celebrityIndicator.visibility = if (user.isCelebrity()) View.VISIBLE else View.GONE
            }

            // INFO: 2019-08-28    Account verification is currently disabled
            userFullnameTextview.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                /*if (user.verified) R.drawable.ic_ttu_badge else*/ 0,
                0
            )

            //TODO: Uncomment when moments and tags are available
            /*user.posts?.let {
                profileBinding.profileTablayout.setTabItemCount(0, it.size)
            }
            user.tagsList?.let {
                profileBinding.profileTablayout.setTabItemCount(1, it.size)
            }
            user.momentsList?.let {
                profileBinding.profileTablayout.setTabItemCount(2, it.size)
            }*/

            if (!user.detailsInfo.donatingInfo.isBlank()) donationList.text = user.detailsInfo.donatingInfo
            else donatingGroup.visibility = View.GONE

            profileLoader.progressbar.hide()
            profileLoader.root.visibility = View.GONE
            hideProgressBar()
        }
    }

    protected abstract fun loadProfileData()

    protected open fun subscribeToLiveData() {
        viewModel.profileErrorResponseLiveData.observe(viewLifecycleOwner, Observer {
            if (profileBinding.profileLoader.root.visibility == View.VISIBLE) {
                profileBinding.profileLoader.progressbar.hide()
                profileBinding.profileLoader.root.visibility = View.GONE
                showMessage(getString(R.string.profile_fetch_failed))
            }
            hideProgressBar()
        })
    }
    fun showProgressBar() {
        //showLoader(R.string.progress_profile)
    }

    fun hideProgressBar() {
        //hideLoader()
    }

    private var previousScrollY: Int? = null
    fun addScrollListener(postList: RecyclerView?) {
        postList?.let { recView ->
            scrollView.setOnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, _, _: Int ->

                val dy: Int
                if (previousScrollY == null) {
                    dy = scrollY
                    previousScrollY = scrollY
                }
                else {
                    dy = scrollY - previousScrollY!!
                    previousScrollY = scrollY
                }

                v?.let {
                    if (it.getChildAt(v.childCount - 1) != null) {
                        (recView.adapter as? NestedItemVisibilityListener)?.onScrolled(recView, scrollX, dy)
                    }
                }
            }
        }
    }
}