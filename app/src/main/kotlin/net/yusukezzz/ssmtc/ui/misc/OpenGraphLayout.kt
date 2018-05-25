package net.yusukezzz.ssmtc.ui.misc

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.open_graph.view.*
import net.yusukezzz.ssmtc.data.og.OpenGraph
import net.yusukezzz.ssmtc.data.og.OpenGraphLoadable
import net.yusukezzz.ssmtc.ui.timeline.TweetItemView
import net.yusukezzz.ssmtc.util.gone
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import net.yusukezzz.ssmtc.util.visible

class OpenGraphLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attrs, defStyle), OpenGraphLoadable {

    private lateinit var listener: TweetItemView.TweetItemListener
    private var loaded: Boolean = false

    fun setListener(listener: TweetItemView.TweetItemListener) {
        this.listener = listener
    }

    fun isLoaded(): Boolean = loaded

    fun reset() {
        PicassoUtil.cancel(og_image)
        this.gone()
        loaded = false
    }

    override fun onStart() {
        if (!loaded) {
            this.isClickable = false
            this.setOnClickListener { /* unregister listener */ }
            og_contents.gone()
            og_loading.visible()
            this.visible()
        }
    }

    override fun onComplete(og: OpenGraph) {
        loaded = true
        og_title.text = og.title
        og_host.text = Uri.parse(og.url).host
        PicassoUtil.opengraph(og.image, og_image)
        this.setOnClickListener {
            listener.onUrlClick(og.url)
        }
        og_loading.gone()
        og_contents.visible()
        this.isClickable = true
    }
}
