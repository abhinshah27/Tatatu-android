package rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_FOLLOW_REQUESTS
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_TOTAL_FOLLOW_REQUESTS
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_MANAGE_FOLLOWER_REQUEST
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.util.FOLLOW_REQUESTS_INCOMING
import rs.highlande.app.tatatu.feature.account.common.BaseAccountViewModel
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.User
import java.util.concurrent.TimeUnit

class FollowRequestsViewModel(usersRepository: UsersRepository, relationshipManager: RelationshipManager): BaseAccountViewModel(usersRepository, relationshipManager) {

    val followRequestsListLiveData = MutableLiveData<List<User>>()
    val followRequestsConfirmedLiveData = MutableLiveData<Pair<Boolean, User>>()
    val followRequestsRemovedLiveData = MutableLiveData<Pair<Boolean, User>>()

    fun fetchFollowCounts() {
        addDisposable(
            usersRepository.fetchUserTotalFollowRequests(this, FOLLOW_REQUESTS_INCOMING)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun fetchFollowRequests(resetPageCount: Boolean = false) {
        if (resetPageCount) {
            currentPage = 0
        }
        addDisposable(
            Observable.just(usersRepository.fetchUserFollowRequests(this, FOLLOW_REQUESTS_INCOMING, currentPage))
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )

    }

    fun handleRemoveFollowRequest(user: User) {
        cachedFollower = user
        user.detailsInfo.requestID?.let {
            addDisposable(Observable.just(relationshipManager.manageDeclineRequest(this, it))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
            )
        }
    }

    fun handleConfirmFollowRequest(user: User) {
        cachedFollower = user
        user.detailsInfo.requestID?.let {
            addDisposable(
                Observable.just(relationshipManager.manageAuthorizeRequest(this, it))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {},
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    override fun performSearch(query: String) {
        super.performSearch(query)
        getUser()?.let {
            addDisposable(
                usersRepository.searchFollowingFollowers(this, query, it)
                    .subscribeOn(Schedulers.computation())
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {},
                        { thr -> thr.printStackTrace() }
                    )
            )
        }
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_GET_FOLLOW_REQUESTS -> {
                addDisposable(Observable.just(response)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        {
                            val tmpList = mutableListOf<User>()
                            for (i in 0 until it!!.length()) {
                                User.get(it.optJSONObject(i))?.let { data ->
                                    tmpList.add(data)
                                }
                            }
                            followRequestsListLiveData.postValue(tmpList)
                        },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
            SERVER_OP_MANAGE_FOLLOWER_REQUEST -> {
                addDisposable(
                    Observable.just(response)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                cachedFollower?.let {follower ->
                                    if (it!!.length() > 0) {
                                        followRequestsRemovedLiveData.postValue(Pair(true, follower))
                                    } else {
                                        followRequestsRemovedLiveData.postValue(Pair(false, follower))
                                    }
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_GET_TOTAL_FOLLOW_REQUESTS -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            itemCount = if (it!!.length() > 0 && !it.isNull(0)) {
                                it.getJSONObject(0).getInt("total")
                            } else {
                                0
                            }
                            fetchFollowRequests(true)
                        },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when (callCode) {
            SERVER_OP_GET_FOLLOW_REQUESTS -> {
                followRequestsListLiveData.postValue(emptyList())
            }
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf<MutableLiveData<*>>(followRequestsConfirmedLiveData, followRequestsRemovedLiveData, followRequestsListLiveData).plus(super.getAllObservedData())
    }


}