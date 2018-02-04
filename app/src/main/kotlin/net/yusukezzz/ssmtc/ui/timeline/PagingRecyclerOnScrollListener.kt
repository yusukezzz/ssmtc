package net.yusukezzz.ssmtc.ui.timeline

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil

// https://gist.github.com/ssinss/e06f12ef66c51252563e
class PagingRecyclerOnScrollListener(private val mLinearLayoutManager: LinearLayoutManager) : RecyclerView.OnScrollListener() {
    interface ScrollListener {
        fun onLoadMore()
    }

    private var disable = false // True if we can not paging any further
    private var loading = true // True if we are still waiting for the last set of data to load.
    private val visibleThreshold = 5 // The minimum amount of items to have below your current scroll position before loading more.

    private var listener: ScrollListener? = null

    private val totalItemCount: Int
        get() = mLinearLayoutManager.itemCount

    fun setLoadMoreListener(listener: ScrollListener) {
        this.listener = listener
    }

    fun reset() {
        disable = false
        loading = true
    }

    fun stopLoading() {
        loading = false
    }

    fun disable() {
        disable = true
        loading = false
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_TOUCH_SCROLL) {
            PicassoUtil.resumeThumbnail()
        } else {
            PicassoUtil.pauseThumbnail()
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = recyclerView.childCount
        val firstVisibleItemPos = mLinearLayoutManager.findFirstVisibleItemPosition()

        if (!disable && !loading && totalItemCount - visibleItemCount <= firstVisibleItemPos + visibleThreshold) {
            // End has been reached

            // Do something
            listener?.onLoadMore()

            loading = true
        }
    }
}
