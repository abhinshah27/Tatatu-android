package rs.highlande.app.tatatu.feature.suggested.view

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_SUGGESTED_V2
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_MAKE_NEW_FOLLOWER_REQUEST
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.commonView.UnFollowViewModel
import rs.highlande.app.tatatu.feature.suggested.api.LIMIT_SUGGESTED_AFTER_FOLLOW
import rs.highlande.app.tatatu.feature.suggested.repository.SuggestedRepository
import rs.highlande.app.tatatu.model.SuggestedPerson

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-03.
 */
class SuggestedViewModel(
    val relationshipManager: RelationshipManager): BaseViewModel(), UnFollowViewModel {

    private val repo: SuggestedRepository by inject()

    val suggestedConnected = MutableLiveData<Pair<Boolean, Int>>()
    val suggestedReceived = MutableLiveData<Pair<Boolean, Int>>()
    val suggested = MutableLiveData<MutableList<SuggestedPerson>>()
    val relationshipChanged = MutableLiveData<Boolean>()
    val loadMoreCanFetch = MutableLiveData<Boolean>()

    private var currentSkip: Int = 0
    private var fetchingAfterFollow = false


    fun getSuggested(limit: Int? = null) {
        addDisposable(
            repo.getSuggested(this, currentSkip, limit)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        LogUtils.d(logTag, "getSuggested request status: $it")
                        suggestedConnected.postValue((it == RequestStatus.SENT) to currentSkip)
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun removeSuggested(item: SuggestedPerson) {
        repo.removeSuggested(item)
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return mutableListOf<MutableLiveData<*>>().apply {
            add(suggested)
            add(suggestedConnected)
            add(suggestedReceived)
            add(relationshipChanged)
            add(loadMoreCanFetch)
        }.toTypedArray()
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        when(callCode) {
            SERVER_OP_GET_SUGGESTED_V2 -> handleSuggestedResult(response!!)
            SERVER_OP_MAKE_NEW_FOLLOWER_REQUEST -> {
                handleNewFollowerRequestResult(response!!)
                fetchMoreSuggested()
            }
        }

    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        if (callCode == SERVER_OP_GET_SUGGESTED_V2)
            LogUtils.e(logTag, "GET SUGGESTED ERROR")
    }


    private fun handleSuggestedResult(response: JSONArray) {
        addDisposable(
            Observable.just(response).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {

                        val tmpList = mutableListOf<SuggestedPerson>()
                        for (i in 0 until it.length()) {
                            SuggestedPerson.get(it.optJSONObject(i))?.let { data ->
                                tmpList.add(data)
                            }
                        }

                        loadMoreCanFetch.postValue(
                            if (fetchingAfterFollow) tmpList.size == LIMIT_SUGGESTED_AFTER_FOLLOW
                            else tmpList.size == PAGINATION_SIZE
                        )

                        fetchingAfterFollow = false

                        currentSkip += tmpList.size

                        suggested.postValue(tmpList)

                        suggestedReceived.postValue(true to currentSkip)
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    private fun handleNewFollowerRequestResult(response: JSONArray) {
        relationshipChanged.postValue(response.length() > 0)
    }

    override fun followSuggested(userID: String) {
        addDisposable(
            Observable.just(relationshipManager.manageNewFollowerRequest(this, userID))
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    override fun fetchMoreSuggested() {
        fetchingAfterFollow = true
        getSuggested(LIMIT_SUGGESTED_AFTER_FOLLOW)
    }


    companion object {

        val logTag = SuggestedViewModel::class.java.simpleName

    }

}