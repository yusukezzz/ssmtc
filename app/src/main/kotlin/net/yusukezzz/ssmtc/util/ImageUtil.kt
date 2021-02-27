package net.yusukezzz.ssmtc.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ImageUtil(
    private val context: Context,
    private val reqWidth: Int = DEFAULT_WIDTH,
    private val reqHeight: Int = DEFAULT_HEIGHT,
    private val quality: Int = DEFAULT_QUALITY
) {
    companion object {
        const val DEFAULT_QUALITY = 85
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 960
        const val OUTPUT_FILENAME = "tmp"
    }

    private val tmpDir: File = File(context.cacheDir, this.javaClass.simpleName)

    fun compress(uri: Uri): File {
        val format = determineFormat(uri)
        val output = File(tmpDir, "${OUTPUT_FILENAME}.${format.extension()}").apply {
            parentFile?.mkdirs()
            delete()
        }

        val bitmap = decodeSampledBitmapFromFile(uri)?.let {
            determineImageRotation(uri, it)
        } ?: throw RuntimeException("bitmap decode failed.")

        FileOutputStream(output).use {
            bitmap.compress(format, quality, it)
        }

        return output
    }

    private fun determineFormat(uri: Uri): Bitmap.CompressFormat {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        return when {
            mimeType == "image/png" -> Bitmap.CompressFormat.PNG
            mimeType == "image/webp" -> Bitmap.CompressFormat.WEBP
            mimeType.startsWith("image/") -> Bitmap.CompressFormat.JPEG
            else -> throw IllegalArgumentException("Illegal uri: $uri")
        }
    }

    private fun Bitmap.CompressFormat.extension() = when (this) {
        Bitmap.CompressFormat.PNG -> "png"
        Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "webp"
        else -> "jpg"
    }

    private fun open(uri: Uri): InputStream? = context.contentResolver.openInputStream(uri)?.buffered()

    private fun decodeSampledBitmapFromFile(uri: Uri): Bitmap? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            open(uri).use {
                BitmapFactory.decodeStream(it, null, this)
            }

            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            inJustDecodeBounds = false
            open(uri).use {
                BitmapFactory.decodeStream(it, null, this)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }


    private fun determineImageRotation(uri: Uri, bitmap: Bitmap): Bitmap? {
        return open(uri)?.use {
            val exif = ExifInterface(it)
            val matrix = Matrix()
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}

