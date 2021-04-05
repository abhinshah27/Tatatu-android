package rs.highlande.app.tatatu.core.api

import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.WebSocketTracker

/**
 * Base api class providing children with an accessible instance of [WebSocketTracker].
 * @author mbaldrighi on 2019-07-15.
 */
abstract class BaseApi : KoinComponent {

    protected val tracker: WebSocketTracker by inject()

}