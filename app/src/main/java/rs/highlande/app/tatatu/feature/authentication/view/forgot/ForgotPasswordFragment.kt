package rs.highlande.app.tatatu.feature.authentication.view.forgot


import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_access_forgot.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.showDialogGeneric
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.ForgotViewModelBinding

class ForgotPasswordFragment : BaseFragment() {

    private val mForgotViewModel: ForgotViewModel by viewModel()
    private var mForgotViewModelBinding: ForgotViewModelBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mForgotViewModelBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_access_forgot, container, false)
        mForgotViewModelBinding?.forgotViewModel = mForgotViewModel
        mForgotViewModelBinding!!.lifecycleOwner = this
        return mForgotViewModelBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    private fun init() {
        txt_back_to_sign_in.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
        }

        mForgotViewModel.callSuccess.observe(viewLifecycleOwner, Observer {
            if (it) {
                showDialogGeneric(
                    context!!,
                    R.string.forgot_pwd_success_body,
                    R.string.forgot_pwd_success_title,
                    R.string.btn_ok,
                    onPositive = DialogInterface.OnClickListener { p0, p1 ->
                        p0.dismiss()
                    })
            }
        })

        mForgotViewModel.callError.observe(viewLifecycleOwner, Observer {
            if (it) {
                showError(getString(R.string.error_generic))
            }
        })

        mForgotViewModel.showProgress.observe(viewLifecycleOwner, Observer {
            if (it) showLoader(getString(R.string.forgot_pwd_sending))
            else hideLoader()
        })
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    companion object {
        val logTag = ForgotPasswordFragment::class.java.simpleName
    }

}
