package rs.highlande.app.tatatu.feature.search.view.viewModel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.http.ACCOUNT_ID
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DO_SEARCH_USERS
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.feature.search.repository.SearchRepository
import rs.highlande.app.tatatu.model.TTUVideo
import rs.highlande.app.tatatu.model.User
import java.util.concurrent.TimeUnit

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class SearchViewModel(private val searchRepository: SearchRepository): BaseViewModel() {

    private var offset = 0

    var currentQuery: String? = null
    private var ignoreResult: Boolean = false

    val mediaSearchResultsLiveData = MutableLiveData<List<TTUVideo>?>()
    val usersSearchResultsLiveData = MutableLiveData<List<User>?>()
    val noResultsLiveData = MediatorLiveData<Boolean>()

    init {
        noResultsLiveData.addSource(mediaSearchResultsLiveData) {
            it?.let {
                if (it.isEmpty() && usersSearchResultsLiveData.value != null) {
                    noResultsLiveData.postValue(handleEmptyResults(usersSearchResultsLiveData.value!!, it))
                }
            }
        }
        noResultsLiveData.addSource(usersSearchResultsLiveData) {
            it?.let {
                if (it.isEmpty() && mediaSearchResultsLiveData.value != null) {
                    noResultsLiveData.postValue(handleEmptyResults(it, mediaSearchResultsLiveData.value!!))
                }
            }
        }
    }

    fun performSearch(query: String) {
        offset = 0
        currentQuery = query
        ignoreResult = false
        noResultsLiveData.value = false
        doSearch(query)
    }

    private fun doSearch(query: String) {
        clearDisposables()

        addDisposables(listOf(
            searchRepository.doMediaSearch(ACCOUNT_ID, query, offset, MEDIA_LIMIT)
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe(
                    { result ->
                        if (!ignoreResult) {
                            mediaSearchResultsLiveData.postValue(
                                result?.let {
                                    if (it.videos.isEmpty()) {
                                        mutableListOf()
                                    } else {
                                        it.videos
                                    }
                                }?: run {
                                    mutableListOf<TTUVideo>()
                                })
                        }
                    },
                    {
                        it.printStackTrace()
                        mediaSearchResultsLiveData.postValue(mutableListOf())
                    }
                ),
            searchRepository.doUserSearch(this, query)
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe(
                    {},
                    { it.printStackTrace() }
                )
        ))
    }

    private fun handleEmptyResults(usersList: List<User>, mediaList: List<TTUVideo>) =
        usersList.isEmpty() && mediaList.isEmpty()

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_DO_SEARCH_USERS -> {
                addDisposable(
                    Observable.just(response)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (!ignoreResult) {
                                    val tmpList = mutableListOf<User>()
                                    for (i in 0 until it!!.length()) {
                                        User.get(it.optJSONObject(i))?.let { data ->
                                            tmpList.add(data)
                                        }
                                    }
                                    usersSearchResultsLiveData.postValue(tmpList)
                                }
                            },
                            { thr ->
                                thr.printStackTrace()
                                usersSearchResultsLiveData.postValue(emptyList())
                            }
                        )
                )
            }
        }
    }

    override fun handleErrorResponse(
        idOp: String,
        callCode: Int,
        errorCode: Int,
        description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when (callCode) {
            SERVER_OP_DO_SEARCH_USERS -> {
                usersSearchResultsLiveData.postValue(mutableListOf())
            }
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> = arrayOf(mediaSearchResultsLiveData)

    fun cancelSearch() {
        ignoreResult = true
    }

    companion object {
        const val MEDIA_LIMIT = 100
    }

}