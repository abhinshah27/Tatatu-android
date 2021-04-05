/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.chatRoomList

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import io.realm.Sort
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.ItemChatRoomBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.chat.view.ChatMessagesFragment
import rs.highlande.app.tatatu.feature.chat.view.adapter.ChatMessagesAdapter
import rs.highlande.app.tatatu.model.chat.ChatRoom
import java.text.DateFormat
import java.util.*


/**
 * Custom adapter for chat ROOMS view.
 * @author mbaldrighi on 10/15/2018.
 */
class ChatRoomsAdapter(
    val context: Context,
    rooms: OrderedRealmCollection<ChatRoom>,
    private val fragment: ChatRoomsFragment
): RealmRecyclerViewAdapter<ChatRoom, ChatRoomsAdapter.ChatRoomVH>(rooms, true), Filterable {

    init {
        setHasStableIds(true)
    }

    companion object {
        val LOG_TAG = ChatMessagesAdapter::class.java.simpleName
    }

    private val bindedItems = mutableMapOf<Int, ItemChatRoomBinding>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomVH {
        return ChatRoomVH(
            DataBindingUtil.inflate(LayoutInflater.from(parent.context),R.layout.item_chat_room, parent, false),
            fragment)
    }

    override fun onBindViewHolder(holder: ChatRoomVH, position: Int) {
        holder.setRoom(data?.get(position), position)
        bindedItems[holder.itemBinding.hashCode()] = holder.itemBinding
    }

    fun doTransition() {
        bindedItems.forEach {
            it.value.apply {
                if (fragment.viewModel.editMode) {
                    chevron.visibility = View.GONE
                    checkbox.visibility = View.VISIBLE
                } else {
                    chevron.visibility = View.VISIBLE
                    checkbox.visibility = View.GONE
                    root.isSelected = false
                    checkbox.isSelected = false
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        return data?.get(position)?.chatRoomID.hashCode().toLong()
    }


    //region == Class custom methods ==

    enum class FilterBy { PARTICIPANT, ROOM_ID }
    fun getRoomByID(fieldName: FilterBy, valueID: String): Pair<Int, ChatRoom?> {
        val rooms = data?.filter { chatRoom ->
            when (fieldName) {
                FilterBy.PARTICIPANT -> chatRoom.getParticipantId() == valueID
                FilterBy.ROOM_ID -> chatRoom.chatRoomID == valueID
            }
        }

        return if (rooms?.size == 1) data!!.indexOf(rooms[0]) to rooms[0]
        else -1 to null
    }

    //endregion

    //region == Filter ==

    fun filterResults(text: String) {
        val query = fragment.viewModel.getFilterQuery(text)?: return
        updateData(
                query.sort("dateObj", Sort.DESCENDING).findAllAsync()
                        .also {
                            it.addChangeListener { t, _ ->
                                fragment.activity?.runOnUiThread {
                                    fragment.binding.noResult?.setText(
                                            if (text.isBlank()) R.string.no_result_chat_rooms
                                            else R.string.no_result_chat_rooms_search
                                    )
                                    fragment.binding.noResult?.visibility = if (t.isEmpty()) View.VISIBLE else View.GONE
                                    fragment.binding.chatRoomsRV?.visibility = if (t.isEmpty()) View.GONE else View.VISIBLE
                                }
                            }
                        }
        )
    }

    override fun getFilter(): Filter {
        return ChatRoomFilter(
            this,
            fragment
        )
    }


    private class ChatRoomFilter(val adapter: ChatRoomsAdapter, val fragment: ChatRoomsFragment): Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults()
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            adapter.filterResults(constraint.toString())
        }
    }

    //endregion

    //region == View Holder ==

    inner class ChatRoomVH(val itemBinding: ItemChatRoomBinding, private val fragment: ChatRoomsFragment):
            RecyclerView.ViewHolder(itemBinding.root) {

        fun setRoom(currentRoom: ChatRoom?, position: Int) {
            with(itemBinding) {
                if (currentRoom != null) {

                    if (fragment.viewModel.selectedChats.contains(currentRoom.chatRoomID)) {
                        root.isSelected = true
                        checkbox.isSelected = true
                    } else {
                        root.isSelected = false
                        checkbox.isSelected = false
                    }

                    itemContainer.layoutTransition.setDuration(100)

                    root.setOnClickListener {
                        if (fragment.viewModel.editMode) {
                            if (!checkbox.isSelected) {
                                root.isSelected = true
                                checkbox.isSelected = true
                                fragment.viewModel.addChatToSelection(currentRoom.chatRoomID!!)
                            } else {
                                root.isSelected = false
                                checkbox.isSelected = false
                                fragment.viewModel.removeChatFromSelection(currentRoom.chatRoomID!!)
                            }
                        } else {
                            if (isContextValid(fragment.context) && currentRoom.isValid())
                                fragment.addReplaceFragment(R.id.container, ChatMessagesFragment.newInstance(currentRoom.chatRoomID!!), false, true, null)
                        }
                    }
                    root.setOnLongClickListener {
                        if (!fragment.viewModel.editMode) {
                            fragment.handleEditMode(true)
                            root.isSelected = true
                            checkbox.isSelected = true
                            fragment.viewModel.addChatToSelection(currentRoom.chatRoomID!!)
                            true
                        } else false
                    }

                    if (!currentRoom.getRoomAvatar().isNullOrBlank())
                        picturePlusIndicator.picture.setProfilePicture(currentRoom.getRoomAvatar())
                    else
                        picturePlusIndicator.picture.setImageResource(R.drawable.ic_profile_placeholder)
                    picturePlusIndicator.setOnClickListener {
                        if (fragment.viewModel.editMode) {
                            root.performClick()
                        } else if (!currentRoom.getParticipantId().isNullOrBlank()) {
                            AccountActivity.openProfileFragment(context, currentRoom.getParticipantId()!!)
                        }
                    }

                    chatRoomName.text = currentRoom.getRoomName()
                    chatText.apply {
                        text = currentRoom.text
                        visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                    }

                    val date = currentRoom.dateObj
                    if (date != null) {
                        chatLastSeen.text =
                            try {
                                if (date.isSameDateAsNow()) formatTime(fragment.context, date)
                                else DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
                            } catch (e: Exception) {
                                LogUtils.e(LOG_TAG, e)
                                ""
                            }
                    }

                    with(currentRoom.toRead) {
                        if (this == 0) toReadCount.visibility = View.GONE
                        else {
                            toReadCount.text = this.toString()
                            toReadCount.visibility = View.VISIBLE
                        }
                    }
                    if (fragment.viewModel.editMode) {
                        chevron.visibility = View.GONE
                        checkbox.visibility = View.VISIBLE
                    } else {
                        chevron.visibility = View.VISIBLE
                        checkbox.visibility = View.GONE
                    }

                    earningValue.text = formatTTUTokens(currentRoom.earnings)
                }

            }
        }

    }

    interface OnDeleteRoomListener {
        fun onRoomDeleted(chatID: String)
    }

    //endregion

}