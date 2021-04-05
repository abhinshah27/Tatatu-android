package rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel

import android.app.Application
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DO_SEND_INVITATION
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_INVITATION_LINK
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.inviteFriends.repository.InviteFriendsRepository
import rs.highlande.app.tatatu.model.InvitationLinkResponse
import rs.highlande.app.tatatu.model.SendInvitationResponse

/**
 * Created by Abhin.
 */
class InviteFriendsViewModel(application: Application) : BaseAndroidViewModel(application) {
    var isProgress = mutableLiveData(false)
    var isEmailProgress = mutableLiveData(false)
    var mInvitationLink = mutableLiveData(ArrayList<InvitationLinkResponse>())
    var mSendInvitation = mutableLiveData(ArrayList<SendInvitationResponse>())
    var mErrorInvitation = mutableLiveData("")
    private val repo: InviteFriendsRepository by inject()

    fun getInvitationLink() {
        isProgress.postValue(true)
        addDisposable(repo.getInvitationLink(this, Bundle()).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, "GET INVITE LINK: SUCCESS")
            else LogUtils.e(logTag, "GET INVITE LINK: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }


    fun getSendInvitation(mFullName: String = "", mEmail: String = "", mUserFullName: String = "") {
        isEmailProgress.postValue(true)

        val mBundle = Bundle()
        mBundle.apply {
            putString("fullnames", mUserFullName)
            putString("email", mEmail)
            putString("fullnamer", mFullName)
            if (!mInvitationLink.value.isNullOrEmpty()) putString("invitationlink", mInvitationLink.value!![0].invitationLink)
        }
        //        printBundle(mBundle)


        Handler().postDelayed({
        addDisposable(repo.doSendInvitation(this, mBundle).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, " call: SUCCESS")
            else LogUtils.e(logTag, "call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))

        }, 5000)
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        LogUtils.d(CommonTAG, "Invite Friends response->${response.toString()}")
        when (callCode) {
            SERVER_OP_GET_INVITATION_LINK -> {
                isProgress.postValue(false)
                mInvitationLink.postValue(getInvitationLinkArrayList(response.toString()))
            }
            SERVER_OP_DO_SEND_INVITATION -> {
                isEmailProgress.postValue(false)
                mSendInvitation.postValue(getSendInvitationArrayList(response.toString()))
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when (callCode) {
            SERVER_OP_GET_INVITATION_LINK -> {
                isProgress.postValue(false)
                mErrorInvitation .postValue(description)
                LogUtils.d(CommonTAG, "invitationLink->${description.toString()}")
            }
            SERVER_OP_DO_SEND_INVITATION -> {
                isEmailProgress.postValue(false)
                mErrorInvitation.postValue(
                    if (errorCode == 3024) getApplication<TTUApp>().getString(R.string.error_invite_email_already)
                    else description
                )
                LogUtils.d(CommonTAG, "Do Send Invitation->${description.toString()}")
            }
        }
    }

    //print the bundle for testing
    private fun printBundle(bundle: Bundle) {
        SocketRequest(bundle, callCode = SERVER_OP_DO_SEND_INVITATION, logTag = "SERVER_OP_DO_SEND_INVITATION", caller = object : OnServerMessageReceivedListener {
            override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
            }

            override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {

            }
        })
    }

    private fun getInvitationLinkArrayList(json: String): ArrayList<InvitationLinkResponse> {
        val type = object : TypeToken<ArrayList<InvitationLinkResponse>>() {}.type
        var mInvitationLinkResponseList: ArrayList<InvitationLinkResponse> = ArrayList()
        mInvitationLinkResponseList = Gson().fromJson(json, type)
        return mInvitationLinkResponseList
    }

    fun getSendInvitationArrayList(json: String): ArrayList<SendInvitationResponse> {
        val type = object : TypeToken<ArrayList<SendInvitationResponse>>() {}.type
        var mSendInvitationResponseList: ArrayList<SendInvitationResponse> = ArrayList()
        mSendInvitationResponseList = Gson().fromJson(json, type)
        return mSendInvitationResponseList
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }


    companion object {

        val logTag = InviteFriendsViewModel::class.java.simpleName

    }


}