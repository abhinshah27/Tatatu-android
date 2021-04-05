package rs.highlande.app.tatatu.feature.commonView

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.common_progress.*
import rs.highlande.app.tatatu.R

/**
 * Created by Abhin.
 * user indicator for running on background via progress bar
 */
class BaseProgressDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AppCompatDialog {
        val dialog = AppCompatDialog(context!!, R.style.TTUDialogProgress)
        dialog.window!!.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        dialog.setContentView(R.layout.common_progress)
        return dialog
    }

    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        isCancelable = false
        return inflater.inflate(R.layout.common_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val int = arguments?.getInt(BUNDLE_KEY_MESSAGE_INT, 0) ?: 0
        val string = arguments?.getString(BUNDLE_KEY_MESSAGE_STRING, "") ?: ""
        setMessage(int, string)
    }


    fun setMessage(@StringRes iMessage: Int = 0, sMessage: String = "") {
        when {
            (iMessage != 0) || (!sMessage.isBlank()) -> {
                this@BaseProgressDialogFragment.message.apply {
                    text = getString(
                        R.string.dialog_progress_generic_2,
                        if (iMessage != 0) getString(iMessage) else sMessage
                    )
                    visibility = View.VISIBLE
                }
            }
            else -> {
                this@BaseProgressDialogFragment.message.visibility = View.GONE
            }
        }
    }


    companion object {

        val logTag = BaseProgressDialogFragment::class.java.simpleName

        const val BUNDLE_KEY_MESSAGE_INT = "message_int"
        const val BUNDLE_KEY_MESSAGE_STRING = "message_string"


        fun newInstance(@StringRes iMessage: Int = 0, sMessage: String = ""): BaseProgressDialogFragment {

            val args = Bundle().apply {
                putInt(BUNDLE_KEY_MESSAGE_INT, iMessage)
                putString(BUNDLE_KEY_MESSAGE_STRING, sMessage)
            }

            val fragment = BaseProgressDialogFragment()
            fragment.arguments = args
            return fragment
        }


    }


}