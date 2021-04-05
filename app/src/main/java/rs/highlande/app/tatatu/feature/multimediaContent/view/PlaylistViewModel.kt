package rs.highlande.app.tatatu.feature.multimediaContent.view

import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.core.inject
import retrofit2.HttpException
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.feature.multimediaContent.repository.MultimediaRepository
import rs.highlande.app.tatatu.model.HomeNavigationData
import rs.highlande.app.tatatu.model.TTUVideo

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-18.
 */
class PlaylistViewModel : BaseViewModel() {

    companion object {
        const val LIMIT = 50
    }

    private val multimediaRepository: MultimediaRepository by inject()

    var navigationData: HomeNavigationData? = null
        set(value) {
            playlistIds.addAll(value?.navigationData?.split(",") ?: listOf())
            if (currentPlaylistId.isBlank()) currentPlaylistId = playlistIds[0]
            field = value
        }

    val videoList = MutableLiveData<Pair<List<TTUVideo>?, Boolean>>()

    private var currentPlaylistId = ""
    private var nextPlaylistId = ""

    private var offset = 0


    private val playlistIds = mutableListOf<String>()


    fun getVideos(accountId: String) {
        if (!currentPlaylistId.isBlank()) {
            val obs = multimediaRepository.getFullPlaylist(
                accountId,
                if (!nextPlaylistId.isBlank()) nextPlaylistId else currentPlaylistId,
                offset,
                LIMIT
            )

            addDisposable(
                obs.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { resultList ->

                            currentPlaylistId = resultList?.id ?: ""

                            videoList.value =
                                if (resultList == null) {

                                    // if result == null, pass empty list to view
                                    mutableListOf<TTUVideo>() to false
                                } else {
                                    val size = resultList.videos.size

                                    val newIndex = playlistIds.indexOf(currentPlaylistId) + 1

                                    var canFetch = true

                                    if (size == 0) {
                                        // if no results check if it's the end of the playlistIds
                                        if (playlistIds.size > newIndex) {
                                            // if ids array still has elements, assign nextPlaylistId
                                            nextPlaylistId = playlistIds[newIndex]
                                            getVideos(accountId)
                                        } else {
                                            // if no other elements, stop loading more, reset nextPlaylistId
                                            nextPlaylistId = ""
                                            canFetch = false
                                        }
                                    } else {
                                        if (newIndex == -1) {
                                            // if ids array reached the end, reset nextPlaylistId
                                            nextPlaylistId = ""
                                            canFetch = false
                                        } else {
                                            // checking response size
                                            if (size < LIMIT) {

                                                if (playlistIds.size > newIndex) {
                                                    // if ids array still has elements, assign nextPlaylistId
                                                    nextPlaylistId = playlistIds[newIndex]
                                                } else {
                                                    // if no other elements, stop loading more, reset nextPlaylistId
                                                    nextPlaylistId = ""
                                                    canFetch = false
                                                }

                                                // if size < default_elem_count, next call must use nextPlaylistId -> reset offset
                                                offset = 0
                                            } else {
                                                // size == default_elem_count, next call must still be with currentPlaylistId
                                                // (offset will be increased by LoadMore action)

                                                if (!nextPlaylistId.isBlank()) {
                                                    currentPlaylistId = nextPlaylistId
                                                    nextPlaylistId = ""
                                                }

                                                offset += LIMIT
                                            }
                                        }
                                    }
                                    resultList.videos to canFetch
                                }
                        },
                        {
                            it.printStackTrace()

                            if (it is HttpException) {
                                if (nextPlaylistId.isNotBlank())
                                    currentPlaylistId = nextPlaylistId

                                val newIndex = playlistIds.indexOf(currentPlaylistId) + 1

                                var canFetch = true

                                if (playlistIds.size > newIndex) {
                                    // if ids array still has elements, assign nextPlaylistId
                                    nextPlaylistId = playlistIds[newIndex]
                                    getVideos(accountId)
                                } else {
                                    nextPlaylistId = ""
                                    canFetch = false
                                }

                                videoList.value = mutableListOf<TTUVideo>() to canFetch
                            }
                        }
                    )
            )
        }
    }

    fun getMoreVideos(accountId: String) {
        getVideos(accountId)
    }

    fun refreshVideos(accountId: String) {
        offset = 0
        currentPlaylistId = playlistIds[0]
        nextPlaylistId = ""
        getVideos(accountId)
    }

    fun resetResultLiveData() {
        videoList.value = null to true
    }


    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(
            videoList
        )
    }
}