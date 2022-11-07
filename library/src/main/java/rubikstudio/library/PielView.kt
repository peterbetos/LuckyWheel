package rubikstudio.library

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import rubikstudio.library.model.LuckyItem
import java.util.*
import kotlin.math.*


/**
 * Created by kiennguyen on 11/5/16.
 */

open class PielView : View {

    companion object {
        private const val SWIPE_THRESHOLD = 200
        private const val VELOCITY_THRESHOLD = 500
        private const val FULL_ROTATION = 360f
    }

    private val centerX: Float
        get() = centerView.let { it.left + (it.width / 2f) }
    private val centerY: Float
        get() = centerView.let { it.top + (it.height / 2f) }
    private lateinit var centerView: View
    private var lastTouchAngle: Double = 0.0
    private var touchAngle: Double = 0.0

    private var wheelBlur: Boolean = false

    private var spinCount = 5
    private var isFirstSpin: Boolean = true

    // Declare interaction state.
    private var flingGestureDetector: GestureDetector
    private var wheelSpinListener = mutableListOf<WheelSpinListener>()

    private var trueCenter = 0

    private var hemisphere = Hemisphere.LEFT // may not be needed if wheel is always half way off the screen
    private var previousRotation = 0f
    private var currentRotation = 0f
    private var deltaX = 0f
    private var deltaY = 0f

    private var mRange = RectF()
    private var mEdgeRange = RectF()
    private var mRadius: Int = 0

    private var mArcPaint: Paint? = null
    private var mBackgroundPaint: Paint? = null
    private var mTextPaint: TextPaint? = null

    private val mStartAngle = 0f
    private var mCenter: Int = 0
    private var mPadding: Int = 0
    private var mTopTextPadding: Int = 0
    private var mSecondaryTextPadding: Int = 0
    private var mTopTextSize: Int = 0
    private var mSecondaryTextSize: Int = 0
    private var mEdgeWidth = -1
    private var isRunning = false
    private var borderColor = 0
    private var defaultBackgroundColor = 0
    private var textColor = 0
    private var predeterminedNumber = -1
    private var mLuckyItemList: List<LuckyItem>? = null
    private var mPieRotateListener: PieRotateListener? = null
    private var luckyWheelWheelRotation: Int = 0
    private var lastTap: Int = -1
    private var scaledDensity: Double = 0.0

    val fallBackRandomIndex: Int
        get() {
            val rand = Random()
            return rand.nextInt(mLuckyItemList!!.size - 1) + 0
        }

    interface PieRotateListener {
        fun onRotationStart()

        fun rotateDone(index: Int)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun setPieRotateListener(listener: PieRotateListener) {
        this.mPieRotateListener = listener
    }

    init {
        flingGestureDetector = GestureDetector(context, WheelGestureListener())

        // Turn off long press--this control doesn't use it, and if long press is enabled,
        // you can't scroll for a bit, pause, then scroll some more (the pause is interpreted
        // as a long press, apparently)
        flingGestureDetector.setIsLongpressEnabled(false)
    }

    private fun setupMeasurements() {
        mArcPaint = Paint()
        mArcPaint!!.isAntiAlias = true
        mArcPaint!!.isDither = true

        mTextPaint = TextPaint()
        mTextPaint!!.isAntiAlias = true


        if (textColor != 0) mTextPaint!!.color = textColor
        mTextPaint!!.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f,
                resources.displayMetrics)

        mRange = RectF(mPadding.toFloat(), mPadding.toFloat(), (mPadding + mRadius).toFloat(), (mPadding + mRadius).toFloat())
        wheelSpinListener.forEach { listener ->
            listener.setRectF(mRange)
        }

        val widgetwidthOffset = measuredWidth / 160f
        val mPaddingWithoutEdge = mPadding + (mEdgeWidth - widgetwidthOffset) / 2f
        val mRadiusWithoutEdge = mRadius - (mEdgeWidth - widgetwidthOffset)
        mEdgeRange = RectF(mPaddingWithoutEdge, mPaddingWithoutEdge, mPaddingWithoutEdge + mRadiusWithoutEdge, mPaddingWithoutEdge + mRadiusWithoutEdge)
        wheelSpinListener.forEach { listener ->
            listener.setEdgeRectF(mEdgeRange)
        }

        scaledDensity = Math.round(resources.displayMetrics.scaledDensity * 100.0) / 100.0
    }

    fun setData(luckyItemList: List<LuckyItem>) {
        this.mLuckyItemList = luckyItemList
        invalidate()
    }

    fun setPieBackgroundColor(color: Int) {
        defaultBackgroundColor = color
        invalidate()
    }

    fun setBorderColor(color: Int) {
        borderColor = color
        invalidate()
    }

    fun setTopTextPadding(padding: Int) {
        mTopTextPadding = padding
        invalidate()
    }

    fun setTopTextSize(size: Int) {
        mTopTextSize = size
        invalidate()
    }

    fun setSecondaryTextSizeSize(size: Int) {
        mSecondaryTextSize = size
        invalidate()
    }

    fun setSecondaryTextPadding(padding: Int) {
        mSecondaryTextPadding = padding
        if (mSecondaryTextPadding <= 0) mSecondaryTextPadding = 1
        invalidate()
    }

    fun setBorderWidth(width: Int) {
        mEdgeWidth = width
        invalidate()
    }

    fun setPieTextColor(color: Int) {
        textColor = color
        invalidate()
    }

    fun addListener(listener: WheelSpinListener) {
        wheelSpinListener.add(listener)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        trueCenter = left + measuredWidth / 2
    }

    /**
     * @param canvas
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mLuckyItemList == null) {
            return
        }

        drawBackgroundColor(canvas, defaultBackgroundColor)

        setupMeasurements()

        var tmpAngle = mStartAngle
        val sweepAngle = 360f / mLuckyItemList!!.size

        for (i in mLuckyItemList!!.indices) {

            if (mLuckyItemList!![i].color != 0) {
                mArcPaint!!.style = Paint.Style.FILL
                mArcPaint!!.color = mLuckyItemList!![i].color
                canvas.drawArc(mRange, tmpAngle, sweepAngle, true, mArcPaint!!)
            }

            if (borderColor != 0 && mEdgeWidth > 0) {
                mArcPaint!!.style = Paint.Style.STROKE
                mArcPaint!!.color = borderColor
                mArcPaint!!.strokeWidth = mEdgeWidth.toFloat()
                canvas.drawArc(mEdgeRange, tmpAngle, sweepAngle, true, mArcPaint!!)
            }

            val sliceColor = if (mLuckyItemList!![i].color != 0) mLuckyItemList!![i].color else defaultBackgroundColor

            if (!TextUtils.isEmpty(mLuckyItemList!![i].topText))
                drawTopText(canvas, tmpAngle, sweepAngle, mLuckyItemList!![i].topText, sliceColor)
            if (!TextUtils.isEmpty(mLuckyItemList!![i].secondaryText))
                drawSecondaryText(canvas, tmpAngle, mLuckyItemList!![i].secondaryText, sliceColor)

            if (mLuckyItemList!![i].icon != 0)
                drawImage(canvas, tmpAngle, BitmapFactory.decodeResource(resources,
                        mLuckyItemList!![i].icon))

            tmpAngle += sweepAngle
        }

    }

    private fun drawBackgroundColor(canvas: Canvas, color: Int) {
        if (color == 0)
            return
        mBackgroundPaint = Paint()
        mBackgroundPaint!!.color = color
        canvas.drawCircle(mCenter.toFloat(), mCenter.toFloat(), (mCenter - 5).toFloat(), mBackgroundPaint!!)
    }

    /**
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = min(measuredWidth, measuredHeight)

        mPadding = if (paddingLeft == 0) 10 else paddingLeft
        mRadius = width - mPadding * 2

        mCenter = width / 2

        setMeasuredDimension(width, width)
    }


    /**
     * @param canvas
     * @param tmpAngle
     * @param bitmap
     */
    private fun drawImage(canvas: Canvas, tmpAngle: Float, bitmap: Bitmap) {
        val imgWidth = mRadius / mLuckyItemList!!.size

        val angle = ((tmpAngle + 360f / mLuckyItemList!!.size.toFloat() / 2f) * Math.PI / 180).toFloat()

        val x = (mCenter + mRadius / 2 / 2 * cos(angle.toDouble())).toInt()
        val y = (mCenter + mRadius / 2 / 2 * sin(angle.toDouble())).toInt()

        val rect = Rect(x - imgWidth / 2, y - imgWidth / 2,
                x + imgWidth / 2, y + imgWidth / 2)
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    private fun isColorDark(color: Int): Boolean {
        val colorValue = ColorUtils.calculateLuminance(color)
        val compareValue = 0.30
        return colorValue <= compareValue
    }

    /**
     * @param canvas
     * @param tmpAngle
     * @param sweepAngle
     * @param mStr
     */
    private fun drawTopText(canvas: Canvas, tmpAngle: Float, sweepAngle: Float, mStr: String, backgroundColor: Int) {
        val path = Path()
        path.addArc(mRange, tmpAngle, sweepAngle)

        if (textColor == 0)
            mTextPaint!!.color = if (isColorDark(backgroundColor)) -0x1 else -0x1000000

        val typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        mTextPaint!!.typeface = typeface
        mTextPaint!!.textAlign = Paint.Align.LEFT
        mTextPaint!!.textSize = mTopTextSize.toFloat()

        mTextPaint!!.textScaleX = if (mStr.length > 8 && scaledDensity > 6) {
            (1f / mStr.length * 0.5f) * 12f
        } else if (mStr.length > 9 && scaledDensity > 5) {
            (1f / mStr.length * 0.5f) * 14f
        } else if (mStr.length > 10 && scaledDensity > 4) {
            (1f / mStr.length * 0.5f) * 16f
        } else if (mStr.length > 10 && scaledDensity > 3) {
            (1f / mStr.length * 0.5f) * 16f
        } else if (mStr.length > 10 && scaledDensity > 2) {
            (1f / mStr.length * 0.5f) * 20f
        } else {
            0.9f
        }

        val textWidth = mTextPaint!!.measureText(mStr)
        val hOffset = (mRadius * Math.PI / mLuckyItemList!!.size.toDouble() / 2.0 - textWidth / 2).toInt()

        val vOffset = mTopTextPadding

        canvas.drawTextOnPath(mStr, path, hOffset.toFloat(), vOffset.toFloat(), mTextPaint!!)
    }


    /**
     * @param canvas
     * @param tmpAngle
     * @param mStr
     * @param backgroundColor
     */
    private fun drawSecondaryText(canvas: Canvas, tmpAngle: Float, mStr: String, backgroundColor: Int) {
        canvas.save()
        val arraySize = mLuckyItemList!!.size

        if (textColor == 0)
            mTextPaint!!.color = if (isColorDark(backgroundColor)) -0x1 else -0x1000000

        val typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        mTextPaint!!.typeface = typeface
        mTextPaint!!.textSize = mSecondaryTextSize.toFloat()
        mTextPaint!!.textAlign = Paint.Align.LEFT

        mTextPaint!!.textScaleX = when (mStr.length > 2 && scaledDensity > 3.3) {
            true -> 0.55f
            false -> 0.9f
        }
        val textWidth = mTextPaint!!.measureText(mStr)

        val initFloat = tmpAngle + 360f / arraySize.toFloat() / 2f
        val angle = (initFloat * Math.PI / 180).toFloat()

        val x = (mCenter + mRadius / 2 / 2 * Math.cos(angle.toDouble())).toInt()
        val y = (mCenter + mRadius / 2 / 2 * Math.sin(angle.toDouble())).toInt()

        val xStart = mRadius / 2f / 3f

        val rect = RectF(x + xStart, y.toFloat(), x - textWidth, y.toFloat())

        val path = Path()
        path.addRect(rect, Path.Direction.CW)
        path.close()
        canvas.rotate(initFloat + arraySize / 18f, x.toFloat(), y.toFloat())
        canvas.drawTextOnPath(mStr, path, mSecondaryTextPadding.toFloat(), mTextPaint!!.textSize / 2.75f, mTextPaint!!)
        canvas.restore()
    }

    /**
     * @return
     */
    private fun getAngleOfIndexTarget(index: Int): Float {
        return 360f / mLuckyItemList!!.size * index
    }

    fun setPredeterminedNumber(predeterminedNumber: Int) {
        this.predeterminedNumber = predeterminedNumber
    }

    fun setWheelRotation(wheelRotation: Int) {
        luckyWheelWheelRotation = wheelRotation
    }

    fun enableWheelBlur(wheelBlurValue: Boolean) {
        this.wheelBlur = wheelBlurValue
    }

    fun stopRotation() {
        isRunning = false
    }

    private fun onSwipeBottom() {
        when (hemisphere) {
            Hemisphere.LEFT -> spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.COUNTERCLOCKWISE)
            Hemisphere.RIGHT -> spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.CLOCKWISE)
        }
    }

    private fun onSwipeTop() {
        when (hemisphere) {
            Hemisphere.LEFT -> {
                spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.COUNTERCLOCKWISE)
            }
            Hemisphere.RIGHT -> {
                spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.CLOCKWISE)
            }
        }
    }

    private fun onSwipeRight() {
        //not yet implemented
    }

    private fun onSwipeLeft() {
        //not yet implemented
    }


    /**
     * @param index Index of wheel for spin to land on
     * @param spinDirection Spin orientation of the wheel if clockwise or counterclockwise
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun spinTo(index: Int, spinDirection: SpinDirection) {

        // Do nothing if wheel is currently animating
        if (isRunning) {
            return
        }

        //Get the direction of the spin based on sign
        val spinDirectionModifier = when (spinDirection) {
            SpinDirection.CLOCKWISE -> 1f
            SpinDirection.COUNTERCLOCKWISE -> -1f
        }

        if (rotation == 0.0f)
            rotation %= 360f

        var targetAngle = (FULL_ROTATION * 3 * spinDirectionModifier) + ((270f - getAngleOfIndexTarget(index)) - (360f / mLuckyItemList!!.size) / 2) + luckyWheelWheelRotation

        if (spinDirection == SpinDirection.CLOCKWISE) {
            targetAngle += (FULL_ROTATION * 3)
        }

        animate()
                .setInterpolator(DecelerateInterpolator())
                .setDuration(spinCount * 1000 + 900L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        wheelSpinListener.forEach { listener ->
                            listener.onSpinStart(spinDirection)
                        }

                        isRunning = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        rotation %= FULL_ROTATION

                        wheelSpinListener.forEach { listener ->
                            listener.onSpinComplete(index)
                        }

                        isFirstSpin = false
                    }
                })
                .rotation(targetAngle)
                .setUpdateListener {
                    triggerSegmentTap(spinDirection)
                    wheelSpinListener.forEach { listener ->
                        listener.onRotation(it.animatedValue as Float)
                    }
                }
                .start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        val xc = width / 2.0f
        val yc = height / 2.0f

        if (!isRunning) {
            deltaX = event.x
            deltaY = event.y

            // Pass the event to the detector to allow onDown event to be registered first
            flingGestureDetector.onTouchEvent(event)

            // Handle wheel preview
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isInCircle(event.x, event.y)) {
                        return false
                    }

                    touchAngle = Math.toDegrees(Math.atan2((deltaX - xc).toDouble(), (yc - deltaY).toDouble()))

                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    lastTouchAngle = Math.toDegrees(Math.atan2((deltaX - xc).toDouble(), (yc - deltaY).toDouble()))

                    // save current rotation
                    previousRotation = currentRotation

                    updateCurrentRotation()

                    // rotate view by angle difference
                    val angle = currentRotation - previousRotation
                    rotation += angle

                    if ((touchAngle - lastTouchAngle) > 45) {
                        //Going CCW across the boundary
                        hemisphere = Hemisphere.LEFT
                    } else if ((touchAngle - lastTouchAngle) < -45) {
                        //Going CW across the boundary
                        hemisphere = Hemisphere.RIGHT
                    } else {
                        //Normal rotation, rotate the difference
                        hemisphere = if ((lastTouchAngle - touchAngle) > 0) {
                            Hemisphere.RIGHT
                        } else {
                            Hemisphere.LEFT
                        }
                    }

                    triggerSegmentTap(if (hemisphere == Hemisphere.LEFT) SpinDirection.COUNTERCLOCKWISE else SpinDirection.CLOCKWISE)

                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isInCircle(event.x, event.y)) {
                        return false;
                    }

                    lastTouchAngle = Math.toDegrees(Math.atan2((deltaX - xc).toDouble(), (yc - deltaY).toDouble()))
                    touchAngle = lastTouchAngle
                    return true
                }
            }
        }

        return true
    }

    private fun triggerSegmentTap(spinDirection: SpinDirection) {
        val fullwheelRotationAssess: Float = rotation % 360f
        val wheelSizeAssess: Float = mLuckyItemList!!.size.toFloat()
        val segmentAssess: Float = 360f / wheelSizeAssess
        val segmentAssessHalved: Float = segmentAssess / 2f

        if (((fullwheelRotationAssess % segmentAssess) <= (segmentAssessHalved - 0.25f) ||
                        (fullwheelRotationAssess % segmentAssess) >= (segmentAssessHalved + 0.25f))
                && ((fullwheelRotationAssess / segmentAssess).toInt() != lastTap)
        ) {
            lastTap = (fullwheelRotationAssess / segmentAssess).toInt()
            wheelSpinListener.forEach { listener ->
                listener.onSegmentHit(spinDirection)
            }
        }


    }


    /**
     * This method is called in the @OnTouchevent to allow rotation to continue from last touch point
     */

    fun updateCurrentRotation() {
        currentRotation = rotation +
                Math.toDegrees(
                        atan2(
                                deltaX.toDouble() - centerX,
                                centerY - deltaY.toDouble()
                        )
                ).toFloat()
    }

    // Custom Gesture listener to gain fling velocity properties
    private inner class WheelGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            if (!isRunning) {
                updateCurrentRotation()
            }

            return true
        }

        override fun onFling(
                downEvent: MotionEvent,
                upEvent: MotionEvent,
                velocityX: Float,
                velocityY: Float
        ): Boolean {

            // Determine which side of the wheel is touched to figure out spin direction
            // Get deltas to determine swipe direction
            val deltaX = upEvent.rawX - downEvent.rawX
            val deltaY = upEvent.rawY - downEvent.rawY

            // Determine which was greater -> movement along Y or X?
            if (abs(deltaX) > abs(deltaY)) {
                // Now we know this is a horizontal swipe.
                // Let's determine the direction of swipe and also make sure it was an official fling.
                // We may need them to perform spin on fling
                if (abs(deltaX) > SWIPE_THRESHOLD && abs(velocityX) > VELOCITY_THRESHOLD) {
                    if (deltaX > 0) onSwipeRight() else onSwipeLeft()
                    return true
                }

            } else {
                // Otherwise, this is a vertical swipe
                if (abs(deltaY) > SWIPE_THRESHOLD && abs(velocityY) > VELOCITY_THRESHOLD) {
                    // Determine swipe direction
                    if (deltaY > 0) onSwipeBottom() else onSwipeTop()
                }
            }

            return true
        }
    }

    private enum class Hemisphere {
        LEFT,
        RIGHT
    }

    enum class SpinDirection {
        CLOCKWISE,
        COUNTERCLOCKWISE
    }

    interface WheelSpinListener {
        fun onSpinStart(spinDirection: SpinDirection)
        fun onSpinComplete(index: Int)
        fun onSegmentHit(spinDirection: SpinDirection)
        fun onRotation(value: Float)
        fun setRectF(rect: RectF)
        fun setEdgeRectF(rect: RectF)

    }

    fun setCenterView(centerPoint: View) {
        centerView = centerPoint
    }

    private fun isInCircle(x: Float, y: Float): Boolean {
        // find the distance between center of the view and x,y point
        val distance = Math.sqrt(
                Math.pow((mRange.centerX() - x).toDouble(), 2.0) + Math.pow((mRange.centerY() - y).toDouble(), 2.0)
        )
        return distance <= mRange.width() / 2
    }

    override fun onDetachedFromWindow() {
        this.mPieRotateListener = null
        this.wheelSpinListener = mutableListOf()
        if (this.animation != null) {
            this.animation.setAnimationListener(null)
            this.animation.reset()
        }
        this.animate().setListener(null)
        this.stopRotation()
        this.clearAnimation()
        this.clearFocus()
        super.onDetachedFromWindow()
    }

    fun getLuckyItemListSize(): Int {
        return mLuckyItemList!!.size
    }
}
