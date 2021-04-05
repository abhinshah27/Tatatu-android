package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.ui.SearchTextWatcher
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter.FriendsAdapter
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.FriendsViewModel
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.InvitesViewModel
import rs.highlande.app.tatatu.feature.account.profile.view.fragment.ProfileFragment
import rs.highlande.app.tatatu.model.FriendsResponse
import rs.highlande.app.tatatu.model.FriendsUser

class FriendsFragment : BaseFollowFragment() {

    companion object {
        fun newInstance(userId: String): FriendsFragment {
            val bundle = Bundle()
            val instance = FriendsFragment()
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var friendsAdapter: FriendsAdapter
    private val viewModel: FriendsViewModel by viewModel()
    private var isSearch = false
    private var first=true
    override fun subscribeToLiveData() {
        viewModel.mFriends.observe(viewLifecycleOwner, Observer {
            when {
                it.isNullOrEmpty() -> setUI(true)
                else -> setDataWithAdapter(it)
            }
            scrollListener.endLoading()
        })
        viewModel.isProgress.observe(viewLifecycleOwner, Observer {
            if (it && first) {
                first=false
                mBaseActivity?.setProgressBarNull()
                showLoader(resources.getString(R.string.loader_fetching_friends))
            } else {
                hideLoader()
            }
        })
    }

    private fun setUI(noResult: Boolean) {
        if (noResult) {
            followBinding.noResultsFoundTextView.visibility = View.VISIBLE
            followBinding.followUserList.visibility = View.GONE
        } else {
            followBinding.noResultsFoundTextView.visibility = View.GONE
            followBinding.followUserList.visibility = View.VISIBLE
        }
    }

    private fun setDataWithAdapter(it: ArrayList<FriendsResponse>) {
        setUI(false)
        when {
            viewModel.mInviteFriendsPageNumber <=1 && it[0].user.isNullOrEmpty() -> setUI(true)
            else -> {
                viewModel.mInviteFriendsPageNumber += 1
                (parentFragment as FriendsTabsFragment).setInvitedCount(it[0].pendingCount!!)
                LogUtils.e(CommonTAG, "size-->${viewModel.mCachedOriginalList?.size}")
                when {
                    viewModel.replaceItemsOnNextUpdate || isSearch -> {
                        if (!isSearch) {
                            viewModel.mFriendsAllData.value?.clear()
                            viewModel.mFriendsAllData.value?.addAll(it[0].user!!)
                        }
                        viewModel.mSignedCount = it[0].signedCount!!
                        friendsAdapter.setItems(it[0].user!!.toMutableList())
                        viewModel.replaceItemsOnNextUpdate = false
                    }
                    else -> {
                        viewModel.mFriendsAllData.value?.addAll(it[0].user!!)
                        friendsAdapter.addAll(it[0].user!!.toMutableList())
                    }
                }
            }
        }
    }

    override fun configureLayout(view: View) {
        super.configureLayout(view)

        followBinding.followSRL.isEnabled = false

        getUser()?.let {
            viewModel.setUser(it, false)
        }
        friendsAdapter = FriendsAdapter(object : OnItemClickListener<FriendsUser> {
            override fun onItemClick(item: FriendsUser) {
                addReplaceFragment(R.id.container, ProfileFragment.newInstance(item.mainUserInfo!!.uid!!), false, true, NavigationAnimationHolder())
            }
        })
    }

    override fun bindLayout() {
        with(followBinding.followSearchBox) {
            searchLabelEditText.setHint(R.string.follow_search_friends_label)
            searchLabelEditText.setOnFocusChangeListener { view, b ->
                if (searchLabelEditText.hasFocus()) {
                    scrollListener.isUpdateOnScrollEnabled = false
                    clearSearchIconImageView.visibility = View.VISIBLE
                    isSearch = true
                    (parentFragment as FriendsTabsFragment).expandActionBar(false)
                } else {
                    scrollListener.isUpdateOnScrollEnabled = true
                    clearSearchIconImageView.visibility = View.GONE
                    isSearch = false
                    followBinding.noResultsFoundTextView.visibility = View.GONE
                    followBinding.followUserList.visibility = View.VISIBLE
                }
                clearSearchIconImageView.setOnClickListener {
                    searchLabelEditText.setText("")
                    searchLabelEditText.clearFocus()
                    clearSearchIconImageView.visibility = View.GONE
                    isSearch = false
                    viewModel.mCachedOriginalList?.let {
                        friendsAdapter.setItems(it.toMutableList())
                    }
                }
            }

            searchLabelEditText.addTextChangedListener(SearchTextWatcher(object : SearchTextWatcher.SearchListener {
                override fun onQueryStringAvailable(query: String) {
                    if (viewModel.mCachedOriginalList.isNullOrEmpty()) {
                        viewModel.mCachedOriginalList = viewModel.mFriendsAllData.value
                        LogUtils.e(CommonTAG, "Search Size-->${viewModel.mCachedOriginalList?.size}")
                    }
                    isSearch = true
                    clearSearchIconImageView.visibility = View.VISIBLE
                    viewModel.getPerformSearch(InvitesViewModel.InviteType.REGISTERED.value, query)
                }

                override fun onQueryStringIsEmpty() {
                    searchLabelEditText.clearFocus()
                    clearSearchIconImageView.visibility = View.GONE
                    isSearch = false
                    viewModel.mCachedOriginalList?.let {
                        friendsAdapter.setItems(it.toMutableList())
                    }
                }
            }))
        }
        viewModel.replaceItemsOnNextUpdate = true
        fetchFriends(true)
//        viewModel.dummyData()
        followBinding.followUserList.addOnScrollListener(scrollListener)
        followBinding.followUserList.adapter = friendsAdapter
    }

    override fun updateList() {}

    fun hasMoreItem(): Boolean {
        return if (viewModel.mSignedCount != -1) {
            viewModel.mSignedCount != friendsAdapter.itemCount
        } else {
            true
        }
    }

    private val scrollListener = object : CommonScrollListener() {
        override fun hasMoreItems() = hasMoreItem()
        override fun startLoading() {
//            showProgressBar()
            fetchFriends()
//            viewModel.dummyData()
        }
    }


    override fun onPause() {
        viewModel.clearObservers(this)
        super.onPause()
    }

    fun fetchFriends(resetPageCount: Boolean = false) {
        if (resetPageCount) {

            viewModel.mInviteFriendsPageNumber = 1
            viewModel.getUsersInvited(InvitesViewModel.InviteType.REGISTERED.value)
        } else {
            viewModel.getUsersInvited(InvitesViewModel.InviteType.REGISTERED.value, "", viewModel.mInviteFriendsPageNumber)
        }
    }
}