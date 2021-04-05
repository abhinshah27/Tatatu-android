package rs.highlande.app.tatatu.feature.notification.view.fragment

/**
 * Created by Abhin.
 */
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.common_toolbar_back.*
import kotlinx.android.synthetic.main.fragment_notification.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.webSocket.realTime.RTCommHelper
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecyclerListener
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.NOTIFICATION_TYPE_FEED
import rs.highlande.app.tatatu.core.util.NOTIFICATION_TYPE_PROFILE
import rs.highlande.app.tatatu.databinding.NotificationMainViewModelBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.commonView.BottomNavigationActivity
import rs.highlande.app.tatatu.feature.notification.view.adapter.NotificationSimpleAdapter
import rs.highlande.app.tatatu.feature.notification.view.viewModel.NotificationViewModel
import rs.highlande.app.tatatu.feature.post.PostActivity
import rs.highlande.app.tatatu.model.DetailUserInfo
import rs.highlande.app.tatatu.model.event.NotificationEvent

class NotificationFragment : BaseFragment() {

    private var mAdapter: NotificationSimpleAdapter? = null
    private val mViewModel: NotificationViewModel by viewModel()
    private var mBinding: NotificationMainViewModelBinding? = null
    private var isFirstCall = true

    private val rtCommHelper: RTCommHelper by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_notification, container, false)
        mBinding?.lifecycleOwner = this // view model attach with lifecycle
        mBinding?.mViewModel = mViewModel //set up view model
        return mBinding!!.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setNotificationData()
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
        if (isFirstCall) {
            mViewModel.replaceItems = true
            isFirstCall = false
            mViewModel.getNotifications(true)
        }
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(logTag)

        //set the notification data
        initObserver()
    }

    override fun onPause() {
        hideLoader()
        (mBaseActivity as BottomNavigationActivity).removeBadge()
        mViewModel.clearObservers(this)
        super.onPause()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    private fun initObserver() {
        mViewModel.mResponse.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                when {
                    it.first.isNullOrEmpty() -> {
                        setUI(true)
                    }
                    !it.first[0].notifications.isNullOrEmpty() -> {
                        setUI(false)
                        mViewModel.currentPage += 1
                        if (!it.second) {
                            mAdapter?.addAll(it.first[0].notifications)
                        } else {
                            mAdapter?.setItems(it.first[0].notifications)
                        }
                    }
                    else -> {
                        if (mViewModel.currentPage == 0) {
                            setUI(true)
                        }
                    }
                }
                mViewModel.mResponse.value = null
                if (BottomNavigationActivity.mNotificationList.size > 0) {
                    BottomNavigationActivity.mNotificationList[0].notifications?.clear()
                    BottomNavigationActivity.mNotificationList[0].notifications?.addAll(mAdapter!!.getItems())
                }
                mViewModel.callingApi = false
                if (notification_srl.isRefreshing) notification_srl.isRefreshing = false
                scrollListener.endLoading()
            }
        })

        mViewModel.mSingleNotifResponse.observe(viewLifecycleOwner, Observer {
            it?.let {
                mAdapter?.let { adapter ->
                    if (!adapter.isEmpty()) {
                        val firstNotif = adapter.getItem(0)
                        if (firstNotif.notificationID != it.notificationID) {
                            mAdapter?.addAtStart(it)
                            if ((mBinding?.rvNotification?.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition() == 0)
                                mBinding?.rvNotification?.scrollToPosition(0)
                            mViewModel.mSingleNotifResponse.value = null                        }
                    }
                }

            }
        })

        //        mViewModel.isProcess.observe(this, Observer {
        //            if (it && firstTime) {
        //                showLoader(resources.getString(R.string.loader_notification))
        //                firstTime = false
        //            } else {
        //                hideLoader()
        //                notification_srl.isRefreshing = false
        //            }
        //        })
    }


    private fun setUI(noResultFound: Boolean) {
        if (!noResultFound) {
            txt_no_results_found.visibility = View.GONE
            rv_notification.visibility = View.VISIBLE
        } else {
            txt_no_results_found.visibility = View.VISIBLE
            rv_notification.visibility = View.GONE
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
        notification_srl.setOnRefreshListener {
            mViewModel.replaceItems = true
            mViewModel.getNotifications(true)
        }
    }

    private fun setNotificationData() {
        mViewModel.mResponse.value = BottomNavigationActivity.mNotificationList to true
    }


    private fun init() {
        (mBaseActivity as BottomNavigationActivity).removeBadge()
        backArrow.visibility = View.INVISIBLE
        title.text = resources.getString(R.string.tb_notification)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(context!!)
        rv_notification.layoutManager = layoutManager
        mAdapter = NotificationSimpleAdapter(object : NotificationItemClickListener {
            override fun onActionClick(uid: String, detailInfo: DetailUserInfo) {
                AccountActivity.openProfileFragment(context!!, uid)
            }

            override fun onItemClick(actionType: String, uid: String) {
                when(actionType) {
                    NOTIFICATION_TYPE_PROFILE -> {
                        AccountActivity.openProfileFragment(context!!, uid)
                    }
                    NOTIFICATION_TYPE_FEED -> {
                        getUser()?.let {
                            PostActivity.openPostDetailFragment(context!!, it.uid, uid)
                        }
                    }
                }
            }

        })
        rv_notification.adapter = mAdapter
        rv_notification.addOnScrollListener(scrollListener)
    }

    val scrollListener = object: CommonScrollListener() {
        override fun hasMoreItems() = mViewModel.hasMoreItems()

        override fun startLoading() {
            mViewModel.replaceItems = false
            mViewModel.getNotifications()
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationEvent(event: NotificationEvent?) {
        if (event != null) {
            if (event.mToReadCount != null && event.mToReadCount!! > 0) {
                mViewModel.getNotification()
            }
        } else {
            LogUtils.d(logTag, "onNotificationEvent: Null")
        }
    }


    override fun configureLayout(view: View) {}
    override fun bindLayout() {}

    companion object {

        val logTag = NotificationFragment::class.java.simpleName

        fun newInstance(): NotificationFragment {
            return NotificationFragment()
        }
    }

    interface NotificationItemClickListener: BaseRecyclerListener {

        fun onActionClick(uid: String, detailInfo: DetailUserInfo)

        fun onItemClick(actionType: String, uid: String)

    }

}