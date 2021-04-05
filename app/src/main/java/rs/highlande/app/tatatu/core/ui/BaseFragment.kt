package rs.highlande.app.tatatu.core.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.KoinComponent
import rs.highlande.app.tatatu.core.util.CompositeDisposableHandler
import rs.highlande.app.tatatu.core.util.DisposableHandler
import rs.highlande.app.tatatu.feature.commonView.MyUserChangeListener
import rs.highlande.app.tatatu.feature.commonView.UsersViewModel
import rs.highlande.app.tatatu.model.User

/**
 * Created by Abhin.
 */
abstract class BaseFragment : Fragment(), BaseView, DisposableHandler by CompositeDisposableHandler(), MyUserChangeListener, KoinComponent {

    private val myUserViewModel: UsersViewModel by sharedViewModel()

    protected var mBaseActivity: BaseActivity? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        fetchUser()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mBaseActivity = context as BaseActivity?
    }

    override fun onStart() {
        super.onStart()

        bindLayout()
    }

    override fun onPause() {
        myUserViewModel.clearObservers(this)
        super.onPause()
    }

    override fun onStop() {
        clearDisposables()
        super.onStop()
    }

    override fun onDestroy() {
        disposeOf()
        super.onDestroy()
    }


    fun addReplaceFragment(@IdRes container: Int, fragment: Fragment, addFragment: Boolean, addToBackStack: Boolean, animationHolder: AnimationHolder?, nested: Boolean = false) {
        if (activity == null) return

        val transaction = (if (nested) childFragmentManager else activity!!.supportFragmentManager).beginTransaction()

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

        mBaseActivity!!.hideKeyboard(activity!!)

        // INFO: 2019-09-30    Reverts crash fix: allowingStateLoss is not the solution
//        transaction.commitAllowingStateLoss()
        transaction.commit()
    }

    fun addReplaceFragmentSharedElement(@IdRes container: Int, fragment: Fragment, addFragment: Boolean, addToBackStack: Boolean, animationHolder: AnimationHolder?, view: View) {
        if (activity == null) return

        val transaction = activity!!.supportFragmentManager.beginTransaction()

        if (animationHolder != null) {
            transaction.setCustomAnimations(animationHolder.enterAnim, animationHolder.exitAnim, animationHolder.popEnterAnim, animationHolder.popExitAnim)
        }

        transaction.addSharedElement(view, ViewCompat.getTransitionName(view).toString())

        if (addFragment) {
            transaction.add(container, fragment, fragment.javaClass.simpleName)
        } else {
            transaction.replace(container, fragment, fragment.javaClass.simpleName)
        }

        if (addToBackStack) {
            transaction.addToBackStack(fragment.tag)
        }

        mBaseActivity!!.hideKeyboard(activity!!)

        // INFO: 2019-09-30    Reverts crash fix: allowingStateLoss is not the solution
//        transaction.commitAllowingStateLoss()
        transaction.commit()
    }

    override fun showMessage(message: String) {
        try {
            if (mBaseActivity != null) mBaseActivity!!.showMessage(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showMessage(message: Int) {
        showMessage(getString(message))
    }

    override fun showError(error: String) {
        try {
            if (mBaseActivity != null) mBaseActivity!!.showError(error)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showLoader(message: Int?) {
        try {
            if (mBaseActivity != null) mBaseActivity!!.showLoader(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showLoader(message: String?) {
        try {
            if (mBaseActivity != null) mBaseActivity!!.showLoader(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun hideLoader() {
        try {
            if (mBaseActivity != null) mBaseActivity!!.hideLoader()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }




    fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) view = View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }




    override fun observeOnMyUserAction(user: User) {
        // TODO: 2019-07-07    leave the body to every class own implementation
    }

    protected abstract fun configureLayout(view: View)
    protected abstract fun bindLayout()

    protected fun getUser(): User? {
        return myUserViewModel.getUser().value
    }

    protected fun clearCachedUser() = myUserViewModel.clearCachedUser()


    protected fun fetchUser(fromNetwork: Boolean = false) {
        myUserViewModel.apply {
            getUser().observe(viewLifecycleOwner, Observer {
                if (it != null)
                    observeOnMyUserAction(it)
            })
            fetchMyUser(fromNetwork)
        }
    }



    fun isAudioPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun isVideoPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

}