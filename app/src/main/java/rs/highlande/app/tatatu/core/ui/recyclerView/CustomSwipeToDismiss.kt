package rs.highlande.app.tatatu.core.ui.recyclerView

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-04.
 */
abstract class CustomSwipeToDismiss(private val dragDirs: Int = 0, private val swipeDirs: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return dragDirs != 0
    }

}