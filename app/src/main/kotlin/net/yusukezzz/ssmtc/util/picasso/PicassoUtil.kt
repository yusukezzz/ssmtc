package net.yusukezzz.ssmtc.util.picasso

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import net.yusukezzz.ssmtc.BuildConfig
import net.yusukezzz.ssmtc.R
import net.yusukezzz.ssmtc.data.api.model.User
import net.yusukezzz.ssmtc.util.getVectorDrawable
import java.net.URLEncoder

object PicassoUtil {
    private const val THUMBNAIL_IMAGE_TAG = "thumbnail_image_tag"
    private const val ROUNDED_CORNER_RADIUS = 8
    private const val HTTP_IMAGE_WIDTH = 300
    private const val HTTP_OGP_IMAGE_WIDTH = 200

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

    fun thumbnail(url: String, imgView: ImageView) = thumbnail(Uri.parse(url), imgView)

    fun thumbnail(uri: Uri, imgView: ImageView) {
        Picasso.get().load(resizedUri(uri, HTTP_IMAGE_WIDTH))
            .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
            .into(imgView)
    }

    fun opengraph(path: String, imgView: ImageView) {
        val ph = imgView.context.getVectorDrawable(R.drawable.og_placeholder, R.color.light_grey)
        Picasso.get().load(resizedUri(Uri.parse(path), HTTP_OGP_IMAGE_WIDTH))
            .priority(Picasso.Priority.LOW)
            .placeholder(ph)
            .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
            .into(imgView)
    }

    private fun resizedUri(uri: Uri, width: Int): Uri = if (uri.toString().startsWith("http")) {
        val url = BuildConfig.MY_API_BASE_URL + "/thumbnail/" +
                URLEncoder.encode(uri.toString(), "UTF-8") + "?w=$width"
        Uri.parse(url)
    } else {
        uri
    }

    fun resumeThumbnail() = Picasso.get().resumeTag(THUMBNAIL_IMAGE_TAG)
    fun pauseThumbnail() = Picasso.get().pauseTag(THUMBNAIL_IMAGE_TAG)
}
