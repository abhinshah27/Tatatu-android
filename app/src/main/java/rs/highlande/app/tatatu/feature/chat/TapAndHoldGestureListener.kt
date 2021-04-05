/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat

import android.Manifest
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PermissionHelper
import rs.highlande.app.tatatu.feature.chat.view.ChatMessagesFragment

/**
 * Former TapAndHoldGestureListener.java (@author mabldrighi on 11/17/2017).
 * @author mabldrighi on 11/13/2018.
 */
class TapAndHoldGestureListener(private val listener: OnHoldListener,val fragment: ChatMessagesFragment):
        View.OnTouchListener {

    companion object {

        val LOG_TAG = TapAndHoldGestureListener::class.java.simpleName

        private const val LIMIT_FOR_HOLD: Long = 750
    }

    private val mPermissionRequestCode = 100055
    private var permissionHelper: PermissionHelper? = null

    private var isDown = false
    private var hasPermissions = false

    private var isHolding = false
    private var downX = 0f
    private var holdPointerId: Int? = null

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downX = motionEvent.rawX

                isDown = true
                view.isSelected = true

                permissionHelper = PermissionHelper(fragment, arrayOf(
                    Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), mPermissionRequestCode)
                permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
                    override fun onPermissionGranted() {
                    Handler().postDelayed({
                        LogUtils.d(LOG_TAG, "CHECKING Hold")

                        holdPointerId = motionEvent.getPointerId(0)

                        if (isDown) {
                            isHolding = true
                            listener.onHoldActivated()

                            LogUtils.d(LOG_TAG, "Hold ACTIVATED")
                        }
                    }, LIMIT_FOR_HOLD)
                    }

                    override fun onPermissionDenied() {

                    }

                    override fun onPermissionDeniedBySystem() {

                    }
                })

                return true
            }

            MotionEvent.ACTION_UP -> {
                isDown = false
                if (isHolding && hasPermissions) {
                    isHolding = false
                    listener.onHoldReleased()
                    view.isSelected = false

                    LogUtils.d(LOG_TAG, "Hold RELEASED")
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                LogUtils.d(LOG_TAG, "DOWN position X: $downX, with pointerID: $holdPointerId \n CURRENT position X: ${motionEvent.rawX}, with pointerID: ${motionEvent.getPointerId(0)}")

                val movingLeft = motionEvent.rawX < downX
                val samePointer = holdPointerId == motionEvent.getPointerId(0)

                if (isHolding && samePointer) {
                    listener.onSlideEvent(motionEvent, movingLeft)
                    downX = motionEvent.rawX
                }
                return true
            }
        }

        return false
    }

    interface OnHoldListener {
        fun onHoldActivated()
        fun onHoldReleased()
        fun onSlideEvent(motionEvent: MotionEvent, movingLeft: Boolean)
    }
}
