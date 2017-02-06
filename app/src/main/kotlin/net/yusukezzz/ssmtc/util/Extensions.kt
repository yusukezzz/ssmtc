package net.yusukezzz.ssmtc.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

fun ViewGroup.inflate(resId: Int): View = LayoutInflater.from(context).inflate(resId, this, false)
fun ViewGroup.setView(resId: Int): Unit = this.addView(inflate(resId), 0)
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

fun View.beVisibleIf(visible: Boolean) = if (visible) this.visibility = View.VISIBLE else this.visibility = View.GONE
fun View.hide() = this.beVisibleIf(false)
fun View.show() = this.beVisibleIf(true)

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
fun Context.toast(resId: Int) = this.toast(this.resources.getString(resId))
fun Context.toast(error: Throwable) {
    Log.e("net.yusukezzz.ssmtc", "ERROR", error)
    toast(error.toString())
}

fun Context.getCompatColor(id: Int) = ContextCompat.getColor(this, id)
fun Context.getVectorDrawable(id: Int, tint: Int? = null): VectorDrawable {
    val drawable = getDrawable(id) as VectorDrawable
    tint?.let { drawable.setTint(this.getCompatColor(it)) }

    return drawable
}

fun Context.resolveAttributeId(resId: Int): Int {
    val attr = TypedValue()
    this.theme.resolveAttribute(resId, attr, true)
    return attr.resourceId
}


fun Intent.getLongExtraOrNull(key: String): Long? {
    return if (this.hasExtra(key)) {
        this.getLongExtra(key, 0)
    } else {
        null
    }
}
fun Intent.getStringExtraOrNull(key: String): String? {
    return if (this.hasExtra(key)) {
        this.getStringExtra(key)
    } else {
        null
    }
}

fun Intent.getExtraStreamOrNull(): Any? = this.extras?.get(Intent.EXTRA_STREAM)

fun VectorDrawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)

    return bitmap
}

fun ContentResolver.getImagePath(content: Uri): String {
    val column = MediaStore.Images.Media.DATA
    val cursor = this.query(content, arrayOf(column), null, null, null) ?: return content.path
    val realpath = cursor.use {
        it.moveToFirst()
        cursor.getString(cursor.getColumnIndex(column))
    }

    return realpath
}

fun File.mimeType(): String {
    return if (extension.isEmpty()) {
        "application/octet-stream" // default
    } else {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}

fun File.toRequestBody(): RequestBody = RequestBody.create(MediaType.parse(mimeType()), this)
