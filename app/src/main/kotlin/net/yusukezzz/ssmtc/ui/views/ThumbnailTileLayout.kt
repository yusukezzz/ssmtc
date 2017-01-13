package net.yusukezzz.ssmtc.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

class ThumbnailTileLayout : ViewGroup {
    companion object {
        const val WIDTH_RATIO: Int = 16
        const val HEIGHT_RATIO: Int = 9
        const val MARGIN_DIP: Int = 1
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 0) {
            setMeasuredDimension(0, 0)
            return
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        if (widthMode != MeasureSpec.EXACTLY) {
            throw IllegalStateException("Must measure with an exact width")
        }

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = Math.round(widthSize * 1f / WIDTH_RATIO * HEIGHT_RATIO)
        setMeasuredDimension(widthSize, heightSize)

        val layoutWidth = widthSize - paddingLeft - paddingRight
        val layoutHeight = heightSize - paddingTop - paddingBottom

        val childWidth = calcChildSize(childCount, layoutWidth)
        val childHeight = calcChildSize(childCount, layoutHeight)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
            val childHeightSpec = if (childCount == 3 && i == 0) {
                // use full height for layoutThree
                MeasureSpec.makeMeasureSpec(layoutHeight, MeasureSpec.EXACTLY)
            } else {
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
            }
            child.measure(childWidthSpec, childHeightSpec)
        }
    }

    private fun calcChildSize(num: Int, fullSize: Int): Int = when (num) {
        0 -> 0
        1 -> fullSize
        else -> Math.round(fullSize * 1f / 2)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = (r - l) - paddingLeft - paddingRight
        val height = (b - t) - paddingBottom - paddingTop

        val left = paddingLeft
        val top = paddingTop
        val right = left + width
        val bottom = top + height

        when (childCount) {
            0 -> {
                // no thumbnails, do nothing
            }
            1 -> layoutOne(left, top, right, bottom)
            2 -> layoutTwo(left, top, right, bottom)
            3 -> layoutThree(left, top, right, bottom)
            4 -> layoutFour(left, top, right, bottom)
            else -> throw IllegalStateException("cant layout thumbnails")
        }
    }

    private val margin: Int
        get() = (context.resources.displayMetrics.density * MARGIN_DIP).toInt()

    private fun layoutOne(left: Int, top: Int, right: Int, bottom: Int) {
        // ---------
        // |       |
        // |   1   |
        // |       |
        // ---------
        val child1 = getChildAt(0)
        child1.layout(left, top, right, bottom)
    }

    private fun layoutTwo(left: Int, top: Int, right: Int, bottom: Int) {
        // ---------
        // |   |   |
        // | 1 | 2 |
        // |   |   |
        // ---------
        val halfWidth = Math.round(right * 1f / 2)
        val child1 = getChildAt(0)
        child1.layout(left, top, halfWidth, bottom) // left half

        val child2 = getChildAt(1)
        child2.layout(left + halfWidth + margin, top, right, bottom) // right half
    }

    private fun layoutThree(left: Int, top: Int, right: Int, bottom: Int) {
        // ---------
        // |   | 2 |
        // | 1 |---|
        // |   | 3 |
        // ---------
        val halfWidth = Math.round(right * 1f / 2)
        val halfHeight = Math.round(bottom * 1f / 2)

        // left half & full height
        val child1 = getChildAt(0)
        child1.layout(left, top, halfWidth, bottom)

        // right half & top half height
        val child2 = getChildAt(1)
        child2.layout(left + halfWidth + margin, top, right, halfHeight)

        // right half & bottom half height
        val child3 = getChildAt(2)
        child3.layout(left + halfWidth + margin, top + halfHeight + margin, right, bottom)
    }

    private fun layoutFour(left: Int, top: Int, right: Int, bottom: Int) {
        // ---------
        // | 1 | 2 |
        // |---|---|
        // | 3 | 4 |
        // ---------
        val halfWidth = Math.round(right * 1f / 2)
        val halfHeight = Math.round(bottom * 1f / 2)

        // left half & top half height
        val child1 = getChildAt(0)
        child1.layout(left, top, halfWidth, halfHeight)

        // right half & top half height
        val child2 = getChildAt(1)
        child2.layout(left + halfWidth + margin, top, right, bottom)

        // left half & bottom half height
        val child3 = getChildAt(2)
        child3.layout(left, top + halfHeight + margin, halfWidth, bottom)

        // right half & bottom half height
        val child4 = getChildAt(3)
        child4.layout(left + halfWidth + margin, top + halfHeight + margin, right, bottom)
    }
}
