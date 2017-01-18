package net.yusukezzz.ssmtc.util.picasso

import android.graphics.*
import android.graphics.Bitmap.Config
import com.squareup.picasso.Transformation

// https://gist.github.com/aprock/6213395
class RoundedTransformation(private val radiusDp: Int, private val marginDp: Int = 0): Transformation {
    private val KEY: String = "rounded(radius=$radiusDp, margin=$marginDp)"

    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val output = Bitmap.createBitmap(source.width, source.height, Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawRoundRect(
            RectF(marginDp.toFloat(), marginDp.toFloat(), (source.width - marginDp).toFloat(), (source.height - marginDp).toFloat()),
            radiusDp.toFloat(),
            radiusDp.toFloat(),
            paint)

        source.recycle()

        return output
    }

    override fun key(): String = KEY
}
