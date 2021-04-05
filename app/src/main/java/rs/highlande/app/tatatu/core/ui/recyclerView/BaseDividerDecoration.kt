package rs.highlande.app.tatatu.core.ui.recyclerView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import kotlin.math.roundToInt

/**
 * Class holding the implementation of a custom [DividerItemDecoration].
 * @author mbaldrighi on 2019-07-03.
 */
class BaseDividerDecoration(
    context: Context,
    divider: Drawable? = null,
    @DimenRes padding: Int? = null,
    private val mOrientation: Int = VERTICAL
) : DividerItemDecoration(context, mOrientation) {

    private val mDivider = divider ?: context.resources.getDrawable(R.drawable.default_divider, null)
    private val customPadding = if (padding != null) context.resources.getDimensionPixelSize(padding) else null

    private val mBounds = Rect()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        if (parent.getChildAdapterPosition(view) == 0) {
            return
        }

        if (mDivider == null) {
            outRect.set(0, 0, 0, 0)
            return
        }

        if (mOrientation == VERTICAL) {
            outRect.set(0, 0, 0, mDivider.intrinsicHeight)
        } else {
            outRect.set(0, 0, mDivider.intrinsicWidth, 0)
        }
    }


    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {

        if (parent.layoutManager == null || mDivider == null) {
            return
        }

        if (mOrientation == VERTICAL) {
            drawVertical(canvas, parent)
        } else {
            drawHorizontal(canvas, parent)
        }


//        // if a custom padding value is specified, then it will be used, otherwise the parent RecView padding is used
//        val dividerLeft = customPadding ?: parent.paddingLeft
//        val dividerRight = parent.width - (customPadding ?: parent.paddingRight)
//
//        val childCount = parent.childCount
//        for (i in 0 until childCount - 1) {
//            val child = parent.getChildAt(i)
//
//            val params = child.layoutParams as RecyclerView.LayoutParams
//
//            val dividerTop = child.bottom + params.bottomMargin
//            val dividerBottom = dividerTop + divider.intrinsicHeight
//
//            divider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
//            divider.draw(canvas)
//        }

    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {

        canvas.save()
        val left: Int
        val right: Int

        if (parent.clipToPadding) {
            // if a custom padding value is specified, then it will be used, otherwise the parent RecView padding is used
            left = customPadding ?: parent.paddingLeft
            right = parent.width - (customPadding ?: parent.paddingRight)
            canvas.clipRect(
                left, parent.paddingTop, right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }

        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, mBounds)
            val bottom = mBounds.bottom + child.translationY.roundToInt()
            val top = bottom - mDivider.intrinsicHeight
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        canvas.save()
        val top: Int
        val bottom: Int

        if (parent.clipToPadding) {
            top = customPadding ?: parent.paddingTop
            bottom = parent.height - (customPadding ?: parent.paddingBottom)
            canvas.clipRect(
                parent.paddingLeft, top,
                parent.width - parent.paddingRight, bottom
            )
        } else {
            top = 0
            bottom = parent.height
        }

        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.layoutManager!!.getDecoratedBoundsWithMargins(child, mBounds)
            val right = mBounds.right + child.translationX.roundToInt()
            val left = right - mDivider.intrinsicWidth
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(canvas)
        }
        canvas.restore()
    }

}