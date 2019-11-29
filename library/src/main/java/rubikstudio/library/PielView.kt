package rubikstudio.library

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import androidx.core.view.animation.PathInterpolatorCompat
import rubikstudio.library.model.LuckyItem
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
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

    private var lastTouchAngle: Double = 0.0
    private var touchAngle: Double = 0.0

    private var wheelBlur: Boolean = false

    private var spinCount = 3

    // Declare interaction state.
    private var flingGestureDetector: GestureDetector
    private var wheelSpinListener = mutableListOf<WheelSpinListener>()

    private var trueCenter = 0

    private var hemisphere = Hemisphere.LEFT // may not be needed if wheel is always half way off the screen
    private var previousRotation = 0f
    private var currentRotation = 0f
    private var offsetRotation = 0f
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
    private val mRoundOfNumber = 1
    private var mEdgeWidth = -1
    private var isRunning = false

    private var borderColor = 0
    private var defaultBackgroundColor = 0
    private var drawableCenterImage: Drawable? = null
    private var textColor = 0

    private var predeterminedNumber = -1

    internal var viewRotation: Float = 0.toFloat()
    internal var fingerRotation: Double = 0.toDouble()
    internal var downPressTime: Long = 0
    internal var upPressTime: Long = 0
    internal var newRotationStore = DoubleArray(3)


    private var mLuckyItemList: List<LuckyItem>? = null

    private var mPieRotateListener: PieRotateListener? = null

    private val constantVelocity = 2f

    private var spinDuration = 0L
    private var decelarationDuration = 0L

    private var luckyWheelWheelRotation: Int = 0

    private var isAnimate: Boolean = false
    private var angleToAnimate: Float = 0.0F

    val luckyItemListSize: Int
        get() = mLuckyItemList!!.size

    private val fallBackRandomIndex: Int
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
        Log.d("antonhttp", "mRange: " + mRange)

        val widgetwidthOffset = measuredWidth / 160f
        val mPaddingWithoutEdge = mPadding + (mEdgeWidth - widgetwidthOffset) / 2f
        val mRadiusWithoutEdge = mRadius - (mEdgeWidth - widgetwidthOffset)
        mEdgeRange = RectF(mPaddingWithoutEdge, mPaddingWithoutEdge, mPaddingWithoutEdge + mRadiusWithoutEdge, mPaddingWithoutEdge + mRadiusWithoutEdge)
        wheelSpinListener.forEach { listener ->
            listener.setEdgeRectF(mEdgeRange)
        }
        Log.d("antonhttp", "mEdgeRange: " + mEdgeRange)
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


    fun setPieCenterImage(drawable: Drawable) {
        drawableCenterImage = drawable
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

    private fun drawPieBackgroundWithBitmap(canvas: Canvas, bitmap: Bitmap) {
        canvas.drawBitmap(bitmap, null, Rect(mPadding / 2, mPadding / 2,
                measuredWidth - mPadding / 2,
                measuredHeight - mPadding / 2), null)
    }

    fun addListener(listener: WheelSpinListener) {
        wheelSpinListener.add(listener)
    }

    fun removeListener(listener: WheelSpinListener) {
        wheelSpinListener.remove(listener)
    }

    fun clearListeners() {
        wheelSpinListener.clear()
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
        var sweepAngle = 360f / mLuckyItemList!!.size

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

        drawCenterImage(canvas, drawableCenterImage)
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

        val width = Math.min(measuredWidth, measuredHeight)

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

        val x = (mCenter + mRadius / 2 / 2 * Math.cos(angle.toDouble())).toInt()
        val y = (mCenter + mRadius / 2 / 2 * Math.sin(angle.toDouble())).toInt()

        val rect = Rect(x - imgWidth / 2, y - imgWidth / 2,
                x + imgWidth / 2, y + imgWidth / 2)
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    private fun drawCenterImage(canvas: Canvas, drawable: Drawable?) {
        var bitmap = LuckyWheelUtils.drawableToBitmap(drawable)
        bitmap = Bitmap.createScaledBitmap(bitmap, drawable!!.intrinsicWidth, drawable.intrinsicHeight, false)
        canvas.drawBitmap(bitmap, (measuredWidth / 2 - bitmap.width / 2).toFloat(),
                (measuredHeight / 2 - bitmap.height / 2).toFloat(), null)
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

    fun rotateTo(index: Int) {
        val rand = Random()
        rotateTo(index, rand.nextInt() * 3 % 2, true)
    }

    /**
     * @param targetIndex
     * @param rotation,  spin orientation of the wheel if clockwise or counterclockwise
     * @param startSlow, either animates a slow start or an immediate turn based on the trigger
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun rotateTo(targetIndex: Int, @SpinRotation rotation: Int, startSlow: Boolean) {
        isRunning = true

        val rotationAssess = if (rotation <= 0) 1 else 0

        //If the staring position is already off 0 degrees, make an illusion that the rotation has smoothly been triggered.
        // But this inital animation will just reset the position of the circle to 0 degreees.
        if (getRotation() % 360f != 0.0f) {
            setRotation((getRotation() + 36000f) % 360f)

            val animationStart = if (startSlow) AccelerateInterpolator() else LinearInterpolator()
            //The multiplier is to do a big rotation again if the position is already near 360.
            val multiplier = 1f

            //This value wil be used for the duration,
            // this uses modulo of 360
            // or a difference from 360 degree
            // depending on the rotation value
            // so that angle value can be controlled to be until 360 degreees.
            val duration = if (rotationAssess > 0)
                (360L - abs((getRotation() % 360f).toLong())) * 1000L / 360L
            else
                abs((getRotation() % 360f).toLong()) * 1000L / 360L

            animate()
                    .setDuration(duration)
                    .setInterpolator(animationStart)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {

                            isRunning = true
                            if (mPieRotateListener != null)
                                mPieRotateListener!!.onRotationStart()
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            isRunning = true

                            if (spinDuration > 0L) {
                                constantSpin(rotation, targetIndex)
                            } else {
                                val decelerateRotationAssess = if (rotation <= 0) 1 else -1
                                prepareDecelerate(decelerateRotationAssess, targetIndex)
                            }
                        }

                        override fun onAnimationCancel(animation: Animator) {}

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    .rotation(360f * multiplier * rotationAssess.toFloat())
                    .start()
        } else {
            if (spinDuration > 0L) {
                constantSpin(rotation, targetIndex)
            } else {
                val decelerateRotationAssess = if (rotation <= 0) 1 else -1
                prepareDecelerate(decelerateRotationAssess, targetIndex)
            }
        }
    }

    private fun prepareDecelerate(assessRotate: Int, targetIndex: Int) {
        var numberOfRotations = mRoundOfNumber

        // This addition of another round count for counterclockwise is to simulate the perception of the same number of spin
        // if you still need to reach the same outcome of a positive degrees rotation with the number of rounds reversed.
        if (assessRotate < 0) numberOfRotations++

        var targetAngle = 360f * numberOfRotations.toFloat() * assessRotate.toFloat() + (270f - getAngleOfIndexTarget(targetIndex)) - 360f / mLuckyItemList!!.size / 2

        if (this.spinDuration <= 0L) {
            val multiplierTurn = (if (this.decelarationDuration / 1000f * 0.25f < 1f) 1f else this.decelarationDuration / 1000f * 0.25f).toInt()

            if (targetAngle > 0f && targetAngle <= 600f) {
                targetAngle += 360f * multiplierTurn
            }
            if (targetAngle < 0f && targetAngle >= -600f) {
                targetAngle -= 360f * multiplierTurn
            }

        }

        decelerateSpin(targetAngle, targetIndex)
    }


    private fun constantSpin(@SpinRotation rotation: Int, targetIndex: Int) {
        val rotationAssess = if (rotation <= 0) 1 else -1

        val multiplier = this.spinDuration / 1000f * mRoundOfNumber.toFloat()
        val rotationValue = (360f * multiplier * rotationAssess.toFloat()) + if (rotationAssess < 0) 0f else 360f
        animate()
                .setDuration(this.spinDuration)
                .setInterpolator(LinearInterpolator())
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        isRunning = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        prepareDecelerate(rotationAssess, targetIndex)
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        isRunning = false
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                })
                .rotation(rotationValue)
                .start()
    }


    private fun decelerateSpin(endAngle: Float, targetIndex: Int) {
        rotation = 0f

        val customInterpolator: Interpolator
        if (spinDuration == 0L && decelarationDuration != 0L) {
            customInterpolator = PathInterpolatorCompat.create(0.000f, 0.000f, 0.580f, 1.000f)
        } else {
            customInterpolator = DecelerateInterpolator()
        }

        animate()
                .setInterpolator(customInterpolator)
                .setDuration(this.decelarationDuration)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        isRunning = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        val finalizedAngle = (endAngle + 36000f) % 360f
                        rotation = finalizedAngle
                        if (mPieRotateListener != null)
                            mPieRotateListener!!.rotateDone(targetIndex)

                        isRunning = false
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        isRunning = false
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                })
                .rotation(endAngle)
                .start()
    }

    fun setSpinDuration(spinDuration: Long) {
        if (spinDuration >= 0L) {
            this.spinDuration = spinDuration
            makeAnimationDurationConsistent(this.context)
        }
    }

    fun setWheelRotation(wheelRotation: Int) {
        luckyWheelWheelRotation = wheelRotation
    }

    fun enableWheelBlur(wheelBlurValue: Boolean) {
        this.wheelBlur = wheelBlurValue
    }

    fun setDecelarationDuration(decelarationDuration: Long) {
        if (decelarationDuration >= 0L) {
            this.decelarationDuration = decelarationDuration
            makeAnimationDurationConsistent(this.context)
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun makeAnimationDurationConsistent(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Get duration scale from the global settings.
            var durationScale = Settings.Global.getFloat(context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            // If global duration scale is not 1 (default), try to override it
            // for the current application.
            if (durationScale != 1f) {
                try {
                    ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType).invoke(null, 1f)
                    durationScale = 1f
                } catch (t: Throwable) {
                    // It means something bad happened, and animations are still
                    // altered by the global settings.
                }

            }
        }
    }

    fun stopRotation() {
        isRunning = false
    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (isRunning || !isEnabled) {
//            return false
//        }
//
//        val x = event.x
//        val y = event.y
//
//        val xc = width / 2.0f
//        val yc = height / 2.0f
//
//        val newFingerRotation: Double
//
//
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                viewRotation = (rotation + 360f) % 360f
//                fingerRotation = Math.toDegrees(Math.atan2((x - xc).toDouble(), (yc - y).toDouble()))
//                downPressTime = event.eventTime
//                return true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                newFingerRotation = Math.toDegrees(Math.atan2((x - xc).toDouble(), (yc - y).toDouble()))
//
//                if (isRotationConsistent(newFingerRotation)) {
//                    rotation = newRotationValue(viewRotation, fingerRotation, newFingerRotation)
//                }
//                return true
//            }
//            MotionEvent.ACTION_UP -> {
//                newFingerRotation = Math.toDegrees(Math.atan2((x - xc).toDouble(), (yc - y).toDouble()))
//                var computedRotation = newRotationValue(viewRotation, fingerRotation, newFingerRotation)
//
//                fingerRotation = newFingerRotation
//
//                // This computes if you're holding the tap for too long
//                upPressTime = event.eventTime
//                if (upPressTime - downPressTime > 700L) {
//                    // Disregarding the touch since the tap is too slow
//                    return true
//                }
//
//                // These operators are added so that fling difference can be evaluated
//                // with usually numbers that are only around more or less 100 / -100.
//                if (computedRotation <= -250f) {
//                    computedRotation += 360f
//                } else if (computedRotation >= 250f) {
//                    computedRotation -= 360f
//                }
//
//                var flingDiff = (computedRotation - viewRotation).toDouble()
//                if (flingDiff >= 200 || flingDiff <= -200) {
//                    if (viewRotation <= -50f) {
//                        viewRotation += 360f
//                    } else if (viewRotation >= 50f) {
//                        viewRotation -= 360f
//                    }
//                }
//
//                flingDiff = (computedRotation - viewRotation).toDouble()
//
//                if (flingDiff <= -60 ||
//                        //If you have a very fast flick / swipe, you an disregard the touch difference
//                        flingDiff < 0 && flingDiff >= -59 && upPressTime - downPressTime <= 200L) {
//                    if (predeterminedNumber > -1) {
//                        rotateTo(predeterminedNumber, SpinRotation.COUNTERCLOCKWISE, false)
//                    } else {
//                        rotateTo(fallBackRandomIndex, SpinRotation.COUNTERCLOCKWISE, false)
//                    }
//                }
//
//                if (flingDiff >= 60 ||
//                        //If you have a very fast flick / swipe, you an disregard the touch difference
//                        flingDiff > 0 && flingDiff <= 59 && upPressTime - downPressTime <= 200L) {
//                    if (predeterminedNumber > -1) {
//                        rotateTo(predeterminedNumber, SpinRotation.CLOCKWISE, false)
//                    } else {
//                        rotateTo(fallBackRandomIndex, SpinRotation.CLOCKWISE, false)
//                    }
//                }
//
//                return true
//            }
//        }
//        return super.onTouchEvent(event)
//    }

    private fun newRotationValue(originalWheenRotation: Float, originalFingerRotation: Double, newFingerRotation: Double): Float {
        val computationalRotation = newFingerRotation - originalFingerRotation
        return (originalWheenRotation + computationalRotation.toFloat() + 360f) % 360f
    }

    /**
     * This detects if your finger movement is a result of an actual raw touch event of if it's from a view jitter.
     * This uses 3 events of rotation temporary storage so that differentiation between swapping touch events can be determined.
     *
     * @param newRotValue
     */
    private fun isRotationConsistent(newRotValue: Double): Boolean {

        if (java.lang.Double.compare(newRotationStore[2], newRotationStore[1]) != 0) {
            newRotationStore[2] = newRotationStore[1]
        }
        if (java.lang.Double.compare(newRotationStore[1], newRotationStore[0]) != 0) {
            newRotationStore[1] = newRotationStore[0]
        }

        newRotationStore[0] = newRotValue

        return !(java.lang.Double.compare(newRotationStore[2], newRotationStore[0]) == 0
                || java.lang.Double.compare(newRotationStore[1], newRotationStore[0]) == 0
                || java.lang.Double.compare(newRotationStore[2], newRotationStore[1]) == 0

                //Is the middle event the odd one out
                || newRotationStore[0] > newRotationStore[1] && newRotationStore[1] < newRotationStore[2]
                || newRotationStore[0] < newRotationStore[1] && newRotationStore[1] > newRotationStore[2])
    }


    //  @IntDef(SpinRotation.CLOCKWISE, SpinRotation.COUNTERCLOCKWISE)
    @Retention(RetentionPolicy.SOURCE)
    annotation class SpinRotation {
        companion object {
            val CLOCKWISE = 0
            val COUNTERCLOCKWISE = 1
        }
    }

    /**
     * This method is called in the @OnTouchevent to allow rotation to continue from last touch point
     */

    fun updateCurrentRotation() {
        offsetRotation = (rotation % FULL_ROTATION).absoluteValue
        currentRotation = rotation +
                Math.toDegrees(
                        atan2(
                                deltaX.toDouble() - trueCenter,
                                trueCenter - deltaY.toDouble()
                        )
                ).toFloat()
    }

    private fun onSwipeBottom() {
        //Log.d("antonhttp", "=== ON SWIPE BOTTOM IS CALLED")
        when (hemisphere) {
            Hemisphere.LEFT -> spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.COUNTERCLOCKWISE)
            Hemisphere.RIGHT -> spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.CLOCKWISE)
        }
    }

    private fun onSwipeTop() {
        //Log.d("antonhttp", "=== ON SWIPE TOP IS CALLED")
        when (hemisphere) {
            Hemisphere.LEFT -> spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.COUNTERCLOCKWISE)
            Hemisphere.RIGHT -> spinTo(if (predeterminedNumber == -1) fallBackRandomIndex else predeterminedNumber, SpinDirection.CLOCKWISE)
        }
    }

    private fun onSwipeRight() {
        //Log.d("antonhttp", "=== ON SWIPE RIGHT IS CALLED")
        //not yet implemented
    }

    private fun onSwipeLeft() {
        //Log.d("antonhttp", "=== ON SWIPE LEFT IS CALLED")
        //not yet implemented
    }

    /**
     * @param index Index of wheel for spin to land on
     * @param spinDirection Spin orientation of the wheel if clockwise or counterclockwise
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun spinTo(index: Int, spinDirection: SpinDirection) {

        // Do nothing if wheel is currently animating
        if (isRunning) {
            return
        }

        if (wheelBlur) {
            //not yet implemented
        }

        //Get the direction of the spin based on sign
        val spinDirectionModifier = when (spinDirection) {
            SpinDirection.CLOCKWISE -> 1f
            SpinDirection.COUNTERCLOCKWISE -> -1f
        }

        // Determine spin animation properties and final landing slice
        val targetAngle = (((FULL_ROTATION * (spinCount)) * spinDirectionModifier) + (270f - getAngleOfIndexTarget(index)) - 360f / mLuckyItemList!!.size / 2) + luckyWheelWheelRotation

        //spinCount * 1000 + 900L
        animate()
                .setInterpolator(DecelerateInterpolator())
                .setDuration(spinCount * 1000 + 900L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        // Prevent wheel shadow from interfering with glow effects.
//                        wheel_shadow.animate().apply {
//                            duration = 100L
//                            alpha(0f)
//                        }.start()


                        wheelSpinListener.forEach { listener ->
                            listener.onSpinStart(spinDirection)
                        }

                        isAnimate = false
                        isRunning = true
                    }

                    override fun onAnimationEnd(animation: Animator) {

                        // Fade shadow back in.
//                        wheel_shadow.animate().apply {
//                            duration = 300L
//                            startDelay = 450L
//                            alpha(1f)
//
//                        }.start()

                        rotation %= FULL_ROTATION

                        wheelSpinListener.forEach { listener ->
                            listener.onSpinComplete(index)
                        }

//                        // Add 2 to account for shadow and center children.
//                        val selectedSlice = (wheel_layout.getChildAt(index + 2)) as WheelSliceView
//
//                        selectedSlice.animateSlice()

//                        if (mPieRotateListener != null)
//                            mPieRotateListener!!.rotateDone(index)

                        isAnimate = true
                        isRunning = false
                    }
                })
                .rotation(targetAngle)
                .setUpdateListener {
                    wheelSpinListener.forEach { listener ->
                        listener.onRotation(it.animatedValue as Float)
                    }
                }
                .start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        var xc = width / 2.0f
        var yc = height / 2.0f

        if (!isRunning) {
            deltaX = event.x
            deltaY = event.y

            // Pass the event to the detector to allow onDown event to be registered first
            flingGestureDetector.onTouchEvent(event)

            // Handle wheel preview
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchAngle = Math.toDegrees(Math.atan2((deltaX - xc).toDouble(), (yc - deltaY).toDouble()))
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    lastTouchAngle = Math.toDegrees(Math.atan2((deltaX - xc).toDouble(), (yc - deltaY).toDouble()))

                    if ((touchAngle - lastTouchAngle) > 45) {
                        //Going CCW across the boundary
                        hemisphere = Hemisphere.LEFT
                    } else if ((touchAngle - lastTouchAngle) < -45) {
                        //Going CW across the boundary
                        hemisphere = Hemisphere.RIGHT
                    } else {
                        //Normal rotation, rotate the difference
                        hemisphere = if ((lastTouchAngle - touchAngle) > 0) Hemisphere.RIGHT else Hemisphere.LEFT
                    }

                    // save current rotation
                    previousRotation = currentRotation

                    updateCurrentRotation()

                    // rotate view by angle difference
                    val angle = currentRotation - previousRotation
                    rotation += angle

                    return true
                }
                MotionEvent.ACTION_UP -> {
                    lastTouchAngle = Math.toDegrees(Math.atan2((deltaX - xc).toDouble(), (yc - deltaY).toDouble()))
                    touchAngle = lastTouchAngle
                    return true
                }
            }
        }

        return true
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
        fun onRotation(value: Float)
        fun setRectF(rect: RectF)
        fun setEdgeRectF(rect: RectF)
    }
}
