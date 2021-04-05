package rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper

import android.Manifest
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PermissionHelper
import rs.highlande.app.tatatu.model.User

/**
 * TODO - File description
 * @author mbaldrighi on 2020-01-14.
 */


interface DefaultCallInitializer {

    fun initPermissionsHelper(fragment: Fragment, requestCode: Int): PermissionHelper

    fun initPermissionsForCall(
        fragment: Fragment,
        requestCode: Int,
        classTag: String,
        caller: User,
        callee: User,
        isVideo: Boolean
    )

    fun forwardRequestPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    )

    fun doCallNotification(
        activity: BaseActivity,
        callerID: String,
        callerName: String,
        calledIdentity: User,
        isVideo: Boolean
    )
}


class DefaultCallInitializerDelegate : DefaultCallInitializer {

    private var wantsVideo: Boolean = false
    private var caller: User? = null
    private var callee: User? = null
    private var permissionHelper: PermissionHelper? = null
    private lateinit var classTag: String

    override fun initPermissionsHelper(fragment: Fragment, requestCode: Int): PermissionHelper {
        permissionHelper = PermissionHelper(
            fragment,
            if (wantsVideo) arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE
            ) else arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE),
            requestCode,
            true
        )
        return permissionHelper!!
    }

    override fun initPermissionsForCall(
        fragment: Fragment,
        requestCode: Int,
        classTag: String,
        caller: User,
        callee: User,
        isVideo: Boolean
    ) {
        wantsVideo = isVideo
        initPermissionsHelper(fragment, requestCode)

        this@DefaultCallInitializerDelegate.caller = caller
        this@DefaultCallInitializerDelegate.callee = callee

        if (permissionHelper == null || this@DefaultCallInitializerDelegate.caller == null || this@DefaultCallInitializerDelegate.callee == null) return

        this@DefaultCallInitializerDelegate.classTag = classTag

        if (isVideo) {
            initVideoPermissionCall(fragment, classTag)
        } else {
            initVoicePermissionCall(fragment, classTag)
        }
    }

    override fun doCallNotification(
        activity: BaseActivity,
        callerID: String,
        callerName: String,
        calledIdentity: User,
        isVideo: Boolean
    ) {
        LogUtils.d(classTag, "CN->$classTag FN--> doCallNotification() --> isVideo $isVideo")
        CallsNotificationUtils.sendCallNotification(
            activity,
            callerID,
            callerName,
            calledIdentity,
            if (isVideo) VoiceVideoCallType.VIDEO else VoiceVideoCallType.VOICE
        )
    }

    override fun forwardRequestPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun initVideoPermissionCall(fragment: Fragment, classTag: String) {

        LogUtils.d(classTag, "CN->$classTag FN--> initVideoPermissionCall() --> ")
        permissionHelper!!.requestManually(object : PermissionHelper.PermissionCallbackManually {
            override fun onPermissionGranted() {
                (fragment.activity as? BaseActivity)?.let {
                    doCallNotification(it, caller!!.uid, caller!!.name, callee!!, wantsVideo)
                }
            }

            override fun onCheckPermissionManually(
                requestCode: Int,
                permissions: Array<String>,
                grantResults: IntArray,
                checkPermissionsManuallyGranted: ArrayList<String>,
                checkPermissionsManuallyPending: ArrayList<String>
            ) {
                LogUtils.d(
                    classTag,
                    "CN->$classTag FN--> onCheckPermissionManually() checkPermissionsManuallyGranted--> ${Gson().toJson(
                        checkPermissionsManuallyGranted
                    )}"
                )
                LogUtils.d(
                    classTag,
                    "CN->$classTag FN--> onCheckPermissionManually() checkPermissionsManuallyPending--> ${Gson().toJson(
                        checkPermissionsManuallyPending
                    )}"
                )
                if (checkPermissionsManuallyGranted.contains(Manifest.permission.RECORD_AUDIO) || checkPermissionsManuallyGranted.containsAll(
                        arrayListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                ) {
                    (fragment.activity as? BaseActivity)?.let {
                        doCallNotification(it, caller!!.uid, caller!!.name, callee!!, wantsVideo)
                    }
                }
            }
        })
    }

    private fun initVoicePermissionCall(fragment: Fragment, classTag: String) {

        LogUtils.d(classTag, "CN-> $classTag FN--> initVoicePermissionCall() --> ")
        permissionHelper?.requestManually(object : PermissionHelper.PermissionCallbackManually {
            override fun onPermissionGranted() {
                (fragment.activity as? BaseActivity)?.let {
                    doCallNotification(it, caller!!.uid, caller!!.name, callee!!, wantsVideo)
                }
            }

            override fun onCheckPermissionManually(
                requestCode: Int,
                permissions: Array<String>,
                grantResults: IntArray,
                checkPermissionsManuallyGranted: ArrayList<String>,
                checkPermissionsManuallyPending: ArrayList<String>
            ) {
                LogUtils.d(
                    classTag,
                    "CN-> $classTag FN--> onCheckPermissionManually() " + "checkPermissionsManuallyGranted--> ${Gson().toJson(
                        checkPermissionsManuallyGranted
                    )}"
                )
                LogUtils.d(
                    classTag,
                    "CN-> $classTag FN--> onCheckPermissionManually() " + "checkPermissionsManuallyPending--> ${Gson().toJson(
                        checkPermissionsManuallyPending
                    )}"
                )
                if (checkPermissionsManuallyGranted.contains(Manifest.permission.RECORD_AUDIO)) {
                    (fragment.activity as? BaseActivity)?.let {
                        doCallNotification(it, caller!!.uid, caller!!.name, callee!!, wantsVideo)
                    }
                }
            }
        })
    }

}