package net.yusukezzz.ssmtc.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
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

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
fun Context.toast(error: Throwable) {
    println(error)
    toast(error.toString())
}

fun Context.getVectorDrawable(id: Int, tint: Int? = null): VectorDrawable {
    val drawable = getDrawable(id) as VectorDrawable
    tint?.let { drawable.setTint(ContextCompat.getColor(this, it)) }

    return drawable
}

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
