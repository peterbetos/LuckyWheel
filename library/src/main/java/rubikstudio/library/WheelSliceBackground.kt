package rubikstudio.library

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import android.graphics.RectF

class WheelSliceBackground @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    private var mRange = RectF()

    fun setColor(@ColorInt colorInt: Int) {
        paint.color = colorInt
        postInvalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        //paint.setColor(resources.getColor(android.R.color.transparent))
        canvas?.drawArc(mRange, 165f, 30f, true, paint)
    }

    fun setRectF(rect: RectF) {
        mRange = rect
    }

}