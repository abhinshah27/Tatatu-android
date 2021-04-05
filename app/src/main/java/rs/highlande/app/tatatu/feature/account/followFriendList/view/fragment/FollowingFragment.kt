package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.manager.RelationshipAction
import rs.highlande.app.tatatu.core.ui.ImageSingleBottomSheetFragment
import rs.highlande.app.tatatu.core.ui.SearchTextWatcher
import rs.highlande.app.tatatu.core.ui.hideKeyboard
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_USER_ID
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter.FollowingAdapter
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.FollowingViewModel
import rs.highlande.app.tatatu.model.Relationship
import rs.highlande.app.tatatu.model.User

class FollowingFragment: BaseFollowFragment() {
    companion object {
        fun newInstance(userId: String): FollowingFragment {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_USER_ID, userId)
            val instance = FollowingFragment()
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var followingAdapter: FollowingAdapter

    val viewModel by viewModel<FollowingViewModel>()

    override fun subscribeToLiveData() {
        viewModel.profileErrorResponseLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
        })
        viewModel.searchResultLiveData.observe(viewLifecycleOwner, Observer {
            followBinding.followSRL.isRefreshing = false
            if (it.isEmpty()) {
                followBinding.noResultsFoundTextView.visibility = View.VISIBLE
                followBinding.followUserList.visibility = View.GONE
            } else {
                followBinding.noResultsFoundTextView.visibility = View.GONE
                followBinding.followUserList.visibility = View.VISIBLE
                followingAdapter.setItems(it.toMutableList())
            }
        })
        viewModel.followingListLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
            if (it.isNotEmpty()) {
                viewModel.currentPage += 1

                (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
                if (viewModel.replaceItemsOnNextUpdate) {
                    followingAdapter.setItems(it.toMutableList())
                    viewModel.replaceItemsOnNextUpdate = false
                } else {
                    followingAdapter.addAll(it)
                }
                viewModel.cachedOriginalList = it
            } else {
                followBinding.noResultsFoundTextView.visibility = View.VISIBLE
                followBinding.followUserList.visibility = View.GONE
                followBinding.noResultsFoundTextView.text = getString(R.string.follow_empty_list, getString(R.string.profile_following))
            }
            scrollListener.endLoading()
        })
        viewModel.profileUserLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.itemCount = it.detailsInfo.followingCount
                updateList()
            }
        })
        viewModel.relationshipChangeLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                var remove = false
                getUser()?.let { user ->
                    viewModel.profileUserLiveData.value?.let {profileUser ->
                        if (user.uid == profileUser.uid) {
                            if (it.second.detailsInfo.relationship != Relationship.FOLLOWING &&
                                it.second.detailsInfo.relationship != Relationship.FRIENDS) {
                                remove = true
                            }
                        }
                    }
                }
                if (remove) {
                    followingAdapter.remove(it.second)
                } else {
                    followingAdapter.updateItem(it.second)
                }
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
    }

    override fun configureLayout(view: View) {
        super.configureLayout(view)
        arguments?.let {
            it.getString(BUNDLE_KEY_USER_ID)?.let {userID ->
                viewModel.fetchUser(userID)
            }
        }
        followingAdapter = FollowingAdapter(object: FollowClickListener(this, getUser()) {
            override fun onActionClick(user: User) {
                viewModel.onFollowerRelationshipActionClick(user)
            }
        })
    }

    override fun bindLayout() {

        with(followBinding.followSearchBox) {

            searchLabelEditText.setHint(R.string.follow_search_following_label)

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

            searchLabelEditText.addTextChangedListener(SearchTextWatcher(object: SearchTextWatcher.SearchListener {
                override fun onQueryStringIsEmpty() {
                    with(followBinding.followSearchBox) {
                        searchLabelEditText.clearFocus()
                        searchLabelEditText.hideKeyboard()
                        clearSearchIconImageView.visibility = View.GONE
                        viewModel.cancelSearch()
                        viewModel.cachedOriginalList?.let {
                            followingAdapter.setItems(it.toMutableList())
                        }
                    }
                }

                override fun onQueryStringAvailable(query: String) {
                    followBinding.followSRL.isRefreshing = true
                    viewModel.performSearch(query)
                }
            }))

        }

        followBinding.followUserList.addOnScrollListener(scrollListener)

        followBinding.followUserList.adapter = followingAdapter

    }

    override fun updateList() {
        followBinding.followSRL.isRefreshing = true
        viewModel.replaceItemsOnNextUpdate = true
        viewModel.fetchFollowingList(true)
    }

    private val scrollListener = object: CommonScrollListener() {
        override fun hasMoreItems() = viewModel.hasMoreItems()

        override fun startLoading() {
            showProgressBar()
            viewModel.fetchFollowingList()
        }

        override fun endLoading() {
            super.endLoading()
            hideProgressBar()
        }

    }

    override fun onPause() {
        viewModel.clearObservers(this)
        super.onPause()
    }

}
