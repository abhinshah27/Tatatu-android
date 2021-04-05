package rs.highlande.app.tatatu.feature.wallet.view

/**
 * Created by Abhin.
 */
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.common_toolbar_wallet_balance.*
import kotlinx.android.synthetic.main.common_toolbar_wallet_balance.txt_title
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.include_wallet_expandable.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.WalletViewModelBinding
import rs.highlande.app.tatatu.feature.wallet.view.viewModel.WalletViewModel
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.event.EquivalentEvent


class WalletFragment : BaseFragment() {

    private val mWalletViewModel: WalletViewModel by viewModel()
    private var mBinding: WalletViewModelBinding? = null
    private var isTextShow = false
    private var mUser: User? = null
    private val sharedPreferences: PreferenceHelper by inject()
    private var mCurrency: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_wallet, container, false)
        mBinding?.lifecycleOwner = this
        mBinding?.mViewModel = mWalletViewModel
        return mBinding!!.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        mCurrency = sharedPreferences.getCurrency()!!
        initObserver()
    }

    private fun initObserver() {
        mWalletViewModel.mCurrencyData.observe(this, Observer {
            if (it != null && !it.values.isNullOrEmpty()) {
                LogUtils.e(CommonTAG, "observer -- $it")
                setCurrency(it)
            }
        })

        mWalletViewModel.mUser.observe(this, Observer {
            if (it != null) {
                mUser = it
                setData()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun setCurrency(it: MutableMap<String, String>) {
        if (!mCurrency.isNullOrEmpty()) {
            for ((i, e) in it.keys.withIndex()) {
                if (!mCurrency.isNullOrEmpty() && mCurrency!!.startsWith(e, true)) {
                    val value = it.getValue(e)
                    val balance = mUser?.balanceInfo?.balance ?: 0.0
                    val result = balance * value.toDouble()
                    when (mCurrency) {
                        resources.getString(R.string.currency_euro) -> {
                            txt_account_balance_equivalent_amount.text = resources.getString(R.string.currency_euro_sym) + formatCurrency(result)
                        }
                        resources.getString(R.string.currency_pound) -> {
                            txt_account_balance_equivalent_amount.text = resources.getString(R.string.currency_pound_sym) + formatCurrency(result)
                        }
                        resources.getString(R.string.currency_yen) -> {
                            txt_account_balance_equivalent_amount.text = resources.getString(R.string.currency_yen_sym) + formatCurrency(result)
                        }
                        else -> {
                            txt_account_balance_equivalent_amount.text = resources.getString(R.string.currency_dollar_sym) + formatCurrency(result)
                        }
                    }
                    txt_account_balance_equivalent.text = mCurrency
                    break
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }

    override fun onResume() {
        txt_title.text = resources.getString(R.string.tb_your_wallet_balance)
        super.onResume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        mWalletViewModel.getCurrencyConversionRate()
        mWalletViewModel.fetchMyUser()

        ll_what_is_about.setOnClickListener {
            if (isTextShow) {
                img_expandable.setImageResource(R.drawable.ic_dropdown_icon_plus)
                collapse(txt_expandable_text)
            } else {
                img_expandable.setImageResource(R.drawable.ic_dropdown_icon_minus)
                expand(txt_expandable_text)
            }

            isTextShow = !isTextShow
        }
        btn_redeem_now.setOnClickListener {
            fireOpenBrowserIntent(mBaseActivity!!, getString(R.string.url_redeem))
        }

        btn_view_wallet_bg.setOnClickListener {
            fireOpenBrowserIntent(mBaseActivity!!, getString(R.string.url_wallet))
        }
        clickHolderEquivalent.setOnClickListener {
            addReplaceFragment(R.id.container, WalletCurrenciesFragment(), false, true, NavigationAnimationHolder())
        }
        img_back_arrow.setOnClickListener {
            activity!!.onBackPressed()
        }
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}

    companion object {
        fun newInstance() = WalletFragment()

        val logTag = WalletFragment::class.java.simpleName

    }

    private fun expand(v: View) {
        val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height = if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT
                else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Expansion speed of 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

    private fun collapse(v: View) {
        val initialHeight = v.measuredHeight

        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Collapse speed of 1dp/ms
        a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

//    override fun observeOnMyUserAction(user: User) {
//        super.observeOnMyUserAction(user)
//        mUser = user
//        setData()
//    }

    fun setData() {
        txt_account_balance.text = formatTTUTokens(mUser?.balanceInfo?.balance ?: 0.0)
        setCurrency(mWalletViewModel.mCurrencyData.value!!)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEquivalentEvent(event: EquivalentEvent) {
        if (!event.mName.isNullOrEmpty()) {
            LogUtils.d(CommonTAG, "getData->${event.mName}")
            sharedPreferences.setCurrency(event.mName!!)
            mCurrency = event.mName!!
            setCurrency(mWalletViewModel.mCurrencyData.value!!)
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}