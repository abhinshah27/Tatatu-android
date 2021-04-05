package rs.highlande.app.tatatu.core.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Created by Abhin.
 */
@Suppress("UNCHECKED_CAST")
class PermissionHelper {

    private val TAG = "PermissionHelper"
    private val permissionHelper: PermissionHelper? = null
    private var REQUEST_CODE: Int = 0

    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var permissions: Array<String>? = null
    private var mPermissionCallback: PermissionCallback? = null
    private var mPermissionCallbackManually: PermissionCallbackManually? = null
    private var showRationale: Boolean = false
    private var checkPermissionsManually: Boolean = false
    private var checkPermissionsManuallyGranted = ArrayList<String>()
    private var checkPermissionsManuallyPending= ArrayList<String>()

    private constructor(activity: Activity, fragment: Fragment, permissions: Array<String>, requestCode: Int) {
        this.activity = activity
        this.fragment = fragment
        this.permissions = permissions
        this.REQUEST_CODE = requestCode
        //        checkIfPermissionPresentInAndroidManifest();
    }

    constructor(activity: Activity, permissions: Array<String>, requestCode: Int) {
        this.activity = activity
        this.permissions = permissions
        this.REQUEST_CODE = requestCode
        //        checkIfPermissionPresentInAndroidManifest();
    }

    constructor(fragment: Fragment, permissions: Array<String>, requestCode: Int) {
        this.fragment = fragment
        this.permissions = permissions
        this.REQUEST_CODE = requestCode
        //        checkIfPermissionPresentInAndroidManifest();
    }

    constructor(fragment: Fragment, permissions: Array<String>, requestCode: Int, checkPermissionIndividually: Boolean) {
        this.fragment = fragment
        this.permissions = permissions
        this.REQUEST_CODE = requestCode
        this.checkPermissionsManually = checkPermissionIndividually
        //        checkIfPermissionPresentInAndroidManifest();
    }

    private fun checkIfPermissionPresentInAndroidManifest() {
        for (permission in permissions!!) {
            if (!hasPermission(permission)) {
                throw RuntimeException("Permission ($permission) Not Declared in manifest")
            }
        }
    }

    fun request(permissionCallback: PermissionCallback) {
        this.mPermissionCallback = permissionCallback
        if (!checkSelfPermission(permissions!!)) {
            showRationale = shouldShowRational(permissions!!)
            if (activity != null) ActivityCompat.requestPermissions(activity!!, filterNotGrantedPermission(permissions!!), REQUEST_CODE)
            else fragment!!.requestPermissions(filterNotGrantedPermission(permissions!!), REQUEST_CODE)
        } else {
            LogUtils.i(TAG, "PERMISSION: Permission Granted")
            if (mPermissionCallback != null) mPermissionCallback!!.onPermissionGranted()
        }
    }

    fun requestManually(mPermissionCallbackManual: PermissionCallbackManually) {
        this.mPermissionCallbackManually = mPermissionCallbackManual
        if (!checkSelfPermission(permissions!!)) {
            showRationale = shouldShowRational(permissions!!)
            if (activity != null) ActivityCompat.requestPermissions(activity!!, filterNotGrantedPermission(permissions!!), REQUEST_CODE)
            else fragment!!.requestPermissions(filterNotGrantedPermission(permissions!!), REQUEST_CODE)
        } else {
            LogUtils.i(TAG, "PERMISSION: Permission Granted")
            if (mPermissionCallbackManually != null) mPermissionCallbackManually!!.onPermissionGranted()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE) {
            if (checkPermissionsManually) {
                checkPermissionsManuallyPending.clear()
                for ((i, permission ) in permissions.withIndex()) {
                    if (ContextCompat.checkSelfPermission(fragment?.context!!, permission) == PackageManager.PERMISSION_GRANTED) {
                        checkPermissionsManuallyGranted.add(permissions[i])
                    }else{
                        checkPermissionsManuallyPending.add(permissions[i])
                    }
                }
                mPermissionCallbackManually?.onCheckPermissionManually(requestCode, permissions, grantResults,checkPermissionsManuallyGranted,checkPermissionsManuallyPending)
            }

            var denied = false
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true
                    break
                }
            }

            if (denied) {
                val currentShowRational = shouldShowRational(permissions)
                if (!showRationale && !currentShowRational) {
                    LogUtils.d(TAG, "PERMISSION: Permission Denied By System")
                    if (mPermissionCallback != null) mPermissionCallback!!.onPermissionDeniedBySystem()
                } else {
                    LogUtils.i(TAG, "PERMISSION: Permission Denied")
                    if (mPermissionCallback != null) mPermissionCallback!!.onPermissionDenied()
                }
            } else {
                LogUtils.i(TAG, "PERMISSION: Permission Granted")
                if (mPermissionCallback != null) mPermissionCallback!!.onPermissionGranted()
            }
        }
    }

    //====================================
    //====================================

    private fun <T : Context> getContext(): T {
        return if (activity != null) activity as T else fragment!!.context as T
    }

    /**
     * Return list that is not granted and we need to ask for permission
     *
     * @param permissions
     * @return
     */
    private fun filterNotGrantedPermission(permissions: Array<String>): Array<String> {
        val notGrantedPermission = ArrayList<String>()
        if (checkPermissionsManually) checkPermissionsManuallyGranted.clear()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermission.add(permission)
            }else{
                if (checkPermissionsManually) checkPermissionsManuallyGranted.add(permission)
            }
        }
        return notGrantedPermission.toTypedArray()
    }

    /**
     * Check permission is there or not for fit_watch of permissions
     *
     * @param permissions
     * @return
     */
    private fun checkSelfPermission(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Checking if there is need to show rational for fit_watch of permissions
     *
     * @param permissions
     * @return
     */
    private fun shouldShowRational(permissions: Array<String>): Boolean {
        var currentShowRational = false
        for (permission in permissions) {

            if (activity != null) {
                if (shouldShowRequestPermissionRationale(activity!!, permission)) {
                    currentShowRational = true
                    break
                }
            } else {
                if (fragment!!.shouldShowRequestPermissionRationale(permission)) {
                    currentShowRational = true
                    break
                }
            }
        }
        return currentShowRational
    }

    private fun hasPermission(permission: String): Boolean {
        try {
            val context = if (activity != null) activity else fragment!!.activity
            val info = context!!.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            if (info.requestedPermissions != null) {
                for (p in info.requestedPermissions) {
                    if (p == permission) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun removePermissionListenerCallback() {
        if (mPermissionCallback != null) mPermissionCallback = null
    }

    interface PermissionCallback {
        fun onPermissionGranted()

        fun onPermissionDenied()

        fun onPermissionDeniedBySystem()

    }

    interface PermissionCallbackManually {
        fun onPermissionGranted()
        fun onCheckPermissionManually(requestCode: Int, permissions: Array<String>, grantResults: IntArray, checkPermissionsManuallyGranted: ArrayList<String>, checkPermissionsManuallyPending: ArrayList<String>)
    }
}
