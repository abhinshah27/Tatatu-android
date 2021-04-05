package rs.highlande.app.tatatu.core.ui.recyclerView

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL


/**
 * Class holding the implementation of a custom [DividerItemDecoration].
 * @author mbaldrighi on 2019-07-03.
 */
class HorizSpaceDecoration(
    context: Context,
    @DimenRes padding: Int? = null,
    @DimenRes defaultPaddingLeft: Int? = null,
    @DimenRes defaultPaddingTop: Int? = null,
    @DimenRes defaultPaddingRight: Int? = null,
    @DimenRes defaultPaddingBottom: Int? = null,
    private val mOrientation: Int = VERTICAL
) : RecyclerView.ItemDecoration() {

    private val customPadding = if (padding != null) context.resources.getDimensionPixelSize(padding) else 0
    private val defaultPaddingLeft  = if (defaultPaddingLeft != null) context.resources.getDimensionPixelSize(defaultPaddingLeft) else 0
    private val defaultPaddingTop = if (defaultPaddingTop != null) context.resources.getDimensionPixelSize(defaultPaddingTop) else 0
    private val defaultPaddingRight = if (defaultPaddingRight != null) context.resources.getDimensionPixelSize(defaultPaddingRight) else 0
    private val defaultPaddingBottom = if (defaultPaddingBottom != null) context.resources.getDimensionPixelSize(defaultPaddingBottom) else 0

    private val mBounds = Rect()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)


        val dataSize = state.itemCount
        val position = parent.getChildAdapterPosition(view)

        if (mOrientation == VERTICAL) {
            if (dataSize > 0 && position == 0) {
                outRect.set(defaultPaddingLeft, customPadding, defaultPaddingRight, defaultPaddingBottom)
            }
            else if (dataSize > 0 && position == dataSize - 1) {
                outRect.set(defaultPaddingLeft, defaultPaddingTop, defaultPaddingRight, customPadding)
            }
            else {
                outRect.set(defaultPaddingLeft, defaultPaddingTop, defaultPaddingRight, defaultPaddingBottom)
            }
        }
        else {
            if (dataSize > 0 && position == 0) {
                outRect.set(customPadding, defaultPaddingTop, defaultPaddingRight, defaultPaddingBottom)
            }
            else if (dataSize > 0 && position == dataSize - 1) {
                outRect.set(defaultPaddingLeft, defaultPaddingTop, customPadding, defaultPaddingBottom)
            }
            else {
                outRect.set(defaultPaddingLeft, defaultPaddingTop, defaultPaddingRight, defaultPaddingBottom)
            }
        }
    }

}