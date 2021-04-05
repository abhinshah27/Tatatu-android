package rs.highlande.app.tatatu.feature.home.repository

import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.feature.commonRepository.CreatePostRepository
import rs.highlande.app.tatatu.feature.commonRepository.PostRepository
import rs.highlande.app.tatatu.feature.commonRepository.UnFollowRepository
import rs.highlande.app.tatatu.feature.home.api.HomeApi
import rs.highlande.app.tatatu.feature.multimediaContent.repository.MultimediaRepository
import rs.highlande.app.tatatu.feature.suggested.api.LIMIT_SUGGESTED_AFTER_FOLLOW
import rs.highlande.app.tatatu.feature.suggested.repository.SuggestedRepository
import rs.highlande.app.tatatu.model.*

/**
 * Repository handling all operations for fetching and saving for [HomeNavigationData].
 * @author mbaldrighi on 2019-07-01.
 */
class HomeRepository : BaseRepository(), CreatePostRepository, UnFollowRepository {

    private val homeApi: HomeApi by inject()

    private val postRepo: PostRepository by inject()
    private val suggestedRepo: SuggestedRepository by inject()
    private val multimediaRepository: MultimediaRepository by inject()

    private var cachedHomeData = emptyList<HomeNavigationData>()
    private var cachedData = mutableMapOf<HomeNavigationData, MutableList<HomeDataObject>?>()

    fun getHomeTypes(caller: OnServerMessageReceivedListener): Observable<RequestStatus> {

        return fetchHomeData(caller)

//        return if (cachedHomeData.isEmpty()) {
//            //Returning users from API
//            fetchHomeData(caller)
//        } else {
//            //Returning cached users first, and then API users
//            Observable.just(cachedHomeData)
//                .mergeWith(fetchHomeData(caller))
//                .doOnNext { cachedHomeData = it }
//        }
    }

    fun getSingleTypes(single: HomeNavigationData, caller: OnServerMessageReceivedListener, suggestedSkip: Int? = null): Observable<RequestStatus>? {
        return fetchHomeSingleTypes(single, caller, suggestedSkip)
    }

    fun getSingleTypesStreaming(single: HomeNavigationData, accountId: String): Observable<MutableList<out HomeDataObject>> {
        return fetchHomeSingleTypesStreaming(single, accountId).map { it.videos }
    }


    fun removeSuggested(data: HomeNavigationData, item: HomeDataObject) {
        cachedData[data]?.remove(item)
    }

    fun savePost(post: Post) {
        postRepo.savePost(post)
    }

    fun addCreateItem(posts: List<Post>): Observable<List<Post>> {
        return postRepo.addNewItem(posts)
    }

    private fun fetchHomeData(caller: OnServerMessageReceivedListener): Observable<RequestStatus> {
        return homeApi.getHomeContent(caller)
    }

    private fun fetchHomeSingleTypes(
        data: HomeNavigationData,
        caller: OnServerMessageReceivedListener,
        suggestedSkip: Int? = null
    ): Observable<RequestStatus>? {
        return when (data.homeType) {
            HomeUIType.POST -> postRepo.getHomePosts(caller)
            HomeUIType.SUGGESTED -> {
                if (suggestedSkip != null)
                    suggestedRepo.getSuggested(caller, suggestedSkip, if (suggestedSkip > 0) LIMIT_SUGGESTED_AFTER_FOLLOW else null)
                else Observable.just(RequestStatus.ERROR)
            }
            else -> null
        }
    }

    private fun fetchHomeSingleTypesStreaming(data: HomeNavigationData, accountId: String): Observable<TTUPlaylist?> {
        return fetchHomeStreamingData(data, accountId)
            .doOnNext {
                if (it != null) {
                    cachedData[data] = mutableListOf<HomeDataObject>().apply { addAll(it.videos) }
                }
            }
    }


    private fun fetchHomeStreamingData(data: HomeNavigationData, accountId: String): Observable<TTUPlaylist?> {
        return multimediaRepository.getHomePlaylist(accountId, data)
    }

}