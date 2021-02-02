package net.yusukezzz.ssmtc.ui.media.photo.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.databinding.PhotoGalleryPageBinding
import net.yusukezzz.ssmtc.util.toast

class GalleryPageAdapter(private val context: Context, private val images: List<Media>) :
    PagerAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun isViewFromObject(view: View, obj: Any): Boolean = (view == obj)

    override fun getCount(): Int = images.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any =
        PhotoGalleryPageBinding.inflate(inflater).apply {
            val image = images[position]
            Picasso.get().load(image.largeUrl)
                .config(android.graphics.Bitmap.Config.ARGB_8888)
                .fit().centerInside()
                .into(pageImage, object : Callback {
                    override fun onSuccess() {
                        loadingBar.visibility = View.GONE
                    }

                    override fun onError(e: Exception) {
                        loadingBar.visibility = View.GONE
                        context.toast(R.string.gallery_load_fail)
                    }
                })

            container.addView(root)
        }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) =
        container.removeView((obj as View))
}
