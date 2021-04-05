package rs.highlande.app.tatatu.core.util

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableString
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.crashlytics.android.Crashlytics
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.feature.authentication.AuthActivity
import rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper.VoiceVideoCallType
import rs.highlande.app.tatatu.model.User
import kotlin.math.roundToInt


/**
 * File holding all the methods of general interest.
 *
 * @author mbaldrighi on 2019-06-26.
 */


//region = Units =

/**
 * This method converts any unit to pixels.
 *
 * @param unit      the wanted unit.
 * @param value     the wanted value units.
 * @param resources the application's `Resources`
 * @return The corresponding pixel value.
 */
private fun convertToPx(unit: Int, value: Float, resources: Resources): Int {
    val px = TypedValue.applyDimension(unit, value, resources.displayMetrics)
    return px.toInt()
}


/**
 * This method converts dp units to pixels.
 *
 * @param dp        the wanted dp units.
 * @param resources the application's `Resources`
 * @return The corresponding pixel value.
 */
fun dpToPx(dp: Float, resources: Resources): Int {
    return convertToPx(TypedValue.COMPLEX_UNIT_DIP, dp, resources)
}

/**
 * This method converts dp units to pixels.
 *
 * @param sp        the wanted dp units.
 * @param resources the application's `Resources`
 * @return The corresponding pixel value.
 */
fun spToPx(sp: Float, resources: Resources): Int {
    return convertToPx(TypedValue.COMPLEX_UNIT_SP, sp, resources)
}

//endregion


/**
 * Extension function to apply custom font to any provided [MenuItem].
 *
 * @param context the application's/activity's context.
 * @param fontRes the provided font.
 */
fun MenuItem.applyFont(context: Context?, @FontRes fontRes: Int) {
    context?.let {
        if (it.isValid()) {
            ResourcesCompat.getFont(it, fontRes)?.let { font ->
                val title = SpannableString(title)
                title.setSpan(HLCustomTypefaceSpan("", font), 0, title.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                setTitle(title)
            }
        }
    }
}


//hide the keyboard
fun hideKeyboard(activity: Activity) {
    val imm: InputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = activity.currentFocus
    if (view == null) view = View(activity)
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun getScreenHeight(activity: Activity): Int {
    val dip = 32f
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, activity.resources.displayMetrics)
    return (Resources.getSystem().displayMetrics.heightPixels - px).roundToInt()
}

//show error in snack bar
fun showError(activity: FragmentActivity, error: String) {
    Snackbar.make(activity.findViewById(android.R.id.content), error, Snackbar.LENGTH_SHORT).show()
}


fun expand(v: View) {
    val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
    val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight = v.measuredHeight

    // Older versions of android (pre API 21) cancel animations for views with a height of 0.
    v.layoutParams.height = 1
    v.visibility = View.VISIBLE
    val a = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            v.layoutParams.height = if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT
            else (targetHeight * interpolatedTime).toInt()
            v.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    // Expansion speed of 1dp/ms
    a.duration = (targetHeight / v.context.resources.displayMetrics.density).toInt().toLong()
    v.startAnimation(a)
}

fun collapse(v: View) {
    val initialHeight = v.measuredHeight

    val a = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            if (interpolatedTime == 1f) {
                v.visibility = View.GONE
            } else {
                v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                v.requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    // Collapse speed of 1dp/ms
    a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
    v.startAnimation(a)
}


//region = Android API Version =


/**
 * Checks if OS is at least [Build.VERSION_CODES.KITKAT].
 */
fun hasKitKat(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.KITKAT)
}

/**
 * Checks if OS is at least [Build.VERSION_CODES.KITKAT_WATCH].
 */
fun hasKitKatWatch(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.KITKAT_WATCH)
}

/**
 * Checks if OS is at least [Build.VERSION_CODES.LOLLIPOP].
 */
fun hasLollipop(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.LOLLIPOP)
}

/**
 * Checks if OS is at least [Build.VERSION_CODES.M].
 */
fun hasMarshmallow(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.M)
}

/**
 * Checks if OS is at least [Build.VERSION_CODES.N].
 */
fun hasNougat(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.N)
}

/**
 * Checks if OS is at least [Build.VERSION_CODES.O].
 */
fun hasOreo(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.O)
}

/**
 * Checks if OS is at least [Build.VERSION_CODES.P].
 */
fun hasPie(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.P)
}

/**
 * Checks if OS is at least [Build.VERSION_CODES.Q].
 */
fun hasQ(): Boolean {
    return hasAPIVersion(Build.VERSION_CODES.Q)
}


/**
 * Checks if OS is at least the provided [Build.VERSION_CODES].
 */
fun hasAPIVersion(version: Int): Boolean {
    return Build.VERSION.SDK_INT >= version
}

//endregion


/**
 * Checks if calling [Context] is valid.
 */
fun Context.isValid(): Boolean {
    return if (this is Activity) {
        !isFinishing && !isDestroyed
    } else true
}


//region = Common intents =

/**
 * Fires intent to open EMAIL app.
 * @param context the current Activity's [Context].
 */
fun fireOpenEmailIntent(context: BaseActivity) {
    try {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.dialog_signup_email_title)))
    } catch (e: Exception) {
        e.printStackTrace()
        context.showError(context.getString(R.string.no_email_apps))
        val redirection = Intent(context, AuthActivity::class.java)
        redirection.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivityForResult(redirection, 0)
    } finally {
        context.finish()
    }
}

/**
 * Fires intent to open EMAIL app.
 * @param context the current Activity's [Context].
 */
fun fireOpenBrowserIntent(context: Activity, url: String) {
    if (url.isNotBlank()) {
        try {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                if (resolveActivity(context.packageManager) != null) context.startActivity(Intent.createChooser(this, context.getString(R.string.choose_to)))
                else if (context is BaseActivity) context.showError(context.getString(R.string.no_browser_apps))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (context is BaseActivity) context.showError(context.getString(R.string.no_browser_apps))
        }
    }
}

//endregion


fun User.logInfoForCrashlytics() {
    Crashlytics.setUserIdentifier(uid)
    Crashlytics.setUserName("$username ($name)")
    Crashlytics.setUserEmail(privateInfo.email)
}

fun resolveColorAttribute(context: Context, @AttrRes attrId: Int): Int {
    val typedValue = TypedValue()
    val theme = context.theme
    theme.resolveAttribute(attrId, typedValue, true)
    return typedValue.data
}

/**
 * Extension function for [View] that calculates the visible portion of the calling view.
 */
fun View.getVisiblePercentage() : Float {
    if (visibility != View.VISIBLE || parent == null) return 0f

    val mGlobalRect = Rect()
    if (!getGlobalVisibleRect(mGlobalRect)) return 0f

    return mGlobalRect.height().toFloat() / height
}

fun isContextValid(context: Context?): Boolean {
    return context?.let {
        context is Activity && !context.isFinishing && !context.isDestroyed
    }?: run {
        false
    }
}

fun vibrateForChat(context: Context?) {
    if (context?.isValid() == true) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        if (vibrator?.hasVibrator() == true) {
            if (hasOreo())
                vibrator.vibrate(VibrationEffect.createOneShot(25, 15))
            else
                vibrator.vibrate(25)
        }
    }
}

fun vibrateForCalls(context: Context?, cancel: Boolean = false) {
    if (context?.isValid() == true) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        if (vibrator?.hasVibrator() == true) {
            if (cancel) vibrator.cancel()
            else {
                val pattern = longArrayOf(0, 1000, 500, 1000)
                if (hasOreo())
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, intArrayOf(0, 200, 0, 100), 0))
                else
                    vibrator.vibrate(pattern, 0)
            }
        }
    }
}
    
// check the service are running or not
fun isMyServiceRunning(context: Context?, serviceClass: Class<*>): Boolean {
    val manager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
    for (service in manager!!.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

fun isAudioPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

fun isVideoPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

fun isPermissionGranted(context: Context,callType: VoiceVideoCallType): Boolean {
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    return if (callType == VoiceVideoCallType.VOICE) hasMic
    else {
        hasMic && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Checks if device has a camera hardware.
 * @param context the application/activity's Context
 * @return True if device has camera hardware, false otherwise
 */
fun hasDeviceCamera(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
}

fun View.baseAnimateAlpha(duration: Long, show: Boolean, repeat: Boolean = false, delay: Long = 0):
        ViewPropertyAnimator {

    var localShow = show

    val animator = animate()
        .alpha(if (show) 1f else 0f)
        .setDuration(duration)
        .setListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                if (show)
                    this@baseAnimateAlpha.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {

                if (!repeat && !show)
                    this@baseAnimateAlpha.visibility = View.GONE
                else if (repeat) {
                    localShow = !localShow
                    this@baseAnimateAlpha.baseAnimateAlpha(duration, localShow, repeat, delay)
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
                // reset initial value deducted from the initial value of show argument
                alpha = if (show) 0f else 1f
            }

            override fun onAnimationRepeat(animation: Animator?) {}
        })
        .setStartDelay(delay)

    animator?.start()

    return animator
}

fun View.baseAnimateHeight(duration: Long, expand: Boolean, fullHeight: Int, delay: Long = 0,
                           automaticCollapse: Long = 0, customListener: Animator.AnimatorListener? = null,
                           start: Boolean = true):
        ValueAnimator? {

    if (fullHeight > 0) {
        val animator = if (expand) ValueAnimator.ofInt(0, fullHeight) else ValueAnimator.ofInt(fullHeight, 0)
        animator.duration = duration
        animator.startDelay = delay
        animator.addUpdateListener {
            val lp = this.layoutParams
            lp.height = it.animatedValue as Int
            this.layoutParams = lp
        }
        if (customListener != null) animator.addListener(customListener)
        else {
            animator.addListener(object: Animator. AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    if (automaticCollapse > 0 && expand) {
                        this@baseAnimateHeight.postDelayed(
                            { baseAnimateHeight(duration, false, fullHeight, delay, 0) },
                            automaticCollapse
                        )
                    }
                }

                override fun onAnimationStart(animation: Animator?) {}
            })
        }
        if (start) animator.start()
        return animator
    }

    return null

}


/**
 * Returns the wanted color int value.
 * @param resources the application's [Resources].
 * @param colorId the provided [ColorRes].
 * @return The wanted color int value.
 */
fun getColor(resources: Resources?, @ColorRes colorId: Int): Int {
    return if (resources != null) ResourcesCompat.getColor(
        resources,
        colorId,
        null
    ) else android.R.color.transparent
}

private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
/**
 * Checks the device to make sure it has the Google Play Services APK. If
 * it doesn't, display a dialog that allows users to download the APK from
 * the Google Play Store or enable it in the device's system settings.
 */
fun checkPlayServices(context: Context): Boolean {
    val googleAPI = GoogleApiAvailability.getInstance()

    val result = googleAPI.isGooglePlayServicesAvailable(context)
    if (result != ConnectionResult.SUCCESS) {
        if (context is Activity) {
            if (googleAPI.isUserResolvableError(result))
                googleAPI.getErrorDialog(context, result, PLAY_SERVICES_RESOLUTION_REQUEST).show()
            else {
                GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(context)
                context.finish()
            }
        }
        return false
    }
    return true
}