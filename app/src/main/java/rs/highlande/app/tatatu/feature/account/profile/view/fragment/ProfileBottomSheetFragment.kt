package rs.highlande.app.tatatu.feature.account.profile.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.ProfileBottomSheetBinding


class ProfileBottomSheetFragment: BottomSheetDialogFragment() {

    companion object {
        fun newInstance(listener: ProfileBottomSheetListener): ProfileBottomSheetFragment {
            val fragment = ProfileBottomSheetFragment()
            fragment.listener = listener
            return fragment
        }

    }

    lateinit var binding: ProfileBottomSheetBinding
    lateinit var listener: ProfileBottomSheetListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.profile_bottom_sheet, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener.onBottomSheetReady(this)
    }

    interface ProfileBottomSheetListener {
        fun onBottomSheetReady(bottomSheet: ProfileBottomSheetFragment )
    }

}