package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.common_search_box.*
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.ui.SearchTextWatcher
import rs.highlande.app.tatatu.core.ui.hideKeyboard
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.CustomSwipeToDismiss
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.FollowRequestsActionBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter.FollowRequestsAdapter
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.FollowRequestsViewModel
import rs.highlande.app.tatatu.model.User

class FollowRequestsFragment: BaseFollowFragment() {
    companion object {
        val logTag = FollowRequestsFragment::class.java.simpleName

        fun newInstance() = FollowRequestsFragment()
    }

    private lateinit var followRequestsAdapter: FollowRequestsAdapter
    private lateinit var followersRequestsBinding: FollowRequestsActionBinding
    private val viewModel: FollowRequestsViewModel by viewModel()

    override fun subscribeToLiveData() {
        viewModel.profileErrorResponseLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
        })
        viewModel.followRequestsRemovedLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                followRequestsAdapter.remove(it.second)
            } else {
                showError(context!!.getString(R.string.error_generic))
            }
        })
        viewModel.followRequestsConfirmedLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                followRequestsAdapter.remove(it.second)
            } else {
                followRequestsAdapter.refreshItem(it.second)
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
                followRequestsAdapter.setItems(it.toMutableList())
            }
        })
        viewModel.followRequestsListLiveData.observe(viewLifecycleOwner, Observer {
            (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
            if (it.isNotEmpty()) {
                viewModel.currentPage += 1
                (followBinding.followSRL as SwipeRefreshLayout).isRefreshing = false
                if (viewModel.replaceItemsOnNextUpdate) {
                    followRequestsAdapter.setItems(it.toMutableList())
                    viewModel.replaceItemsOnNextUpdate = false
                } else {
                    followRequestsAdapter.addAll(it)
                }
            } else {
                followBinding.noResultsFoundTextView.visibility = View.VISIBLE
                followBinding.followUserList.visibility = View.GONE
                followBinding.noResultsFoundTextView.text = getString(R.string.follow_empty_list, getString(R.string.follow_requests_label))
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

        followRequestsAdapter = FollowRequestsAdapter(object: FollowClickListener(this, getUser()) {
            override fun onActionClick(user: User) {
                viewModel.handleConfirmFollowRequest(user)
            }
        })

        (activity as? AccountActivity)?.apply {
            followBinding.followToolbar.title.text = resources.getString(R.string.follow_requests_label)
            followBinding.followToolbar.visibility = View.VISIBLE
            followBinding.followToolbar.backArrow.setOnClickListener {
                activity!!.onBackPressed()
            }
            setSupportActionBar(followBinding.followToolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun bindLayout() {
        followBinding.followViewstub.setOnInflateListener { _, view ->
            DataBindingUtil.bind<FollowRequestsActionBinding>(view)?.let {
                followersRequestsBinding = it
                followersRequestsBinding.followRequestsLabelTextView.setText(R.string.follow_sent_requests_label)
                followersRequestsBinding.followSentRequestsCountBadge.visibility = View.GONE
                followersRequestsBinding.root.setOnClickListener {
                    addReplaceFragment(
                        R.id.container,
                        SentFollowRequestsFragment.newInstance(),
                        false,
                        true,
                        NavigationAnimationHolder()
                    )
                }
            }
        }

        followBinding.followViewstub.viewStub?.let {
            it.layoutResource = R.layout.follow_requests_action
            it.inflate()
        }

        searchLabelEditText.setHint(R.string.follow_search_requests_label)

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
                        followRequestsAdapter.setItems(it.toMutableList())
                    }
                }
            }

            override fun onQueryStringAvailable(query: String) {
                if (viewModel.cachedOriginalList.isNullOrEmpty()) {
                    viewModel.cachedOriginalList = followRequestsAdapter.getItems()
                }
                viewModel.performSearch(query)
            }
        }))

        followBinding.followUserList.addOnScrollListener(scrollListener)
        followBinding.followUserList.adapter = followRequestsAdapter

        val swipe = object : CustomSwipeToDismiss() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = followRequestsAdapter.getItem(viewHolder.adapterPosition)
                viewModel.handleRemoveFollowRequest(item)
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(followBinding.followUserList)

    }

    override fun updateList() {
        viewModel.replaceItemsOnNextUpdate = true
        //viewModel.fetchFollowRequests(true)
        viewModel.fetchFollowCounts()
    }

    private val scrollListener = object: CommonScrollListener() {
        override fun hasMoreItems() = viewModel.hasMoreItems()

        override fun startLoading() {
            showProgressBar()
            viewModel.fetchFollowRequests()
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
        viewModel.clearObservers(this@FollowRequestsFragment)
        super.onPause()
    }

    override fun onStop() {
        followBinding.followUserList.clearOnScrollListeners()
        super.onStop()
    }

}