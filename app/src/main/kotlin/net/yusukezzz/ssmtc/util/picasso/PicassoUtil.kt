package net.yusukezzz.ssmtc.util.picasso

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Transformation
import net.yusukezzz.ssmtc.data.json.User
import java.io.File

object PicassoUtil {
    const val THUMBNAIL_IMAGE_TAG = "thumbnail_image_tag"

    private val rounded: Transformation = RoundedTransformation(8)

    fun cancel(view: View): Unit {
        if (view is ImageView) {
            Picasso.with(view.context).cancelRequest(view)
            view.setImageDrawable(null)
        }
    }

    fun userIcon(user: User, imgView: ImageView) {
        Picasso.with(imgView.context)
            .load(user.profileImageUrl)
            .priority(Picasso.Priority.HIGH)
            .fit().centerCrop()
            .transform(rounded)
            .into(imgView)
    }

    fun thumbnail(path: String, imgView: ImageView) {
        Picasso.with(imgView.context).loadFrom(path)
            .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
            .into(imgView)
    }

    private fun Picasso.loadFrom(from: String): RequestCreator = if (from.startsWith("http")) {
        this.load(Uri.parse(from))
    } else {
        this.load(File(from))
    }
}
