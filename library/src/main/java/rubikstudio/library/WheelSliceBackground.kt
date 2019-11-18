package rubikstudio.library

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

class WheelSliceBackground @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
    }
    private val emptyVertices = FloatArray(0)
    private val slicePath = Path()

    fun setColor(@ColorInt colorInt: Int) {
        paint.color = colorInt
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        // Define a triangle with these vertices. [X1, Y1, X2 ...]
        val vertices = emptyVertices
                .plus(0f)
                .plus(0f)
                .plus(width * 1.03f)
                .plus(height / 2f)
                .plus(0f)
                .plus(height.toFloat())

        canvas?.drawPath(
                slicePath.apply {
                    moveTo(vertices[0], vertices[1])
                    lineTo(vertices[2], vertices[3])
                    lineTo(vertices[4], vertices[5])
                    lineTo(vertices[0], vertices[1])
                    close()
                },
                paint
        )
//
//        val oval = RectF(10.0f, 10.0f, 1520.0f, 1520.0f)
//        canvas?.drawArc(oval, 170f, 30f, true, paint)
    }

}