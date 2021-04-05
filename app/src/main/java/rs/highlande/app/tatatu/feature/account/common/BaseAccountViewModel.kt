package rs.highlande.app.tatatu.feature.account.common

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.manager.RelationshipAction
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.User

abstract class BaseAccountViewModel(val usersRepository: UsersRepository, val relationshipManager: RelationshipManager,
                                    realm: Realm? = null): BaseViewModel() {

    companion object {
        val logTag = BaseAccountViewModel::class.java.simpleName
    }


    val userLiveData = MutableLiveData<User>()
    val searchResultLiveData = MutableLiveData<List<User>>()
    var profileUserLiveData = MutableLiveData<User?>()
    var profileDeeplinkLiveData = MutableLiveData<String>()
    var profileReportedLiveData = MutableLiveData<Boolean>()
    var profileBlockedLiveData = MutableLiveData<Boolean>()
    var profileErrorResponseLiveData = MutableLiveData<Boolean>()
    val actionRequestLiveData = MutableLiveData<RelationshipAction>()
    val relationshipChangeLiveData = MutableLiveData<Pair<Boolean, User>>()

    var currentPage = 0
    var itemCount = 0
    var replaceItemsOnNextUpdate: Boolean = false
    var ignoreSearch: Boolean = false
    var cachedFollower: User? = null

    var cachedOriginalList: List<User>? = null
    set(value) {
        LogUtils.d("cachedList", "setted")
        field = value
    }

    fun getUser() = userLiveData.value
    open fun setUser(user: User, post: Boolean) {
        if (post) userLiveData.postValue(user)
        else userLiveData.value = user
    }

    fun hasMoreItems(): Boolean {
        val nextPageSize = (currentPage * PAGINATION_SIZE)
        return if (nextPageSize > 0 ) {
            itemCount > (currentPage * PAGINATION_SIZE)
        } else {
            false
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(userLiveData, searchResultLiveData, profileUserLiveData, profileErrorResponseLiveData, profileBlockedLiveData,
            profileDeeplinkLiveData, profileReportedLiveData)
    }

    open fun performSearch(query: String) {
        ignoreSearch = false
    }

    fun onFollowerRelationshipActionClick(follower: User) {
        cachedFollower = follower
        actionRequestLiveData.value = relationshipManager.getRelationshipAction(follower.detailsInfo.relationship)
    }

    fun handleFollowerRelationChange() {
        cachedFollower?.let { follower ->
            addDisposable(
                Observable.just(relationshipManager.manageRelationship(follower, this))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {
                            relationshipChangeLiveData.postValue(Pair(true, it))
                        },
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_GET_USER -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                User.get(it!!.optJSONObject(0))?.let { result ->
                                    profileUserLiveData.postValue(result)
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DO_REPORT_USER -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    profileReportedLiveData.postValue(true)
                                }
                                LogUtils.d("user reported", "user reported")
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DO_SHARE_USER_PROFILE -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    profileDeeplinkLiveData.postValue(it.getJSONObject(0).getString("deepLink"))
                                }
                                LogUtils.d("profile shared", "profile shared")
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DO_ADD_REMOVE_BLACKLIST -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    profileBlockedLiveData.postValue(true)
                                }
                                LogUtils.d("profile blocked", "profile blocked")
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DO_SEARCH_FOLLOWING_FOLLOWERS -> {
                if (ignoreSearch) {
                    return
                }
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            val tmpList = mutableListOf<User>()
                            for (i in 0 until it!!.length()) {
                                User.get(it.optJSONObject(i))?.let { data ->
                                    tmpList.add(data)
                                }
                            }
                            searchResultLiveData.postValue(tmpList)
                        },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
            SERVER_OP_MAKE_NEW_FOLLOWER_REQUEST -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe({
                        cachedFollower?.let { follower ->
                            val requestID = it!!.getJSONObject(0).getString("requestID")
                            follower.detailsInfo.requestID = requestID
                        }
                    }, { thr -> thr.printStackTrace() }
                    )
                )
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        LogUtils.e(logTag, "CALL FAILED with error=$errorCode and description=$description")
        profileErrorResponseLiveData.postValue(true)
    }

    fun fetchUser(userId: String) {
        fetchUser(userId, arrayOf(CommonApi.UserInfo.GENERIC, CommonApi.UserInfo.DETAIL))
    }

    fun fetchUser(userId: String, userInfo: Array<CommonApi.UserInfo>) {
        addDisposable(
            Observable.just(
                usersRepository.fetchUser(
                    this,
                    userId,
                    userInfo
                ))
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun fetchShareUserProfileDeeplink() {
        profileUserLiveData.value?.let {
            addDisposable(
                Observable.just(usersRepository.fetchShareUserProfileDeeplink(this, it.uid))
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {},
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    fun reportUser() {
        profileUserLiveData.value?.let {
            addDisposable(
                Observable.just(usersRepository.reportUser(this, it.uid))
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {},
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    fun blockUser() {
        profileUserLiveData.value?.let {
            addDisposable(
                Observable.just(usersRepository.addRemoveBlacklist(this, it.uid, 1))
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {},
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    //TODO: 11/08 Where will this be called from?
    fun unBlockUser() {
        profileUserLiveData.value?.let {
            addDisposable(
                Observable.just(usersRepository.addRemoveBlacklist(this, it.uid, 0))
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {},
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    fun cancelSearch() {
        ignoreSearch = true
    }

}