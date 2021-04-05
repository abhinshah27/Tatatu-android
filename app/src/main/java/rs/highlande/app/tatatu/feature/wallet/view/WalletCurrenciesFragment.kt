package rs.highlande.app.tatatu.feature.wallet.view

/**
 * Created by Abhin.
 */
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.common_toolbar_wallet_balance.*
import kotlinx.android.synthetic.main.fragment_equivalent.*
import org.greenrobot.eventbus.EventBus
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.feature.wallet.view.adapter.WalletEquivalentAdapter
import rs.highlande.app.tatatu.model.Equivalent
import rs.highlande.app.tatatu.model.event.EquivalentEvent

class WalletCurrenciesFragment : BaseFragment() {

    private var mAdapter: WalletEquivalentAdapter? = null
    private var mArrayList = ArrayList<Equivalent>()
    private var selectCurrency: String? = ""
    private var clickDone = false
    private var mEquivalent: String? = null
    private val sharedPreferences: PreferenceHelper by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_equivalent, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        mEquivalent = sharedPreferences.getCurrency()!!

        img_back_arrow.setOnClickListener {
            fragmentManager?.popBackStack()
        }
        initRecyclerView()
        initData()
        txt_done.setOnClickListener {
            for ((i, e) in mArrayList.withIndex()) {
                if (e.isSelected) selectCurrency = e.name
            }
            clickDone = true
            fragmentManager?.popBackStack()
        }
    }

    //Set the RecyclerView
    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(context!!)
        rv_equivalent.layoutManager = layoutManager
        mAdapter = WalletEquivalentAdapter(context!!, mArrayList, object : WalletEquivalentAdapter.ItemClickListener {
            override fun itemClick(position: Int) {
                selectCurrency = mArrayList[position].name
            }
        })
        rv_equivalent.adapter = mAdapter
        mAdapter!!.notifyDataSetChanged()
    }

    private fun initData() {
        mArrayList.clear()
        for ((i, e) in resources.getStringArray(R.array.Equivalent).withIndex()) {
            if (!mEquivalent.isNullOrEmpty() && e.startsWith(mEquivalent!!, true)) {
                mArrayList.add(Equivalent(e, true))
                mAdapter?.oldPosition = i
            } else {
                mArrayList.add(Equivalent(e))
            }
        }
        mAdapter?.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    override fun onResume() {
        super.onResume()
        txt_done.visibility = View.VISIBLE
        txt_title.text = resources.getString(R.string.tb_equivalent_to)
    }

    override fun onStop() {
        super.onStop()
        txt_done.visibility = View.GONE
    }

    override fun onDestroy() {
        if (clickDone) EventBus.getDefault().post(EquivalentEvent(selectCurrency))
        super.onDestroy()
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    companion object {
        val logTag = WalletCurrenciesFragment::class.java.simpleName
    }
}