package net.yusukezzz.ssmtc.ui.media.photo.gallery

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.photo_gallery_page.view.*
import net.yusukezzz.ssmtc.R

class GalleryPageAdapter(private val context: Context, private val images: List<String>) : PagerAdapter() {
    private val inflater: LayoutInflater

    init {
        inflater = LayoutInflater.from(context)
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj

    override fun getCount(): Int = images.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = inflater.inflate(R.layout.photo_gallery_page, container, false)
        val url = images[position]

        Picasso.with(context).load(url)
            .fit().centerInside()
            .into(view.page_image, object : Callback {
                override fun onSuccess() {
                    view.loading_bar.visibility = View.GONE
                }

                override fun onError() {
                }
            })

        container.addView(view)

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) = container.removeView((obj as View))
}
