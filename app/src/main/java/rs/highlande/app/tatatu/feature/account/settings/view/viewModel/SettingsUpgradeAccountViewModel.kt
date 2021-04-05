package rs.highlande.app.tatatu.feature.account.settings.view.viewModel

import android.os.Bundle
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_ST_CHANGE_ACCOUNT_TYPE
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.account.settings.repository.SettingsRepository
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData
import rs.highlande.app.tatatu.model.AccountType

/**
 * Created by Abhin.
 */
class SettingsUpgradeAccountViewModel : BaseViewModel(), OnServerMessageReceivedListener {

    val mPublicFigure = mutableLiveData(false)
    val mCelebrity = mutableLiveData(false)
    val mCharity = mutableLiveData(false)

    val showVerification = mutableLiveData(false)
    val openEmail = mutableLiveData(false)

    var savedType: AccountType? = null

    private val repo: SettingsRepository by inject()

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.txt_public_figure -> {
                mPublicFigure.value = !mPublicFigure.value!!
                mCelebrity.value = false
                mCharity.value = false
            }
            R.id.txt_celebrity -> {
                mCelebrity.value = !mCelebrity.value!!
                mPublicFigure.value = false
                mCharity.value = false
            }
            R.id.txt_charity -> {
                mCharity.value = !mCharity.value!!
                mPublicFigure.value = false
                mCelebrity.value = false
            }

        }
    }

    fun changeAccountType() {
        val bundle = Bundle().apply {
            putString("accountType", setData())
        }

        addDisposable(
            repo.getChangeAccountType(this, bundle)
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

    fun verifyAccount() {
        val bundle = Bundle()
        addDisposable(repo.getVerifyAccount(this, bundle)
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

    private fun setData(): String {
        var mAccountType = ""
        mAccountType = when {
            mPublicFigure.value!! -> AccountType.PUBLIC_FIGURE.value
            mCelebrity.value!! -> AccountType.CELEBRITY.value
            mCharity.value!! -> AccountType.CHARITY.value
            else -> AccountType.NORMAL.value
        }

        savedType = AccountType.toEnum(mAccountType)

        return mAccountType
    }

    // INFO: 2019-08-28    No verification is allowed at the moment -> just open email after account change

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        LogUtils.d(CommonTAG, "verify response-->$response")
        if (callCode == SERVER_OP_ST_CHANGE_ACCOUNT_TYPE) {

            openEmail.postValue(true)

//            showVerification.postValue(true)
        }
//        else if (callCode == SERVER_OP_ST_VERIFY_ACCOUNT) {
//            openEmail.postValue(true)
//        }
    }

    override fun handleErrorResponse(
        idOp: String,
        callCode: Int,
        errorCode: Int,
        description: String?
    ) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        if (callCode == SERVER_OP_ST_CHANGE_ACCOUNT_TYPE) {
            LogUtils.d(CommonTAG, "CHANGE ACCOUNT TYPE FAILED with error: $errorCode and description: $description")
        }
    }

    companion object {
        val logTag = SettingsUpgradeAccountViewModel::class.java.simpleName
    }

}