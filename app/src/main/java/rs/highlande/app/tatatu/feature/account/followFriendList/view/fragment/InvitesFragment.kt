package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.SearchTextWatcher
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_USER_ID
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.account.followFriendList.view.adapter.InvitesAdapter
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.InvitesViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.InviteFriendsViewModel
import rs.highlande.app.tatatu.model.InviteFriendsResponse
import rs.highlande.app.tatatu.model.Users

class InvitesFragment : BaseFollowFragment() {
    companion object {
        fun newInstance(userId: String): InvitesFragment {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_USER_ID, userId)
            val instance = InvitesFragment()
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var invitesAdapter: InvitesAdapter
    private val viewModel: InvitesViewModel by viewModel()
    private val mInviteFriendsViewModel: InviteFriendsViewModel by sharedViewModel()

    private var isSearch = false

    override fun subscribeToLiveData() {
        viewModel.mInviteFriends.observe(viewLifecycleOwner, Observer {
            when {
                it.isNullOrEmpty() -> setUI(true)
                else -> setDataWithAdapter(it)
            }
            scrollListener.endLoading()
        })

        viewModel.isProgress.observe(viewLifecycleOwner, Observer {
            if (it) {
                mBaseActivity?.setProgressBarNull()
                showLoader(resources.getString(R.string.loader_fetching_friends_invited))
            } else {
                hideLoader()
            }
        })

        //Progress Dialog in Email Send
        mInviteFriendsViewModel.isEmailProgress.observe(viewLifecycleOwner, Observer {
            if (it) {
                mBaseActivity?.setProgressBarNull()
                showLoader(resources.getString(R.string.loader_email))
            } else {
                hideLoader()
            }
        })

        //get Email Success
        mInviteFriendsViewModel.mSendInvitation.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty() && !it[0].id.isNullOrEmpty()) {
                showError(resources.getString(R.string.msg_email))
            }
        })

        //Error Handle
        mInviteFriendsViewModel.mErrorInvitation.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                showError(mInviteFriendsViewModel.mErrorInvitation.value!!)
            }
        })
    }


    private fun setDataWithAdapter(it: ArrayList<InviteFriendsResponse>) {
        setUI(false)
        when {
            viewModel.mInviteFriendsPageNumber == 1 && it[0].users.isNullOrEmpty() -> setUI(true)
            else -> {
                viewModel.mInviteFriendsPageNumber += 1
                (parentFragment as FriendsTabsFragment).setInvitedCount(it[0].pendingCount)
                LogUtils.e(CommonTAG, "size-->${viewModel.mCachedOriginalList?.size}")
                when {
                    viewModel.replaceItemsOnNextUpdate || isSearch -> {
                        if (!isSearch) {
                            viewModel.mInviteFriendsAllData.value?.clear()
                            viewModel.mInviteFriendsAllData.value?.addAll(it[0].users!!)
                        }
                        viewModel.mPendingCount = it[0].pendingCount
                        invitesAdapter.setItems(it[0].users!!.toMutableList())
                        viewModel.replaceItemsOnNextUpdate = false
                    }
                    else -> {
                        viewModel.mInviteFriendsAllData.value?.addAll(it[0].users!!)
                        invitesAdapter.addAll(it[0].users!!.toMutableList())
                    }
                }
            }
        }
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


    override fun configureLayout(view: View) {
        super.configureLayout(view)

        followBinding.followSRL.isEnabled = false

        getUser()?.let {
            viewModel.setUser(it, false)
        }
        invitesAdapter = InvitesAdapter(object : OnItemClickListener<Users> {
            override fun onItemClick(item: Users) {
                sendInvite(item)
            }
        })
    }

    override fun bindLayout() {
        followBinding.followViewstub.viewStub?.let {
            it.layoutResource = R.layout.invite_label
            it.inflate()
        }
        with(followBinding.followSearchBox) {
            searchLabelEditText.setHint(R.string.follow_search_invited_friends_label)

            searchLabelEditText.setOnFocusChangeListener { _, _ ->
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
                        viewModel.mInviteFriendsPageNumber = viewModel.mCachedPageNumber
                        scrollListener.isUpdateOnScrollEnabled = true
                        invitesAdapter.setItems(it.toMutableList())
                    }
                }
            }

            searchLabelEditText.addTextChangedListener(SearchTextWatcher(object : SearchTextWatcher.SearchListener {
                override fun onQueryStringAvailable(query: String) {
                    if (viewModel.mCachedOriginalList.isNullOrEmpty()) {
                        viewModel.mCachedOriginalList = viewModel.mInviteFriendsAllData.value
                        viewModel.mCachedPageNumber = viewModel.mInviteFriendsPageNumber
                        LogUtils.e(CommonTAG, "Search Size-->${viewModel.mCachedOriginalList?.size}")
                    }
                    isSearch = true
                    clearSearchIconImageView.visibility = View.VISIBLE
                    viewModel.mInviteFriendsPageNumber=1
                    viewModel.getPerformSearch(InvitesViewModel.InviteType.EMAIL_INVITED.value, query)
                }

                override fun onQueryStringIsEmpty() {
                    searchLabelEditText.clearFocus()
                    clearSearchIconImageView.visibility = View.GONE
                    isSearch = false
                    viewModel.mCachedOriginalList?.let {
                        invitesAdapter.setItems(it.toMutableList())
                    }
                }
            }))
        }

        viewModel.replaceItemsOnNextUpdate = true
        followBinding.followUserList.addOnScrollListener(scrollListener)
        followBinding.followUserList.adapter = invitesAdapter


    }

    override fun updateList() {}

    fun hasMoreItem(): Boolean {
        return if (viewModel.mPendingCount != -1) {
            viewModel.mPendingCount != invitesAdapter.itemCount
        } else {
            true
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.replaceItemsOnNextUpdate = true
        fetchInvites(true)
    }

    private val scrollListener = object : CommonScrollListener() {
        override fun hasMoreItems() = hasMoreItem()
        override fun startLoading() {
            viewModel.isScrolling = true
//            followLoadingProgressBar.visibility=View.VISIBLE
//            followLoadingProgressBar.show()
            //            viewModel.dummyData()
            fetchInvites()
        }

    }

    override fun onPause() {
        viewModel.clearObservers(this)
        super.onPause()
    }

    fun fetchInvites(resetPageCount: Boolean = false) {
        if (resetPageCount) {
            viewModel.mInviteFriendsPageNumber = 1
            viewModel.getUsersInvited(InvitesViewModel.InviteType.EMAIL_INVITED.value)
            mInviteFriendsViewModel.getInvitationLink()
        } else {
            viewModel.getUsersInvited(InvitesViewModel.InviteType.EMAIL_INVITED.value, "", viewModel.mInviteFriendsPageNumber)
        }
    }

    //send the invitation
    private fun sendInvite(item: Users) {
        if (!mInviteFriendsViewModel.mInvitationLink.value!![0].invitationLink.isNullOrEmpty()) {
            if (!getUser()?.name.isNullOrBlank())
                mInviteFriendsViewModel.getSendInvitation(item.name!!, item.indentifier!!, getUser()!!.name)
        } else {
            showError(resources.getString(R.string.link_not_available))
        }
    }

}