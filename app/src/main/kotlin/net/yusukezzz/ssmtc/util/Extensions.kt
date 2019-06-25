package net.yusukezzz.ssmtc.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import okhttp3.MediaType
import okhttp3.RequestBody
import org.threeten.bp.OffsetDateTime
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

fun ViewGroup.inflate(resId: Int): View = LayoutInflater.from(context).inflate(resId, this, false)
fun ViewGroup.setView(resId: Int) = this.addView(inflate(resId), 0)
fun ViewGroup.children(func: (View) -> Unit) {
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
fun View.gone() = this.beVisibleIf(false)
fun View.visible() = this.beVisibleIf(true)

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
fun Context.toast(resId: Int) = this.toast(this.resources.getString(resId))
fun Context.toast(error: Throwable) {
    Log.e("net.yusukezzz.ssmtc", "ERROR", error)
    toast(error.toString())
}

fun Context.getCompatColor(id: Int): Int = ContextCompat.getColor(this, id)
fun Context.getCompatDrawable(id: Int): Drawable = ContextCompat.getDrawable(this, id)!!
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
    return cursor.use {
        it.moveToFirst()
        cursor.getString(cursor.getColumnIndex(column))
    }
}

fun File.mimeType(): String {
    return if (extension.isEmpty()) {
        "application/octet-stream" // default
    } else {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}

fun File.toRequestBody(): RequestBody = RequestBody.create(MediaType.parse(mimeType()), this)

fun Throwable.prettyMarkdown(): String {
    val c = cause?.let { "\n[cause] ${it.message}\n${it.stackTrace.joinToString("\n")}" } ?: ""
    return """|```[error] ${OffsetDateTime.now()}
            |Message:
            |$message
            |
            |Stacktrace:
            |${stackTrace.joinToString("\n")}
            |
            |Cause:
            |$c```
        """.trimMargin()
}

// https://qiita.com/nukka123/items/205c93c72a35a17a5c3b
fun String.truncateBytes(bytes: Int): String {
    val charset = StandardCharsets.UTF_8
    val encoder = charset.newEncoder()
        .onMalformedInput(CodingErrorAction.IGNORE)
        .onUnmappableCharacter(CodingErrorAction.IGNORE)
        .reset()

    val estimate = this.length * (Math.ceil(encoder.maxBytesPerChar().toDouble()).toInt())
    if (estimate <= bytes) {
        return this
    }

    val srcBuffer = ByteBuffer.allocate(bytes)
    val res = encoder.encode(CharBuffer.wrap(this), srcBuffer, true)
    encoder.flush(srcBuffer)
    srcBuffer.flip()
    if (res.isUnderflow) {
        return this
    }

    val dstBuffer = CharBuffer.allocate(this.length)
    val decoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.IGNORE)
        .onUnmappableCharacter(CodingErrorAction.IGNORE)
        .reset()
    decoder.decode(srcBuffer, dstBuffer, true)
    decoder.flush(dstBuffer)
    dstBuffer.flip()

    return dstBuffer.toString()
}
