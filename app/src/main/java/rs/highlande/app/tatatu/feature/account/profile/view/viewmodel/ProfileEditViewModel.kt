package rs.highlande.app.tatatu.feature.account.profile.view.viewmodel

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.http.ProgressRequestBody
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DELETE_PROFILE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_EDIT_PROFILE
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.account.profile.repository.ProfileRepository
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.authentication.repository.AuthRepository
import rs.highlande.app.tatatu.feature.commonRepository.*
import rs.highlande.app.tatatu.feature.createPost.view.getParentActivity
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.createPost.uploadImage.UploadMediaResponse
import java.util.concurrent.TimeUnit


/**
 * Created by Abhin.
 */
class ProfileEditViewModel(application: Application) : BaseAndroidViewModel(application), UploadingInterFace {

    enum class Action { DELETE_PROFILE, LOGOUT }
    enum class ProgressAction { UPLOAD, EDIT }

    val mEditPrivateInformation = mutableLiveData(false)
    val mDiscardChanges = mutableLiveData(false)

    val mLocalUri = mutableLiveData("")
    val mServerUri = mutableLiveData("")
    val mFullName = mutableLiveData("")
    val mUserName = mutableLiveData("")
    val mBio = mutableLiveData("")
    val mWebsite = mutableLiveData("")
    val mEmail = mutableLiveData("")
    val mPassword = mutableLiveData("")
    val mPhone = mutableLiveData("")
    val mDOB = mutableLiveData("")
    val mSelectedGender = mutableLiveData("")
    val mSelectedCountry = mutableLiveData("")
    val mDeleteProfile = mutableLiveData(false)
    val mLogout = mutableLiveData(false)
    val mEditProfile = mutableLiveData(false)
    val mErrorMessage = mutableLiveData("")

    val errorUserName = mutableLiveData("")
    val previousUserName = mutableLiveData("")
    var verifyUserName = mutableLiveData(View.GONE)
    private var userNameValid = true

    val mShowProgress: MutableLiveData<Triple<Boolean, ProgressAction?, Long>> = mutableLiveData(Triple(false, null, 0L))

    private val authManager: AuthManager by inject()
    private val repository: AuthRepository by inject()
    private val mProfileRepository: ProfileRepository by inject()
    private val mRepository: UsersRepository by inject()
    private var mUser: User? = null

    val mUserNameTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (!p0!!.toString().isBlank() && p0.length >= 3) {
                mUserName.value = p0.toString()
                performUserNameCheck(mUserName.value!!)
            } else {
                mUserName.value = ""
                verifyUserName.postValue(View.GONE) // Gone
            }
        }
    }

    fun performUserNameCheck(username: String) {
        addDisposable(repository.checkUserName(username).subscribeOn(Schedulers.io()).debounce(300, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe({
            LogUtils.e("username-->", username)
            if (username == previousUserName.value) {
                verifyUserName.value = View.GONE
                return@subscribe
            }
            if (it.second != null) {
                userNameValid = false
                errorUserName.value = username
                verifyUserName.postValue(View.GONE)
            } else {
                userNameValid = it.first
                //   this@ProfileEditViewModel.mUserName.value = username
                verifyUserName.postValue(View.VISIBLE)
            }

        }, {
            it.printStackTrace()
        }))
    }

    //Click Handling in ViewModel
    fun onClick(view: View) {
        val activity = view.getParentActivity()!!
        when (view.id) {

            // FIXME: 2019-08-30    change this: even if I toggle again the box I must be able to save the fields
            R.id.ll_edit_private_info -> {
                mEditPrivateInformation.value = !mEditPrivateInformation.value!!
            }
            R.id.btn_save_changes -> {
                checkValidation(activity)
            }
            R.id.btn_discard_changes -> {
                mDiscardChanges.value = !mDiscardChanges.value!!
            }
        }
    }

    //check the validation
    private fun checkValidation(activity: AppCompatActivity) {
        when {
            mFullName.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_full_name))
            mUserName.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_user_name))
            !userNameValid -> showError(activity, activity.resources.getString(R.string.error_existing_username, errorUserName.value))
            //            mBio.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_bio))
            //            mWebsite.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_personal_website))
            //            !URLUtil.isValidUrl(mWebsite.value!!) -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_personal_website))
            //            mEditPrivateInformation.value!! && mEmail.value!!.isNotEmpty() && !isEmailValid(mEmail.value!!) -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_email))
//                        mEditPrivateInformation.value!! && mPassword.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_password))
                        mEditPrivateInformation.value!! && mPassword.value!!.isNotEmpty() && !isPasswordValid(mPassword.value!!) -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_password))
            //            mEditPrivateInformation.value!! && mPhone.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_phone_no))
            //            mEditPrivateInformation.value!! && !isPhoneValid(mPhone.value!!) -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_phone_no))
            //            mEditPrivateInformation.value!! && !mSelectedCountry.value!!.equals("Select...", true) && mSelectedCountry.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_country))
            //            mEditPrivateInformation.value!! && !mSelectedGender.value!!.equals("Gender...", true) && mSelectedGender.value!!.isEmpty() -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_gender))
            mEditPrivateInformation.value!! && mDOB.value!!.isNotEmpty() && !isDateOfBirthValid(mDOB.value!!) -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_dob))
            mEditPrivateInformation.value!! && mDOB.value!!.isNotEmpty() && !isUser18Older(mDOB.value!!) -> showError(activity, activity.resources.getString(R.string.setting_msg_error_valid_dob_age))
            !mLocalUri.value.isNullOrEmpty() -> {
                mShowProgress.postValue(Triple(true, ProfileEditViewModel.ProgressAction.UPLOAD, 0L))
                uploadMedia(activity)
            }
            else -> {
                mShowProgress.postValue(Triple(true, ProgressAction.EDIT, 0L))
                updateEditProfile(mServerUri.value)
            }
        }
    }

    // uploading image as avatar
    private fun uploadMedia(activity: AppCompatActivity) {
        UploadingMedia(this, activity, mLocalUri.value, null, false, "0").uploadingData(object : ProgressRequestBody.ProgressListener {
            override fun transferred(num: Long) {
                mShowProgress.postValue(Triple(true, ProgressAction.UPLOAD, num))
            }

            override fun exceptionCaught(e: Exception) {
                mErrorMessage.postValue(getApplication<TTUApp>().getString(R.string.error_upload_media))
                mShowProgress.postValue(Triple(false, null, 0))
            }
        }, object : CompressInterface {
            override fun getCompressionProgress(progress: Long) {
                mShowProgress.postValue(Triple(true, ProgressAction.UPLOAD, progress))
            }

            override fun compressVideoPath(path: String?, mContext: Context) {}
        })
    }


    //call edit post data
    private fun updateEditProfile(picture: String? = "") {
        val bundle = Bundle()
        bundle.apply {
            putString(BUNDLE_KEY_PICTURE, picture)
            putString(BUNDLE_KEY_NAME, mFullName.value)
            putString(BUNDLE_KEY_USERNAME, mUserName.value)
            putString(BUNDLE_KEY_BIO, mBio.value)
            putString(BUNDLE_KEY_WEBSITE, mWebsite.value)
            putString(BUNDLE_KEY_EMAIL, mEmail.value)
            putString(BUNDLE_KEY_PASSWORD, mPassword.value)
            putString(BUNDLE_KEY_PHONE_NO, mPhone.value)
            putString(BUNDLE_KEY_COUNTRY, mSelectedCountry.value)
            putString(BUNDLE_KEY_DATE_OF_BIRTH, convertEditProfileDateToDB(mDOB.value))
            putString(BUNDLE_KEY_GENDER, mSelectedGender.value)
        }

        //                printBundle(bundle)
        addDisposable(mProfileRepository.getEditProfile(this, bundle).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) {
                LogUtils.d(logTag, "Edit Profile call: SUCCESS")
            } else LogUtils.e(logTag, "Edit Profile call: FAILED with $it")
        }, { thr -> thr.printStackTrace() }))
    }


    //call edit post data
    fun getDeleteProfile() {
        val bundle = Bundle()
        //        printBundle(bundle)
        addDisposable(mProfileRepository.getDeleteProfile(this, bundle).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, "Delete Profile call: SUCCESS")
            else LogUtils.e(logTag, "Delete Profile call: FAILED with $it")
        }, { thr -> thr.printStackTrace() }))
    }

    //print the bundle for testing
    private fun printBundle(bundle: Bundle) {
        SocketRequest(bundle, callCode = SERVER_OP_DELETE_PROFILE, logTag = "Edit Profile call", caller = object : OnServerMessageReceivedListener {
            override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {

            }

            override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {

            }
        })
    }

    fun redirectResponseSuccessful(action: Action) {
        authManager.logout()
        if (action == Action.DELETE_PROFILE) mDeleteProfile.postValue(true)
        else mLogout.postValue(true)
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_DELETE_PROFILE -> {
                redirectResponseSuccessful(Action.DELETE_PROFILE)
            }
            SERVER_OP_EDIT_PROFILE -> {
                mEditProfile.postValue(true)
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when (callCode) {
            SERVER_OP_DELETE_PROFILE -> {
                mDeleteProfile.postValue(false)
            }

            SERVER_OP_EDIT_PROFILE -> {
                mErrorMessage.postValue(description)
            }
        }
    }

    override fun getSuccessResponse(response: UploadMediaResponse?) {
        if (!response!!.mData!!.preview.isNullOrEmpty()) {
            mShowProgress.postValue(Triple(true, ProgressAction.EDIT, 0L))
            updateEditProfile(response.mData!!.preview)
        }
    }

    override fun getErrorResponse(error: String?) {
        mErrorMessage.postValue(error)
    }


    companion object {
        val logTag = ProfileEditViewModel::class.java.simpleName
    }

}