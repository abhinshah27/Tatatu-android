package rs.highlande.app.tatatu.feature.account.settings.view.viewModel

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_ST_GET_BLACKLIST
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_ST_REMOVE_BLACKLIST
import rs.highlande.app.tatatu.connection.webSocket.SocketRequest
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.account.settings.repository.SettingsRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.model.BlockAccountDataList

/**
 * Created by Abhin.
 */
class SettingsBlockAccountViewModel : BaseViewModel(), OnServerMessageReceivedListener {
    val mArrayList = mutableLiveData(ArrayList<BlockAccountDataList>())
    val mUnBlock = mutableLiveData(false)
    val mPosition = mutableLiveData(-1)
    val isProgress = mutableLiveData(false)
    private val repo: SettingsRepository by inject()

    fun blockListType() {
        isProgress.value = true
        val bundle = Bundle().apply {
            putInt("skip", 1)
        }
        //print module
        //printBundle(bundle)
        addDisposable(
            repo.getBlockAccountList(this, bundle)
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

    fun removeBlockAccount(userID: String, action: Int) {
        isProgress.value = true
        //Action:- 0: remove user from blacklist 1: add user to blacklist
        val bundle = Bundle().apply {
            putInt("action", action)
            putString("userID", userID)
        }
        //print module
        //        printBundle(bundle)

        addDisposable(
            repo.getRemoveBlockAccount(this, bundle)
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
        SocketRequest(bundle, callCode = SERVER_OP_ST_REMOVE_BLACKLIST, logTag = "POST call", caller = object : OnServerMessageReceivedListener {
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
            SERVER_OP_ST_GET_BLACKLIST -> mArrayList.postValue(getArrayList(response.toString()))
            SERVER_OP_ST_REMOVE_BLACKLIST -> mUnBlock.postValue(true)
        }
        LogUtils.d(logTag, "response-->$response")
    }

    //getting the error form web sockets
    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        isProgress.postValue(false)
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        LogUtils.d(logTag, "error response-->$description")

    }

    //convert into Array List
    private fun getArrayList(json: String): ArrayList<BlockAccountDataList> {
        val type = object : TypeToken<ArrayList<BlockAccountDataList>>() {}.type
        var teamDataList: ArrayList<BlockAccountDataList> = ArrayList()
        teamDataList = Gson().fromJson(json, type)
        return teamDataList
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    companion object {
        val logTag = SettingsBlockAccountViewModel::class.java.simpleName
    }

}