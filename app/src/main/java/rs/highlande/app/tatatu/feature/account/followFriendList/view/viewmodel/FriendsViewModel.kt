package rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel

import android.os.Bundle
import android.os.Handler
import com.google.gson.Gson
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
import rs.highlande.app.tatatu.model.BalanceUser
import rs.highlande.app.tatatu.model.FriendsResponse
import rs.highlande.app.tatatu.model.FriendsUser
import rs.highlande.app.tatatu.model.MainUser
import java.util.concurrent.TimeUnit

class FriendsViewModel(usersRepository: UsersRepository, relationshipManager: RelationshipManager) : BaseAccountViewModel(usersRepository, relationshipManager) {
    var mInviteFriendsPageNumber = 1
    var mSignedCount = -1
    var isProgress = mutableLiveData(false)


    private val repo: FriendRepository by inject()
    val mFriends = mutableLiveData(ArrayList<FriendsResponse>())
    val mFriendsAllData = mutableLiveData(ArrayList<FriendsUser>())
    var mCachedOriginalList: List<FriendsUser>? = null

    fun getUsersInvited(type: String? = "", searchText: String = "", pageNumber: Int = mInviteFriendsPageNumber, pageSize: Int = PAGINATION_SIZE) {
        isProgress.postValue(true)
        val mBundle = Bundle()
        mBundle.apply {
            putString("type", type)
            putString("searchText", searchText)
            putInt("pageNumber", pageNumber)
            putInt("pageSize", pageSize)
        }
        addDisposable(repo.getUsersInvited(this, mBundle).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(InvitesViewModel.logTag, " call: SUCCESS")
            else LogUtils.e(InvitesViewModel.logTag, "call: FAILED with $it")
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
            if (it == RequestStatus.SENT) LogUtils.d(InvitesViewModel.logTag, " call: SUCCESS")
            else LogUtils.e(InvitesViewModel.logTag, "call: FAILED with $it")
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
                mFriends.postValue(getFriendsArrayList(response.toString()))
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

    private fun getFriendsArrayList(json: String): ArrayList<FriendsResponse> {
        val type = object : TypeToken<ArrayList<FriendsResponse>>() {}.type
        var mFriendsResponseList: ArrayList<FriendsResponse> = ArrayList()
        mFriendsResponseList = Gson().fromJson(json, type)
        return mFriendsResponseList
    }

    override fun performSearch(query: String) {
        super.performSearch(query)
        getUser()?.let {
            addDisposable(usersRepository.searchFollowingFollowers(this, query, it).subscribeOn(Schedulers.computation()).debounce(300, TimeUnit.MILLISECONDS).observeOn(Schedulers.computation()).subscribe({}, { thr -> thr.printStackTrace() }))
        }
    }


    fun dummyData() {
        isProgress.postValue(true)
        Handler().postDelayed(Runnable {

            //code here
            val mInviteFriendsResponse = FriendsResponse()

            val mArrayList = ArrayList<FriendsUser>()
            if (currentPage == 2) {
                for (i in 1..10) {
                    val mMainUserInfo= MainUser("normal","855bfc03-a02c-4062-b065-08abd23ac60d","5d5ce14ea336f840d2018607","Stefano galizia $i","mec20","https://uploadedprofilefiles-stage.s3.us-east-2.amazonaws.com/EU/43ac8e2f-4bab-419b-80d4-98419e403670.jpeg?X-Amz-Expires=259200&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAJSSNR7K36YOV3PRA/20190821/us-east-2/s3/aws4_request&X-Amz-Date=20190821T184100Z&X-Amz-SignedHeaders=host&X-Amz-Signature=39d8261836b46aad7e3ba1d85ce598d2dfecfe5f35586ba77b52fc030c1a6e38",false,false)
                    val mBalanceUserInfo=BalanceUser(i+.0,0)
                    val mFriendsUser=FriendsUser(mMainUserInfo,mBalanceUserInfo)
                    mArrayList.add(mFriendsUser)
                }
            } else {
                mArrayList.clear()
                for (i in 1..20) {
                    val mMainUserInfo= MainUser("normal","855bfc03-a02c-4062-b065-08abd23ac60d","5d5ce14ea336f840d2018607","Stefano galizia $i","mec20","https://uploadedprofilefiles-stage.s3.us-east-2.amazonaws.com/EU/43ac8e2f-4bab-419b-80d4-98419e403670.jpeg?X-Amz-Expires=259200&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAJSSNR7K36YOV3PRA/20190821/us-east-2/s3/aws4_request&X-Amz-Date=20190821T184100Z&X-Amz-SignedHeaders=host&X-Amz-Signature=39d8261836b46aad7e3ba1d85ce598d2dfecfe5f35586ba77b52fc030c1a6e38",false,false)
                    val mBalanceUserInfo=BalanceUser(i+.0,0)
                    val mFriendsUser=FriendsUser(mMainUserInfo,mBalanceUserInfo)
                    mArrayList.add(mFriendsUser)
                }
            }

            mInviteFriendsResponse.user = mArrayList
            mInviteFriendsResponse.pendingCount = 100
            mInviteFriendsResponse.signedCount = 100
            itemCount = mArrayList.size
            val mInviteFriendsArrayList = ArrayList<FriendsResponse>()
            mInviteFriendsArrayList.clear()
            mInviteFriendsArrayList.add(mInviteFriendsResponse)

            if (currentPage >= 5) {
                val mInviteFriendsResponse2 = FriendsResponse()
                mInviteFriendsResponse.user = arrayListOf()
                mInviteFriendsResponse.pendingCount = 100
                mInviteFriendsResponse.signedCount = 100
                val mInviteFriendsArrayList2 = ArrayList<FriendsResponse>()
                mInviteFriendsArrayList2.clear()
                mInviteFriendsArrayList2.add(mInviteFriendsResponse2)
                mFriends.value = mInviteFriendsArrayList2
            } else {
                mFriends.value = mInviteFriendsArrayList
            }
            isProgress.postValue(false)
        }, 1000)
    }
}