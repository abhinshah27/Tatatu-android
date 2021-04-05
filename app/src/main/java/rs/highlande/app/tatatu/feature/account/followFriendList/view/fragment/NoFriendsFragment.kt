package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_USER_ID
import rs.highlande.app.tatatu.databinding.FragmentNoFriendsBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.FriendsViewModel
import rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel.InvitesViewModel
import rs.highlande.app.tatatu.feature.inviteFriends.view.InviteFriendsActivity

class NoFriendsFragment: BaseFragment() {
    companion object {
        fun newInstance(userId: String): NoFriendsFragment {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_USER_ID, userId)
            val instance = NoFriendsFragment()
            instance.arguments = bundle
            return instance
        }
    }
    lateinit var noFriendsBinding: FragmentNoFriendsBinding
    private val viewModel: FriendsViewModel by viewModel()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        noFriendsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_no_friends, container, false)
        return noFriendsBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        viewModel.mFriends.observe(this, Observer {
            when {
                !it.isNullOrEmpty() -> (parentFragment as FriendsTabsFragment).setInvitedCount(it[0].pendingCount!!)
            }
        })
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {
        noFriendsBinding.resendInviteButton.setOnClickListener {
            InviteFriendsActivity.openInvite(context!!)
        }
        viewModel.getUsersInvited(InvitesViewModel.InviteType.REGISTERED.value, "", viewModel.mInviteFriendsPageNumber)
    }
}