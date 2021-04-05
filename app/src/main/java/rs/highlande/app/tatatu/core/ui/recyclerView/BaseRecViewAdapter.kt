package rs.highlande.app.tatatu.core.ui.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.core.util.clear


const val VIEW_TYPE_FIRST = 1000
const val VIEW_TYPE_LAST = 1001


/**
 * Base generic RecyclerView adapter.
 * Handles basic logic such as adding/removing items,
 * setting listener, binding ViewHolders.
 * Extend the adapter for appropriate use case.
 *
 * @param T  type of objects, which will be used in the adapter's data set
 * @param L  click listener [BaseRecyclerListener]
 * @param VH ViewHolder [BaseViewHolder]
 *
 * @author mbaldrighi on 2019-06-27.
 */
abstract class BaseRecViewAdapter<T, L : BaseRecyclerListener, VH : BaseViewHolder<T, L>>(
    private val clickListener: L?,
    protected val stableIds: Boolean = true
) : RecyclerView.Adapter<VH>() {

    private val items = mutableListOf<T>()

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
        val item = items[position]
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


    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return items.size
    }


    /**
     * Sets items to the adapter and notifies that data set has been changed.
     *
     * @param items items to set to the adapter
     * @throws IllegalArgumentException in case of setting `null` items
     */
    fun setItems(items: MutableList<T>?) {
        if (items == null) {
            throw IllegalArgumentException("Cannot set `null` item to the Recycler adapter")
        }
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    /**
     * Returns all items from the data set held by the adapter.
     *
     * @return All of items in this adapter.
     */
    fun getItems(): List<T> {
        return items
    }

    /**
     * Returns an items from the data set at a certain position.
     * @param position the location of the wanted element
     *
     * @return The item located at the expected position in this adapter.
     */
    fun getItem(position: Int): T {
        return items[position]
    }

    /**
     * Returns whether the provided item is first in the data set or not.
     *
     * @return True if the provided item is the first in the data set.
     */
    fun isItemFirst(item: T): Boolean {
        return getItems().indexOf(item) == 0
    }

    /**
     * Returns whether the provided item is last in the data set or not.
     *
     * @return True if the provided item is the last in the data set.
     */
    fun isItemLast(item: T): Boolean {
        return getItems().indexOf(item) == (itemCount - 1)
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
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    /**
     * Adds item to the end of the data set.
     * Notifies that item has been inserted.
     *
     * @param item item which has to be added to the adapter.
     */
    fun addAtStart(item: T?) {
        if (item == null) {
            throw IllegalArgumentException("Cannot add null item to the Recycler adapter")
        }
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun addAt(position: Int, item: T?) {
        if (item == null) {
            throw IllegalArgumentException("Cannot add null item to the Recycler adapter")
        }
        items.add(position, item)
        notifyItemInserted(position)
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
        this.items.addAll(items)
        notifyItemRangeInserted(this.items.size - items.size, items.size)
    }

    /**
     * Updates item at position
     *
     * @param position item position
     * @param newValue new item value
     */
    fun updateAt(position: Int, newItem: T) {
        items[position] = newItem
        notifyItemChanged(position)
    }

    /**
     * Updates item at position
     *
     * @param position item position
     * @param newValue new item value
     */
    fun updateRange(items: List<T>?) {
        if (items == null) {
            throw IllegalArgumentException("Cannot update `null` items to the Recycler adapter")
        }

        if (this.items.isEmpty()) addAll(items)
        else {
            val startIndex = if (items.isNotEmpty()) this.items.indexOf(items[0]) else -1
            if (startIndex > -1) {
                this.items.removeAll(items.toMutableList())
                notifyItemRangeRemoved(startIndex, items.size)

                this.items.addAll(startIndex, items)
                notifyItemRangeChanged(startIndex, items.size)
            }
        }
    }

    /**
     * Redraws an specific item
     * @param item item to be redraw
     */
    fun refreshItem(item: T) {
        val position = items.indexOf(item)
        notifyItemChanged(position)
    }

    /**
     * Clears all the items in the adapter.
     */
    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    /**
     * Removes an item from the adapter.
     * Notifies that item has been removed.
     *
     * @param item to be removed
     */
    fun remove(item: T) {
        val position = items.indexOf(item)
        if (position > -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
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
            items.removeAt(position)
            notifyItemRemoved(position)
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
 * Generic [RecyclerView.ViewHolder].
 *
 * @param itemView the root view of the item.
 */
abstract class BaseViewHolder<T, L : BaseRecyclerListener>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /**
     * Bind data to the item and set listener if needed.
     *
     * @param item     object, associated with the item.
     * @param listener listener a listener [BaseRecyclerListener] which has to b set at the item (if not `null`).
     */
    abstract fun onBind(item: T, @Nullable listener: L?)

    abstract fun getImageViewsToRecycle(): Set<ImageView?>
}

/**
 * Subclass of [BaseViewHolder] conceived to handle margins for the first and last item of the [RecyclerView].
 *
 * @param itemView     the root view of the item.
 * @param marginStart  the provided start/left margin to be assigned to itemview.
 * @param marginTop    the provided top margin to be assigned to itemview.
 * @param marginEnd    the provided end/right margin to be assigned to itemview.
 * @param marginBottom the provided bottom margin to be assigned to itemview.
 */
abstract class BaseViewHolderMargins<T, L : BaseRecyclerListener>(
    itemView: View,
    private val marginStart: Int? = null,
    private val marginTop: Int? = null,
    private val marginEnd: Int? = null,
    private val marginBottom: Int? = null
) : BaseViewHolder<T, L>(itemView) {

    /**
     * Bind data to the item and set listener if needed.
     *
     * @param item     object, associated with the item.
     * @param listener listener a listener [BaseRecyclerListener] which has to b set at the item (if not `null`).
     */
    override fun onBind(item: T, @Nullable listener: L?) {

        bindMargins(item, marginStart, marginTop, marginEnd, marginBottom)
    }

    /**
     * Bind data to the item and set listener if needed.
     *
     * @param mStart  object, associated with the item.
     * @param mTop    object, associated with the item.
     * @param mEnd    object, associated with the item.
     * @param mBottom object, associated with the item.
     */
    protected abstract fun bindMargins(
        item: T,
        @Nullable mStart: Int?,
        @Nullable mTop: Int?,
        @Nullable mEnd: Int?,
        @Nullable mBottom: Int?
    )

    /**
     * Clears margins previously set.
     * Called in [RecyclerView.Adapter.onViewRecycled].
     */
    abstract fun clearMargins()
}


interface OnItemClickListener<T> : BaseRecyclerListener {
    /**
     * Item has been clicked.
     *
     * @param item object associated with the clicked item.
     */
    fun onItemClick(item: T)
}

interface OnItemClickListenerWithView<T> : BaseRecyclerListener {
    /**
     * Item has been clicked.
     *
     * @param item object associated with the clicked item.
     */
    fun onItemClick(view: View, item: T)
}

interface BaseRecyclerListener