package rs.highlande.app.tatatu.feature.inviteFriends.view

import android.content.Context
import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.inviteFriends.view.fragment.InviteFriendsFragment
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.ContactViewModel

class InviteFriendsActivity : BaseActivity() {

    private val contactsViewModel: ContactViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invite_friends)
        val fragment = supportFragmentManager.findFragmentByTag(InviteFriendsFragment::class.java.simpleName)
        if (fragment == null) {
            //replace the Invite Friends Fragment
            addReplaceFragment(R.id.fl_invite_friend_container, InviteFriendsFragment(), addFragment = false, addToBackStack = false, animationHolder = null)
        }
    }

    override fun configureLayout() {}

    override fun bindLayout() {}

    override fun manageIntent() {

        val intent = intent ?: return

        val showFragment = intent.getIntExtra(
            FRAGMENT_KEY_CODE,
            FRAGMENT_INVALID
        )
        val requestCode = intent.getIntExtra(REQUEST_CODE_KEY, NO_RESULT)
        val extras = intent.extras

        when (showFragment) {
            FRAGMENT_INVITE -> {
                addReplaceFragment(R.id.fl_container, InviteFriendsFragment.newInstance(), false, false, null)
            }
        }

    }


    companion object {

        val logTag = InviteFriendsActivity::class.java.simpleName

        fun openInvite(context: Context) {
            openFragment<InviteFriendsActivity>(
                context,
                FRAGMENT_INVITE,
                animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation)
            )
        }
    }

}
