/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.chat_room_activity_container.view.*
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.ActivityChatBinding
import rs.highlande.app.tatatu.feature.chat.BaseSoundPoolManager
import rs.highlande.app.tatatu.feature.chat.HandleUnsentMessagesService
import rs.highlande.app.tatatu.feature.chat.chatRoomList.ChatRoomsFragment
import rs.highlande.app.tatatu.feature.chat.view.viewModel.ChatOrCallCreationViewModel
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonView.BottomNavigationActivity
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.CallsSDKTokenUtils
import java.lang.ref.WeakReference

/**
 * @author mbaldrighi on 10/15/2018.
 */
class ChatActivity : BottomNavigationActivity() {

    private var messagesFragmentRef: WeakReference<ChatMessagesFragment>? = null
    private val usersRepository by inject<UsersRepository>()

    private val chatSoundPool by lazy {
        BaseSoundPoolManager(
            this,
            arrayOf(R.raw.chat_send),
            null
        )
    }

    private lateinit var binding: ActivityChatBinding

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)

        chatSoundPool.init()

        setupBottomNavigation(binding.chatContainer.bottomBar as BottomNavigationView)
        configureLayout()

        if (CallsSDKTokenUtils.callToken.isNullOrBlank()) {
            //call for token generate
            CallsSDKTokenUtils.init(usersRepository.fetchCachedMyUserId())
        }
    }

    override fun onPause() {
        HandleUnsentMessagesService.startService(this)
        super.onPause()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
        }
    }

    override fun configureLayout() {
        binding.chatContainer.chatRoomEditDeleteTextview.setOnClickListener {
            EventBus.getDefault().post(ChatEditEvent(deleteClick = true))
        }
        binding.chatContainer.chatRoomEditExportTextview.setOnClickListener {
            EventBus.getDefault().post(ChatEditEvent(exportClick = true))
        }
    }

    override fun bindLayout() {}

    override fun manageIntent() {
        val intent = intent
        if (intent != null) {
            val showFragment = intent.getIntExtra(
                FRAGMENT_KEY_CODE,
                FRAGMENT_INVALID
            )
            val requestCode = intent.getIntExtra(REQUEST_CODE_KEY, NO_RESULT)
            val extras = intent.extras ?: Bundle()

            when (showFragment) {
                FRAGMENT_CHAT_MESSAGES -> {
                    val roomId = extras.getString(EXTRA_PARAM_1)
                    if (roomId != null)
                        addChatMessagesFragment(roomId)
                }
                FRAGMENT_CHAT_ROOMS -> addChatRoomsFragment()
                FRAGMENT_CHAT_CREATION -> addCallCreationFragment()
            }
        }
    }

    fun playSendTone() {
        chatSoundPool.playOnce(R.raw.chat_send)
    }

    fun playIncomingMessageTone() {
        chatSoundPool.playOnce(R.raw.chat_incoming_message)
    }

    fun showChatRoomEditBottomNavigation() {
        bottomNav?.visibility = View.INVISIBLE
        binding.chatContainer.chatEditBottomNavigation.visibility = View.VISIBLE
    }

    fun restoreMainBottomNavigation() {
        bottomNav?.visibility = View.VISIBLE
        binding.chatContainer.chatEditBottomNavigation.visibility = View.GONE
    }

    fun hideMainBottomNavigation() {
        bottomNav?.visibility = View.GONE
    }

    //region == Fragments section ==

    private fun addChatRoomsFragment() {
        addReplaceFragment(R.id.container, ChatRoomsFragment.newInstance(), false, true, null)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    private fun addChatMessagesFragment(chatRoomId: String) {
        addReplaceFragment(
            R.id.container,
            ChatMessagesFragment.newInstance(chatRoomId),
            false, true, null
        )

    }

    private fun addCallCreationFragment() {
        addReplaceFragment(
            R.id.container,
            ChatOrCallCreationFragment.newInstance(ChatOrCallCreationViewModel.ChatOrCallType.CALL),
            false,
            true,
            null
        )
    }

    companion object {

        fun openCallsCreationFragment(context: Context) {
            openFragment<ChatActivity>(
                context,
                FRAGMENT_CHAT_CREATION,
                requestCode = NO_RESULT,
                animations = ActivityAnimationHolder(
                    R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left
                )
            )
        }

        fun openChatRoomsFragment(context: Context) {
            openFragment<ChatActivity>(
                context,
                FRAGMENT_CHAT_ROOMS,
                requestCode = NO_RESULT,
                animations = ActivityAnimationHolder(
                    R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left
                )
            )

        }

        fun openChatMessageFragment(context: Context, chatRoomId: String) {
            openFragment<ChatActivity>(
                context,
                FRAGMENT_CHAT_MESSAGES,
                Bundle().apply {
                    this.putString(EXTRA_PARAM_1, chatRoomId)
                },
                requestCode = NO_RESULT,
                animations = ActivityAnimationHolder(
                    R.anim.slide_in_from_right,
                    R.anim.no_animation
                )
            )
        }
    }

    //endregion
}

class ChatEditEvent(val exportClick: Boolean = false, val deleteClick: Boolean = false, val error: Boolean = false)
