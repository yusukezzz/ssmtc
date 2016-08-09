package net.yusukezzz.ssmtc.screens.timeline

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import com.squareup.picasso.Picasso

// https://gist.github.com/ssinss/e06f12ef66c51252563e
class EndlessRecyclerOnScrollListener(private val context: Context,
                                      private val mLinearLayoutManager: LinearLayoutManager): RecyclerView.OnScrollListener() {
    interface ScrollListener {
        fun onLoadMore(currentPage: Int)
    }

    private var previousTotal = 0 // The total number of items in the dataset after the last load
    private var loading = true // True if we are still waiting for the last set of data to load.
    private val visibleThreshold = 5 // The minimum amount of items to have below your current scroll position before loading more.
    internal var firstVisibleItem: Int = 0
    internal var visibleItemCount: Int = 0
    internal var totalItemCount: Int = 0

    private var currentPage = 1
    private var listener: ScrollListener? = null

    fun setLoadMoreListener(listener: ScrollListener) {
        this.listener = listener
    }

    fun reset() {
        previousTotal = 0
        loading = true
        firstVisibleItem = 0
        visibleItemCount = 0
        totalItemCount = 0
    }

    fun forceLoadingStop() {
        // force stop when error occurred
        loading = false
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        val picasso = Picasso.with(context)
        if (newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_TOUCH_SCROLL) {
            picasso.resumeTag(TimelineAdapter.TweetViewHolder.LARGE_IMAGE_TAG)
        } else {
            picasso.pauseTag(TimelineAdapter.TweetViewHolder.LARGE_IMAGE_TAG)
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        visibleItemCount = recyclerView.childCount
        totalItemCount = mLinearLayoutManager.itemCount
        firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition()

        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
            // End has been reached

            // Do something
            currentPage++

            listener?.onLoadMore(currentPage)

            loading = true
        }
    }
}
