package rs.highlande.app.tatatu.feature.multimediaContent.api

import android.os.Bundle
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.http.*
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DO_SHARE_MOVIE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_RT_SEND_PLAYER_EARNINGS
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.model.HomeNavigationData
import rs.highlande.app.tatatu.model.TTUPlaylist
import rs.highlande.app.tatatu.model.playlistIds

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-12.
 */
class MultimediaApi : BaseApi() {

    private val headerAcceptValue = "$APPL_JSON;$POLICY_KEY_HEADER"
    private val headerAcceptValueSearch = "$APPL_JSON;pk=$POLICY_KEY_SEARCH"


    //region = Plylists =

    interface PlaylistService {

        @GET(URL_PLAYLIST)
        fun getPlaylist(
            @Header(HEADER_ACCEPT) policyKey: String,
            @Path(PARAM_ACCOUNT_ID) accountId: String,
            @Path(PARAM_PLAYLIST_ID) playlistId: String,
            @Query(PARAM_OFFSET) offset: String? = null,
            @Query(PARAM_LIMIT) limit: String? = null
        ): Observable<TTUPlaylist?>

        @GET(URL_PLAYBACK_SEARCH)
        fun search(
            @Header(HEADER_ACCEPT) policyKey: String,
            @Path(PARAM_ACCOUNT_ID) accountId: String,
            @Query(PARAM_QUERY) query: String,
            @Query(PARAM_OFFSET) offset: String? = null,
            @Query(PARAM_LIMIT) limit: String? = null,
            @Query(PARAM_SORT) sort: String? = null
        ): Observable<TTUPlaylist?>

    }


    fun getHomePlaylist(accountId: String, data: HomeNavigationData): Observable<TTUPlaylist?> {
        return getFullPlaylist(
            accountId,
            playlistIds[data]?.get(0) ?: "",
            0,
            DEFAULT_HOME_ELEMENTS_NUMBER
        )
    }

    fun getFullPlaylist(accountId: String, playlistId: String, offset: Int = 0, limit: Int = PAGINATION_SIZE): Observable<TTUPlaylist?> {

        LogUtils.d("testBRIGHTCOVEKEY - LIST", headerAcceptValue)

        return getRetrofitService<PlaylistService>().getPlaylist(
            headerAcceptValue,
            accountId,
            playlistId,
            offset.toString(),
            limit.toString()
        )
    }

    fun doSearch(accountId: String, query: String, offset: Int = 0, limit: Int = PAGINATION_SIZE): Observable<TTUPlaylist?> {

        LogUtils.d("testBRIGHTCOVEKEY - SEARCH", headerAcceptValueSearch)

        return getRetrofitService<PlaylistService>().search(
            headerAcceptValueSearch,
            accountId,
            query,
            offset.toString(),
            limit.toString()
        )
    }

    fun doSendPlayerEarnings(
        caller: OnServerMessageReceivedListener,
        contentID: String? = null,
        contentLength: String? = null,
        isFirst: Boolean = false
    ): Observable<RequestStatus> {

        return if (!contentID.isNullOrEmpty() && !contentLength.isNullOrEmpty()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("contentID", contentID)
                    putString("contentLength", contentLength)
                    putBoolean("isFirst", isFirst)
                },
                callCode = SERVER_OP_RT_SEND_PLAYER_EARNINGS,
                logTag = "SEND PLAYER EARNINGS call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    fun doShareMovie(
        caller: OnServerMessageReceivedListener,
        id: String? = null,
        poster: String? = null,
        name: String? = null
    ): Observable<RequestStatus> {

        return if (!id.isNullOrEmpty()
            && !poster.isNullOrEmpty()
            && !name.isNullOrEmpty()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("id", id)
                    putString("poster", poster)
                    putString("name", name)
                },
                callCode = SERVER_OP_DO_SHARE_MOVIE,
                logTag = "SHAEE MOVIE call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    //endregion


}


const val DEFAULT_HOME_ELEMENTS_NUMBER: Int = 10