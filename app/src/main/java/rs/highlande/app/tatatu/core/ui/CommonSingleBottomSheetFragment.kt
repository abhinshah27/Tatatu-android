package rs.highlande.app.tatatu.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.CommonSingleActionBottomSheetBinding


class CommonSingleBottomSheetFragment: BottomSheetDialogFragment() {

    companion object {
        fun newInstance(listener: BottomSheetListener): CommonSingleBottomSheetFragment {
            val commonSingleBottomSheetFragment = CommonSingleBottomSheetFragment()
            commonSingleBottomSheetFragment.listener = listener
            return commonSingleBottomSheetFragment
        }

    }

    lateinit var binding: CommonSingleActionBottomSheetBinding
    lateinit var listener: BottomSheetListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.common_single_action_bottom_sheet, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener.onBottomSheetReady(this)
    }

    interface BottomSheetListener {
        fun onBottomSheetReady(bottomSheet: CommonSingleBottomSheetFragment)
    }


}