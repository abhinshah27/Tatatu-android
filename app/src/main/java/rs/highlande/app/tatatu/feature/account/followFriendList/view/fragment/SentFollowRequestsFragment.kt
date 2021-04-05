package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.R.string.follow_sent_requests_label
import rs.highlande.app.tatatu.core.ui.SearchTextWatcher
import rs.highlande.app.tatatu.core.ui.hideKeyboard
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter.SentFollowRequestsAdapter
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.SentFollowRequestsViewModel
import rs.highlande.app.tatatu.model.User

class SentFollowRequestsFragment: BaseFollowFragment() {
    companion object {
        val logTag = SentFollowRequestsFragment::class.java.simpleName
        fun newInstance() = SentFollowRequestsFragment()
    }

    private lateinit var sentFollowRequestsAdapter: SentFollowRequestsAdapter
    private val viewModel: SentFollowRequestsViewModel by viewModel()

    override fun subscribeToLiveData() {
        viewModel.profileErrorResponseLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
        })
        viewModel.sentFollowRequestCancelLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                sentFollowRequestsAdapter.remove(it.second)
            } else {
                sentFollowRequestsAdapter.refreshItem(it.second)
                showError(context!!.getString(R.string.error_generic))
            }
        })
        viewModel.searchResultLiveData.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                followBinding.noResultsFoundTextView.visibility = View.VISIBLE
                followBinding.followUserList.visibility = View.GONE
            } else {
                followBinding.noResultsFoundTextView.visibility = View.GONE
                followBinding.followUserList.visibility = View.VISIBLE
                sentFollowRequestsAdapter.setItems(it.toMutableList())
            }
        })
        viewModel.sentFollowRequestsListLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
            if (it.isNotEmpty()) {
                viewModel.currentPage += 1
                (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
                if (viewModel.replaceItemsOnNextUpdate) {
                    sentFollowRequestsAdapter.setItems(it.toMutableList())
                    viewModel.replaceItemsOnNextUpdate = false
                } else {
                    sentFollowRequestsAdapter.addAll(it)
                }
            } else {
                followBinding.noResultsFoundTextView.visibility = View.VISIBLE
                followBinding.followUserList.visibility = View.GONE
                followBinding.noResultsFoundTextView.text = getString(R.string.follow_empty_list, getString(
                    follow_sent_requests_label
                ))
            }
            scrollListener.endLoading()
        })
    }

    override fun observeOnMyUserAction(user: User) {
        if (!ignoreMyUserAction) {
            ignoreMyUserAction = true
            viewModel.setUser(user, false)
            updateList()
        }
    }

    override fun configureLayout(view: View) {
        super.configureLayout(view)

        sentFollowRequestsAdapter = SentFollowRequestsAdapter(object: FollowClickListener(this, getUser()) {
            override fun onActionClick(user: User) {
                viewModel.handleCancelSentRequest(user)
            }
        })

        (activity as? AccountActivity)?.apply {
            followBinding.followToolbar.title.text = resources.getString(follow_sent_requests_label)
            followBinding.followToolbar.visibility = View.VISIBLE
            followBinding.followToolbar.backArrow.setOnClickListener {
                activity!!.onBackPressed()
            }
            setSupportActionBar(followBinding.followToolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun bindLayout() {

        with(followBinding.followSearchBox) {

            searchLabelEditText.setHint(R.string.follow_search_sent_requests_label)

            searchLabelEditText.setOnFocusChangeListener { view, b ->
                if (searchLabelEditText.hasFocus()) {
                    scrollListener.isUpdateOnScrollEnabled = false
                    clearSearchIconImageView.visibility = View.VISIBLE
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
                            sentFollowRequestsAdapter.setItems(it.toMutableList())
                        }
                    }
                }

                override fun onQueryStringAvailable(query: String) {
                    if (viewModel.cachedOriginalList.isNullOrEmpty()) {
                        viewModel.cachedOriginalList = sentFollowRequestsAdapter.getItems()
                    }
                    viewModel.performSearch(query)
                }
            }))

        }

        followBinding.followUserList.addOnScrollListener(scrollListener)
        followBinding.followUserList.adapter = sentFollowRequestsAdapter
    }

    override fun updateList() {
        viewModel.replaceItemsOnNextUpdate = true
        //viewModel.fetchSentFollowRequests(true)
        viewModel.fetchFollowCounts()
    }

    private val scrollListener = object: CommonScrollListener() {
        override fun hasMoreItems() = viewModel.hasMoreItems()

        override fun startLoading() {
            showProgressBar()
            viewModel.fetchSentFollowRequests()
        }

        override fun endLoading() {
            super.endLoading()
            hideProgressBar()
        }

    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    override fun onPause() {
        viewModel.clearObservers(this)
        super.onPause()
    }


}