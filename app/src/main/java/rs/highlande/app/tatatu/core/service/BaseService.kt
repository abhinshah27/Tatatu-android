package rs.highlande.app.tatatu.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.json.JSONArray
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.core.util.CompositeDisposableHandler
import rs.highlande.app.tatatu.core.util.DisposableHandler
import rs.highlande.app.tatatu.feature.commonApi.CommonApi

/**
 * Child class of [Service] serving as common base for custom implementation of services across TTU app.
 * It provides instance by DI of [CommonApi] class.
 *
 * @author mbaldrighi on 2019-07-11.
 */
abstract class BaseService : Service(), OnServerMessageReceivedListener, KoinComponent,
    DisposableHandler by CompositeDisposableHandler() {

    protected val commonApi: CommonApi by inject()


    override fun onDestroy() {
        disposeOf()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? { return null }


    @Throws(IllegalArgumentException::class)
    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {

        if (response == null) throw IllegalArgumentException("SocketResponse object cannot be null")

        // TODO: 2019-07-11    leave to children classes the implementation
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        // TODO: 2019-07-11    leave to children classes the implementation
    }

}