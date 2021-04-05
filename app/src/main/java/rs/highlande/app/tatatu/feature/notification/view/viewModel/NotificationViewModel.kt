package rs.highlande.app.tatatu.feature.notification.view.viewModel

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_NOTIFICATIONS
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostViewModel
import rs.highlande.app.tatatu.feature.notification.repository.NotificationRepository
import rs.highlande.app.tatatu.model.NotificationSimpleResponse
import rs.highlande.app.tatatu.model.UserNotification
import rs.highlande.app.tatatu.model.event.NotificationEvent

/**
 * Created by Abhin.
 */
class NotificationViewModel : BaseViewModel() {

    private val repo: NotificationRepository by inject()
    var mResponse: MutableLiveData<Pair<ArrayList<NotificationSimpleResponse>, Boolean>> = mutableLiveData(ArrayList<NotificationSimpleResponse>() to false)
    var mSingleNotifResponse: MutableLiveData<UserNotification?> = mutableLiveData(null)
//    var isProcess = mutableLiveData(false)
    var itemCount = 0
    var callingApi = false
    var currentPage = 0
    var replaceItems = false
    var addAtFirst = false

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(mResponse, mSingleNotifResponse)
    }

    fun getNotifications(resetPageCount: Boolean = false) {
        callingApi = true
        if (resetPageCount) {
            currentPage = 0
        }
        addDisposable(repo.getNotifications(this, currentPage).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(CreatePostViewModel.logTag, "Get Notification: SUCCESS")
            else LogUtils.e(CreatePostViewModel.logTag, "Get Notification call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }

    fun getNotification() {
        callingApi = true
        addAtFirst = true
        addDisposable(repo.getNotifications(this).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(CreatePostViewModel.logTag, "Get Notification: SUCCESS")
            else LogUtils.e(CreatePostViewModel.logTag, "Get Notification call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_GET_NOTIFICATIONS -> {
                LogUtils.d(CommonTAG, "Get Notification Response--> ${response.toString()}")
                val covertValue = getNotificationArrayList(response.toString())
                if (addAtFirst) {
                    covertValue[0].notifications?.let {
                        addAtFirst = false
                        if (it.isNotEmpty()) mSingleNotifResponse.postValue(it[0])
                    }
                } else {
                    mResponse.postValue(covertValue to replaceItems)
                    covertValue[0].notifications?.let {
                        itemCount = it.size
                    }
                }
            }
        }
    }

    private fun getNotificationArrayList(json: String): ArrayList<NotificationSimpleResponse> {
        val type = object : TypeToken<ArrayList<NotificationSimpleResponse>>() {}.type
        return Gson().fromJson(json, type)
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        when (callCode) {
            SERVER_OP_GET_NOTIFICATIONS -> {
//                isProcess.postValue(false)
                LogUtils.d(CommonTAG, "Get Notification error Response--> ${Gson().toJson(description)}")
            }
        }
    }

    fun hasMoreItems(): Boolean {
        return itemCount >= PAGINATION_SIZE
    }

}