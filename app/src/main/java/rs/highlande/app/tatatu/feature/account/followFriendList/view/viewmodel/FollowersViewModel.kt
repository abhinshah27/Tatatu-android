package rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_FOLLOWERS
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_TOTAL_FOLLOW_REQUESTS
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.util.FOLLOW_REQUESTS_INCOMING
import rs.highlande.app.tatatu.core.util.SEARCH_FOLLOWERS
import rs.highlande.app.tatatu.feature.account.common.BaseAccountViewModel
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.User
import java.util.concurrent.TimeUnit

class FollowersViewModel(usersRepository: UsersRepository, relationshipManager: RelationshipManager): BaseAccountViewModel(usersRepository, relationshipManager) {

    val followCountsLiveData = MutableLiveData<Boolean>()
    val followersListLiveData = MutableLiveData<List<User>>()

    var sentRequestsCount: Int = 0

    fun fetchFollowCounts() {
        addDisposable(
            usersRepository.fetchUserTotalFollowRequests(this, FOLLOW_REQUESTS_INCOMING)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun fetchFollowers(resetPageCount: Boolean = false) {
        profileUserLiveData.value?.let {
            if (resetPageCount) {
                currentPage = 0
            }
            addDisposable(Observable.just(usersRepository.fetchUserFollowers(this, it, currentPage))
                .debounce(300, TimeUnit.MILLISECONDS)
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
        profileUserLiveData.value?.let {
            addDisposable(
                usersRepository.searchFollowingFollowers(this, query, it, SEARCH_FOLLOWERS)
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
            SERVER_OP_GET_FOLLOWERS -> {
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
                            followersListLiveData.postValue(tmpList)
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
                            sentRequestsCount = if (it!!.length() > 0 && !it.isNull(0)) {
                                it.getJSONObject(0).getInt("total")
                            } else {
                                0
                            }
                            followCountsLiveData.postValue(sentRequestsCount > 0)
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
            SERVER_OP_GET_FOLLOWERS -> {
                followersListLiveData.postValue(emptyList())
            }
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf<MutableLiveData<*>>(followCountsLiveData, relationshipChangeLiveData, followersListLiveData).plus(super.getAllObservedData())
    }

}