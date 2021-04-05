package rs.highlande.app.tatatu.feature.account.settings.view

/**
 * Created by Abhin.
 */
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.toolbar_settings.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.SettingsNotificationsViewModelBinding
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.SettingsNotificationsViewModel

class SettingsNotificationsFragment : BaseFragment() {

    private val mSettings: SettingsNotificationsViewModel by viewModel()
    private var mBinding: SettingsNotificationsViewModelBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_notification_settings, container, false)
        mBinding?.lifecycleOwner = this // view model attach with lifecycle
        mBinding?.mViewModel = mSettings //setting up view model
        return mBinding!!.root
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
        txt_tb_title.text = resources.getString(R.string.tb_notification_settings)
        img_tb_back.setOnClickListener {
            fragmentManager!!.popBackStack()
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    companion object {
        val logTag = SettingsNotificationsFragment::class.java.simpleName
    }
}