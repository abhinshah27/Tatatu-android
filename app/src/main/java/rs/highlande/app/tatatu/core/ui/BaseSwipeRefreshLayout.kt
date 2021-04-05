package rs.highlande.app.tatatu.core.ui

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import rs.highlande.app.tatatu.R

/**
 * Inits a [SwipeRefreshLayout] with default TaTaTu config.
 * @author mbaldrighi on 2019-07-03.
 */
class BaseSwipeRefreshLayout : SwipeRefreshLayout {

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setColorSchemeResources(R.color.colorAccent)
        setDistanceToTriggerSync(200)
        setSlingshotDistance(100)
    }

}