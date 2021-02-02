package net.yusukezzz.ssmtc.util

import android.app.Activity
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import net.yusukezzz.ssmtc.R
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import kotlin.math.ceil

fun ViewGroup.inflate(resId: Int): View = LayoutInflater.from(context).inflate(resId, this, false)
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

fun View.beVisibleIf(visible: Boolean) =
    if (visible) this.visibility = View.VISIBLE else this.visibility = View.GONE

fun View.gone() = this.beVisibleIf(false)
fun View.visible() = this.beVisibleIf(true)

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
fun Context.toast(resId: Int) = this.toast(this.resources.getString(resId))
fun Context.toast(error: Throwable) {
    Log.e("net.yusukezzz.ssmtc", "ERROR", error)
    toast(error.toString())
}

fun Activity.snackbar(error: Throwable) {
    error.printStackTrace()
    this.findViewById<View>(R.id.content)?.let { view ->
        error.message?.let {
            Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
        }
    }
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
fun Intent.isImage(): Boolean = this.type?.startsWith("image/") == true
fun Intent.isVideo(): Boolean = this.type?.startsWith("video/") == true

fun VectorDrawable.toBitmap(): Bitmap {
    val bitmap =
        Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)

    return bitmap
}

fun ContentResolver.getSize(uri: Uri): Long =
    this.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null).use {
        it?.moveToFirst()
        it?.getLong(0)
    } ?: 0L

fun File.mimeType(): String {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: "application/octet-stream"
}

fun File.mediaType(): MediaType = this.mimeType().toMediaType()

fun Throwable.prettyMarkdown(): String =
    "```[error] ${OffsetDateTime.now()}\n\n${this.stackTraceToString()}```".trim()

// https://qiita.com/nukka123/items/205c93c72a35a17a5c3b
fun String.truncateBytes(bytes: Int): String {
    val charset = StandardCharsets.UTF_8
    val encoder = charset.newEncoder()
        .onMalformedInput(CodingErrorAction.IGNORE)
        .onUnmappableCharacter(CodingErrorAction.IGNORE)
        .reset()

    val estimate = this.length * (ceil(encoder.maxBytesPerChar().toDouble()).toInt())
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

fun CoroutineScope.launchUI(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job =
    launch(coroutineContext, start, block)

fun <T> CoroutineScope.async(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> =
    async(Dispatchers.IO, start, block)

suspend fun <T> withIO(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO, block)
