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
    private var mEdgeRange = RectF()
    private var colorInt : Int = 0
    private var mEdgeWidth: Int = 0
    private var mBorderColor: Int = 0

    fun setColor(@ColorInt colorInt: Int) {
        paint.color = colorInt
        this.colorInt = colorInt
        postInvalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        //paint.setColor(resources.getColor(android.R.color.white))
        paint.style = Paint.Style.FILL
        paint.color = colorInt
        canvas?.drawArc(mRange, 165f, 30f, true, paint)
        paint.style = Paint.Style.STROKE
        paint.color = mBorderColor
        paint.strokeWidth = mEdgeWidth.toFloat()
        canvas?.drawArc(mEdgeRange, 165f, 30f, true, paint)
    }

    fun setRectF(rect: RectF) {
        mRange = rect
    }

    fun setEdgeRectF(rect:RectF){
        mEdgeRange = rect
    }

    fun setSliceAttributes(edgeWith: Int, borderColor: Int){
        this.mEdgeWidth = edgeWith
        this.mBorderColor = borderColor
    }

}