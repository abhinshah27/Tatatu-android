package rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel

import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_USERS_INVITED
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.account.common.BaseAccountViewModel
import rs.highlande.app.tatatu.feature.account.followFriendList.repository.FriendRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.model.InviteFriendsResponse
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.Users
import java.util.concurrent.TimeUnit

class InvitesViewModel(usersRepository: UsersRepository, relationshipManager: RelationshipManager) : BaseAccountViewModel(usersRepository, relationshipManager) {

    val inivteResentLiveData = MutableLiveData<Pair<Boolean, User>>()
    val mInviteFriends = mutableLiveData(ArrayList<InviteFriendsResponse>())
    val mInviteFriendsAllData = mutableLiveData(ArrayList<Users>())
    var isProgress = mutableLiveData(false)
    var mInviteFriendsPageNumber = 1
    var mPendingCount = -1
    var mCachedOriginalList: List<Users>? = null
    var mCachedPageNumber = 1
    var isScrolling = false
    private val repo: FriendRepository by inject()

    override fun performSearch(query: String) {
        super.performSearch(query)
        getUser()?.let {
            addDisposable(usersRepository.searchFollowingFollowers(this, query, it).subscribeOn(Schedulers.computation()).debounce(300, TimeUnit.MILLISECONDS).observeOn(Schedulers.computation()).subscribe({}, { thr -> thr.printStackTrace() }))
        }
    }

    fun getUsersInvited(type: String? = "", searchText: String = "", pageNumber: Int = mInviteFriendsPageNumber, pageSize: Int = PAGINATION_SIZE) {
        if (searchText.isEmpty() && !isScrolling) isProgress.postValue(true)

        val mBundle = Bundle()
        mBundle.apply {
            putString("type", type)
            putString("searchText", searchText)
            putInt("pageNumber", pageNumber)
            putInt("pageSize", pageSize)
        }
        addDisposable(repo.getUsersInvited(this, mBundle).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, " call: SUCCESS")
            else LogUtils.e(logTag, "call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }

    fun getPerformSearch(type: String? = "", searchText: String = "", pageNumber: Int = 1, pageSize: Int = PAGINATION_SIZE) {
        val mBundle = Bundle()
        mBundle.apply {
            putString("type", type)
            putString("searchText", searchText)
            putInt("pageNumber", pageNumber)
            putInt("pageSize", pageSize)
        }
        addDisposable(repo.getUsersInvited(this, mBundle).debounce(300, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, " call: SUCCESS")
            else LogUtils.e(logTag, "call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        isProgress.postValue(false)
        when (callCode) {
            SERVER_OP_GET_USERS_INVITED -> {
                LogUtils.d(CommonTAG, "response-->${response.toString()}")
                val arrayList = getInviteFriendsArrayList(response.toString())
                mInviteFriends.postValue(arrayList)
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        isProgress.postValue(false)
        when (callCode) {
            SERVER_OP_GET_USERS_INVITED -> {
                LogUtils.d(CommonTAG, "errorCode-->$description")
            }
        }
    }

    private fun getInviteFriendsArrayList(json: String): ArrayList<InviteFriendsResponse> {
        val type = object : TypeToken<ArrayList<InviteFriendsResponse>>() {}.type
        var mInviteFriendsResponseList: ArrayList<InviteFriendsResponse> = ArrayList()
        mInviteFriendsResponseList = Gson().fromJson(json, type)
        return mInviteFriendsResponseList
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf<MutableLiveData<*>>(inivteResentLiveData).plus(super.getAllObservedData())
    }

    enum class InviteType(val value: String) {
        @SerializedName("registered")
        REGISTERED("registered"),
        @SerializedName("twitterinvited")
        TWITTER_INVITED("twitterinvited"),
        @SerializedName("emailinvited")
        EMAIL_INVITED("emailinvited")
    }

    companion object {
        val logTag = InvitesViewModel::class.java.simpleName
    }
}