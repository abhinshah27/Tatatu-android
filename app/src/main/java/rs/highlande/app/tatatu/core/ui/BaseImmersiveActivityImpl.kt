package rs.highlande.app.tatatu.core.ui

import android.os.Handler
import android.view.View

/**
 * Interface dealing with immersive fullscreen implementation.
 * @author mbaldrighi on 2019-07-23.
 */
interface BaseImmersiveActivityImpl {

    fun hideSystemUI()
    fun showSystemUI()
    fun setDecorView(decorView: View)
    fun handleImmersiveOnFocusChange(hasFocus: Boolean)

}


/**
 * Class delegated to handle immersive fullscreen implementation.
 */
class ImmersiveImplHandler(private val immersive: Boolean = true) : BaseImmersiveActivityImpl {

    private lateinit var decorView: View

    private val mHideHandler = Handler(Handler.Callback {
        hideSystemUI()
        true
    })


    override fun hideSystemUI() {
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

    }

    override fun showSystemUI() {
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    override fun setDecorView(decorView: View) {
        this.decorView = decorView
    }


    fun reset(emptyMessDelayed: Boolean) {
        mHideHandler.removeMessages(0)
        if (emptyMessDelayed)
            mHideHandler.sendEmptyMessageDelayed(0, 300L)
    }


    override fun handleImmersiveOnFocusChange(hasFocus: Boolean) {
        // When the window loses focus (e.g. the action overflow is shown),
        // cancel any pending hide action. When the window gains focus,
        // hide the system UI.
        if (hasFocus) {
            if (immersive) reset(true)
        } else {
            mHideHandler.removeMessages(0)
        }
    }


}