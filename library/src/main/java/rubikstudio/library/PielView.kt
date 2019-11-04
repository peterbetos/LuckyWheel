package rubikstudio.library

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.core.view.animation.PathInterpolatorCompat

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.reflect.Method
import java.util.Random

import rubikstudio.library.model.LuckyItem

/**
 * Created by kiennguyen on 11/5/16.
 */

class PielView : View {
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

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    fun setPieRotateListener(listener: PieRotateListener) {
        this.mPieRotateListener = listener
    }

    private fun init() {
        mArcPaint = Paint()
        mArcPaint!!.isAntiAlias = true
        mArcPaint!!.isDither = true

        mTextPaint = TextPaint()
        mTextPaint!!.isAntiAlias = true


        if (textColor != 0) mTextPaint!!.color = textColor
        mTextPaint!!.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f,
                resources.displayMetrics)

        mRange = RectF(mPadding.toFloat(), mPadding.toFloat(), (mPadding + mRadius).toFloat(), (mPadding + mRadius).toFloat())
        val widgetwidthOffset = measuredWidth / 160f
        val mPaddingWithoutEdge = mPadding + (mEdgeWidth - widgetwidthOffset) / 2f
        val mRadiusWithoutEdge = mRadius - (mEdgeWidth - widgetwidthOffset)
        mEdgeRange = RectF(mPaddingWithoutEdge, mPaddingWithoutEdge, mPaddingWithoutEdge + mRadiusWithoutEdge, mPaddingWithoutEdge + mRadiusWithoutEdge)
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

    /**
     * @param canvas
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mLuckyItemList == null) {
            return
        }

        drawBackgroundColor(canvas, defaultBackgroundColor)

        init()

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
        val paint = Paint()
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
                (360L - Math.abs((getRotation() % 360f).toLong())) * 1000L / 360L
            else
                Math.abs((getRotation() % 360f).toLong()) * 1000L / 360L

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
                        Log.d("Anim", "Rotation  value $rotationValue")
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

        val interpolates = if (decelarationDuration > 0L && spinDuration > 0L) DecelerateInterpolator(decelarationDuration.toFloat() / spinDuration) else DecelerateInterpolator()

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
                    Log.i("Animation", "Forcefully setting animation successful")
                } catch (t: Throwable) {
                    // It means something bad happened, and animations are still
                    // altered by the global settings.
                    Log.w("Animation", "Changing animation duration not possible")
                }

            }
        }
    }

    fun stopRotation() {
        isRunning = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isRunning || !isEnabled) {
            return false
        }

        val x = event.x
        val y = event.y

        val xc = width / 2.0f
        val yc = height / 2.0f

        val newFingerRotation: Double


        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                viewRotation = (rotation + 360f) % 360f
                fingerRotation = Math.toDegrees(Math.atan2((x - xc).toDouble(), (yc - y).toDouble()))
                downPressTime = event.eventTime
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                newFingerRotation = Math.toDegrees(Math.atan2((x - xc).toDouble(), (yc - y).toDouble()))

                if (isRotationConsistent(newFingerRotation)) {
                    rotation = newRotationValue(viewRotation, fingerRotation, newFingerRotation)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                newFingerRotation = Math.toDegrees(Math.atan2((x - xc).toDouble(), (yc - y).toDouble()))
                var computedRotation = newRotationValue(viewRotation, fingerRotation, newFingerRotation)

                fingerRotation = newFingerRotation

                // This computes if you're holding the tap for too long
                upPressTime = event.eventTime
                if (upPressTime - downPressTime > 700L) {
                    // Disregarding the touch since the tap is too slow
                    return true
                }

                // These operators are added so that fling difference can be evaluated
                // with usually numbers that are only around more or less 100 / -100.
                if (computedRotation <= -250f) {
                    computedRotation += 360f
                } else if (computedRotation >= 250f) {
                    computedRotation -= 360f
                }

                var flingDiff = (computedRotation - viewRotation).toDouble()
                if (flingDiff >= 200 || flingDiff <= -200) {
                    if (viewRotation <= -50f) {
                        viewRotation += 360f
                    } else if (viewRotation >= 50f) {
                        viewRotation -= 360f
                    }
                }

                flingDiff = (computedRotation - viewRotation).toDouble()

                if (flingDiff <= -60 ||
                        //If you have a very fast flick / swipe, you an disregard the touch difference
                        flingDiff < 0 && flingDiff >= -59 && upPressTime - downPressTime <= 200L) {
                    if (predeterminedNumber > -1) {
                        rotateTo(predeterminedNumber, SpinRotation.COUNTERCLOCKWISE, false)
                    } else {
                        rotateTo(fallBackRandomIndex, SpinRotation.COUNTERCLOCKWISE, false)
                    }
                }

                if (flingDiff >= 60 ||
                        //If you have a very fast flick / swipe, you an disregard the touch difference
                        flingDiff > 0 && flingDiff <= 59 && upPressTime - downPressTime <= 200L) {
                    if (predeterminedNumber > -1) {
                        rotateTo(predeterminedNumber, SpinRotation.CLOCKWISE, false)
                    } else {
                        rotateTo(fallBackRandomIndex, SpinRotation.CLOCKWISE, false)
                    }
                }

                return true
            }
        }
        return super.onTouchEvent(event)
    }

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

        return if (java.lang.Double.compare(newRotationStore[2], newRotationStore[0]) == 0
                || java.lang.Double.compare(newRotationStore[1], newRotationStore[0]) == 0
                || java.lang.Double.compare(newRotationStore[2], newRotationStore[1]) == 0

                //Is the middle event the odd one out
                || newRotationStore[0] > newRotationStore[1] && newRotationStore[1] < newRotationStore[2]
                || newRotationStore[0] < newRotationStore[1] && newRotationStore[1] > newRotationStore[2]) {
            false
        } else true
    }


  //  @IntDef(SpinRotation.CLOCKWISE, SpinRotation.COUNTERCLOCKWISE)
    @Retention(RetentionPolicy.SOURCE)
    annotation class SpinRotation {
        companion object {
            val CLOCKWISE = 0
            val COUNTERCLOCKWISE = 1
        }
    }
}
