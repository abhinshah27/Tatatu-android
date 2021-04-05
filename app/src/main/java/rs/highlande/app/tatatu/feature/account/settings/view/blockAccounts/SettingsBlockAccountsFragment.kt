package rs.highlande.app.tatatu.feature.account.settings.view.blockAccounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_block_account_list.*
import kotlinx.android.synthetic.main.toolbar_settings.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.SettingsBlockAccountViewModelBinding
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.SettingsBlockAccountViewModel
import rs.highlande.app.tatatu.model.BlockAccountDataList

/**
 * Created by Abhin.
 */
class SettingsBlockAccountsFragment : BaseFragment() {

    var mAdapterSettings: SettingsBlockAccountsAdapter? = null
    private var mArrayList = ArrayList<BlockAccountDataList>()

    private val mSettings: SettingsBlockAccountViewModel by viewModel()
    private var mBinding: SettingsBlockAccountViewModelBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        //get the block AccountList
        mSettings.mArrayList.observe(this, Observer {
            if (it != null && !it.isNullOrEmpty()) {
                mArrayList.clear()
                mArrayList.addAll(it)
                if (mAdapterSettings != null) mAdapterSettings!!.notifyDataSetChanged()
                txt_no_block_account.visibility = View.GONE
                rv_block_account.visibility = View.VISIBLE
            } else {
                txt_no_block_account.visibility = View.VISIBLE
                rv_block_account.visibility = View.INVISIBLE
            }
        })

        //remove form ArrayList
        mSettings.mUnBlock.observe(this, Observer {
            if (it != null && it) {
                if (mSettings.mPosition.value != -1) {
                    mArrayList.remove(mArrayList[mSettings.mPosition.value!!])
                    if (mAdapterSettings != null) mAdapterSettings!!.notifyDataSetChanged()
                    if (mArrayList.isEmpty()) {
                        txt_no_block_account.visibility = View.VISIBLE
                        rv_block_account.visibility = View.INVISIBLE
                    }
                }
            }
        })

        //set progress
        mSettings.isProgress.observe(this, Observer {
            if (it != null && it) {

            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_block_account_list, container, false)
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
        //get the block list
        mSettings.blockListType()
        initRecyclerView()
        txt_tb_title.text = resources.getString(R.string.settings_block_profile)
        img_tb_back.setOnClickListener {
            fragmentManager?.popBackStack()
        }
    }

    //Set the RecyclerView
    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(context!!)
        rv_block_account.layoutManager = layoutManager
        mAdapterSettings = SettingsBlockAccountsAdapter(context!!, mArrayList, object : SettingsBlockAccountsAdapter.ItemClickListener {
            override fun itemClick(position: Int) {
                mSettings.mPosition.value = position
                mSettings.mUnBlock.value = false
                mSettings.removeBlockAccount(mArrayList[position].uid!!, 0)
            }
        })
        rv_block_account.adapter = mAdapterSettings
        mAdapterSettings!!.notifyDataSetChanged()
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    companion object {
        val logTag = SettingsBlockAccountsFragment::class.java.simpleName
    }
}