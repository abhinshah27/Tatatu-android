package rs.highlande.app.tatatu.feature.account.profile.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.manager.RelationshipAction
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.ui.CommonSingleBottomSheetFragment
import rs.highlande.app.tatatu.core.ui.ImageSingleBottomSheetFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.ProfileActionBinding
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.FollowTabsFragment
import rs.highlande.app.tatatu.feature.account.settings.view.SettingsFragment
import rs.highlande.app.tatatu.feature.chat.view.ChatActivity
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializer
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.DefaultCallInitializerDelegate
import rs.highlande.app.tatatu.model.Relationship
import rs.highlande.app.tatatu.model.User

class ProfileFragment : BaseProfileFragment(), DefaultCallInitializer by DefaultCallInitializerDelegate() {

    companion object {

        val logTag = ProfileFragment::class.java.simpleName

        fun newInstance(userId: String): ProfileFragment {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_USER_ID, userId)
            val instance = ProfileFragment()
            instance.arguments = bundle
            return instance
        }
    }

    private lateinit var profileActionBinding: ProfileActionBinding
    private var privateProfile = false
    private val mPermissionRequestCode = 10045
    private lateinit var menu: Menu

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        (activity as? BaseActivity)?.apply {
            setSupportActionBar(profileBinding.profileToolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
            supportActionBar!!.setDisplayShowHomeEnabled(false)
        }
        return profileBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        subscribeToLiveData()
    }


    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        if (privateProfile) setupPrivateProfile()
        else loadProfileData()
    }

    override fun onPause() {
        viewModel.clearObservers(this@ProfileFragment)
        super.onPause()
    }

    private fun setupPrivateProfile() {
        profileBinding.followGroup.visibility = View.INVISIBLE
        profileBinding.donatingGroup.visibility = View.GONE
        profileBinding.userDescriptionTextview.visibility = View.INVISIBLE
        profileBinding.userWebSiteTextView.visibility = View.INVISIBLE
        if (::profileActionBinding.isInitialized) {
            profileActionBinding.root.visibility = View.INVISIBLE
        }
        //TODO: Uncomment when moments and tags are available
        /*profileBinding.profileTablayout.touchables.forEach {
            it.isClickable = false
            it.isEnabled = false
            it.isSelected = false
        }*/
        profileBinding.privateProfileTextview.visibility = View.VISIBLE
        loadPrivateProfileData()
    }

    private fun loadPrivateProfileData() {
        viewModel.profileUserLiveData.value?.let { user ->
            viewModel.profileImage = user.picture
            profileBinding.apply {
                userFullnameTextview.text = user.name
                profileToolbar.title.text = user.username
                userPicture.picture.setProfilePicture(user.picture)
                userPicture.celebrityIndicator.visibility = if (user.isCelebrity()) View.VISIBLE else View.GONE
            }
            profileBinding.profileLoader.progressbar.hide()
            profileBinding.profileLoader.root.visibility = View.GONE
            if (isMyProfile(user)) showMyToolbar()
        }
    }

    override fun updateProfileUI(user: User) {
        super.updateProfileUI(user)
        updateProfileAction(viewModel.getUserFollowStatus())
        profileActionBinding.profileActionButton.setOnClickListener {
            viewModel.onProfileActionButtonClick()
        }
        profileActionBinding.profileRequestedButton.setOnClickListener {
            viewModel.onProfileActionButtonClick()
        }

        if (isMyProfile(user)) {
            showMyToolbar()
        } else {
            if (::menu.isInitialized) {

                // TODO: 2019-12-18    review if still OK
                viewModel.getUserFollowStatus().let {
                    menu.findItem(R.id.actionCall).isEnabled = it.value == Relationship.FRIENDS.value
                    menu.findItem(R.id.actionVideoCall).isEnabled = it.value == Relationship.FRIENDS.value
                }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (privateProfile) {
            inflater.inflate(R.menu.menu_private_profile, menu)
        } else {
            inflater.inflate(R.menu.menu_profile, menu)
        }
        this.menu = menu

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionVideoCall -> {
                if (getUser() != null && viewModel.profileUserLiveData.value != null) {
                    initPermissionsForCall(this, mPermissionRequestCode, logTag, getUser()!!, viewModel.profileUserLiveData.value!!, true)
                }
            }
            R.id.actionCall -> {
                if (getUser() != null && viewModel.profileUserLiveData.value != null) {
                    initPermissionsForCall(this, mPermissionRequestCode, logTag, getUser()!!, viewModel.profileUserLiveData.value!!, false)
                }
            }

            R.id.actionOverflow -> {
                if (privateProfile) {
                    val bottomSheet = CommonSingleBottomSheetFragment.newInstance(object : CommonSingleBottomSheetFragment.BottomSheetListener {
                        override fun onBottomSheetReady(bottomSheet: CommonSingleBottomSheetFragment) {
                            bottomSheet.binding.bottomSheetAction.setText(R.string.bottom_sheet_report)
                            bottomSheet.binding.bottomSheetAction.setOnClickListener {
                                viewModel.reportUser()
                                bottomSheet.dismiss()
                            }
                        }
                    })
                    bottomSheet.show(childFragmentManager, "bottomSheet")
                } else {
                    val bottomSheet = ProfileBottomSheetFragment.newInstance(object : ProfileBottomSheetFragment.ProfileBottomSheetListener {
                        override fun onBottomSheetReady(bottomSheet: ProfileBottomSheetFragment) {
                            bottomSheet.binding.apply {
                                bottomSheetReport.setOnClickListener {
                                    viewModel.reportUser()
                                    bottomSheet.dismiss()
                                }
                                bottomSheetShare.setOnClickListener {
                                    viewModel.profileDeeplinkLiveData.observe(viewLifecycleOwner, Observer {
                                        Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, it)
                                            type = "text/plain"
                                            startActivity(Intent.createChooser(this, getString(R.string.share_with_label)))
                                        }
                                        viewModel.profileDeeplinkLiveData.removeObservers(viewLifecycleOwner)
                                    })
                                    viewModel.fetchShareUserProfileDeeplink()
                                    bottomSheet.dismiss()
                                }
                                bottomSheetBlock.setOnClickListener {
                                    viewModel.blockUser()
                                    bottomSheet.dismiss()
                                }
                            }
                        }

                    })
                    bottomSheet.show(childFragmentManager, "bottomSheet")
                }
            }
            R.id.actionSettings -> {
                // if (!getUser()?.uid.isNullOrBlank()) AccountActivity.openSettingsFragments(context!!, getUser()!!.uid)
                if (!getUser()?.uid.isNullOrBlank()) {
                    addReplaceFragment(R.id.container, SettingsFragment.newInstance(getUser()!!.uid), false, true, NavigationAnimationHolder())
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    // check voice permission
    //    private fun initVoicePermissionCall() {
    //        LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> initVoicePermissionCall() --> ")
    //        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_PHONE_STATE), mPermissionRequestCode, true)
    //        permissionHelper!!.requestManually(object : PermissionHelper.PermissionCallbackManually {
    //            override fun onPermissionGranted() {
    //                doCallNotification(false)
    //            }
    //
    //            override fun onCheckPermissionManually(requestCode: Int, permissions: Array<String>, grantResults: IntArray, checkPermissionsManuallyGranted: ArrayList<String>, checkPermissionsManuallyPending: ArrayList<String>) {
    //                LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> onCheckPermissionManually() checkPermissionsManuallyGranted--> ${Gson().toJson(checkPermissionsManuallyGranted)}")
    //                LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> onCheckPermissionManually() checkPermissionsManuallyPending--> ${Gson().toJson(checkPermissionsManuallyPending)}")
    //                if (checkPermissionsManuallyGranted.contains(Manifest.permission.RECORD_AUDIO)) {
    //                    doCallNotification(false)
    //                }
    //            }
    //        })
    //    }
    //
    //    // check video permissions
    //    private fun initVideoPermissionCall() {
    //        LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> initVideoPermissionCall() --> ")
    //        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_PHONE_STATE), mPermissionRequestCode,true)
    //        permissionHelper!!.requestManually(object : PermissionHelper.PermissionCallbackManually {
    //            override fun onPermissionGranted() {
    //                doCallNotification(true)
    //            }
    //
    //            override fun onCheckPermissionManually(requestCode: Int, permissions: Array<String>, grantResults: IntArray, checkPermissionsManuallyGranted: ArrayList<String>, checkPermissionsManuallyPending: ArrayList<String>) {
    //                LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> onCheckPermissionManually() checkPermissionsManuallyGranted--> ${Gson().toJson(checkPermissionsManuallyGranted)}")
    //                LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> onCheckPermissionManually() checkPermissionsManuallyPending--> ${Gson().toJson(checkPermissionsManuallyPending)}")
    //                if (checkPermissionsManuallyGranted.contains(Manifest.permission.RECORD_AUDIO) || checkPermissionsManuallyGranted.containsAll(arrayListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)))
    //                    doCallNotification(true)
    //            }
    //        })
    //    }
    //
    //    private fun doCallNotification(isVideo: Boolean) {
    //        if (getUser() != null && viewModel.profileUserLiveData.value != null) {
    //            LogUtils.e(CommonTAG, "CN->ProfileFragment FN--> doCallNotification() --> isVideo $isVideo")
    //            CallsNotificationUtils.sendCallNotification(activity as BaseActivity, getUser()!!.uid, getUser()!!.name, viewModel.profileUserLiveData.value!!, if (isVideo) VoiceVideoCallType.VIDEO else VoiceVideoCallType.VOICE)
    //        }
    //    }

    private fun updateProfileAction(relationship: Relationship) {

        LogUtils.d(logTag, "testRelationship - updateProfileAction(): $relationship")

        if (::profileActionBinding.isInitialized) {
            with(profileActionBinding) {
                profileActionButton.visibility = View.VISIBLE
                profileRequestedButton.visibility = View.INVISIBLE

                //TODO: Remove when message functionality is implemented
                var hideButton = false
                var hideIcon = false

                // gets the String for button and Drawable for profile picture
                val text = relationship.toStringForButton()
                when (relationship) {
                    Relationship.FOLLOWER -> profileStatusIcon.setIcon(R.drawable.ic_user_follower)
                    Relationship.FOLLOWING -> {
                        profileStatusIcon.setOnClickListener {
                            viewModel.onProfileActionIconClick()
                        }
                        hideButton = true
                        profileStatusIcon.setIcon(R.drawable.ic_user_following)
                    }
                    Relationship.FRIENDS -> {
                        profileStatusIcon.setOnClickListener {
                            viewModel.onProfileActionIconClick()
                        }
                        profileStatusIcon.setIcon(R.drawable.ic_user_follow_each_other)

                    }
                    Relationship.PENDING_FOLLOW -> {
                        //TODO: Implement custom button with multiple states to handle this
                        profileActionButton.visibility = View.INVISIBLE
                        profileRequestedButton.visibility = View.VISIBLE
                    }
                    Relationship.NA -> hideIcon = true
                    Relationship.MYSELF -> hideButton = true
                    else -> return
                }

                profileActionButton.visibility = if (hideButton) View.GONE else View.VISIBLE
                profileStatusIcon.visibility = if (hideIcon) View.GONE else View.VISIBLE

                profileActionButton.setText(text)
            }
        }
    }

    override fun loadProfileData() {
        viewModel.profileUserLiveData.value?.let { user ->
            viewModel.profileImage = user.picture
            setupProfileTabs(false)
            profileBinding.sectionFollowers.setOnClickListener {
                addReplaceFragment(R.id.container, FollowTabsFragment.newInstance(FollowTabsFragment.TAB_FOLLOWERS, user.uid), false, true, NavigationAnimationHolder())
            }

            profileBinding.sectionFollowing.setOnClickListener {
                addReplaceFragment(R.id.container, FollowTabsFragment.newInstance(FollowTabsFragment.TAB_FOLLOWING, user.uid), false, true, NavigationAnimationHolder())
            }
            profileBinding.profileactionViewstub.setOnInflateListener { _, view ->
                DataBindingUtil.bind<ProfileActionBinding>(view)?.let {
                    profileActionBinding = it
                }
            }

            profileBinding.profileactionViewstub.viewStub?.let {
                it.layoutResource = R.layout.profile_action
                it.inflate()
            }
            updateProfileUI(user)
            profileBinding.profileToolbar.title.text = user.username
            hideProgressBar()
        }
    }

    override fun subscribeToLiveData() {
        super.subscribeToLiveData()
        viewModel.profileUserLiveData.observe(viewLifecycleOwner, Observer {
            setHasOptionsMenu(true)
            it?.let {
                if (viewModel.isPrivate(it)) {
                    privateProfile = it.isPrivate()
                    setupPrivateProfile()
                } else loadProfileData()
            }
        })

        viewModel.profileActionLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                when (it) {
                    RelationshipAction.FOLLOW_BACK_ACTION -> viewModel.updateFollowStatus()
                    RelationshipAction.REQUEST_CANCEL_ACTION -> {
                        val bottomSheet = ImageSingleBottomSheetFragment.newInstance(object : ImageSingleBottomSheetFragment.BottomSheetListener {
                            override fun onBottomSheetReady(bottomSheet: ImageSingleBottomSheetFragment) {
                                bottomSheet.binding.bottomSheetAction.setText(R.string.bottom_sheet_cancel_request)
                                bottomSheet.binding.bottomSheetHint.setText(R.string.bottom_sheet_tap_cancel_request_hint)

                                bottomSheet.binding.bottomImageView.setProfilePicture(viewModel.profileImage)

                                bottomSheet.binding.bottomSheetAction.setOnClickListener {
                                    showMessage(getString(R.string.result_action_cancel_request, viewModel.profileUserLiveData.value?.username))
                                    viewModel.updateFollowStatus()
                                    bottomSheet.dismiss()
                                }
                            }
                        })
                        bottomSheet.show(childFragmentManager, "bottomSheet")
                    }
                    RelationshipAction.FOLLOW_ACTION -> viewModel.updateFollowStatus()
                    RelationshipAction.FOLLOWING_FRIENDS_ACTION, RelationshipAction.UNFOLLOW -> {
                        val bottomSheet = ImageSingleBottomSheetFragment.newInstance(object : ImageSingleBottomSheetFragment.BottomSheetListener {
                            override fun onBottomSheetReady(bottomSheet: ImageSingleBottomSheetFragment) {
                                bottomSheet.binding.bottomSheetAction.setText(R.string.bottom_sheet_unfollow)
                                bottomSheet.binding.bottomSheetHint.setText(R.string.bottom_sheet_tap_unfollow_hint)

                                bottomSheet.binding.bottomImageView.setProfilePicture(viewModel.profileImage)

                                bottomSheet.binding.bottomSheetAction.setOnClickListener {
                                    showMessage(getString(R.string.result_action_unfollow, viewModel.profileUserLiveData.value?.username))
                                    viewModel.updateFollowStatus()
                                    bottomSheet.dismiss()
                                }
                            }
                        })
                        bottomSheet.show(childFragmentManager, "bottomSheet")
                    }
                    else -> {
                    }
                }
                viewModel.profileActionLiveData.value = null
            }
        })

        viewModel.profileReportedLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                showMessage(getString(R.string.result_action_report, viewModel.profileUserLiveData.value?.username))
            }
        })

        viewModel.profileBlockedLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                showMessage(getString(R.string.result_action_block, viewModel.profileUserLiveData.value?.username))
                doAfterLosingUser()
            }
        })

        viewModel.profileStartChatLiveData.observe(viewLifecycleOwner, Observer {
            if (it.first) {
                ChatActivity.openChatMessageFragment(context!!, it.second)
                viewModel.profileStartChatLiveData.postValue(false to "")
            }
        })
    }

    override fun configureLayout(view: View) {
        arguments?.let {
            it.getString(BUNDLE_KEY_USER_ID)?.let { userID ->
                showProgressBar()
                viewModel.profileUserLiveData.value = null
                viewModel.fetchUser(userID)
            }
        }

        profileBinding.profileToolbar.backArrow.setOnClickListener {
            activity?.onBackPressed()
        }

    }

    override fun bindLayout() {}


    private fun doAfterLosingUser() {
        mBaseActivity?.onBackPressed()
    }

    private fun isMyProfile(user: User): Boolean = getUser()?.let {
        return it.uid == user.uid
    } ?: run {
        false
    }

    private fun showMyToolbar() {
        if (::menu.isInitialized) {
            menu.forEach {
                it.isVisible = false
            }
            activity!!.menuInflater.inflate(R.menu.menu_my_profile, menu)
        }
    }

    // permission callbacks
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        forwardRequestPermissionResult(requestCode, permissions, grantResults)
    }
}