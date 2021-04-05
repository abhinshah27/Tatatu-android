package rs.highlande.app.tatatu.core.ui.recyclerView

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
abstract class CommonScrollListener: RecyclerView.OnScrollListener() {

    private var isLoading = false

    var isUpdateOnScrollEnabled = true

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (isLoading || !isUpdateOnScrollEnabled) {
            return
        }

        if (!hasMoreItems()) {
            return
        }

        val visibleItemCount = recyclerView.layoutManager!!.childCount
        val totalItemCount = recyclerView.layoutManager!!.itemCount
        val firstVisibleItem =
            (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        val visibleThreshold = 10

        if (totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
            isLoading = true
            startLoading()
        }
    }

    abstract fun hasMoreItems(): Boolean

    abstract fun startLoading()

    open fun endLoading() {
        isLoading = false
    }

}