package rs.highlande.app.tatatu.feature.authentication.view.signUp


import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_access_sign_up_add_user_name.btn_next
import kotlinx.android.synthetic.main.fragment_access_signup_welcome.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels.SignUpWelcomeViewModel

class SignUpWelcomeFragment : BaseFragment() {

    private val viewModel: SignUpWelcomeViewModel by viewModel()


    override fun configureLayout(view: View) {}
    override fun bindLayout() {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_access_signup_welcome, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.showProgress.observe(viewLifecycleOwner, Observer {
            if (it) showLoader(R.string.progress_signup)
            else hideLoader()
        })

        val wantsCheckbox = arguments?.getBoolean(BUNDLE_KEY_ADDITIONAL_CHECK, false) == true

        btn_next.setOnClickListener {

            when {
                wantsCheckbox && !checkBoxAccept.isChecked -> showError(getString(R.string.msg_checkBox_Terms_and_Condition))
                !checkBoxAcknowledge.isChecked -> showError(getString(R.string.error_signup_age))
                else -> {
                    viewModel.onDone((it.context as Activity), checkBoxCommunication.isChecked, checkBoxAllow.isChecked)
                }
            }
        }

        // INFO: 2019-10-02    adds checkbox in case social account is to be confirmed
        acceptLayout.visibility =
            if (wantsCheckbox) View.VISIBLE
            else View.GONE

    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    companion object {

        const val BUNDLE_KEY_ADDITIONAL_CHECK = "key_additional_check"

        val logTag = SignUpWelcomeFragment::class.java.simpleName

        fun newInstance(addCheckbox: Boolean): SignUpWelcomeFragment {

            val args = Bundle().apply {
                putBoolean(BUNDLE_KEY_ADDITIONAL_CHECK, addCheckbox)
            }

            val fragment = SignUpWelcomeFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
