package net.yusukezzz.ssmtc.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.EXACTLY
import android.widget.ImageView
import net.yusukezzz.ssmtc.R

class AspectRatioImageView: ImageView {
    companion object {
        val DEFAULT_WIDTH_RATIO: Int = 16
        val DEFAULT_HEIGHT_RATIO: Int = 9
    }

    private var widthRatio: Int = 1
    private var heightRatio: Int = 1

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        val arr = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioImageView)
        widthRatio = arr.getInteger(R.styleable.AspectRatioImageView_widthRatio, DEFAULT_WIDTH_RATIO)
        heightRatio = arr.getInteger(R.styleable.AspectRatioImageView_heightRatio, DEFAULT_HEIGHT_RATIO)
        arr.recycle()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode == EXACTLY) {
            if (heightMode != EXACTLY) {
                heightSize = Math.round(widthSize * 1f / widthRatio * heightRatio)
            }
        } else if (heightMode == EXACTLY) {
            widthSize = Math.round(heightSize * 1f / heightRatio * widthRatio)
        } else {
            throw IllegalStateException("Either width or height must be EXACTLY.")
        }

        val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, EXACTLY)
        val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, EXACTLY)

        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec)
    }
}
