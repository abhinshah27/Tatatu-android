package rs.highlande.app.tatatu.feature.home.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.chat_bubble_with_badge.view.*
import kotlinx.android.synthetic.main.fragment_main_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ACCOUNT_ID
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewDiffAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.HorizSpaceDecoration
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.FragmentMainHomeBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.commonView.UnFollowListener
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostActivity
import rs.highlande.app.tatatu.feature.home.view.adapters.HomeCategoriesAdapter
import rs.highlande.app.tatatu.feature.home.view.adapters.HomePostsAdapter
import rs.highlande.app.tatatu.feature.home.view.adapters.HomeSuggestedAdapter
import rs.highlande.app.tatatu.feature.home.view.adapters.HomeVideoAdapter
import rs.highlande.app.tatatu.feature.inviteFriends.view.InviteFriendsActivity
import rs.highlande.app.tatatu.feature.multimediaContent.view.MultimediaActivity
import rs.highlande.app.tatatu.feature.post.PostActivity
import rs.highlande.app.tatatu.feature.suggested.view.SuggestedActivity
import rs.highlande.app.tatatu.model.*

/**
 * Fragment performing the rendering of the Home page view.
 *
 * This is the main landing point for the application.
 *
 * @author mbaldrighi on 2019-06-24.
 */
class HomePageFragment : BaseFragment() {

    private lateinit var homeBinding: FragmentMainHomeBinding

    private val homeViewModel by viewModel(HomeViewModel::class)
    private val preferenceHelper: PreferenceHelper by inject()

    private val homeAdapters = mutableMapOf<HomeNavigationData, BaseRecViewDiffAdapter<HomeDataObject, OnItemClickListener<HomeDataObject>, out BaseViewHolder<HomeDataObject, OnItemClickListener<HomeDataObject>>>>()

    private var seeAllPost: View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_home, container, false)
        homeBinding.lifecycleOwner = this

        configureLayout(homeBinding.root)

        return homeBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        homeViewModel.apply {
            getHomeReceived.observe(viewLifecycleOwner, Observer {

                homeBinding.srl.isRefreshing = false

                if (it) {
                    dataContainer.removeAllViews()

                    for (item in observableLists.keys) {
                        configureDataLayout(item)

                        observableLists[item]?.observe(viewLifecycleOwner, Observer { list ->

                            if (item.homeType == HomeUIType.SUGGESTED)
                                LogUtils.d(logTag, "testSUGGESTED - list size: ${list.size} and list: $list")

                            showData(item, list)
                        })

                        when {
                            item.isChannels() -> showData(item, observableLists[item]?.value)
                            item.isStreaming() -> getHomeSingleTypeStreaming(item, ACCOUNT_ID)
                            item.isSocial() -> {
                                when (item.homeType) {
                                    HomeUIType.SUGGESTED -> {
                                        suggestedReceived.observe(viewLifecycleOwner, Observer { received ->
                                            if (received) {
                                                // TODO: 2019-07-16    do something
                                            }
                                        })
                                        suggestedConnected.observe(viewLifecycleOwner, Observer { connected ->
                                            LogUtils.d(logTag, if (connected) "SUGGESTED Request successful" else "SUGGESTED Request failed")
                                        })

                                        homeViewModel.resetSuggestedSkip()
                                    }
                                    HomeUIType.POST -> {
                                        postsReceived.observe(viewLifecycleOwner, Observer { received ->
                                            if (received) {
                                                // TODO: 2019-07-16    do something
                                            }
                                        })
                                        postConnected.observe(viewLifecycleOwner, Observer { connected ->
                                            LogUtils.d(logTag, if (connected) "POST Request successful" else "POST Request failed")
                                        })
                                    }
                                    else -> {
                                    }
                                }
                                getHomeSingleType(item)
                            }
                            else -> {
                            }
                        }
                    }
                }
            })
            getHomeConnected.observe(viewLifecycleOwner, Observer {
                LogUtils.d(logTag, if (it) "HOME_DATA Request successful" else "HOME_DATA Request failed")
            })

            showSeeAll.observe(viewLifecycleOwner, Observer {
                seeAllPost?.visibility = if (it) View.VISIBLE else View.GONE
            })
            chatBadgeLiveData.observe(viewLifecycleOwner, Observer {
                if (it.first) {
                    homeBinding.chatsIcon.toReadCount.visibility = View.VISIBLE
                    homeBinding.chatsIcon.toReadCount.text = it.second.toString()
                } else {
                    homeBinding.chatsIcon.toReadCount.visibility = View.INVISIBLE
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        homeViewModel.getHomeTypes()
        storeDeviceToken()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(logTag, sendTracking = true)

        homeViewModel.fetchToReadCount()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    fun setupMenuChatView() {
        homeViewModel.fetchToReadCount()
        homeBinding.chatsIcon.setOnClickListener {
            ChatActivity.openChatRoomsFragment(context!!)
        }
    }

    override fun configureLayout(view: View) {}


    override fun bindLayout() {
        homeBinding.wfiHeader.apply {

            friendsLabel.text = getFormattedHtml(resources, R.string.wfi_friends)
            inviteLabel.text = getFormattedHtml(resources, R.string.wfi_invite)

            sectionWallet.setOnClickListener {
                AccountActivity.openWalletFragments(context!!)
            }

            sectionFriends.setOnClickListener {
                getUser()?.let {
                    if (it.isValid()) AccountActivity.openFriendsFragment(context!!, it.uid)
                }
            }

            sectionInvite.setOnClickListener {
                InviteFriendsActivity.openInvite(context!!)
            }
        }

        homeBinding.srl.setOnRefreshListener {
            srl.isRefreshing = true
            homeViewModel.resetSuggestedSkip()
            homeViewModel.getHomeTypes()
        }
        setupMenuChatView()
    }

    override fun observeOnMyUserAction(user: User) {
        super.observeOnMyUserAction(user)

        homeBinding.wfiHeader.friendsText.text = getReadableCount(resources, user.balanceInfo.friendsCount)
        homeBinding.wfiHeader.walletText.text = formatTTUTokens(user.balanceInfo.balance)
    }


    private fun configureDataLayout(dataType: HomeNavigationData) {
        when (dataType.homeType) {
            HomeUIType.CHANNELS -> {
                if (!homeAdapters.containsKey(dataType)) {
                    homeAdapters[dataType] = HomeCategoriesAdapter(HomeDiffCallback(), object : OnItemClickListener<HomeDataObject> {
                        override fun onItemClick(item: HomeDataObject) {
                            if (item is StreamingCategory) {
                                homeViewModel.getHomeDataFromSectionTitle(item.label)?.let {
                                    MultimediaActivity.openPlaylistFragment(view!!.context, it)
                                }
                            }
                        }
                    })
                }
                addCategoriesView(R.layout.home_section_categories, dataType)
            }
            HomeUIType.POST -> {
                if (!homeAdapters.containsKey(dataType)) {
                    homeAdapters[dataType] = HomePostsAdapter(HomeDiffCallback(), object : OnItemClickListener<HomeDataObject> {
                        override fun onItemClick(item: HomeDataObject) {
                            if (item is Post) {
                                if (item.isCreateNewPost()) CreatePostActivity.openCreatePostActivity(context!!)
                                else {
                                    val userID = getUserID()
                                    if (userID.isNotBlank()) {
                                        PostActivity.openPostTimelineFragment(context!!, userID, item.uid, CommonApi.TimelineType.TIMELINE)
                                    }
                                }
                            }
                        }
                    })
                }
                addSocialView(R.layout.home_section_social, dataType)
            }
            HomeUIType.SUGGESTED -> {
                if (!homeAdapters.containsKey(dataType)) {
                    homeAdapters[dataType] = HomeSuggestedAdapter(HomeDiffCallback(), object : OnItemClickListener<HomeDataObject> {
                        override fun onItemClick(item: HomeDataObject) {
                            if (item is SuggestedPerson) {
                                AccountActivity.openProfileFragment(context!!, item.uid)
                            }
                        }
                    }, object : UnFollowListener<SuggestedPerson> {
                        override fun onFollowClickedListener(item: SuggestedPerson, view: View, position: Int) {
                            homeViewModel.relationshipChanged.observe(viewLifecycleOwner, Observer {
                                if (it) {
                                    homeAdapters[dataType]?.let { adapter ->
                                        adapter.remove(item)
                                        adapter.notifyItemChanged(position)
                                    }
                                    homeViewModel.removeSuggested(dataType, item)
                                }
                                view.isEnabled = true
                                homeViewModel.relationshipChanged.removeObservers(viewLifecycleOwner)
                            })
                            view.isEnabled = false
                            homeViewModel.followSuggested(item.uid)
                        }

                        override fun onUnfollowClickedListener(item: SuggestedPerson, view: View, position: Int) {}
                    })
                }
                addSocialView(R.layout.home_section_social, dataType)
            }
            HomeUIType.MOMENTS -> {
                addSocialView(R.layout.home_section_social, dataType)
            }
            else -> {
                if (!homeAdapters.containsKey(dataType)) {
                    homeAdapters[dataType] = HomeVideoAdapter(HomeDiffCallback(), dataType.homeType!!, object : OnItemClickListener<HomeDataObject> {
                        override fun onItemClick(item: HomeDataObject) {
                            if (item is TTUVideo) {

                                MultimediaActivity.openVideoFragment(context!!, item)

                            }
                        }
                    })
                }
                addStreamingView(R.layout.home_section_streaming, dataType)
            }
        }
    }


    private fun addView(@LayoutRes layout: Int, block: (View) -> Unit) {
        homeBinding.dataContainer.addView(LayoutInflater.from(homeBinding.dataContainer.context).inflate(layout, homeBinding.dataContainer, false).apply { block.invoke(this) })
    }

    private fun addCategoriesView(@LayoutRes layout: Int, dataType: HomeNavigationData) = addView(layout) {
        it.findViewById<RecyclerView>(R.id.categoriesList).apply {
            adapter = homeAdapters[dataType]
            layoutManager = LinearLayoutManager(view!!.context, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                HorizSpaceDecoration(
                    context,
                    R.dimen.padding_margin_default,
                    defaultPaddingRight = R.dimen.padding_margin_small_6,
                    mOrientation = RecyclerView.HORIZONTAL
                )
            )
        }
    }

    private fun addSocialView(@LayoutRes layout: Int, dataType: HomeNavigationData) = addView(layout) {
        it.findViewById<TextView>(R.id.sectionTitle).text = dataType.sectionTitle
        it.findViewById<TextView>(R.id.actionText).let { textView ->

            if (dataType.homeType == HomeUIType.POST) seeAllPost = textView

            textView.setFormattedHtml(R.string.see_all)
            textView.setOnClickListener {
                when (dataType.homeType) {
                    HomeUIType.SUGGESTED -> {

                        // TODO: 2019-07-03    find a better way?
                        startActivity(Intent(context, SuggestedActivity::class.java))

                    }
                    HomeUIType.MOMENTS -> Toast.makeText(context, "GOTO Moments", Toast.LENGTH_SHORT).show()
                    HomeUIType.POST -> {
                        val userID = getUserID()
                        if (userID.isNotBlank()) {
                            PostActivity.openPostTimelineFragment(context!!, userID, type = CommonApi.TimelineType.TIMELINE)
                        }
                    }
                    else -> showError(context!!.getString(R.string.error_generic))
                }
            }
        }

        it.findViewById<RecyclerView>(R.id.list).apply {
            adapter = homeAdapters[dataType]
            layoutManager = LinearLayoutManager(view!!.context, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                HorizSpaceDecoration(
                    context,
                    R.dimen.padding_margin_default,
                    defaultPaddingRight = when (dataType.homeType) {
                        HomeUIType.SUGGESTED -> R.dimen.home_item_suggested_margin
                        HomeUIType.POST -> R.dimen.item_post_margin
                        else -> 0
                    },
                    mOrientation = RecyclerView.HORIZONTAL
                )
            )
        }
    }

    private fun addStreamingView(@LayoutRes layout: Int, dataType: HomeNavigationData) = addView(layout) {
        it.findViewById<TextView>(R.id.sectionTitle).apply {
            text = dataType.sectionTitle
            setOnClickListener {
                MultimediaActivity.openPlaylistFragment(view!!.context, dataType)
            }
        }
        it.findViewById<RecyclerView>(R.id.list).apply {
            adapter = homeAdapters[dataType]
            layoutManager = LinearLayoutManager(view!!.context, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                HorizSpaceDecoration(
                    context,
                    R.dimen.padding_margin_default,
                    defaultPaddingRight = R.dimen.item_post_margin,
                    mOrientation = RecyclerView.HORIZONTAL
                )
            )
        }
    }


    private fun showData(dataType: HomeNavigationData, data: List<HomeDataObject>?) {

        LogUtils.d(logTag, "testHOME: NOTIFIED (${dataType.sectionTitle})\tsize: ${data?.size}\tadapter: ${homeAdapters[dataType]}")

        if (dataType.homeType == HomeUIType.SUGGESTED)
           LogUtils.d(logTag, "testSUGGESTED - fetchingMore: ${homeViewModel.fetchingMoreSuggested}")

        if (dataType.homeType == HomeUIType.SUGGESTED && homeViewModel.fetchingMoreSuggested) {
            homeAdapters[dataType]?.addAll(data?.toMutableList())
            return
        }

        homeAdapters[dataType]?.submitList(data?.toMutableList())
    }


    private fun getUserID(): String {
        return when {
            getUser() != null -> getUser()!!.uid
            homeViewModel.getCachedUserID().isNotEmpty() -> {
                LogUtils.d(tag, "User not available, getting cached userID")
                homeViewModel.getCachedUserID()
            }
            else -> {
                LogUtils.d(tag, "UserID not available")
                showError(context!!.getString(R.string.error_generic))
                ""
            }
        }
    }

    //Firebase token store.
    private fun storeDeviceToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            with(it.token) {
                if (preferenceHelper.hasDeviceTokenChanged(this)) {
                    preferenceHelper.storeDeviceToken(this)
                    homeViewModel.storeDeviceToken(this)
                    LogUtils.d(logTag, "FCM DeviceToken stored: $this")
                } else LogUtils.d(logTag, "FCM DeviceToken not needed: $this")
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChatToReadEvent(event: ChatToReadEvent) {
        Handler().postDelayed(
            { homeViewModel.fetchToReadCount() },
            500
        )
    }


    companion object {

        val logTag = HomePageFragment::class.java.simpleName

        fun newInstance(): HomePageFragment {
            return HomePageFragment()
        }

    }

}


/**
 * Custom class to handle to-read count update for chat.
 */
class ChatToReadEvent