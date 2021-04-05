package rs.highlande.app.tatatu.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager


/**
 * Subclass of [ViewPager] made by default NON SCROLLABLE.
 *
 * The provided method [setScrollable()] provides a way to change this setting.
 *
 * @author mbaldrighi on 2019-06-24.
 */
class ViewPagerNoScroll(context: Context, attrs: AttributeSet? = null): ViewPager(context, attrs) {

    private var scrollEnable: Boolean = false


    /**
     * This method enables ViewPager's scrolling when
     * @param scroll
     */
    fun setScrollable(scroll: Boolean) {
        this.scrollEnable = scroll
    }


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return scrollEnable && super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return scrollEnable && super.onInterceptTouchEvent(ev)
    }


}