package net.yusukezzz.ssmtc.ui.media.photo.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.daimajia.slider.library.Indicators.PagerIndicator
import com.daimajia.slider.library.Transformers.BaseTransformer
import kotlinx.android.synthetic.main.photo_gallery.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.ui.media.MediaBaseActivity

class GalleryActivity: MediaBaseActivity() {
    companion object {
        val ARG_IMAGES = "images"
        val ARG_START_POSITION = "start_position"

        fun newIntent(context: Context, images: List<String>, pos: Int): Intent =
            Intent(context, GalleryActivity::class.java).apply {
                putExtra(ARG_IMAGES, images.toTypedArray())
                putExtra(ARG_START_POSITION, pos)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo_gallery)

        val images = intent.getStringArrayExtra(ARG_IMAGES)
        val pos = intent.getIntExtra(ARG_START_POSITION, 0)

        slider.stopAutoCycle()
        images.toList().forEach {
            val view = GallerySliderView(applicationContext).image(it)
            slider.addSlider(view)
        }
        slider.currentPosition = pos

        if (1 == images.size) {
            // disable indicator and swipe pager
            slider.indicatorVisibility = PagerIndicator.IndicatorVisibility.Invisible
            slider.setPagerTransformer(false, object: BaseTransformer() {
                override fun onTransform(view: View?, position: Float) {
                    // do nothing
                }
            })
        }
    }
}
