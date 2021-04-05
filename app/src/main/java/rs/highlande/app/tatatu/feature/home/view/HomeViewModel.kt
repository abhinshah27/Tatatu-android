package rs.highlande.app.tatatu.feature.home.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonView.UnFollowViewModel
import rs.highlande.app.tatatu.feature.home.repository.HomeRepository
import rs.highlande.app.tatatu.feature.notification.repository.NotificationRepository
import rs.highlande.app.tatatu.model.*
import java.util.concurrent.TimeUnit

/**
 * Class holding the implementation for [HomePageFragment]'s [ViewModel].
 * @author mbaldrighi on 2019-06-27.
 */
class HomeViewModel(val relationshipManager: RelationshipManager,
                    val usersRepository: UsersRepository,
                    val notificationRepository: NotificationRepository,
                    val chatRepository: ChatRepository) : BaseViewModel(), UnFollowViewModel {

    private val homeRepo: HomeRepository by inject()

    val getHomeConnected = MutableLiveData<Boolean>()
    val getHomeReceived = MutableLiveData<Boolean>()

    val observableLists = mutableMapOf<HomeNavigationData, MutableLiveData<MutableList<out HomeDataObject>>>()

    val suggestedConnected = MutableLiveData<Boolean>()
    val suggestedReceived = MutableLiveData<Boolean>()
    val postConnected = MutableLiveData<Boolean>()
    val postsReceived = MutableLiveData<Boolean>()
    val relationshipChanged = MutableLiveData<Boolean>()
    val chatBadgeLiveData = MutableLiveData<Pair<Boolean, Int>>()
    val showSeeAll = MutableLiveData<Boolean>()


    private var suggestedSkip = 0
    private var prevSuggestedSkip: Int? = null

    var fetchingMoreSuggested = false


    fun getHomeTypes() {
        //Create the data for your UI, the users lists and maybe some additional data needed as well
        addDisposable(
            homeRepo.getHomeTypes(this)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        LogUtils.d("", "getHome request status: $it")
                        getHomeConnected.postValue(it == RequestStatus.SENT)
                    },
                    { it.printStackTrace() }
                )
        )
    }

    fun getHomeSingleType(obj: HomeNavigationData) {
        //Create the data for your UI, the users lists and maybe some additional data needed as well

        addDisposable(
            homeRepo.getSingleTypes(
                obj,
                this,
                if (obj.homeType == HomeUIType.SUGGESTED) prevSuggestedSkip ?: suggestedSkip
                else null
            )
                ?.observeOn(Schedulers.io())
                ?.subscribeOn(Schedulers.io())
                ?.subscribe (
                    {
                        val sent = it == RequestStatus.SENT

                        when (obj.homeType) {
                            HomeUIType.POST -> {
                                postConnected.postValue(sent)
                            }
                            HomeUIType.SUGGESTED -> {
                                suggestedConnected.postValue(sent)
                            }
                            else -> {}
                        }
                    },
                    { it.printStackTrace() }
                )
        )
    }

    fun getHomeSingleTypeStreaming(obj: HomeNavigationData, accountId: String) {
        //Create the data for your UI, the users lists and maybe some additional data needed as well
        addDisposable(
            homeRepo.getSingleTypesStreaming(obj, accountId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe (
                    { resultList ->
                        LogUtils.d(logTag, "testHOME: notifying STREAMING (${obj.sectionTitle})\tsize: ${resultList.size}")
                        observableLists[obj]?.value = resultList
                    },
                    {
                        it.printStackTrace()
                        mutableListOf<HomeDataObject>()
                    }
                )
        )
    }

    fun removeSuggested(dataID: HomeNavigationData, item: HomeDataObject) {
        homeRepo.removeSuggested(dataID, item)
    }


    /**
     * Retrieves [HomeNavigationData] object "linked" to the provided category.
     * Although working, this is not ideal: the check is performed on [String]s, and currently [StreamingCategory] objects
     * and [HomeNavigationData] are not related.
     *
     * @param category the category title.
     * @return The wanted [HomeNavigationData] object with the same [HomeNavigationData.sectionTitle].
     */
    fun getHomeDataFromSectionTitle(category: String): HomeNavigationData? {
        observableLists.keys.filter { it.sectionTitle == category }.apply {
            return if (isNotEmpty()) get(0) else null
        }
    }

    fun storeDeviceToken(deviceToken: String) {
        addDisposable(
            notificationRepository.storeDeviceToken(this, deviceToken)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { it.printStackTrace() }
                )
        )
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {

        // TODO: 2019-07-15    put every observed

        val list = mutableListOf<MutableLiveData<*>>()
        list.addAll(observableLists.values)
        list.add(getHomeConnected); list.add(getHomeReceived)
        list.add(suggestedConnected); list.add(suggestedReceived)
        list.add(postConnected); list.add(postsReceived)

        return list.toTypedArray()
    }


    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        when (callCode) {
            SERVER_OP_GET_HOME -> handleHomeResult(response!!)
            SERVER_OP_GET_SUGGESTED_V2 -> handleSuggestedResult(response!!)
            SERVER_OP_GET_TIMELINE -> handleTimelineResult(response!!)
            SERVER_OP_MAKE_NEW_FOLLOWER_REQUEST -> {
                handleNewFollowerRequestResult(response!!)
                fetchMoreSuggested()
                refreshTimeline()
            }
            SERVER_OP_STORE_TOKEN -> {
                LogUtils.d("fcmTOKEN", "token stored")
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        when (callCode) {
            SERVER_OP_GET_HOME -> {
                LogUtils.e("", "GET HOME ERROR")
                getHomeReceived.postValue(false)
            }
            SERVER_OP_GET_SUGGESTED_V2 -> {
                LogUtils.e("", "GET SUGGESTED ERROR")
            }
            SERVER_OP_GET_TIMELINE -> LogUtils.e("", "GET TIMELINE ERROR")
            SERVER_OP_GET_NOTIFICATIONS -> {

                LogUtils.d(CommonTAG, "Get Notification error Response--> ${Gson().toJson(description)}")
            }
        }
    }


    private fun handleHomeResult(response: JSONArray) {
        addDisposable(
            Observable.just(response).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())

                // INFO: 2019-07-31    delay added to solve Home bug [CAUSES STILL TO BE INVESTIGATED]
                .delay(500, TimeUnit.MILLISECONDS)

                .subscribe(
                    {

                        playlistIds.clear()

                        for (i in 0 until it.length()) {
                            HomeNavigationData.get(it.optJSONObject(i))?.let { data ->

                                if (!data.isValid()) return@let

                                if (!observableLists.containsKey(data))
                                    observableLists[data] = MutableLiveData()

                                when {
                                    data.isChannels() -> {
                                        // INFO: 2019-07-15    takes for granted that the only section without title is CHANNELS

                                        val categories = mutableListOf<StreamingCategory>()
                                        val navData = data.navigationData
                                        for (s in navData.split(","))
                                            categories.add(StreamingCategory(s, ""))

                                        observableLists[data]?.postValue(categories)
                                    }
                                    data.isStreaming() -> {
                                        playlistIds[data] = data.navigationData.split(",")
                                    }
                                    else -> {}
                                }
                            }
                        }

                        getHomeReceived.postValue(true)
                    },
                    { it.printStackTrace() }
                )
        )
    }


    private fun handleSuggestedResult(response: JSONArray) {
        addDisposable(
            Observable.just(response).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {

                        LogUtils.d(logTag, "testSUGGESTED - list size: ${it.length()}")

                        val tmpList = mutableListOf<SuggestedPerson>()
                        for (i in 0 until it.length()) {
                            SuggestedPerson.get(it.optJSONObject(i))?.let { data ->
                                tmpList.add(data)
                            }
                        }

                        prevSuggestedSkip = suggestedSkip
                        suggestedSkip += tmpList.size

                        observableLists
                            .filterKeys { key -> key.homeType == HomeUIType.SUGGESTED }
                            .forEach { entry ->

                                LogUtils.d(logTag, "testHOME: notifying SUGGESTED\tsize: ${tmpList.size}")

                                entry.value.postValue(tmpList)
                            }

                        suggestedReceived.postValue(true)
                    },
                    { it.printStackTrace() }
                )
        )
    }


    private fun handleTimelineResult(response: JSONArray) {
        addDisposable(
            Observable.just(response).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {

                        // notify view to hide/show action "seeAll" depending on the response length
                        showSeeAll.postValue(it.length() > 0)

                        val tmpList = mutableListOf<Post>()
                        for (i in 0 until it.length()) {
                            Post.get(it.optJSONObject(i))?.let { data ->
                                tmpList.add(data)
                            }
                        }

                        observableLists
                            .filterKeys { key -> key.homeType == HomeUIType.POST }
                            .forEach { entry ->

                                addDisposable(
                                    homeRepo.addCreateItem(tmpList)
                                        .subscribeOn(Schedulers.trampoline())
                                        .observeOn(Schedulers.trampoline())
                                        .subscribe(
                                            { editedList ->

                                                LogUtils.d(logTag, "testHOME: notifying TIMELINE\tsize: ${tmpList.size}")

                                                entry.value.postValue(editedList.toMutableList())
                                            },
                                            { thr -> thr.printStackTrace() }
                                        )
                                )

                            }

                        postsReceived.postValue(true)
                    },
                    { it.printStackTrace() }
                )
        )
    }

    private fun refreshTimeline() = getHomeSingleType(HomeNavigationData(HomeUIType.POST))

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
                    { it.printStackTrace() }
                )
        )
    }

    override fun fetchMoreSuggested() {
        prevSuggestedSkip = null
        fetchingMoreSuggested = true
        getHomeSingleType(HomeNavigationData(HomeUIType.SUGGESTED))
    }

    fun resetSuggestedSkip() {
        prevSuggestedSkip = 0
        suggestedSkip = 0

        // INFO: 2019-10-29    resetting property only when the first 20 elements are actually called
        //      allows not to call submitList() with only 0-5 elements, since LiveData gets triggered
        //      more than once after bg/fg cycles
        fetchingMoreSuggested = false
    }

    fun getCachedUserID(): String {
        return usersRepository.fetchCachedMyUserId()
    }

    fun fetchToReadCount() {
        addDisposable(
            Observable.just(chatRepository.getToReadCount())
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    if (it > 0) chatBadgeLiveData.postValue(true to it)
                    else chatBadgeLiveData.postValue(false to 0)
                },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    companion object {
        val logTag = HomeViewModel::class.java.simpleName
    }


}