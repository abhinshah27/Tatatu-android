package rs.highlande.app.tatatu.feature.account.followFriendList.view.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_FOLLOWING
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.util.SEARCH_FOLLOWING
import rs.highlande.app.tatatu.feature.account.common.BaseAccountViewModel
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.User
import java.util.concurrent.TimeUnit

@SuppressLint("CheckResult")
class FollowingViewModel(usersRepository: UsersRepository, relationshipManager: RelationshipManager): BaseAccountViewModel(usersRepository, relationshipManager) {

    val followingListLiveData = MutableLiveData<List<User>>()

    fun fetchFollowingList(resetPageCount: Boolean = false) {
        profileUserLiveData.value?.let {
            if (resetPageCount) {
                currentPage = 0
            }
            addDisposable(
                usersRepository.fetchUserFollowing(this, it, currentPage)
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

    override fun performSearch(query: String) {
        super.performSearch(query)
        profileUserLiveData.value?.let {
            addDisposable(
                usersRepository.searchFollowingFollowers(this, query, it, action = SEARCH_FOLLOWING)
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
            SERVER_OP_GET_FOLLOWING -> {
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
                            followingListLiveData.postValue(tmpList)
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
            SERVER_OP_GET_FOLLOWING -> {
                followingListLiveData.postValue(emptyList())
            }
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf<MutableLiveData<*>>(followingListLiveData).plus(super.getAllObservedData())
    }


}