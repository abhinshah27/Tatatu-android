/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import rs.highlande.app.tatatu.TTUApp
import java.lang.ref.WeakReference

/**
 * @author mbaldrighi on 10/18/2018.
 */
abstract class BaseHelper(context: Context) {

    val contextRef: WeakReference<Context> by lazy {
        WeakReference(context)
    }

    open fun onCreate(view: View? = null) {}
    open fun onViewCreated(view: View) {}
    open fun onActivityCreated(activity: TTUApp) {}
    open fun onStart() {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onStop() {}
    open fun onDestroy() {}

    open fun onBackPressed() {}

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    open fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {}

    internal fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) {
        if (isContextValid(contextRef.get()) && receiver != null && filter != null)
            LocalBroadcastManager.getInstance(contextRef.get()!!).registerReceiver(receiver, filter)
    }

    internal fun unregisterReceiver(receiver: BroadcastReceiver?) {
        try {
            if (isContextValid(contextRef.get()) && receiver != null)
                LocalBroadcastManager.getInstance(contextRef.get()!!).unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

}