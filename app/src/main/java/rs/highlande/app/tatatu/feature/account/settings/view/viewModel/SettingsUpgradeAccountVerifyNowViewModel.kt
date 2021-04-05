package rs.highlande.app.tatatu.feature.account.settings.view.viewModel

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_ST_VERIFY_ACCOUNT
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.account.settings.repository.SettingsRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData

/**
 * Created by Abhin.
 */
class SettingsUpgradeAccountVerifyNowViewModel : BaseViewModel(), OnServerMessageReceivedListener {
    var activity: AppCompatActivity? = null
    private val repo: SettingsRepository by inject()
    var openEmail = mutableLiveData(false)
    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    fun verifyAccount() {
        val bundle = Bundle().apply {}
        //print module
        //        printBundle(bundle)
        addDisposable(
            repo.getVerifyAccount(this, bundle)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        LogUtils.d("TAG", "Success Code-->${Gson().toJson(it)}")
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }


    //print the bundle for testing
    private fun printBundle(bundle: Bundle) {
        SocketRequest(bundle, callCode = SERVER_OP_ST_VERIFY_ACCOUNT, logTag = "POST call", caller = object : OnServerMessageReceivedListener {
            override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {

            }

            override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {

            }
        })
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        LogUtils.d(CommonTAG, "verify response-->$response")

        if (callCode == SERVER_OP_ST_VERIFY_ACCOUNT) {
            openEmail.postValue(true)
        }
    }


    companion object {
        val logTag = SettingsUpgradeAccountVerifyNowViewModel::class.java.simpleName
    }
}