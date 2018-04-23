package net.yusukezzz.ssmtc.util.picasso

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Transformation
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.User
import net.yusukezzz.ssmtc.util.Rszio
import net.yusukezzz.ssmtc.util.getVectorDrawable
import java.io.File

object PicassoUtil {
    private const val THUMBNAIL_IMAGE_TAG = "thumbnail_image_tag"
    private const val ROUNDED_CORNER_RADIUS = 8

    private val rounded: Transformation = RoundedTransformation(ROUNDED_CORNER_RADIUS)

    fun cancel(view: View) {
        if (view is ImageView) {
            Picasso.get().cancelRequest(view)
            view.setImageDrawable(null)
        }
    }

    fun userIcon(user: User, imgView: ImageView) {
        Picasso.get()
            .load(user.profileImageUrl)
            .priority(Picasso.Priority.HIGH)
            .fit().centerCrop()
            .transform(rounded)
            .into(imgView)
    }

    fun thumbnail(path: String, imgView: ImageView) {
        Picasso.get().loadFrom(path)
            .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
            .into(imgView)
    }

    fun opengraph(path: String, imgView: ImageView) {
        val ph = imgView.context.getVectorDrawable(R.drawable.og_placeholder, R.color.light_grey)
        // use zimage.io resized url
        val url = if (path.startsWith("http")) Rszio.url(path) else ""
        Picasso.get().loadFrom(url)
            .priority(Picasso.Priority.LOW)
            .placeholder(ph)
            .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
            .into(imgView)
    }

    fun resumeThumbnail() = Picasso.get().resumeTag(THUMBNAIL_IMAGE_TAG)
    fun pauseThumbnail() = Picasso.get().pauseTag(THUMBNAIL_IMAGE_TAG)

    private fun Picasso.loadFrom(from: String): RequestCreator = if (from.startsWith("http")) {
        this.load(Uri.parse(from))
    } else {
        this.load(File(from))
    }
}
