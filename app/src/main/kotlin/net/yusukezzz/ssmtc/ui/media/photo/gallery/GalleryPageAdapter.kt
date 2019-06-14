package net.yusukezzz.ssmtc.ui.media.photo.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.photo_gallery_page.view.*
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.Media
import net.yusukezzz.ssmtc.util.toast

class GalleryPageAdapter(private val context: Context, private val images: List<Media>) : PagerAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun isViewFromObject(view: View, obj: Any): Boolean = (view == obj)

    override fun getCount(): Int = images.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = inflater.inflate(R.layout.photo_gallery_page, container, false)
        val media = images[position]

        Picasso.get().load(media.large_url)
            .config(android.graphics.Bitmap.Config.ARGB_8888)
            .fit().centerInside()
            .into(view.page_image, object : Callback {
                override fun onSuccess() {
                    view.loading_bar.visibility = View.GONE
                }

                override fun onError(e: Exception) {
                    view.loading_bar.visibility = View.GONE
                    context.toast(R.string.gallery_load_fail)
                }
            })

        container.addView(view)

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) = container.removeView((obj as View))
}
