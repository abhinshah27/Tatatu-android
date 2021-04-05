package rs.highlande.app.tatatu.core.ui

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.koin.core.KoinComponent
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.core.util.CompositeDisposableHandler
import rs.highlande.app.tatatu.core.util.DisposableHandler
import rs.highlande.app.tatatu.core.util.hideKeyboard
import rs.highlande.app.tatatu.feature.commonRepository.mutableLiveData

abstract class BaseViewModel(): ViewModel(), DisposableHandler by CompositeDisposableHandler(), OnServerMessageReceivedListener, KoinComponent {


    val showProgress =  mutableLiveData(false)

    val errorOnRx: MutableLiveData<Int?> = mutableLiveData(null)


    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {

        if (response == null) throw IllegalArgumentException("SocketResponse object cannot be null")

    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        // TODO: 2019-07-15    leave implementation to children
    }


    override fun onCleared() {
        disposeOf()
        super.onCleared()
    }

    private fun getAllObservedDataBase() : Set<MutableLiveData<*>> {

        return mutableSetOf<MutableLiveData<*>>(
            showProgress, errorOnRx
        ).apply { addAll(getAllObservedData().toList()) }
    }

    abstract fun getAllObservedData(): Array<MutableLiveData<*>>
//    abstract fun resetObservedData()

    open fun clearObservers(owner: LifecycleOwner, data: MutableLiveData<*>? = null) {
        if (data != null)
            data.removeObservers(owner)
        else {
            for (it in getAllObservedDataBase()) {
                it.removeObservers(owner)
            }
        }
    }

    protected fun notifyRxError(@StringRes msg: Int) {
        if (msg != 0) errorOnRx.postValue(msg)
    }


    /**
     * Add/replace provided fragment.
     *
     * @param activity the parent activity
     * @param container
     * @param fragment
     * @param addFragment
     * @param addToBackStack
     * @param animationHolder
     */
    protected fun addReplaceFragment(activity: FragmentActivity, @IdRes container: Int, fragment: Fragment, addFragment: Boolean, addToBackStack: Boolean, animationHolder: AnimationHolder?) {
        val transaction: FragmentTransaction = activity.supportFragmentManager.beginTransaction()
        if (animationHolder != null) {
            transaction.setCustomAnimations(animationHolder.enterAnim, animationHolder.exitAnim, animationHolder.popEnterAnim, animationHolder.popExitAnim)
        }
        if (addFragment) {
            transaction.add(container, fragment, fragment.javaClass.simpleName)
        } else {
            transaction.replace(container, fragment, fragment.javaClass.simpleName)
        }
        if (addToBackStack) {
            transaction.addToBackStack(fragment.tag)
        }
        hideKeyboard(activity)
        transaction.commit()
    }


}