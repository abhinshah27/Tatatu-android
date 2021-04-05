package rs.highlande.app.tatatu.core.ui.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import rs.highlande.app.tatatu.core.util.clear

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-16.
 */
abstract class BaseRecViewDiffAdapter<T, L : BaseRecyclerListener, VH : BaseViewHolder<T, L>>(
    diffUtilCallback: BaseDiffUtilCallback<T>,
    private val clickListener: L,
    protected val stableIds: Boolean = true
) : ListAdapter<T, VH>(diffUtilCallback) {

    private var isAttached: Boolean = false


    /**
     * To be implemented in as specific adapter
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the itemView to reflect the item at the given
     * position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.onBind(item, clickListener)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        isAttached = true
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        isAttached = false
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)

        if (isAttached) {
            if (holder is BaseViewHolderMargins<*, *>)
                holder.clearMargins()

            with(holder.getImageViewsToRecycle()) {
                for (view in this) view?.clear()
            }
        }
    }


//    /**
//     * Called by user to trigger DiffUtil ops and display the data.
//     *
//     * @param list The items to be displayed.
//     */
//    override fun submitList(list: List<T>?) {
//        super.submitList(if (list != null) ArrayList(list) else null)
//    }


    /**
     * Returns all items from the data set held by the adapter.
     *
     * @return All of items in this adapter.
     */
    fun getItems(): List<T> {
        return mutableListOf<T>().apply { addAll(currentList) }
    }

    /**
     * Returns an items from the data set at a certain position.
     * @param position the location of the wanted element
     *
     * @return The item located at the expected position in this adapter.
     */
    fun getSingleItem(position: Int): T {
        return currentList[position]
    }

    /**
     * Returns whether the provided item is first in the data set or not.
     *
     * @return True if the provided item is the first in the data set.
     */
    fun isItemFirst(item: T): Boolean {
        return currentList.indexOf(item) == 0
    }

    /**
     * Returns whether the provided item is last in the data set or not.
     *
     * @return True if the provided item is the last in the data set.
     */
    fun isItemLast(item: T): Boolean {
        return currentList.indexOf(item) == (itemCount - 1)
    }

    /**
     * Adds item to the end of the data set.
     * Notifies that item has been inserted.
     *
     * @param item item which has to be added to the adapter.
     */
    fun add(item: T?) {
        if (item == null) {
            throw IllegalArgumentException("Cannot add null item to the Recycler adapter")
        }

        submitList(
            mutableListOf<T>().apply {
                addAll(currentList)
                add(item)
            }
        )
    }

    /**
     * Adds list of items to the end of the adapter's data set.
     * Notifies that item has been inserted.
     *
     * @param items items which has to be added to the adapter.
     */
    fun addAll(items: List<T>?) {
        if (items == null) {
            throw IllegalArgumentException("Cannot add `null` items to the Recycler adapter")
        }

        submitList(
            mutableListOf<T>().apply {
                addAll(currentList)
                addAll(items)
            }
        )
    }

    /**
     * Clears all the items in the adapter.
     */
    fun clear() = submitList(mutableListOf())

    /**
     * Removes an item from the adapter.
     * Notifies that item has been removed.
     *
     * @param item to be removed
     */
    fun remove(item: T) {
        val position = currentList.indexOf(item)
        if (position > -1) {
            submitList(
                mutableListOf<T>().apply {
                    addAll(currentList)
                    removeAt(position)
                }
            )
        }
    }

    /**
     * Removes an item from the adapter at the given position.
     * Notifies that item has been removed.
     *
     * @param position of the item to be removed
     */
    fun removeAt(position: Int) {
        if (position > -1) {
            submitList(
                mutableListOf<T>().apply {
                    addAll(currentList)
                    removeAt(position)
                }
            )
        }
    }

    /**
     * Updates an item of the current data set not knowing its position.
     *
     * @param item the item to be updated.
     */
    fun updateItem(item: T) {
        val position = getItems().indexOf(item)

        //TODO: Sometimes this is called with an invalid item position, causing an exception. Check why is happening
        if (position >= 0) {
            mutableListOf<T>().apply {
                addAll(getItems())
                removeAt(position)
                add(position, item)
                submitList(this)
            }
        }
    }

    /**
     * Updates range of items.
     *
     * @param items the items to be updated.
     */
    fun updateRange(items: List<T>?) {
        if (items == null) {
            throw IllegalArgumentException("Cannot update `null` items to the Recycler adapter")
        }

        if (currentList.isEmpty()) addAll(items)
        else {
            val startIndex = if (items.isNotEmpty()) currentList.indexOf(items[0]) else -1
            if (startIndex > -1) {
                mutableListOf<T>().apply {
                    addAll(currentList)
                    removeAll(items)
                    addAll(startIndex, items)
                    submitList(this)
                }
            }
        }
    }

    /**
     * Returns whether adapter is empty or not.
     *
     * @return `true` if adapter is empty or `false` otherwise
     */
    fun isEmpty(): Boolean {
        return itemCount == 0
    }

    /**
     * Inflates a view.
     *
     * @param layout       layout to be inflated
     * @param parent       container where to inflate
     * @param attachToRoot whether to attach to root or not
     * @return inflated View
     */
    @NonNull
    protected fun inflate(@LayoutRes layout: Int, parent: ViewGroup, attachToRoot: Boolean): View {
        return LayoutInflater.from(parent.context).inflate(layout, parent, attachToRoot)
    }

    /**
     * Inflates a view.
     *
     * @param layout layout to me inflater
     * @param parent container where to inflate
     * @return inflated View
     */
    @NonNull
    protected fun inflate(@LayoutRes layout: Int, parent: ViewGroup): View {
        return inflate(layout, parent, false)
    }

}


/**
 * Base child class of [DiffUtil.ItemCallback].
 * @author mbaldrighi on 2019-07-16.
 */
abstract class BaseDiffUtilCallback<T> : DiffUtil.ItemCallback<T>() {

    abstract override fun areItemsTheSame(oldItem: T, newItem: T): Boolean
    abstract override fun areContentsTheSame(oldItem: T, newItem: T): Boolean

}