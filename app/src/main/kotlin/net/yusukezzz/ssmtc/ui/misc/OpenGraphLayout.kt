package net.yusukezzz.ssmtc.ui.misc

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import net.yusukezzz.ssmtc.data.og.OpenGraph
import net.yusukezzz.ssmtc.data.og.OpenGraphLoadable
import net.yusukezzz.ssmtc.databinding.OpenGraphBinding
import net.yusukezzz.ssmtc.ui.timeline.TweetBinder.TweetItemListener
import net.yusukezzz.ssmtc.util.gone
import net.yusukezzz.ssmtc.util.picasso.PicassoUtil
import net.yusukezzz.ssmtc.util.visible

class OpenGraphLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
    FrameLayout(context, attrs, defStyle), OpenGraphLoadable {

    private lateinit var binding: OpenGraphBinding
    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = OpenGraphBinding.bind(this)
    }

    private lateinit var listener: TweetItemListener
    private var loaded: Boolean = false

    fun setListener(listener: TweetItemListener) {
        this.listener = listener
    }

    fun isLoaded(): Boolean = loaded

    fun reset() {
        PicassoUtil.cancel(binding.ogImage)
        this.gone()
        loaded = false
    }

    override fun onStart() {
        if (!loaded) {
            this.isClickable = false
            this.setOnClickListener { /* unregister listener */ }
            binding.ogContents.gone()
            binding.ogLoading.visible()
            this.visible()
        }
    }

    override fun onComplete(og: OpenGraph) {
        loaded = true
        binding.ogTitle.text = og.title
        binding.ogHost.text = Uri.parse(og.url).host
        PicassoUtil.opengraph(og.image, binding.ogImage)
        this.setOnClickListener {
            listener.onUrlClick(og.url)
        }
        binding.ogLoading.gone()
        binding.ogContents.visible()
        this.isClickable = true
    }
}
