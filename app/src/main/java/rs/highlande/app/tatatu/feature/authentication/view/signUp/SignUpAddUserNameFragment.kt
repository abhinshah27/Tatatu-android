package rs.highlande.app.tatatu.feature.authentication.view.signUp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.toolbar_add_user.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.SignUpAddUserBinding
import rs.highlande.app.tatatu.feature.authentication.view.signUp.viewModels.SignUpAddUserViewModel

class SignUpAddUserNameFragment : BaseFragment() {

    private val mSignUpAddUserViewModel: SignUpAddUserViewModel by viewModel()
    private var mSignUpAddUserBinding: SignUpAddUserBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mSignUpAddUserBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_access_sign_up_add_user_name, container, false)
        mSignUpAddUserBinding?.signUpAddUserViewModel = mSignUpAddUserViewModel
        mSignUpAddUserBinding!!.lifecycleOwner = this
        return mSignUpAddUserBinding?.root
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        mSignUpAddUserViewModel.createTextChangeObservable(mSignUpAddUserBinding?.edtUsername)
    }

    @SuppressLint("NewApi")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        txt_tb_title.text = resources.getString(R.string.msg_add_username)
        img_tb_back.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
        }

    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    companion object {
        val logTag = SignUpAddUserNameFragment::class.java.simpleName
    }
}