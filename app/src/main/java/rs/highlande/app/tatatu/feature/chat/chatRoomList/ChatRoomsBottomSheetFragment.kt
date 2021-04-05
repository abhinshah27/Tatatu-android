package rs.highlande.app.tatatu.feature.chat.chatRoomList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.MyPostDetailBottomSheetBinding
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetFragment
import rs.highlande.app.tatatu.feature.post.util.BasePostSheetListener
import rs.highlande.app.tatatu.model.Post


class ChatRoomsBottomSheetFragment: BasePostSheetFragment() {

    companion object {
        fun newInstance(post: Post, listener: BasePostSheetListener): ChatRoomsBottomSheetFragment {
            val fragment = ChatRoomsBottomSheetFragment()
            fragment.post = post
            fragment.listener = listener
            return fragment
        }

    }

    lateinit var binding: MyPostDetailBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.bind(view!!)!!

        return view
    }

    override fun getLayoutId() = R.layout.edit_chat_room_bottom_sheet
}

