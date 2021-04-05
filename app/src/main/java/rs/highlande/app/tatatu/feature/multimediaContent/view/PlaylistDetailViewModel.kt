package rs.highlande.app.tatatu.feature.multimediaContent.view

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DO_SHARE_MOVIE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_RT_SEND_PLAYER_EARNINGS
import rs.highlande.app.tatatu.connection.webSocket.SocketResponse
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.multimediaContent.repository.MultimediaRepository
import rs.highlande.app.tatatu.model.TTUVideo
import kotlin.math.round

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-18.
 */
class PlaylistDetailViewModel(val multimediaRepository: MultimediaRepository) : BaseViewModel() {

    var movieDeeplinkLiveData = MutableLiveData<String>()
    var currentTTUVideo: TTUVideo? = null
    var isFirst: Boolean = true

    private val authManager by inject<AuthManager>()


    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    fun sendPlayerEarnings() {
        currentTTUVideo?.let {
            addDisposable(
                multimediaRepository.sendPlayerEarnings(this, it.id, it.duration.toString(), isFirst)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            LogUtils.d(logTag, "Player send EARNINGS with status: $it")
                        },
                        { it.printStackTrace() }
                    )
            )
        }
        isFirst = false
    }

    fun shareMovie() {
        currentTTUVideo?.let {
            addDisposable(
                multimediaRepository.shareMovie(this, it.id, it.poster, it.name)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            LogUtils.d(logTag, "SHARE MOVIE with status: $it")
                        },
                        { it.printStackTrace() }
                    )
            )
        }
    }


    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        when(callCode) {
            SERVER_OP_RT_SEND_PLAYER_EARNINGS -> {
                LogUtils.d(logTag, "Player send EARNINGS: SUCCESS")
            }
            SERVER_OP_DO_SHARE_MOVIE -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                LogUtils.d(logTag, "SHARE SUCCESS")
                                if (it!!.length() > 0) {
                                    movieDeeplinkLiveData.postValue(it.getJSONObject(0).getString("deepLink"))
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        if (callCode == SERVER_OP_RT_SEND_PLAYER_EARNINGS) {
            LogUtils.d(
                logTag,
                "Player send EARNINGS: FAIL w/ error:$errorCode and desctiption:$description"
            )

            if (SocketResponse.is401Error(errorCode, description))
                authManager.checkAndFetchCredentials()
        }
    }

    fun getVideoEstimatedEarnings() = currentTTUVideo?.let {
        val res = (it.duration.toDouble() / 60 / 1000)
//        val rounded = round(res * 100) / 100
        return round(res * TTU_VALUE * 100) / 100
    } ?: run { 0.0 }


    companion object {

        private const val TTU_VALUE = 0.12

        val logTag = PlaylistDetailViewModel::class.java.simpleName

    }

}