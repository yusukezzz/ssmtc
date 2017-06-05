package net.yusukezzz.ssmtc.ui.misc

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.open_graph.view.*
import net.yusukezzz.ssmtc.data.og.OpenGraph
import net.yusukezzz.ssmtc.data.og.OpenGraphLoadable
import net.yusukezzz.ssmtc.ui.timeline.TweetItemView
import net.yusukezzz.ssmtc.util.gone
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import net.yusukezzz.ssmtc.util.visible

class OpenGraphLayout : FrameLayout, OpenGraphLoadable {
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(context, attrs, defStyle)

    private lateinit var listener: TweetItemView.TweetItemListener

    fun setListener(listener: TweetItemView.TweetItemListener) {
        this.listener = listener
    }

    fun isEmpty(): Boolean = (this.visibility == View.GONE)

    fun reset() {
        PicassoUtil.cancel(og_image)
        this.gone()
    }

    override fun onStart() {
        og_contents.gone()
        og_loading.gone()
        this.visible()
    }

    override fun onLoading() {
        og_loading.visible()
    }

    override fun onComplete(og: OpenGraph) {
        og_title.text = og.title
        og_host.text = Uri.parse(og.url).host
        PicassoUtil.opengraph(og.image, og_image)
        this.setOnClickListener {
            listener.onUrlClick(og.url)
        }
        og_loading.gone()
        og_contents.visible()
    }
}
