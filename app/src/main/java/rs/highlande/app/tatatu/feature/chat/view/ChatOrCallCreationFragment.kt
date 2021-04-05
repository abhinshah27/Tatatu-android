/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.view

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.hideKeyboard
import rs.highlande.app.tatatu.core.ui.showKeyboard
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.databinding.FragmentChatCallCreationBinding
import rs.highlande.app.tatatu.feature.chat.view.adapter.ChatOrCallCreationAdapter
import rs.highlande.app.tatatu.feature.chat.view.viewModel.ChatOrCallCreationViewModel
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializer
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializerDelegate
import rs.highlande.app.tatatu.model.User

class ChatOrCallCreationFragment:
    BaseFragment(),
    Handler.Callback,
    ChatOrCallCreationAdapter.OnChatCallsListener,
    DefaultCallInitializer by DefaultCallInitializerDelegate() {

    companion object {

        val logTag = ChatOrCallCreationFragment::class.java.simpleName

        const val CALL_PERMISSIONS_CODE = 1

        const val BUNDLE_KEY_TYPE = "key_type"

        fun newInstance(viewType: ChatOrCallCreationViewModel.ChatOrCallType) =
            ChatOrCallCreationFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(
                        BUNDLE_KEY_TYPE, viewType
                    )
                }
            }
    }

    val viewModel: ChatOrCallCreationViewModel by viewModel()

    lateinit var binding: FragmentChatCallCreationBinding

    var loadedData = mutableListOf<User>()
    lateinit var adapter: ChatOrCallCreationAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeToLiveData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        viewModel.viewType = arguments?.get(BUNDLE_KEY_TYPE) as? ChatOrCallCreationViewModel.ChatOrCallType

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_call_creation, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureLayout(view)

    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag, if (viewModel.isScreenForCalls()) "Calls" else "Chats")

        (activity as ChatActivity).apply {
            restoreMainBottomNavigation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        forwardRequestPermissionResult(requestCode, permissions, grantResults)
    }

    override fun configureLayout(view: View) {

        binding.apply {

            srl.setOnRefreshListener {
                viewModel.currentPage = 0
                viewModel.replaceItemsOnNextUpdate = true
                viewModel.getUsersForNewChats()
            }

            (activity as? ChatActivity)?.apply {
                setSupportActionBar(toolbar as Toolbar)
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                toolbar.backArrow.setOnClickListener {
                    activity!!.onBackPressed()
                }
                setHasOptionsMenu(true)
            }

            with(binding.searchBox) {
                root.setOnClickListener {
                    searchLabelEditText.requestFocus()
                    searchLabelEditText.showKeyboard()
                }
                searchLabelEditText.setOnFocusChangeListener { _, _ ->
                    if (searchLabelEditText.hasFocus()) {
                        clearSearchIconImageView.visibility = View.VISIBLE
                    } else {
                        clearSearchIconImageView.visibility = View.GONE
                    }
                    clearSearchIconImageView.setOnClickListener {
                        searchLabelEditText.setText("")
                    }
                }
                searchLabelEditText.setHint(R.string.title_search)
                viewModel.createTextChangeObservable(
                    searchLabelEditText
                ) {
                    searchLabelEditText.clearFocus()
                    searchLabelEditText.hideKeyboard()
                    clearSearchIconImageView.visibility = View.GONE
                    viewModel.cancelSearch()
                    viewModel.cachedOriginalList?.let {
                        binding.chatRoomsRV.visibility = View.VISIBLE
                        binding.noResult.visibility = View.GONE
                        adapter.setItems(it)
                        viewModel.scrollListener.canFetch = (it.size == PAGINATION_SIZE)
                    }
                }
            }

            toolbar.title.setText(
                if (viewModel.isScreenForCalls()) R.string.title_new_call
                else R.string.title_new_chat
            )

            adapter = ChatOrCallCreationAdapter(
                this@ChatOrCallCreationFragment,
                viewModel.isScreenForCalls()
            ).apply {
                this.setHasStableIds(true)
            }

            chatRoomsRV.adapter = adapter
            chatRoomsRV.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
            chatRoomsRV.addOnScrollListener(viewModel.scrollListener)

            adapter.clear()
            binding.srl.isRefreshing = true
            viewModel.replaceItemsOnNextUpdate = true
            viewModel.getUsersForNewChats()
        }
    }

    override fun bindLayout() {
    }

    fun subscribeToLiveData() {
        viewModel.newChatsLiveData.observe(viewLifecycleOwner, Observer {
            binding.apply {
                srl.isRefreshing = false
                srl.isEnabled = false
                if (it.isNotEmpty()) {
                    chatRoomsRV.visibility = View.VISIBLE
                    noResult.visibility = View.GONE

                    if (it.size % PAGINATION_SIZE == 0)
                        viewModel.currentPage += 1

                    if (viewModel.replaceItemsOnNextUpdate || viewModel.isSearch) {
                        viewModel.replaceItemsOnNextUpdate = false
                        adapter.setItems(it.toMutableList())
                    } else {
                        adapter.addAll(it)
                    }
                    if (!viewModel.isSearch)
                        viewModel.cachedOriginalList = adapter.getItems().toMutableList()
                } else {
                    chatRoomsRV.visibility = View.GONE
                    noResult.visibility = View.VISIBLE
                }
            }

            //if (it.first) setData(it.second) else setData()
        })
        viewModel.newChatRoomLiveData.observe(viewLifecycleOwner, Observer {
            hideLoader()
            if (it) {
                viewModel.selectedUser?.let { adapter.remove(it) }
                viewModel.chatRoom?.let {
                    addReplaceFragment(R.id.container, ChatMessagesFragment.newInstance(it.chatRoomID!!), false, true, null)
                    viewModel.chatRoom = null
                }
            }
        })
        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            binding.srl.isRefreshing = false
        })

    }

    private fun setData(participants: List<User>? = null) {
        hideLoader()
        loadedData.clear()
        binding.apply {
            if (participants.isNullOrEmpty()) {
                chatRoomsRV.visibility = View.GONE
                noResult.visibility = View.VISIBLE
            } else {
                loadedData.addAll(participants)
                chatRoomsRV.visibility = View.VISIBLE
                noResult.visibility = View.GONE
            }
            adapter.addAll(loadedData)
        }

    }

    //region == Callbacks ==

    override fun onItemClick(item: User) {
        viewModel.initializeNewRoom(item)
    }

    override fun onVideoCallClick(user: User) {
        getUser()?.let{
            initPermissionsForCall(this, CALL_PERMISSIONS_CODE, logTag, it, user, true)
        }
    }

    override fun onCallClicked(user: User) {
        getUser()?.let{
            initPermissionsForCall(this, CALL_PERMISSIONS_CODE, logTag, it, user, false)
        }
    }

    //endregion


    override fun handleMessage(msg: Message): Boolean {
        if (loadedData.isNotEmpty())
            adapter.addAll(loadedData)
        else {
            activity?.runOnUiThread {
                binding.apply {
                    chatRoomsRV.visibility = View.GONE
                    noResult.visibility = View.VISIBLE
                }
            }
        }
        return true
    }

}