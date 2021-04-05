package rs.highlande.app.tatatu.feature.multimediaContent.repository

import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.multimediaContent.api.MultimediaApi
import rs.highlande.app.tatatu.model.HomeNavigationData
import rs.highlande.app.tatatu.model.TTUPlaylist
import rs.highlande.app.tatatu.model.TTUVideo

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-14.
 */
class MultimediaRepository : BaseRepository() {

    private val multimediaApi: MultimediaApi by inject()

    //TODO: 20/08 Persisting here for now, since VideoPlayerActivity wont allow to use viewModel for DI and hence the state of the viewModel is lost. Review approach
    var isFirst: Boolean = true
    var currentTTUVideo: TTUVideo? = null

    fun getHomePlaylist(accountId: String, data: HomeNavigationData): Observable<TTUPlaylist?> {
        return multimediaApi.getHomePlaylist(accountId, data)
    }

    fun getFullPlaylist(accountId: String, playlistId: String, offset: Int, limit: Int = PAGINATION_SIZE): Observable<TTUPlaylist?> {
        return multimediaApi.getFullPlaylist(accountId, playlistId, offset, limit)
    }

    fun sendPlayerEarnings(
        caller: OnServerMessageReceivedListener,
        contentID: String? = null,
        contentLength: String? = null,
        isFirst: Boolean = false
    ): Observable<RequestStatus> = multimediaApi.doSendPlayerEarnings(caller, contentID, contentLength, isFirst)

    fun shareMovie(
        caller: OnServerMessageReceivedListener,
        id: String? = null,
        poster: String? = null,
        name: String? = null
    ): Observable<RequestStatus> = multimediaApi.doShareMovie(caller, id, poster, name)

}