package rubikstudio.library

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import rubikstudio.library.model.LuckyItem
import kotlin.math.roundToInt

class WheelSliceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private var shine_fx: ImageView
    private var slice_background: WheelSliceBackground
    private var slice_type: TextView
    private var slice_amount: TextView
    private var mTopTextSize: Float = 0f
    private var mSecondaryTextSize: Float = 0f
    private var showSliceView = false
    private var mTopTextPadding: Int = 0

    companion object {
        private const val SLICE_HEIGHT_RATIO = 5 / 13f

        private const val ANIM_PULSE_SCALE_MAX = 1.03f
        private const val ANIM_PULSE_SCALE_SPEED = 500L

        private const val ANIM_SHINE_SCALE_MIN = 0f
        private const val ANIM_SHINE_MOVE_SPEED = 1000L
        private const val ANIM_SHINE_FADEOUT_SPEED = 1500L
    }

    private val shineStartX by lazy {
        -1.05f * resources.getDimensionPixelSize(R.dimen.wheel_shine_width)
    }

//    private val shineEndX by lazy {
//        resources.getDimensionPixelSize(R.dimen.wheel_slice_width).toFloat()
//    }

    private var shineEndX: Float = 0f

    private val shineStartY by lazy {
        -1f * resources.getDimensionPixelSize(R.dimen.wheel_shine_y_shift)
    }

    init {
        val rootView: View = LayoutInflater.from(context).inflate(R.layout.wheel_slice_layout, this, true)
        shine_fx = rootView.findViewById(R.id.shine_fx)
        slice_background = rootView.findViewById(R.id.slice_background)
        slice_type = rootView.findViewById(R.id.slice_type)
        slice_amount = rootView.findViewById(R.id.slice_amount)
    }

    fun setSliceViewVisibility(isShow: Boolean) {
        this.showSliceView = isShow
    }

    /**
     * Bind the data from the ActiveReward to the WheelSliceView
     */
    fun bindWheelCard(item: LuckyItem) {

        slice_background.apply {
            setColor(item.color)
        }

        slice_type.apply {
            setTextColor(if (isColorDark(item.color)) -0x1 else -0x1000000)
            text = item.topText
            textSize = mTopTextSize
            if (item.topText.contains("gift", true)) {
                translationX = (mTopTextPadding * -1) / resources.displayMetrics.density
            }
        }

        slice_amount.apply {
            setTextColor(if (isColorDark(item.color)) -0x1 else -0x1000000)
            text = item.secondaryText
            textSize = mSecondaryTextSize
            if (item.topText.contains("gift", true)) {
                translationX = (mTopTextPadding * -1) / resources.displayMetrics.density
            }
        }

        //slice_amount.setPadding((mTopTextPadding / resources.displayMetrics.density).toInt(), 0, 0, 0)
    }

    fun setFontSizes(topTextSize: Int, bottomTextSize: Int) {
        mTopTextSize = (topTextSize / resources.displayMetrics.scaledDensity)
        mSecondaryTextSize = bottomTextSize / resources.displayMetrics.scaledDensity
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val sliceHeight = (MeasureSpec.getSize(heightMeasureSpec) * SLICE_HEIGHT_RATIO).roundToInt()

        shine_fx.layoutParams.height = sliceHeight / 2 + 40

        super.onMeasure(
                widthMeasureSpec,
                heightMeasureSpec
        )
    }

    /**
     * Add pulse and shine animations to selected slice.
     */

    fun animateSlice() {

        with(shine_fx) {
            translationX = shineStartX
            translationY = shineStartY
            alpha = 0f
            visibility = View.VISIBLE
        }

        val pulseFadeIn = ObjectAnimator.ofFloat(this, "alpha", 0.3f, 06f, 1f, 1f).apply {
            duration = ANIM_SHINE_FADEOUT_SPEED
        }

        val pulseX = ObjectAnimator.ofFloat(this, "scaleX", 1f, ANIM_PULSE_SCALE_MAX, ANIM_PULSE_SCALE_MAX, 1f).apply {
            duration = ANIM_PULSE_SCALE_SPEED
        }
        val pulseY = ObjectAnimator.ofFloat(this, "scaleY", 1f, ANIM_PULSE_SCALE_MAX, ANIM_PULSE_SCALE_MAX, 1f).apply {
            duration = ANIM_PULSE_SCALE_SPEED
        }

        val shineMoveX = ObjectAnimator.ofFloat(shine_fx, "translationX", shineEndX).apply {
            duration = ANIM_SHINE_MOVE_SPEED
        }

        val shineScaleY = ObjectAnimator.ofFloat(shine_fx, "scaleY", 1.1f, ANIM_SHINE_SCALE_MIN).apply {
            duration = ANIM_SHINE_MOVE_SPEED
        }

        val shineFadeout = ObjectAnimator.ofFloat(shine_fx, "alpha", 0.8f, 1f, 1f, 0f).apply {
            duration = ANIM_SHINE_FADEOUT_SPEED
        }

//        val pulseFadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, 1f, 0.8f, 0f).apply {
//            duration = ANIM_SHINE_FADEOUT_SPEED
//        }

        val animatorSet = AnimatorSet()
        val animatorEnd = AnimatorSet()

        animatorSet.apply {
            playTogether(pulseFadeIn, pulseX, pulseY)
            start()
        }

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                animatorEnd.apply {
                    playTogether(shineMoveX, shineScaleY, shineFadeout)
                    start()
                }
            }
        })

        animatorEnd.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                visibility = if (showSliceView) View.VISIBLE else View.GONE
            }
        })
    }

    private fun isColorDark(color: Int): Boolean {
        val colorValue = ColorUtils.calculateLuminance(color)
        val compareValue = 0.30
        return colorValue <= compareValue
    }

    fun setRectF(rect: RectF) {
        slice_background.setRectF(rect)
    }

    fun setEdgeRectF(rect: RectF) {
        slice_background.setEdgeRectF(rect)
    }

    fun setSliceAttributes(mEdgeWidth: Int, mBorderColor: Int) {
        slice_background.setSliceAttributes(mEdgeWidth, mBorderColor)
    }

    fun setShineWidth(width: Int) {
        shineEndX = width.toFloat()
    }

    fun setRadius(radius: Float) {
        slice_background.setRadius(radius)
    }

    fun setPadding(padding: Int) {
        mTopTextPadding = padding
    }

}

