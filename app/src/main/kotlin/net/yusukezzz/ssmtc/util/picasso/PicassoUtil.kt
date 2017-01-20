package net.yusukezzz.ssmtc.util.picasso

import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import net.yusukezzz.ssmtc.data.json.User
import java.io.File

object PicassoUtil {
    const val THUMBNAIL_IMAGE_TAG = "thumbnail_image_tag"

    private val rounded: Transformation = RoundedTransformation(8)

    fun clean(view: View): Unit {
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

    fun thumbnail(url: String, imgView: ImageView) {
        Picasso.with(imgView.context).load(url)
            .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
            .into(imgView)
    }

    fun thumbnail(file: File, imgView: ImageView) {
        Picasso.with(imgView.context).load(file)
            .fit().centerCrop().tag(THUMBNAIL_IMAGE_TAG)
            .into(imgView)
    }
}
