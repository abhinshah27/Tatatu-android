package rs.highlande.app.tatatu.feature.account.settings.view.viewModel

import android.os.Bundle
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_ST_CHANGE_ACCOUNT_VISIBILITY
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.account.settings.repository.SettingsRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData

/**
 * Created by Abhin.
 */
class SettingsThemePrivacyViewModel : BaseViewModel(), OnServerMessageReceivedListener {

    val valueChanged = mutableLiveData(false)
    val isProgress = mutableLiveData(false)
    private val repo: SettingsRepository by inject()

    private var currentValue: Boolean? = null

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(valueChanged, isProgress)
    }

    fun onClick(view: View) {

    }

    //get The Account Visibility
    fun getAccountVisibility(isPrivate: Boolean) {
        currentValue = isPrivate

        val mAccountVisibility = if (isPrivate) {
            1
        } else {
            0
        }
        isProgress.value = true
        //Action:-	0: public, 1: private
        val bundle = Bundle().apply {
            putInt("visibility", mAccountVisibility)
        }
        //print module
        //        printBundle(bundle)

        addDisposable(
            repo.getAccountVisibility(this, bundle)
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
        SocketRequest(bundle, callCode = SERVER_OP_ST_CHANGE_ACCOUNT_VISIBILITY, logTag = "CHANGE ACCOUNT VISIBILITY", caller = object : OnServerMessageReceivedListener {
            override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
            }

            override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
            }
        })
    }

    //getting the success response from web sockets
    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        isProgress.postValue(false)
        when (callCode) {
            SERVER_OP_ST_CHANGE_ACCOUNT_VISIBILITY -> {
                valueChanged.postValue(currentValue)
            }
        }
        LogUtils.d(logTag, "response-->$response")
    }

    //getting the error form web sockets
    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        isProgress.postValue(false)
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        LogUtils.d(logTag, "error response-->$description")

    }


    companion object {
        val logTag = SettingsThemePrivacyViewModel::class.java.simpleName
    }

}