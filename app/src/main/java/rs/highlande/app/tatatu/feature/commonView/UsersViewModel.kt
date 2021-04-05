package rs.highlande.app.tatatu.feature.commonView

import androidx.lifecycle.MutableLiveData
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_USER
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.model.User

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-07.
 */
open class UsersViewModel : BaseViewModel() {

    private val repository: UsersRepository by inject()

    private var mUser = MutableLiveData<User?>()
    private var mUserConnected = mutableLiveData(false)
    private var mUserReceived = mutableLiveData(false)
    private var mUserError = mutableLiveData(false)


    fun fetchMyUser(fromNetwork: Boolean = false) {
        addDisposable(repository.getMyUser(fromNetwork, this)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                {

                    mUser.postValue(it.first)

                    mUserConnected.postValue(it.second == RequestStatus.SENT)

                },
                {
                    it.printStackTrace()
                    mUser.postValue(repository.mUser)
                }
            )
        )
    }


    fun getUser() = mUser
    fun getUserId() = mUser.value?.uid

    fun clearCachedUser() = repository.clearCachedUser()


    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        if (response!!.length() == 0 || response.optJSONObject(0).length() == 0) {
            handleErrorResponse(idOp, callCode, 0, null)
            return
        }

        if (callCode == SERVER_OP_GET_USER) {
            mUserReceived.postValue(true)
            User.get(response.optJSONObject(0))?.let {
                repository.cacheUser(it)
                mUser.postValue(it)
            }
        }

    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        if (callCode == SERVER_OP_GET_USER) {
            mUserError.postValue(true)

            LogUtils.e(logTag, "GET USER FAILED with error=$errorCode and description=$description")
        }

    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(mUser, mUserConnected, mUserReceived, mUserError)
    }

    companion object {
        val logTag = UsersViewModel::class.java.simpleName
    }


}


interface MyUserChangeListener {
    fun observeOnMyUserAction(user: User)
}