package rs.highlande.app.tatatu.feature.inviteFriends.view.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_invite_friends_link.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.copyToClipboard
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.InviteFriendsViewModel


class InviteFriendsLinkFragment : BaseFragment() {

    private val mInviteFriendsViewModel: InviteFriendsViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_invite_friends_link, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        mInviteFriendsViewModel.mInvitationLink.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                txt_InviteLink?.text = it[0].invitationLink
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(InviteFriendsFragment.logTag, "Link")
    }

    private fun init() {
        if (!mInviteFriendsViewModel.mInvitationLink.value.isNullOrEmpty()) txt_InviteLink.text = mInviteFriendsViewModel.mInvitationLink.value!![0].invitationLink
        btn_CopyLink.setOnClickListener {
            copyToClipboard(context!!, txt_InviteLink.text.toString())
            showError(resources.getString(R.string.msg_link_copy))
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}
}
