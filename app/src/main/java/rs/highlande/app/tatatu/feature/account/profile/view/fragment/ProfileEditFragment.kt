package rs.highlande.app.tatatu.feature.account.profile.view.fragment

/**
 * Created by Abhin.
 */
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import kotlinx.android.synthetic.main.fragment_profile_edit.*
import kotlinx.android.synthetic.main.toolbar_settings.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.disableNestedScrollOnView
import rs.highlande.app.tatatu.core.ui.showDialogGeneric
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.SettingEditProfileViewModelBinding
import rs.highlande.app.tatatu.feature.account.profile.repository.DateHelper
import rs.highlande.app.tatatu.feature.account.profile.view.adapter.CommonSpinnerAdapter
import rs.highlande.app.tatatu.feature.account.profile.view.viewmodel.ProfileEditViewModel
import rs.highlande.app.tatatu.feature.authentication.AuthActivity
import rs.highlande.app.tatatu.model.CommonSpinnerList
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.event.ImageBottomEvent


class ProfileEditFragment : BaseFragment() {

    private val mSettings: ProfileEditViewModel by viewModel()
    private var mSettingEditProfileViewModelBinding: SettingEditProfileViewModelBinding? = null
    private var oldSelectionGender = 0
    private var mSelectedGender = ""
    private var oldSelectionCountry = 0
    private var mSelectedCountry = ""
    private var mCommonSpinnerAdapter: CommonSpinnerAdapter? = null
    private var mUser: User? = null

    private var imagePath: String? = null
    private var mCameraImageUri: Uri? = null
    private var marshMellowHelper: PermissionHelper? = null
    private val sharedPreferences: PreferenceHelper by inject()

    companion object {

        val logTag = ProfileEditFragment::class.java.simpleName

        fun newInstance() = ProfileEditFragment()
    }

    override fun observeOnMyUserAction(user: User) {
        super.observeOnMyUserAction(user)
        mUser = user
        setUIFields()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mSettingEditProfileViewModelBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_edit, container, false)
        mSettingEditProfileViewModelBinding?.lifecycleOwner = this // view model attach with lifecycle
        mSettingEditProfileViewModelBinding?.mViewModel = mSettings //setting up view model
        return mSettingEditProfileViewModelBinding!!.root
    }

    private fun initObserver() {
        mSettings.mEditPrivateInformation.observe(viewLifecycleOwner, Observer {
            if (it != null && it) {
                //                expand(ll_private_info) //Animation
                ll_private_info.visibility = View.VISIBLE
                img_settings_plus_icon.setImageResource(R.drawable.ic_dropdown_icon_minus)
            } else {
                //                collapse(ll_private_info) //Animation
                ll_private_info.visibility = View.GONE
                img_settings_plus_icon.setImageResource(R.drawable.ic_dropdown_icon_plus)
            }
        })
        mSettings.mDiscardChanges.observe(viewLifecycleOwner, Observer {
            setClearFields()
            setUIFields()
        })

        mSettings.mDeleteProfile.observe(viewLifecycleOwner, Observer {
            if (it != null && it) {
                goToLogin(ProfileEditViewModel.Action.DELETE_PROFILE)
            }
        })

        mSettings.mLogout.observe(viewLifecycleOwner, Observer {
            if (it != null && it) {
                goToLogin(ProfileEditViewModel.Action.LOGOUT)
            }
        })

        mSettings.mErrorMessage.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                showError(mSettings.mErrorMessage.value!!)
            }
        })
        mSettings.mEditProfile.observe(viewLifecycleOwner, Observer {
            if (it) {
                hideLoader()
                activity!!.finish()
            }
        })

        mSettings.mShowProgress.observe(viewLifecycleOwner, Observer {
            if (it.first && it.second != null) {
                showLoader(when (it.second) {
                    ProfileEditViewModel.ProgressAction.UPLOAD -> getString(R.string.progress_media_upload, it.third)
                    ProfileEditViewModel.ProgressAction.EDIT -> getString(R.string.progress_profile_saving)
                    else -> throw IllegalArgumentException("Cannot be Action other than UPLOAD or EDIT")
                })
            } else {
                hideLoader()
            }
        })

        mSettings.errorUserName.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrBlank()) {
                showError(resources.getString(R.string.error_existing_username, it))
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {

        initObserver()

        hideKeyboard(activity!!)
        txt_tb_title.text = resources.getString(R.string.tb_edit_profile)

        img_tb_back.setOnClickListener {
            activity?.onBackPressed()
        }

        // INFO: 2019-09-05    currently a NO-OP -> HIDDEN 
        txt_delete_profile.setOnClickListener {
            showDialogGeneric(context!!, resources.getString(R.string.setting_msg_dialog_delete), resources.getString(R.string.title_dialog_delete), resources.getString(R.string.btn_dialog_delete), resources.getString(R.string.btn_dialog_cancel), DialogInterface.OnClickListener { _, _ ->
                mSettings.getDeleteProfile()
            })
        }

        txt_sign_out.setOnClickListener {
            showDialogGeneric(context!!, resources.getString(R.string.setting_msg_dialog_log_out), resources.getString(R.string.title_dialog_log_out), resources.getString(R.string.btn_dialog_log_out), resources.getString(R.string.btn_dialog_cancel), DialogInterface.OnClickListener { _, _ ->
                mSettings.redirectResponseSuccessful(ProfileEditViewModel.Action.LOGOUT)
            })
        }

        txt_settings_add_image.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //check runtime permission required or not
                marshMellowHelper = PermissionHelper(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
                marshMellowHelper!!.request(object : PermissionHelper.PermissionCallback {
                    override fun onPermissionGranted() {
                        ImageBottomSheetDialog().show(childFragmentManager, ImageBottomSheetDialog::javaClass.name)
                    }

                    override fun onPermissionDenied() {

                    }

                    override fun onPermissionDeniedBySystem() {

                    }
                })
            } else {
                ImageBottomSheetDialog().show(childFragmentManager, ImageBottomSheetDialog::javaClass.name)
            }
        }

        edt_settings_bio.disableNestedScrollOnView()
        edt_settings_dob.addTextChangedListener(DateHelper())
        setSpinner()

    }

    private fun setSpinner() {
        val mArrayCountry = ArrayList<CommonSpinnerList>()
        val mArrayGender = ArrayList<CommonSpinnerList>()

        for (e in countryList) {
            mArrayCountry.add(CommonSpinnerList(e, false))
        }

        for (e in resources.getStringArray(R.array.Gender)) {
            mArrayGender.add(CommonSpinnerList(e, false))
        }

        val mGenderAdapter = CommonSpinnerAdapter(context!!, R.layout.item_spinner_settings_profile_selection, mArrayGender)
        spinner_gender.adapter = mGenderAdapter

        mCommonSpinnerAdapter = CommonSpinnerAdapter(context!!, R.layout.item_spinner_settings_profile_selection, mArrayCountry)
        spinner_country.adapter = mCommonSpinnerAdapter

        spinner_country?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                if (position != 0) {
                    mArrayCountry[oldSelectionCountry].mSelected = false
                    oldSelectionCountry = position
                    mArrayCountry[position].mSelected = true
                    mSelectedCountry = mArrayCountry[position].mName
                    mSettings.mSelectedCountry.value = mSelectedCountry
                }
            }
        }

        spinner_gender?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                if (position != 0) {
                    mArrayGender[oldSelectionGender].mSelected = false
                    oldSelectionGender = position
                    mArrayGender[position].mSelected = true
                    mSelectedGender = mArrayGender[position].mName
                    mSettings.mSelectedGender.value = mSelectedGender
                }
            }
        }
    }

    //clear all fields
    private fun setClearFields() {
        edt_settings_full_name.setText("")
        edt_settings_username.setText("")
        edt_settings_bio.setText("")
        edt_settings_personal_website.setText("")
        edt_settings_email.setText("")
        edt_settings_password.setText("")
        spinner_country.setSelection(0)
        spinner_gender.setSelection(0)
        edt_settings_dob.setText("")
    }

    //set UI all fields
    private fun setUIFields() {
        if (mUser != null) {
            LogUtils.d("Tag", "--> ${Gson().toJson(mUser)}")
            txt_settings_add_image.setText(if (!mUser!!.picture.isBlank()) R.string.settings_change_image
            else R.string.settings_add_image)
            profilePicture.celebrityIndicator.visibility = if (mUser!!.isCelebrity()) View.VISIBLE else View.GONE
            edt_settings_full_name.setText(mUser?.name)
            txt_settings_userName.text = mUser?.username
            mSettings.previousUserName.value = mUser?.username
            edt_settings_username.setText(mUser?.username)
            edt_settings_bio.setText(mUser?.detailsInfo?.bio)
            edt_settings_personal_website.setText(mUser?.detailsInfo?.website)
            edt_settings_email.setText(mUser?.privateInfo?.email)

            // INFO: 2019-08-30    Server shouldn't send it anyway, but to be sure, do not display
            //            edt_settings_password.setText(mUser?.privateInfo?.password)

            edt_settings_phone_no.setText(mUser?.privateInfo?.phoneNo)

            if (!mUser?.privateInfo?.country.isNullOrEmpty()) {
                spinner_country.setSelection(countryList.indexOf(mUser?.privateInfo?.country))
            }
            if (!mUser?.privateInfo?.gender.isNullOrEmpty()) {
                spinner_gender.setSelection(resources.getStringArray(R.array.Gender).indexOf(mUser?.privateInfo?.gender))
            }

            edt_settings_dob.setText(getEditProfileDateFromDB(mUser?.privateInfo?.dateOfBirth))

            if (!mUser?.picture.isNullOrEmpty()) {
                setImage(mUser?.picture!!, false)
            }
        }
    }

    private fun getUserObject(json: String): User? {
        val type = object : TypeToken<User>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun galleryIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT //
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE)
    }

    private fun cameraIntent() {
        val values = ContentValues()
        values.put("title", System.currentTimeMillis().toString() + resources.getString(R.string.app_name) + "_image")
        mCameraImageUri = activity!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intentCamera = Intent("android.media.action.IMAGE_CAPTURE")
        intentCamera.putExtra("output", mCameraImageUri)
        startActivityForResult(intentCamera, REQUEST_CAMERA)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                when (requestCode) {
                    SELECT_FILE -> {
                        if (data != null) {
                            imagePath = PathUtil.getPath(context!!, data.data!!)
                            if (!imagePath.isNullOrBlank()) setImage(imagePath!!, true)
                            else showError(getString(R.string.error_upload_media))
                        }
                    }
                    REQUEST_CAMERA -> {
                        imagePath = PathUtil.getPath(context!!, mCameraImageUri!!)
                        if (!imagePath.isNullOrBlank()) setImage(imagePath!!, true)
                        else showError(getString(R.string.error_upload_media))
                    }
                }
            }
            Activity.RESULT_CANCELED -> {
                //                showError(resources.getString(R.string.user_cancel))
            }
        }
    }

    //Getting Request Permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (marshMellowHelper != null) {
            marshMellowHelper!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    //set image using URI and String
    private fun setImage(mImage: Any, storePicture: Boolean) {
        if (storePicture) mSettings.mLocalUri.value = imagePath
        else mSettings.mServerUri.value = mUser?.picture!!

        profilePicture?.picture?.setProfilePicture(mImage)
    }

    @Subscribe(threadMode = ThreadMode.MAIN) fun onImageBottomSheet(event: ImageBottomEvent) {
        if (event.mImageClick) {
            cameraIntent()
        } else if (event.mGalleryClick) {
            galleryIntent()
        }
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    private fun goToLogin(action: ProfileEditViewModel.Action) {

        clearCachedUser()

        showError(getString(if (action == ProfileEditViewModel.Action.LOGOUT) R.string.edit_profile_logout
        else R.string.edit_profile_delete))

        // TODO: 2019-08-11    if login with G/FB proceed with Auth0.logout()

        Handler().postDelayed({
            activity?.let {
                if (it.isValid()) {
                    AuthActivity.openAfterLogoutOrDelete(activity!!)
                }
            }
        }, 1000)

    }


}


val countryList = arrayOf("Select...", "United States", "Canada", "Afghanistan", "Albania", "Algeria", "American Samoa", "Andorra", "Angola", "Anguilla", "Antarctica", "Antigua and/or Barbuda", "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bermuda", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Bouvet Island", "Brazil", "British Indian Ocean Territory", "Brunei Darussalam", "Bulgaria", "Burkina Faso", "Burundi", "Cambodia", "Cameroon", "Cape Verde", "Cayman Islands", "Central African Republic", "Chad", "Chile", "China", "Christmas Island", "Cocos (Keeling) Islands", "Colombia", "Comoros", "Congo", "Cook Islands", "Costa Rica", "Croatia (Hrvatska)", "Cuba", "Cyprus", "Czech Republic", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "East Timor", "Ecudaor", "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia", "Falkland Islands (Malvinas)", "Faroe Islands", "Fiji", "Finland", "France", "France, Metropolitan", "French Guiana", "French Polynesia", "French Southern Territories", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Gibraltar", "Greece", "Greenland", "Grenada", "Guadeloupe", "Guam", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Heard and Mc Donald Islands", "Honduras", "Hong Kong", "Hungary", "Iceland", "India", "Indonesia", "Iran (Islamic Republic of)", "Iraq", "Ireland", "Israel", "Italy", "Ivory Coast", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Korea, Democratic People's Republic of", "Korea, Republic of", "Kosovo", "Kuwait", "Kyrgyzstan", "Lao People's Democratic Republic", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libyan Arab Jamahiriya", "Liechtenstein", "Lithuania", "Luxembourg", "Macau", "Macedonia", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands", "Martinique", "Mauritania", "Mauritius", "Mayotte", "Mexico", "Micronesia, Federated States of", "Moldova, Republic of", "Monaco", "Mongolia", "Montserrat", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "Netherlands Antilles", "New Caledonia", "New Zealand", "Nicaragua", "Niger", "Nigeria", "Niue", "Norfork Island", "Northern Mariana Islands", "Norway", "Oman", "Pakistan", "Palau", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Pitcairn", "Poland", "Portugal", "Puerto Rico", "Qatar", "Reunion", "Romania", "Russian Federation", "Rwanda", "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent and the Grenadines", "Samoa", "San Marino", "Sao Tome and Principe", "Saudi Arabia", "Senegal", "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Georgia South Sandwich Islands", "South Sudan", "Spain", "Sri Lanka", "St. Helena", "St. Pierre and Miquelon", "Sudan", "Suriname", "Svalbarn and Jan Mayen Islands", "Swaziland", "Sweden", "Switzerland", "Syrian Arab Republic", "Taiwan", "Tajikistan", "Tanzania, United Republic of", "Thailand", "Togo", "Tokelau", "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Turks and Caicos Islands", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States minor outlying islands", "Uruguay", "Uzbekistan", "Vanuatu", "Vatican City State", "Venezuela", "Vietnam", "Virigan Islands (British)", "Virgin Islands (U.S.)", "Wallis and Futuna Islands", "Western Sahara", "Yemen", "Yugoslavia", "Zaire", "Zambia", "Zimbabwe")

