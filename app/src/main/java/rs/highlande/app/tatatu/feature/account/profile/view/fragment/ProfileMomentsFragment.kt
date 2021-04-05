package rs.highlande.app.tatatu.feature.account.profile.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.FragmentProfileMomentListBinding
import rs.highlande.app.tatatu.feature.account.profile.view.ProfileViewModel
import rs.highlande.app.tatatu.feature.account.profile.view.adapter.ProfileMomentListAdapter

class ProfileMomentsFragment: Fragment() {

    companion object {
        private const val SHOW_CREATE_ITEM = "SHOW_CREATE_ITEM"
        fun newInstance(showCreateItem: Boolean = false): ProfileMomentsFragment {
            val bundle = Bundle()
            bundle.putBoolean(SHOW_CREATE_ITEM, showCreateItem)
            val instance = ProfileMomentsFragment()
            instance.arguments = bundle
            return instance
        }
    }

    lateinit var profileMomentBinding: FragmentProfileMomentListBinding
    lateinit var profileMomentsListAdapter: ProfileMomentListAdapter
    private val viewModel by viewModel<ProfileViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        profileMomentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_moment_list, container, false)

        profileMomentsListAdapter = ProfileMomentListAdapter()
        profileMomentBinding.profileList.adapter = profileMomentsListAdapter

        //loadData(arguments!!.getBoolean(SHOW_CREATE_ITEM, false))

        return profileMomentBinding.root
    }

}