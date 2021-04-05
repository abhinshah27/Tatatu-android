package rs.highlande.app.tatatu.feature.voiceVideoCalls.view

import android.os.Bundle
import android.view.View
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.ACTION_MAIN
import rs.highlande.app.tatatu.feature.voiceVideoCalls.services.CallServices.Companion.NOTIFICATION_ACTION_CALL_FIRST_START_ACT


/**
 * Created by Abhin.
 */

class CallActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreen()
        setContentView(R.layout.activity_call)
        manageIntents()
    }

    private fun manageIntents() {
        LogUtils.e(CommonTAG, "CN->CallActivity FN--> manageIntents() --> ${intent.action}")
        when (intent.action) {
            ACTION_MAIN, NOTIFICATION_ACTION_CALL_FIRST_START_ACT -> {
                openFragment()
            }
            else -> init()
        }
    }

    private fun openFragment() {
        val mCallsFragment = CallsFragment.newInstance()
        mCallsFragment.arguments = intent.extras
        addReplaceFragment(R.id.fl_call_container, mCallsFragment, false, false, null)
    }

    private fun init() {
        addReplaceFragment(R.id.fl_call_container, CallsFragment.newInstance(), false, false, null)
    }

    override fun onStart() {
        super.onStart()
        CallServices.isCallActivityForeground = true
    }

    override fun onResume() {
        setFullScreen()
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        CallServices.isCallActivityForeground = false
    }

    private fun setFullScreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun configureLayout() {}
    override fun bindLayout() {}
    override fun manageIntent() {}

}