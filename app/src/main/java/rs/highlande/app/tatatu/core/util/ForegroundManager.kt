/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import java.util.*

/**
 * Class implementing [Application.ActivityLifecycleCallbacks] to register and forward to [rs.highlande.app.tatatu.TTUApp]
 * notifications of transition background -> foreground and viceversa.
 *
 * @author mbaldrighi on 2019-07-10.
 */
class ForegroundManager : Application.ActivityLifecycleCallbacks {

    var isForeground = false
    private val listeners = ArrayList<ForegroundListener>()

    /**
     * Handler used to forward the delayed message of becoming background. This is necessary otherwise every instantaneous
     * [onActivityPaused] callback will trigger the background notification.
     * @see mBackgroundTransition
     */
    private val mBackgroundDelayHandler = Handler()

    /**
     * Runnable used to forward the delayed message of becoming background. This is necessary otherwise every instantaneous
     * [onActivityPaused] callback will trigger the background notification.
     * @see mBackgroundDelayHandler
     */
    private var mBackgroundTransition: Runnable? = null


    interface ForegroundListener {
        fun onBecameForeground()
        fun onBecameBackground()
    }


    fun registerListener(listener: ForegroundListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: ForegroundListener) {
        listeners.remove(listener)
    }

    override fun onActivityResumed(activity: Activity) {
        if (mBackgroundTransition != null) {
            mBackgroundDelayHandler.removeCallbacks(mBackgroundTransition!!)
            mBackgroundTransition = null
        }

        if (!isForeground) {
            isForeground = true
            notifyOnBecameForeground()
            LogUtils.i(LOG_TAG, "Application went to foreground")
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (isForeground && mBackgroundTransition == null) {
            mBackgroundTransition = Runnable {
                isForeground = false
                mBackgroundTransition = null
                notifyOnBecameBackground()
                LogUtils.i(LOG_TAG, "Application went to background")
            }
            mBackgroundDelayHandler.postDelayed(mBackgroundTransition!!, BACKGROUND_DELAY)
        }
    }

    private fun notifyOnBecameForeground() {
        for (listener in listeners) {
            try {
                listener.onBecameForeground()
            } catch (e: Exception) {
                LogUtils.e(LOG_TAG, "Listener threw exception!$e", e)
            }

        }
    }

    private fun notifyOnBecameBackground() {
        for (listener in listeners) {
            try {
                listener.onBecameBackground()
            } catch (e: Exception) {
                LogUtils.e(LOG_TAG, "Listener threw exception!$e")
            }

        }
    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, oustState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        val LOG_TAG = ForegroundManager::class.java.simpleName
        const val BACKGROUND_DELAY: Long = 500
    }

}
