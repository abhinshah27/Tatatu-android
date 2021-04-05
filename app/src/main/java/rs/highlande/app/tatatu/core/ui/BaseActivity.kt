package rs.highlande.app.tatatu.core.ui

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.commonView.BaseProgressDialogFragment
import rs.highlande.app.tatatu.feature.commonView.MyUserChangeListener
import rs.highlande.app.tatatu.feature.commonView.UsersViewModel
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.SERVICE_NOTIFICATION_ID
import rs.highlande.app.tatatu.model.User

/**
 * Created by Abhin.
 */

abstract class BaseActivity :
    AppCompatActivity(),
    BaseActivityListener,
    BaseView,
    DisposableHandler by CompositeDisposableHandler(),
    MyUserChangeListener,
    TTUApp.DeviceConnectionListener {

    private val myUserViewModel: UsersViewModel by viewModel()

    private var mBaseProgressDialog: BaseProgressDialogFragment? = null

    var isImmersiveActivity = false

    protected var canFinish = true

    private var recreated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fetchUser()

        supportFragmentManager.addOnBackStackChangedListener {
            canFinish = supportFragmentManager.backStackEntryCount < 2
            recreated = recreated && supportFragmentManager.backStackEntryCount >= 2
        }
    }

    override fun onStart() {
        super.onStart()

        // Activity starts -> register observer for device connection
        (application as? TTUApp)?.attachObserver(this)


        // If no call is currently active we remove the call notification
        if (CallServices.currentCallID.isNullOrBlank()) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(SERVICE_NOTIFICATION_ID)
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        recreated = true
    }

    override fun onPause() {
        myUserViewModel.clearObservers(this)
        super.onPause()
    }

    override fun onStop() {

        // Activity stops -> unregister observer for device connection
        (application as? TTUApp)?.removeObserver(this)

        hideLoader()
        clearDisposables()
        super.onStop()
    }

    fun setProgressBarNull() {
        mBaseProgressDialog = null
    }

    override fun onDestroy() {
        disposeOf()
        super.onDestroy()
    }

    /**
     * Provides basic implementation for back navigation.
     * If activity is used without fragments (backStack == 0) or if only one fragment remains added (backStack == 1),
     * pressing back triggers finish.
     * Children classes must
     * If progress dialog is showing, no action is performed.
     */
    // FIXME: 2019-08-16    return here: CANNOT BLOCK back action: needed listener + custom action
    override fun onBackPressed() {
        if (!isProgressShowing()) {
            if (canFinish && !recreated) {
                finish()
                val anims = getFinishAnimations()
                if (anims != null) {
                    overridePendingTransition(anims.enterAnim, anims.exitAnim)
                }
            } else super.onBackPressed()
        }
    }


    protected fun addReplaceFragment(
        @IdRes container: Int, fragment: Fragment,
        addFragment: Boolean,
        addToBackStack: Boolean,
        animationHolder: AnimationHolder?
    ) {

        var transaction: FragmentTransaction = supportFragmentManager.beginTransaction()

        if (animationHolder != null) {
            //            transaction.setCustomAnimations(R.anim.animation_slide_from_right, R.anim.animation_slide_to_left, R.anim.animation_slide_from_left, R.anim.animation_slide_to_right);
            transaction.setCustomAnimations(
                animationHolder.enterAnim,
                animationHolder.exitAnim,
                animationHolder.popEnterAnim,
                animationHolder.popExitAnim
            )
        }
        if (addFragment) {
            transaction.add(container, fragment, fragment.javaClass.simpleName)
        } else {
            transaction.replace(container, fragment, fragment.javaClass.simpleName)
        }
        if (addToBackStack) {
            transaction.addToBackStack(fragment.tag)
        }
        hideKeyboard(this)
        transaction.commit()
    }

    fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) view = View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    override fun observeOnMyUserAction(user: User) {
        // TODO: 2019-07-07    leave the body to every class own implementation
    }


    protected abstract fun configureLayout()
    protected abstract fun bindLayout()
    protected abstract fun manageIntent()

    protected fun getUser(): User? {
        return myUserViewModel.getUser().value
    }


    protected fun fetchUser(network: Boolean = false) {
        myUserViewModel.apply {
            getUser().observe(this@BaseActivity, Observer {
                if (it != null) {
                    observeOnMyUserAction(it)

                    it.logInfoForCrashlytics()
                }
            })
            fetchMyUser(network)
        }
    }


    //region == Base listener ==

    override fun getToolbar(): Toolbar? {
        // TODO: 2019-07-18    leave implementation to children
        return null
    }

    override fun getFinishAnimations(): ActivityAnimationHolder? {
        // TODO: 2019-07-19    leave implementation to children
        return null
    }

    //endregion


    //region == BaseView ==

    override fun showMessage(message: String) =
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()

    override fun showMessage(message: Int) = showMessage(getString(message))

    // TODO: 2019-07-07    stays for now but reconsider approach
    override fun showError(error: String) =
        Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_SHORT).show()


    override fun showLoader(message: Int?) = showLoader(getString(message ?: 0))

    override fun showLoader(message: String?) {

        if (isFinishing || isDestroyed) return

        if (mBaseProgressDialog == null || (mBaseProgressDialog!!.context?.isValid() == false)) {
            mBaseProgressDialog = null
            mBaseProgressDialog = BaseProgressDialogFragment.newInstance(sMessage = message ?: "")
        } else
            mBaseProgressDialog!!.setMessage(sMessage = message ?: "")

        if (!isProgressShowing())
            mBaseProgressDialog!!.show(supportFragmentManager, DIALOG_PROGRESS_TAG)
    }

    override fun hideLoader() {
        LogUtils.d(logTag, "testOBSERVERS: DISMISSING progress")
        try {
            mBaseProgressDialog?.dismissAllowingStateLoss()
            mBaseProgressDialog = null
        } catch (e: IllegalStateException) {
            LogUtils.e(logTag, e.message)
        }
    }

    private fun isProgressShowing() =
        supportFragmentManager.findFragmentByTag(DIALOG_PROGRESS_TAG) != null

    //endregion


    //region = Device connection listener =

    override fun notifyConnectionLost() = showMessage(R.string.error_connection_lost)

    override fun notifyConnectionAcquired() = showMessage(R.string.connection_acquired)

    //endregion


    /*
    override fun showLoader() {
        showProgressDialog()
    }

    override fun hideLoader() {
        hideProgressDialog()
    }
    */


    /**
     * show the progress
     */
/*
    private fun showProgressDialog() {
        if (mBaseProgressDialog == null) {
            mBaseProgressDialog =  BaseProgressDialogFragment().getInstance()
        }
        mBaseProgressDialog!!.show(supportFragmentManager, "ProgressDialog")
    }

    private fun hideProgressDialog() {
        mBaseProgressDialog?.dismiss()
    }
*/


    companion object {

        val logTag = BaseActivity::class.java.simpleName

        const val DIALOG_PROGRESS_TAG = "ProgressDialog"

    }

}


interface BaseActivityListener {

    fun setToolbarTitle(title: String) {
        getToolbar()?.findViewById<TextView>(R.id.title)?.text = title
    }

    fun setToolbarTitle(context: Context, @StringRes title: Int) {
        setToolbarTitle(context.getString(title))
    }

    fun getToolbar(): Toolbar?

    fun getFinishAnimations(): ActivityAnimationHolder?

}