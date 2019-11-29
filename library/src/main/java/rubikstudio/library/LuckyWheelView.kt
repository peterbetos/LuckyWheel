package rubikstudio.library

import android.content.Context
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.Random

import rubikstudio.library.model.LuckyItem

/**
 * Created by kiennguyen on 11/5/16.
 */

open class LuckyWheelView : ConstraintLayout, PielView.PieRotateListener, PielView.WheelSpinListener {

    private var mBackgroundColor: Int = 0
    private var mTextColor: Int = 0
    private var mTopTextSize: Int = 0
    private var mSecondaryTextSize: Int = 0
    private var mBorderColor: Int = 0
    private var mTopTextPadding: Int = 0
    private var mSecondaryTextPadding: Int = 0
    private var mEdgeWidth: Int = 0
    private var mCursorImage: Drawable? = null
    private var spinDuration: Int = 0
    private var decelerationDuration: Int = 0
    private var luckyWheelWheelRotation: Int = 0
    private var enableWheelBlur: Boolean = false
    private var mWheelCircleDiameter: Int = 0
    private var mWheelCenterCircleSize: Int = 0
    private var showSliceView = false

    private var pielView: PielView? = null
    private var ivCursorView: ImageView? = null

    private var mLuckyRoundItemSelectedListener: LuckyRoundItemSelectedListener? = null

    private var mLuckyItemList: List<LuckyItem>? = null

    private var wheelSliceView: WheelSliceView? = null

    override fun onRotationStart() {
        if (mLuckyRoundItemSelectedListener != null) {
            mLuckyRoundItemSelectedListener!!.onLuckyWheelRotationStart()
        }
    }

    override fun rotateDone(index: Int) {
        if (mLuckyRoundItemSelectedListener != null) {
            mLuckyRoundItemSelectedListener!!.LuckyRoundItemSelected(index)
        }
    }

    /**
     * @return
     */
    private fun getAngleOfIndexTarget(index: Int): Float {
        return 360f / mLuckyItemList!!.size * index
    }

    override fun onSpinStart(spinDirection: PielView.SpinDirection) {

        ivCursorView!!.startAnimation(
                AnimationUtils.loadAnimation(
                        context, when (spinDirection) {
                    PielView.SpinDirection.CLOCKWISE -> R.anim.spin_indicator_cw
                    else -> R.anim.spin_indicator_ccw
                }
                )
        )
    }

    override fun onSpinComplete(index: Int) {
        wheelSliceView!!.bindWheelCard(mLuckyItemList!![index])
        wheelSliceView!!.visibility = View.VISIBLE
        wheelSliceView!!.animateSlice()
    }

    override fun onRotation(value: Float) {
        if (value > 0.98f) {
            ivCursorView!!.clearAnimation()
        }
    }

    interface LuckyRoundItemSelectedListener {
        fun LuckyRoundItemSelected(index: Int)

        fun onLuckyWheelRotationStart()
    }

    fun setLuckyRoundItemSelectedListener(listener: LuckyRoundItemSelectedListener) {
        this.mLuckyRoundItemSelectedListener = listener
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        this.pielView!!.isEnabled = enabled
    }

    /**
     * @param ctx
     * @param attrs
     */
    private fun init(ctx: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = ctx.obtainStyledAttributes(attrs, R.styleable.LuckyWheelView)
            mBackgroundColor = typedArray.getColor(R.styleable.LuckyWheelView_lkwBackgroundColor, -0x340000)
            mTopTextSize = typedArray.getDimensionPixelSize(R.styleable.LuckyWheelView_lkwTopTextSize, LuckyWheelUtils.convertDpToPixel(10f, context).toInt())
            mSecondaryTextSize = typedArray.getDimensionPixelSize(R.styleable.LuckyWheelView_lkwSecondaryTextSize, LuckyWheelUtils.convertDpToPixel(10f, context).toInt())
            mTextColor = typedArray.getColor(R.styleable.LuckyWheelView_lkwTopTextColor, 0)
            mTopTextPadding = typedArray.getDimensionPixelSize(R.styleable.LuckyWheelView_lkwTopTextPadding, LuckyWheelUtils.convertDpToPixel(10f, context).toInt()) + LuckyWheelUtils.convertDpToPixel(10f, context).toInt()
            mSecondaryTextPadding = typedArray.getDimensionPixelSize(R.styleable.LuckyWheelView_lkwSecondaryTextPadding, 0)
            mCursorImage = typedArray.getDrawable(R.styleable.LuckyWheelView_lkwCursor)
            mEdgeWidth = typedArray.getDimensionPixelSize(R.styleable.LuckyWheelView_lkwEdgeWidth, LuckyWheelUtils.convertDpToPixel(10f, context).toInt())
            mBorderColor = typedArray.getColor(R.styleable.LuckyWheelView_lkwEdgeColor, 0)
            spinDuration = typedArray.getInteger(R.styleable.LuckyWheelView_lkwSpinDuration, 2000)
            decelerationDuration = typedArray.getInteger(R.styleable.LuckyWheelView_lkwDecelarationDuration, 3000)
            luckyWheelWheelRotation = typedArray.getInteger(R.styleable.LuckyWheelView_lkwWheelRotation, -90)
            enableWheelBlur = typedArray.getBoolean(R.styleable.LuckyWheelView_lkwEnableWheelBlur, false)
            mWheelCircleDiameter = typedArray.getDimensionPixelSize(R.styleable.LuckyWheelView_lkwWheelCircleDiameter, 500)
            mWheelCenterCircleSize = typedArray.getDimensionPixelSize(R.styleable.LuckyWheelView_lkwWheelCenterCircleSize, 150)
            showSliceView = typedArray.getBoolean(R.styleable.LuckyWheelView_lkwShowSliceView, false)
            typedArray.recycle()
        }

        val inflater = LayoutInflater.from(context)
        val constraintLayout = inflater.inflate(R.layout.lucky_wheel_layout, this, false) as ConstraintLayout

        pielView = constraintLayout.findViewById(R.id.pieView)
        ivCursorView = constraintLayout.findViewById(R.id.cursorView)

        pielView!!.setSpinDuration(spinDuration.toLong())
        //pielView.setDecelarationDuration((long) decelerationDuration);
        pielView!!.setPieRotateListener(this)
        pielView!!.setPieBackgroundColor(mBackgroundColor)
        pielView!!.setTopTextPadding(mTopTextPadding)
        pielView!!.setTopTextSize(mTopTextSize)
        pielView!!.setSecondaryTextSizeSize(mSecondaryTextSize)
        pielView!!.setSecondaryTextPadding(mSecondaryTextPadding)
        pielView!!.setBorderColor(mBorderColor)
        pielView!!.setBorderWidth(mEdgeWidth)
        pielView!!.setWheelRotation(luckyWheelWheelRotation)
        pielView!!.enableWheelBlur(enableWheelBlur)

        if (mTextColor != 0)
            pielView!!.setPieTextColor(mTextColor)

        ivCursorView!!.setImageDrawable(mCursorImage)

        val centerView = constraintLayout.findViewById<View>(R.id.center_point)
        (centerView.layoutParams as ViewGroup.LayoutParams).height = mWheelCenterCircleSize
        (centerView.layoutParams as ViewGroup.LayoutParams).width = mWheelCenterCircleSize

        wheelSliceView = constraintLayout.findViewById(R.id.wheel_node_1)
        wheelSliceView?.setSliceViewVisibility(showSliceView)
        wheelSliceView?.visibility = if (showSliceView) View.VISIBLE else View.GONE
        wheelSliceView?.setFontSizes(mTopTextSize, mSecondaryTextSize)
        wheelSliceView?.setSliceAttributes(mEdgeWidth, mBorderColor)
        (wheelSliceView?.layoutParams as LayoutParams).height = mWheelCircleDiameter
        (wheelSliceView?.layoutParams as LayoutParams).width = mWheelCircleDiameter / 2
        (wheelSliceView?.layoutParams as LayoutParams).circleRadius = (mWheelCircleDiameter / 2) / 2
        wheelSliceView?.setShineWidth(mWheelCircleDiameter / 2)

        addView(constraintLayout)

        pielView!!.addListener(this)
    }

    fun setInitialAngle(initialAngle: Float) {
        pielView!!.rotation = initialAngle
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        //This is to control that the touch events triggered are only going to the PieView
        for (i in 0 until childCount) {
            if (isPielView(getChildAt(i))) {
                return super.dispatchTouchEvent(ev)
            }
        }
        return false
    }

    private fun isPielView(view: View): Boolean {
        if (view is ViewGroup) {
            for (i in 0 until childCount) {
                if (isPielView(view.getChildAt(i))) {
                    return true
                }
            }
        }
        return view is PielView
    }

    fun setLuckyWheelBackgrouldColor(color: Int) {
        pielView!!.setPieBackgroundColor(color)
    }

    fun setLuckyWheelCursorImage(drawable: Int) {
        ivCursorView!!.setBackgroundResource(drawable)
    }

    fun setBorderColor(color: Int) {
        pielView!!.setBorderColor(color)
    }

    fun setLuckyWheelTextColor(color: Int) {
        pielView!!.setPieTextColor(color)
    }

    /**
     * @param data
     */
    fun setData(data: List<LuckyItem>) {
        this.mLuckyItemList = data
        pielView!!.setData(data)
        wheelSliceView?.setRadius(360f / data.size)
    }

    /**
     * //     * @param fixedNumber
     */
    fun setPredeterminedNumber(fixNumber: Int) {
        pielView!!.setPredeterminedNumber(fixNumber)
        wheelSliceView!!.bindWheelCard(mLuckyItemList!![fixNumber])
    }

    fun startLuckyWheelWithTargetIndex(index: Int) {
        pielView!!.rotateTo(index);
    }

    fun startLuckyWheelWithRandomTarget() {
        val rand = Random()
        pielView!!.rotateTo(rand.nextInt(pielView!!.luckyItemListSize - 1));
    }

    fun stopRotation() {
        pielView!!.stopRotation();
    }

    fun setRouletteSpinDuration(spinDurationParam: Long) {
        pielView!!.setSpinDuration(spinDurationParam)
    }

    fun setRouletteDecelarationDuration(decelarationDurationParam: Long) {
        pielView!!.setDecelarationDuration(decelarationDurationParam)
    }

    override fun setRectF(rect: RectF) {
        wheelSliceView!!.setRectF(rect)
    }

    override fun setEdgeRectF(rect: RectF) {
        wheelSliceView!!.setEdgeRectF(rect)
    }
}
