package rs.highlande.app.tatatu.core.ui.recyclerView

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE

/**
 * Abstract class serving as custom [RecyclerView.OnScrollListener] for endless scrolling with data loading.
 * @author mbaldrighi on 2019-07-16.
 */
abstract class EndlessScrollListener (
    internal val type: Type = Type.GENERAL,
    internal val orientation: Orientation = Orientation.VERTICAL
) : RecyclerView.OnScrollListener() {

    companion object {
        val LOG_TAG = EndlessScrollListener::class.java.simpleName
    }

    enum class Type {
       GENERAL, CHAT
    }

    enum class Orientation {
        VERTICAL, HORIZONTAL
    }


    var canFetch: Boolean?

    /**
     * The total number of items in the dataset after the last load
     */
    private var mPreviousTotal = 0

    /**
     * True if we are still waiting for the last set of data to load.
     */
    internal var mLoading = true


    init {
        canFetch = if (type == Type.CHAT) true else null
    }


    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val correctDelta = if (orientation == Orientation.HORIZONTAL) dx else dy
        val visibleItemCount = recyclerView.childCount
        val totalItemCount = recyclerView.layoutManager!!.itemCount

        // GridLayoutManager is child class of LinearLayoutManager
        val visibleItemForCalc =
            (recyclerView.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0

        val scrollCondition = if (type == Type.CHAT) correctDelta < 0 else correctDelta > 0

        // if layoutManager is GridLayoutManager -> threshold increased multiplying span count 2 times
        val visibleThreshold =
            (recyclerView.layoutManager as? GridLayoutManager)?.spanCount?.times(2) ?: 5

        val loadCondition =
            if (type == Type.CHAT) visibleItemForCalc <= visibleThreshold
            else totalItemCount - visibleItemCount <= visibleItemForCalc + visibleThreshold


        if (scrollCondition) {

            if (type == Type.CHAT && totalItemCount < mPreviousTotal) {
                this.mPreviousTotal = totalItemCount
                if (totalItemCount == 0) {
                    mLoading = true
                }
            }

            val canProceed =
                if (canFetch != null)
                    totalItemCount > mPreviousTotal
                else
                    (totalItemCount % PAGINATION_SIZE == 0 && totalItemCount > mPreviousTotal)
            if (mLoading && canProceed) {
                LogUtils.d(LOG_TAG, "Loading More\twith elements: $totalItemCount and $mPreviousTotal")
                mLoading = false
                mPreviousTotal = totalItemCount
            }
        }
        val canFetch = if (canFetch != null) canFetch else totalItemCount % PAGINATION_SIZE == 0
        if (!mLoading && canFetch!! && loadCondition) {
            // End has been reached

            LogUtils.d(LOG_TAG, "Loading More\twith elements: $totalItemCount and dy: $correctDelta and canFetch = ${this.canFetch}")

            mLoading = true
            onLoadMore()
        }
    }

    abstract fun onLoadMore()

}
