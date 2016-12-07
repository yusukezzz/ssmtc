package net.yusukezzz.ssmtc.util.picasso

import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso

object PicassoUtil {
    fun clean(view: View): Unit {
        if (view is ImageView) {
            Picasso.with(view.context).cancelRequest(view)
            view.setImageDrawable(null)
        }
    }
}
