package rs.highlande.app.tatatu.feature.search.repository

import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.commonApi.UsersApi
import rs.highlande.app.tatatu.feature.multimediaContent.api.MultimediaApi
import rs.highlande.app.tatatu.model.TTUPlaylist

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class SearchRepository(
    private val multimediaApi: MultimediaApi,
    private val usersApi: UsersApi) {

    fun doMediaSearch(accountId: String, query: String, offset: Int, limit: Int = PAGINATION_SIZE): Observable<TTUPlaylist?> {
        return multimediaApi.doSearch(accountId, query, offset, limit)
    }

    fun doUserSearch(caller: OnServerMessageReceivedListener, query: String): Observable<RequestStatus> {
        return usersApi.doSearchUsers(caller, query)
    }

}