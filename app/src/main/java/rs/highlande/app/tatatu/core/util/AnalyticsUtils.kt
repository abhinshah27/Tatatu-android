package rs.highlande.app.tatatu.core.util

import com.comscore.Analytics

//import com.comscore.Analytics

/**
 * Object serving as logic container for all logic related to Analytics event.
 * Currently: comScore
 * @author mbaldrighi on 2019-10-30.
 */
object AnalyticsUtils {

    val logTag = AnalyticsUtils::class.java.simpleName

    private const val LABEL_CATEGORY = "ns_category"
    private const val LABEL_CONTEXT = "context"

    fun trackScreen(screenName: String, context: String? = null, sendTracking: Boolean = false) {

        if (sendTracking) {
            Analytics.notifyViewEvent(
                mutableMapOf(LABEL_CATEGORY to screenName.plus("_Android")).apply {
                    if (!context.isNullOrBlank()) put(LABEL_CONTEXT, context)
                }
            )
        }
    }

}