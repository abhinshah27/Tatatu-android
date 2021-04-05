package rs.highlande.app.tatatu.feature.suggested.repository

import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.feature.commonRepository.UnFollowRepository
import rs.highlande.app.tatatu.feature.suggested.api.SuggestedApi
import rs.highlande.app.tatatu.model.SuggestedPerson

/**
 * Repository handling all operations for fetching and saving for [SuggestedPerson].
 * @author mbaldrighi on 2019-07-03.
 */
class SuggestedRepository : BaseRepository(), UnFollowRepository {

    private val suggestedApi: SuggestedApi by inject()

    private var cachedSuggested = mutableListOf<SuggestedPerson>()


    fun getSuggested(caller: OnServerMessageReceivedListener, skip: Int? = null, limit: Int? = null): Observable<RequestStatus> = fetchSuggested(caller, skip, limit)
    fun removeSuggested(item: SuggestedPerson) = cachedSuggested.remove(item)

    fun cacheNewItems(items: List<SuggestedPerson>) = cachedSuggested.addAll(items)

    private fun fetchSuggested(caller: OnServerMessageReceivedListener, skip: Int? = null, limit: Int? = null): Observable<RequestStatus> {
        return suggestedApi.getSuggestedV2(caller, skip, limit)
    }

}