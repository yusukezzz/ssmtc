package net.yusukezzz.ssmtc.ui.media.photo.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.daimajia.slider.library.SliderTypes.BaseSliderView
import net.yusukezzz.ssmtc.R

class GallerySliderView(context: Context): BaseSliderView(context) {
    override fun getView(): View {
        val v = LayoutInflater.from(context).inflate(R.layout.photo_gallery_slider, null)
        val target = v.findViewById(R.id.slider_image) as ImageView
        scaleType = BaseSliderView.ScaleType.CenterInside
        bindEventAndShow(v, target)
        return v
    }
}
