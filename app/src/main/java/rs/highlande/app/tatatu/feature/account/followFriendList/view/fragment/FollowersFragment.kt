package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.manager.RelationshipAction
import rs.highlande.app.tatatu.core.ui.ImageSingleBottomSheetFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.ui.SearchTextWatcher
import rs.highlande.app.tatatu.core.ui.hideKeyboard
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_USER_ID
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.FollowRequestsActionBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter.FollowersAdapter
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.FollowersViewModel
import rs.highlande.app.tatatu.model.User

class FollowersFragment: BaseFollowFragment() {

    companion object {
        fun newInstance(userId: String): FollowersFragment {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_USER_ID, userId)
            val instance = FollowersFragment()
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var followersAdapter: FollowersAdapter
    private lateinit var followersRequestsBinding: FollowRequestsActionBinding
    private val viewModel: FollowersViewModel by viewModel()

    override fun subscribeToLiveData() {
        viewModel.profileErrorResponseLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
        })
        viewModel.relationshipChangeLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                followersAdapter.updateItem(it.second)
            } else {
                //TODO: 11/07 show error
            }
        })
        viewModel.actionRequestLiveData.observe(viewLifecycleOwner, Observer {
            when(it) {
                RelationshipAction.FOLLOWING_FRIENDS_ACTION -> {
                    val bottomSheet = ImageSingleBottomSheetFragment.newInstance(object : ImageSingleBottomSheetFragment.BottomSheetListener {
                        override fun onBottomSheetReady(bottomSheet: ImageSingleBottomSheetFragment) {
                            viewModel.cachedFollower?.let {
                                bottomSheet.binding.bottomSheetAction.setText(R.string.bottom_sheet_unfollow)
                                bottomSheet.binding.bottomSheetHint.setText(R.string.bottom_sheet_tap_unfollow_hint)

                                bottomSheet.binding.bottomImageView.setProfilePicture(viewModel.cachedFollower!!.picture)

                                bottomSheet.binding.bottomSheetAction.setOnClickListener {
                                    showMessage(getString(R.string.result_action_unfollow, viewModel.cachedFollower!!.username))
                                    viewModel.handleFollowerRelationChange()
                                    bottomSheet.dismiss()
                                }
                            }
                        }
                    })
                    bottomSheet.show(childFragmentManager, "bottomSheet")
                }
                RelationshipAction.FOLLOW_BACK_ACTION -> {
                    viewModel.handleFollowerRelationChange()
                }
                RelationshipAction.REQUEST_CANCEL_ACTION -> {
                    val bottomSheet = ImageSingleBottomSheetFragment.newInstance(object : ImageSingleBottomSheetFragment.BottomSheetListener {
                        override fun onBottomSheetReady(bottomSheet: ImageSingleBottomSheetFragment) {
                            viewModel.cachedFollower?.let {
                                bottomSheet.binding.bottomSheetAction.setText(R.string.bottom_sheet_cancel_request)
                                bottomSheet.binding.bottomSheetHint.setText(R.string.bottom_sheet_tap_cancel_request_hint)

                                bottomSheet.binding.bottomImageView.setProfilePicture(it.picture)

                                bottomSheet.binding.bottomSheetAction.setOnClickListener {
                                    showMessage(getString(R.string.result_action_cancel_request, viewModel.cachedFollower!!.username))
                                    viewModel.handleFollowerRelationChange()
                                    bottomSheet.dismiss()
                                }
                            }
                        }
                    })
                    bottomSheet.show(childFragmentManager, "bottomSheet")
                }
                RelationshipAction.FOLLOW_ACTION -> viewModel.handleFollowerRelationChange()
            }
        })
        viewModel.searchResultLiveData.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                followBinding.noResultsFoundTextView.text = getString(R.string.no_results_found)
                followBinding.noResultsFoundTextView.visibility = View.VISIBLE
                followBinding.followUserList.visibility = View.GONE
            } else {
                followBinding.noResultsFoundTextView.visibility = View.GONE
                followBinding.followUserList.visibility = View.VISIBLE
                followersAdapter.setItems(it.toMutableList())
            }
        })
        viewModel.followersListLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
            if (it.isNotEmpty()) {
                followBinding.noResultsFoundTextView.visibility = View.GONE
                followBinding.followUserList.visibility = View.VISIBLE
                viewModel.currentPage += 1
                if (viewModel.replaceItemsOnNextUpdate) {
                    followersAdapter.setItems(it.toMutableList())
                    viewModel.replaceItemsOnNextUpdate = false
                } else {
                    followersAdapter.addAll(it)
                }
                viewModel.cachedOriginalList = it
            } else {
                followBinding.noResultsFoundTextView.visibility = View.VISIBLE
                followBinding.followUserList.visibility = View.GONE
                followBinding.noResultsFoundTextView.text = getString(R.string.follow_empty_list, getString(R.string.profile_followers))
            }
            scrollListener.endLoading()

        })
        viewModel.profileUserLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.itemCount = it.detailsInfo.followersCount
                showFollowRequests()
                updateList()
            }
        })
    }

    override fun configureLayout(view: View) {
        super.configureLayout(view)
        arguments?.let {
            it.getString(BUNDLE_KEY_USER_ID)?.let {userID ->
                viewModel.fetchUser(userID)
            }
        }
        followersAdapter = FollowersAdapter(object : FollowClickListener(this, getUser()) {
            override fun onActionClick(user: User) {
                viewModel.onFollowerRelationshipActionClick(user)
            }
        })
    }

    override fun bindLayout() {

        with(followBinding.followSearchBox) {
            searchLabelEditText.setOnFocusChangeListener { _, _ ->
                if (searchLabelEditText.hasFocus()) {
                    scrollListener.isUpdateOnScrollEnabled = false
                    clearSearchIconImageView.visibility = View.VISIBLE
                    (parentFragment as FollowTabsFragment).expandActionBar(false)
                } else {
                    scrollListener.isUpdateOnScrollEnabled = true
                    clearSearchIconImageView.visibility = View.GONE
                    followBinding.noResultsFoundTextView.visibility = View.GONE
                    followBinding.followUserList.visibility = View.VISIBLE


                }
                clearSearchIconImageView.setOnClickListener {
                    searchLabelEditText.setText("")
                }

            }
            searchLabelEditText.setHint(R.string.follow_search_followers_label)
            searchLabelEditText.addTextChangedListener(SearchTextWatcher(object: SearchTextWatcher.SearchListener {
                override fun onQueryStringIsEmpty() {
                    with(followBinding.followSearchBox) {
                        searchLabelEditText.clearFocus()
                        searchLabelEditText.hideKeyboard()
                        clearSearchIconImageView.visibility = View.GONE
                        viewModel.cancelSearch()
                        viewModel.cachedOriginalList?.let {
                            followersAdapter.setItems(it.toMutableList())
                        }
                    }
                }

                override fun onQueryStringAvailable(query: String) {
                    viewModel.performSearch(query)
                }
            }))
        }

        followBinding.followUserList.addOnScrollListener(scrollListener)

        followBinding.followUserList.adapter = followersAdapter

    }

    override fun updateList() {
        followBinding.followSRL.isRefreshing = true
        viewModel.replaceItemsOnNextUpdate = true
        viewModel.fetchFollowers(true)
    }

    val scrollListener = object: CommonScrollListener() {
        override fun hasMoreItems() = viewModel.hasMoreItems()

        override fun startLoading() {
            showProgressBar()
            viewModel.fetchFollowers()
        }

        override fun endLoading() {
            super.endLoading()
            hideProgressBar()
        }

    }

    private fun showFollowRequests() {
        getUser()?.let {usr ->
            if (viewModel.profileUserLiveData.value!!.uid != usr.uid ) {
                return
            }

            viewModel.fetchFollowCounts()

            followBinding.followViewstub.setOnInflateListener { _, view ->
                DataBindingUtil.bind<FollowRequestsActionBinding>(view)?.let {
                    followersRequestsBinding = it
                    followersRequestsBinding.followRequestsLabelTextView.setText(R.string.follow_requests_label)
                    followersRequestsBinding.root.setOnClickListener {
                        addReplaceFragment(
                            R.id.container,
                            FollowRequestsFragment.newInstance(),
                            false,
                            true,
                            NavigationAnimationHolder()
                        )
                    }
                }
            }
            viewModel.followCountsLiveData.observe(viewLifecycleOwner, Observer {
                followersRequestsBinding.followSentRequestsCountBadge.text = viewModel.sentRequestsCount.toString()
                followersRequestsBinding.followSentRequestsCountBadge.visibility = if (it) View.VISIBLE else View.GONE
            })

            followBinding.followViewstub.viewStub?.let {
                it.layoutResource = R.layout.follow_requests_action
                it.inflate()
            }
        }
    }

    override fun onPause() {
        viewModel.clearObservers(this)
        super.onPause()
    }

}