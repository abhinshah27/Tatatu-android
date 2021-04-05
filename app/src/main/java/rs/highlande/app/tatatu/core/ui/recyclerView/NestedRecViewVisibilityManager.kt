package rs.highlande.app.tatatu.core.ui.recyclerView

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.getVisiblePercentage
import java.util.concurrent.ConcurrentHashMap

/**
 * File holding everything related to the management of endless scrolling when the [RecyclerView] is
 * nested into another [ScrollView].
 * @author mbaldrighi on 2019-11-26.
 */

/**
 * Provides all properties and methods needed to handle endless scrolling in nested [ScrollView],
 * since for a nested [RecyclerView] all items present in the [RecyclerView.Adapter] are visible at
 * the same time.
 */
interface NestedItemVisibilityListener {

    /**
     * Map containing the references to ALL [RecyclerView.ViewHolder]s and their positions inside
     * the [RecyclerView.Adapter].
     */
    var visibleItemsMap: ConcurrentHashMap<RecyclerView.ViewHolder, Int>

    /**
     * Set containing all the CURRENTLY VISIBLE [RecyclerView.ViewHolder]s.
     */
    var visibleSet: MutableSet<RecyclerView.ViewHolder>

    /**
     * Real first visible item position found on the screen.
     * @see [LinearLayoutManager.findFirstVisibleItemPosition].
     */
    var firstVisiblePosition: Int

    /**
     * Real last visible item position found on the screen.
     * @see LinearLayoutManager.findLastVisibleItemPosition
     */
    var lastVisiblePosition: Int

    /**
     * Adds a new item to the general map.
     * @param holder The provided ViewHolder instance.
     * @param position The ViewHolder's position inside the adapter.
     */
    fun addVisibleItem(holder: RecyclerView.ViewHolder, position: Int) {
        visibleItemsMap[holder] = position
    }

    /**
     * Removes a provided item to the general map.
     * @param holder The provided ViewHolder instance.
     */
    fun removeVisibleItem(holder: RecyclerView.ViewHolder) {
        visibleItemsMap.remove(holder)
    }

    /**
     * Custom implementation of the related [RecyclerView.OnScrollListener] method.
     * @see RecyclerView.OnScrollListener.onScrolled
     */
    fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int)

}


/**
 * Class delegated for the handling of a nested endless scrolling.
 * @param scrollListener The current [EndlessScrollListener].
 */
class NestedItemVisibilityManager (
    private val scrollListener: EndlessScrollListener?
) : NestedItemVisibilityListener {

    override var visibleItemsMap: ConcurrentHashMap<RecyclerView.ViewHolder, Int> = ConcurrentHashMap()
    override var visibleSet: MutableSet<RecyclerView.ViewHolder> = mutableSetOf()
    override var firstVisiblePosition: Int = 0
    override var lastVisiblePosition: Int = 0

    private var mPreviousTotal = 0


    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int)  {

        val correctDelta =
            if (scrollListener?.orientation == EndlessScrollListener.Orientation.HORIZONTAL) dx else dy

        visibleItemsMap.forEach {
            val perc = it.key.itemView.getVisiblePercentage()
            if (perc > 0) {
                visibleSet.add(it.key)

                if (correctDelta > 0 && it.value > lastVisiblePosition) {
                    lastVisiblePosition = it.value
                } else if (correctDelta < 0 && it.value < firstVisiblePosition) {
                    firstVisiblePosition = it.value
                }
            }
            else {
                if (visibleSet.contains(it.key)) {

                    if (correctDelta > 0) {
                        firstVisiblePosition++
                    } else if (correctDelta < 0) {
                        lastVisiblePosition--
                    }
                }
                visibleSet.remove(it.key)
            }


            LogUtils.d(logTag, "testVISIBILITY: visibleSet=${visibleSet.size} and map with " +
                    "size=${visibleItemsMap.size}\nwith first=$firstVisiblePosition and " +
                    "last=$lastVisiblePosition")
        }

        val visibleItemCount = visibleSet.size



        val totalItemCount = recyclerView.layoutManager!!.itemCount

        // GridLayoutManager is child class of LinearLayoutManager
        val visibleItemForCalc =
            if (recyclerView.layoutManager is LinearLayoutManager) firstVisiblePosition else 0

        val scrollCondition =
            if (scrollListener?.type == EndlessScrollListener.Type.CHAT) correctDelta < 0 else correctDelta > 0

        // if layoutManager is GridLayoutManager -> threshold increased multiplying span count 2 times
        val visibleThreshold =
            (recyclerView.layoutManager as? GridLayoutManager)?.spanCount?.times(2) ?: 5

        val loadCondition =
            if (scrollListener?.type == EndlessScrollListener.Type.CHAT) visibleItemForCalc <= visibleThreshold
            else totalItemCount - visibleItemCount <= visibleItemForCalc + visibleThreshold


        if (scrollCondition) {

            if (scrollListener?.type == EndlessScrollListener.Type.CHAT && totalItemCount < mPreviousTotal) {
                this.mPreviousTotal = totalItemCount
                if (totalItemCount == 0) {
                    scrollListener.mLoading = true
                }
            }

            val canProceed =
                if (scrollListener?.canFetch != null)
                    totalItemCount > mPreviousTotal
                else
                    (totalItemCount % PAGINATION_SIZE == 0 && totalItemCount > mPreviousTotal)
            if (scrollListener?.mLoading == true && canProceed) {
                LogUtils.d(EndlessScrollListener.LOG_TAG, "Loading More\twith elements: $totalItemCount and $mPreviousTotal")
                scrollListener.mLoading = false
                mPreviousTotal = totalItemCount
            }
        }
        val canFetch = if (scrollListener?.canFetch != null) scrollListener.canFetch else totalItemCount % PAGINATION_SIZE == 0
        if (scrollListener?.mLoading == false && canFetch!! && loadCondition) {
            // End has been reached

            LogUtils.d(EndlessScrollListener.LOG_TAG, "Loading More\twith elements: $totalItemCount and dy: $correctDelta and canFetch = ${this.scrollListener.canFetch}")

            scrollListener.mLoading = true
            scrollListener.onLoadMore()
        }
    }

    companion object {
        val logTag = NestedItemVisibilityManager::class.java.simpleName
    }

}