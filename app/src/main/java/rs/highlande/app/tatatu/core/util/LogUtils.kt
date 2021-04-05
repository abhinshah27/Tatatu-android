package rs.highlande.app.tatatu.core.util

import android.util.Log

import rs.highlande.app.tatatu.BuildConfig


/**
 * @author mbaldrighi on 9/4/2019.
 */
object LogUtils {

    private val LOGGABLE = BuildConfig.DEBUG

    /*
	 * DEBUG
	 */
    fun d(tag: String?, obj: Any?) {
        d(tag, obj.toString())
    }

    fun d(tag: String?, obj: String?) {
        if (LOGGABLE)
            Log.d(tag, obj ?: "")
    }

    /*
	 * INFO
	 */
    fun i(tag: String?, obj: Any?) {
        i(tag, obj.toString())
    }

    fun i(tag: String?, obj: String?) {
        if (LOGGABLE)
            Log.i(tag, obj ?: "")
    }

    /*
	 * VERBOSE
	 */
    fun v(tag: String?, obj: Any?) {
        v(tag, obj.toString())
    }

    fun v(tag: String?, obj: String?) {
        if (LOGGABLE)
            Log.v(tag, obj ?: "")
    }

    /*
	 * ERROR
	 */
    fun e(tag: String?, obj: Any?) {
        e(tag, obj.toString(), null)
    }

    fun e(tag: String?, obj: String?, th: Throwable?) {
        if (LOGGABLE) {
            Log.e(tag, obj ?: "", th)

            th?.printStackTrace()
        }
    }

}
