package net.yusukezzz.ssmtc.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

fun ViewGroup.inflate(id: Int, attachToRoot: Boolean = false)
    = LayoutInflater.from(context).inflate(id, this, attachToRoot)

fun ViewGroup.children(func: (View) -> Unit): Unit {
    val max = this.childCount - 1
    (0..max).forEach {
        val child = this.getChildAt(it)
        if (child is ViewGroup) {
            child.children(func)
        } else {
            func(child)
        }
    }
}

fun Context.getVectorDrawable(id: Int, tint: Int? = null): VectorDrawable {
    val drawable = getDrawable(id) as VectorDrawable
    if (null != tint) {
        drawable.setTint(ContextCompat.getColor(this, tint))
    }

    return drawable
}

fun VectorDrawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)

    return bitmap
}

fun File.mimeType(): String {
    if (extension.isEmpty()) {
        return "application/octet-stream" // default
    } else {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}

fun File.toRequestBody(): RequestBody = RequestBody.create(MediaType.parse(mimeType()), this)
