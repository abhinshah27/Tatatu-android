package rs.highlande.app.tatatu.feature.wallet.view.viewModel

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_USER
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostViewModel
import rs.highlande.app.tatatu.feature.wallet.repository.WalletRepository
import rs.highlande.app.tatatu.model.User


/**
 * Created by Abhin.
 */
class WalletViewModel : BaseViewModel() {
    private val repo: WalletRepository by inject()
    var mCurrencyData = mutableLiveData(mutableMapOf<String, String>())
    private val repository: UsersRepository by inject()
    var mUser = MutableLiveData<User?>()
    private var mUserConnected = mutableLiveData(false)


    fun fetchMyUser(fromNetwork: Boolean = false) {
        addDisposable(repository.getMyUser(fromNetwork, this).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            mUser.postValue(it.first)
            mUserConnected.postValue(it.second == RequestStatus.SENT)
        }, {
            it.printStackTrace()
            mUser.postValue(repository.mUser)
        }))
    }

    fun getUser() = mUser


    //Get Currency Conversion Rate
    fun getCurrencyConversionRate() {
        addDisposable(repo.getCurrencyConversionRate(this, Bundle()).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(CreatePostViewModel.logTag, "SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE call: SUCCESS")
            else LogUtils.e(CreatePostViewModel.logTag, "SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE -> {
                LogUtils.d(CommonTAG, "SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE Success Response--> ${Gson().toJson(response)}")
                mCurrencyData.postValue(convertStringToMap(response!!.get(0) as JSONObject))
            }
            SERVER_OP_GET_USER -> {
                User.get(response!!.optJSONObject(0))?.let {
                    repository.cacheUser(it)
                    mUser.postValue(it)
                }
            }
        }
    }

    private fun convertStringToMap(jsonObject: JSONObject): HashMap<String, String> {
        val map = HashMap<String, String>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next() as String
            val value = jsonObject.getString(key)
            map[key] = value
        }
        return map
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when (callCode) {
            SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE -> {
                LogUtils.d(CommonTAG, "SERVER_OP_GET_CURRENCY_RATE_CONVERSION_RATE Error Response--> $description")
            }
        }
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }
}