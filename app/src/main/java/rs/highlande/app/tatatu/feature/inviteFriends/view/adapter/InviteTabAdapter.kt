package rs.highlande.app.tatatu.feature.inviteFriends.view.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.feature.inviteFriends.view.fragment.InviteFriendsContactFragment
import rs.highlande.app.tatatu.feature.inviteFriends.view.fragment.InviteFriendsEmailFragment
import rs.highlande.app.tatatu.feature.inviteFriends.view.fragment.InviteFriendsLinkFragment
import rs.highlande.app.tatatu.feature.inviteFriends.view.fragment.InviteFriendsTwitterFragment

/**
 * Created by Abhin.
 */
class InviteTabAdapter(fm: FragmentManager, var context: Context) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                InviteFriendsLinkFragment()
            }
            1 -> {
                InviteFriendsTwitterFragment()
            }
            2 -> {
                InviteFriendsEmailFragment()
            }
            3 -> {
                InviteFriendsContactFragment()
            }
            else -> {
                InviteFriendsLinkFragment()
            }
        }
    }

    override fun getCount(): Int {
        return 4
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> context.getString(R.string.tabs_LINK)
            1 -> context.getString(R.string.tabs_TWITTER)
            2 -> context.getString(R.string.tabs_EMAIL)
            3 -> context.getString(R.string.tabs_CONTACT)
            else -> {
                return context.getString(R.string.tabs_LINK)
            }
        }
    }
}