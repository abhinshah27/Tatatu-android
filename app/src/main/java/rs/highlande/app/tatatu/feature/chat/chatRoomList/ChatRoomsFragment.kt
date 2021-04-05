/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.chatRoomList

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import kotlinx.android.synthetic.main.fragment_chat_rooms.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.webSocket.realTime.RTCommHelper
import rs.highlande.app.tatatu.connection.webSocket.realTime.RealTimeChatListener
import rs.highlande.app.tatatu.core.ui.*
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.EXTRA_PARAM_1
import rs.highlande.app.tatatu.core.util.isValid
import rs.highlande.app.tatatu.databinding.FragmentChatRoomsBinding
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.feature.chat.view.ChatEditEvent
import rs.highlande.app.tatatu.feature.chat.view.ChatOrCallCreationFragment
import rs.highlande.app.tatatu.feature.chat.view.viewModel.ChatOrCallCreationViewModel
import rs.highlande.app.tatatu.model.chat.ChatMessage

/**
 * @author mbaldrighi on 10/15/2018.
 */
class ChatRoomsFragment: BaseFragment(), RealTimeChatListener {

    companion object {
        val logTag = ChatRoomsFragment::class.java.simpleName
        fun newInstance() = ChatRoomsFragment()
    }

    lateinit var menu: Menu

    private val rtCommHelper: RTCommHelper by inject()

    val viewModel: ChatRoomsViewModel by viewModel()

    private lateinit var adapter: ChatRoomsAdapter

    private var savedSearchString: String? = null

    lateinit var binding: FragmentChatRoomsBinding


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeToLiveData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_rooms, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureLayout(view)
    }

    fun subscribeToLiveData() {
        viewModel.roomDeletedLiveData.observe(viewLifecycleOwner, Observer {
            (activity as? BaseActivity)?.hideLoader()
            if (it.first) {
                viewModel.deleteRoomLocally(it.second)
                this@ChatRoomsFragment.noResult?.visibility = if (adapter.itemCount == 0) {
                    handleEditMode(false)
                    View.VISIBLE
                } else View.GONE
            }
        })
        viewModel.chatListUpdatedLiveData.observe(viewLifecycleOwner, Observer {
            (activity as? BaseActivity)?.hideLoader()
        })
        viewModel.genericErrorLiveData.observe(viewLifecycleOwner, Observer {
            (activity as? BaseActivity)?.hideLoader()
        })
        viewModel.chatEditModeUpdate.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                binding.toolbar.title.text = getString(R.string.title_selected_chats_and_calls, it.second)
            } else {
                binding.toolbar.title.setText(R.string.title_chats_and_calls)
            }
        })
        viewModel.chatExportResultLiveData.observe(viewLifecycleOwner, Observer {
            showDialogGeneric(context!!, resources.getString(R.string.chats_exported))
        })
    }

    override fun onResume() {
        super.onResume()

        viewModel.updateRoomsList()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        EventBus.getDefault().register(this)
        rtCommHelper.listenerChat = this
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        onSaveInstanceState(Bundle())
        rtCommHelper.listenerChat = null

        binding.searchBox.searchLabelEditText.hideKeyboard()

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(EXTRA_PARAM_1, savedSearchString)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_chat_room_list, menu)
        this.menu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionNewChat -> addReplaceFragment(
                R.id.container,
                ChatOrCallCreationFragment.newInstance(ChatOrCallCreationViewModel.ChatOrCallType.CHAT),
                false,
                true,
                NavigationAnimationHolder()
            )
            R.id.actionNewCall -> addReplaceFragment(
                R.id.container,
                ChatOrCallCreationFragment.newInstance(ChatOrCallCreationViewModel.ChatOrCallType.CALL),
                false,
                true,
                NavigationAnimationHolder()
            )
            R.id.actionEdit -> {
                handleEditMode(true)
            }
            R.id.actionDone -> {
                handleEditMode(false)
            }
        }
        return true
    }

    fun handleEditMode(editMode: Boolean) {
        if (editMode) {
            menu.findItem(R.id.actionNewChat).isVisible = false
            menu.findItem(R.id.actionNewCall).isVisible = false
            menu.findItem(R.id.actionEdit).isVisible = false
            menu.findItem(R.id.actionDone).isVisible = true
            viewModel.editMode = true
            adapter.doTransition()
            (activity as? ChatActivity)?.showChatRoomEditBottomNavigation()
        } else {
            menu.findItem(R.id.actionNewChat).isVisible = true
            menu.findItem(R.id.actionNewCall).isVisible = true
            menu.findItem(R.id.actionEdit).isVisible = true
            menu.findItem(R.id.actionDone).isVisible = false
            viewModel.editMode = false
            adapter.doTransition()
            (activity as? ChatActivity)?.restoreMainBottomNavigation()
        }
    }


    override fun configureLayout(view: View) {

        (activity as? ChatActivity)?.apply {
            setSupportActionBar(binding.toolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            binding.toolbar.backArrow.setOnClickListener {
                activity!!.onBackPressed()
            }
            setHasOptionsMenu(true)
        }

        binding.toolbar.title.setText(R.string.title_chats_and_calls)

        adapter = ChatRoomsAdapter(context!!, viewModel.getRooms(), this)

        //TODO: 09/01/20 is a srl neccesary for the chatrooms?
        binding.srl.isEnabled = false

        chatRoomsRV.visibility = if (adapter.itemCount == 0) View.GONE else View.VISIBLE
        noResult.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE

        chatRoomsRV.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        chatRoomsRV.adapter = adapter

        with(binding.searchBox) {
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
            searchLabelEditText.setHint(rs.highlande.app.tatatu.R.string.title_search)
            searchLabelEditText.addTextChangedListener(SearchTextWatcher(
                object : SearchTextWatcher.SearchListener {
                    override fun onQueryStringIsEmpty() {
                        adapter.filter.filter("")
                        searchLabelEditText.clearFocus()
                        searchLabelEditText.hideKeyboard()
                    }

                    override fun onQueryStringAvailable(query: String) {
                        adapter.filter.filter(searchLabelEditText.text.toString())
                    }
                }))
            searchLabelEditText.isSingleLine = true
            searchLabelEditText.setOnEditorActionListener { textView, i, _ ->
                if (!textView.text.isNullOrBlank() &&
                    i == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    adapter.filter.filter(textView.text.toString())
                    true

                } else false
            }
        }

    }

    override fun bindLayout() {
    }

    //region == Real time callbacks ==

    override fun onNewMessage(newMessage: ChatMessage) {
        activity?.let {
            if (it.isValid()) {
                it.runOnUiThread {
                    val roomId = newMessage.chatRoomID
                    if (!roomId.isNullOrBlank()) {
                        val room =
                            adapter.getRoomByID(ChatRoomsAdapter.FilterBy.ROOM_ID, roomId).second

                        if (room?.isValid() == true) {
                            val text = when {
//                    newMessage.hasAudio() -> getString(R.string.chat_room_first_line_audio_in)
                                newMessage.hasVideo() -> getString(R.string.chat_room_first_line_video)
                                newMessage.hasPicture() -> getString(R.string.chat_room_first_line_picture)
//                    newMessage.hasLocation() -> getString(R.string.chat_room_first_line_location_in)
//                    newMessage.hasDocument() -> getString(R.string.chat_room_first_line_document_in)
                                newMessage.hasWebLink() -> {
                                    "" //TODO  change with server UNICODE
                                }
                                newMessage.isMissedVoiceCall() -> {
                                    if (newMessage.isIncoming(getUser()?.uid))
                                        getString(R.string.chat_text_incoming_missed_voice)
                                    else
                                        getString(R.string.chat_text_outgoing_missed_voice)
                                }
                                newMessage.isMissedVideoCall() -> {
                                    if (newMessage.isIncoming(getUser()?.uid))
                                        getString(R.string.chat_text_incoming_missed_video)
                                    else
                                        getString(R.string.chat_text_outgoing_missed_video)
                                }
                                else -> newMessage.text
                            }
                            viewModel.updateRoomText(room, text, newMessage.creationDateObj)
                        }
                    }
                }
            }
        }
    }

    override fun onStatusUpdated(userId: String, status: Int, date: String) {
        val room = adapter.getRoomByID(ChatRoomsAdapter.FilterBy.PARTICIPANT, userId).second
        viewModel.updateRoomStatus(room, status, date)
    }

    override fun onActivityUpdated(userId: String, chatId: String, activity: String) {
        val room = adapter.getRoomByID(ChatRoomsAdapter.FilterBy.ROOM_ID, chatId).second
        viewModel.updateRoomActivity(room, chatId, activity)
    }

    //endregion

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChatEditEvent(event: ChatEditEvent) {
        when {
            event.error -> {
                showMessage(R.string.error_chat_rooms_no_selection)
            }
            event.deleteClick -> {
                viewModel.handleSelectedRoomsDeletion()
            }
            event.exportClick -> {
                viewModel.handleSelectedRoomsExport()
            }
        }
    }

    override fun onMessageDelivered(chatId: String, userId: String, date: String) {}

    override fun onMessageRead(chatId: String, userId: String, date: String) {}

    override fun onMessageOpened(chatId: String, userId: String, date: String, messageID: String) {}


}