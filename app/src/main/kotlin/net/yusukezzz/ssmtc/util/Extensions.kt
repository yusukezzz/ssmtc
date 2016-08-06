package net.yusukezzz.ssmtc.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
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

fun AppCompatActivity.toast(message: String?) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
fun Fragment.toast(message: String?) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

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
