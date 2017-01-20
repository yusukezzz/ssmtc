package net.yusukezzz.ssmtc.ui.timeline

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil

// https://gist.github.com/ssinss/e06f12ef66c51252563e
class EndlessRecyclerOnScrollListener(private val context: Context,
                                      private val mLinearLayoutManager: LinearLayoutManager): RecyclerView.OnScrollListener() {
    interface ScrollListener {
        fun onLoadMore()
    }

    private var loading = true // True if we are still waiting for the last set of data to load.
    private val visibleThreshold = 5 // The minimum amount of items to have below your current scroll position before loading more.

    private var listener: ScrollListener? = null

    private val totalItemCount: Int
        get() = mLinearLayoutManager.itemCount

    fun setLoadMoreListener(listener: ScrollListener) {
        this.listener = listener
    }

    fun reset() {
        loading = true
    }

    fun stopLoading() {
        loading = false
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        val picasso = Picasso.with(context)
        if (newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_TOUCH_SCROLL) {
            picasso.resumeTag(PicassoUtil.THUMBNAIL_IMAGE_TAG)
        } else {
            picasso.pauseTag(PicassoUtil.THUMBNAIL_IMAGE_TAG)
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = recyclerView.childCount
        val firstVisibleItemPos = mLinearLayoutManager.findFirstVisibleItemPosition()

        if (!loading && totalItemCount - visibleItemCount <= firstVisibleItemPos + visibleThreshold) {
            // End has been reached

            // Do something
            listener?.onLoadMore()

            loading = true
        }
    }
}
