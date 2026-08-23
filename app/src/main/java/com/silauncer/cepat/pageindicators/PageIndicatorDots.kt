package com.silauncer.cepat.pageindicators

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.FloatProperty
import android.util.IntProperty
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.silauncer.cepat.R
import kotlin.math.abs

// [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicatorDots
// [Penjelasan]: Komponen kustom View penunjuk halaman berbentuk titik (dots pagination) teradaptasi dari AOSP Launcher3. Mendukung transisi peregangan kapsul saat diusap, animasi staggered overshoot saat masuk layar, auto-hide fading, serta dukungan tata letak RTL.
class PageIndicatorDots @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PageIndicator {

    companion object {
        private const val SHIFT_PER_ANIMATION = 0.5f
        private const val SHIFT_THRESHOLD = 0.1f
        private const val ANIMATION_DURATION = 150L
        private val PAGINATION_FADE_DELAY = ViewConfiguration.getScrollDefaultDelay().toLong()
        private const val PAGINATION_FADE_IN_DURATION = 83L
        private const val PAGINATION_FADE_OUT_DURATION = 167L

        private const val ENTER_ANIMATION_START_DELAY = 300L
        private const val ENTER_ANIMATION_STAGGERED_DELAY = 150L
        private const val ENTER_ANIMATION_DURATION = 400L

        private const val PAGE_INDICATOR_ALPHA = 255
        private const val DOT_ALPHA = 128
        private const val DOT_ALPHA_FRACTION = 0.5f
        private const val DOT_GAP_FACTOR = 4
        private const val VISIBLE_ALPHA = 255
        private const val INVISIBLE_ALPHA = 0
        private const val ENTER_ANIMATION_OVERSHOOT_TENSION = 4.9f

        private val CURRENT_POSITION = object : FloatProperty<PageIndicatorDots>("current_position") {
            override fun get(obj: PageIndicatorDots): Float = obj.mCurrentPosition

            override fun setValue(obj: PageIndicatorDots, pos: Float) {
                obj.mCurrentPosition = pos
                obj.invalidate()
                obj.invalidateOutline()
            }
        }

        private val PAGINATION_ALPHA = object : IntProperty<PageIndicatorDots>("pagination_alpha") {
            override fun get(obj: PageIndicatorDots): Int = obj.mPaginationPaint.alpha

            override fun setValue(obj: PageIndicatorDots, alpha: Int) {
                obj.mPaginationPaint.alpha = alpha
                obj.invalidate()
            }
        }
    }

    private val mTempRect = RectF()
    private val mDelayedPaginationFadeHandler = Handler(Looper.getMainLooper())
    private val mPaginationPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.folder_pagination_color)
    }

    private val mDotRadius: Float
    private val mCircleGap: Float
    private val mIsRtl: Boolean

    private var mNumPages = 0
    private var mActivePage = 0
    private var mTotalScroll = 0
    private var mShouldAutoHide = false
    private var mToAlpha = VISIBLE_ALPHA

    private var mCurrentPosition = 0f
    private var mFinalPosition = 0f
    private var mAnimator: ObjectAnimator? = null
    private var mAlphaAnimator: ObjectAnimator? = null
    private var mEntryAnimationRadiusFactors: FloatArray? = null

    private val mHidePaginationRunnable = Runnable { animatePaginationToAlpha(INVISIBLE_ALPHA) }

    init {
        val dotSize = context.resources.getDimension(R.dimen.page_indicator_dot_size)
        mDotRadius = dotSize / 2f
        mCircleGap = DOT_GAP_FACTOR * mDotRadius
        outlineProvider = MyOutlineProvider()
        mIsRtl = context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicatorDots
    // [Penjelasan]: Menghitung posisi interpolasi titik halaman aktif berdasarkan offset scroll kontainer relatif terhadap total panjang scroll.
    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        if (mNumPages <= 1) {
            return
        }

        if (mShouldAutoHide) {
            animatePaginationToAlpha(VISIBLE_ALPHA)
        }

        var adjustedScroll = currentScroll
        if (mIsRtl) {
            adjustedScroll = totalScroll - currentScroll
        }

        mTotalScroll = totalScroll

        val scrollPerPage = if (mNumPages > 1) totalScroll / (mNumPages - 1) else 1
        val safeScrollPerPage = if (scrollPerPage == 0) 1 else scrollPerPage
        val pageToLeft = (adjustedScroll / safeScrollPerPage).coerceIn(0, mNumPages - 1)
        val pageToLeftScroll = pageToLeft * safeScrollPerPage
        val pageToRightScroll = pageToLeftScroll + safeScrollPerPage

        val scrollThreshold = SHIFT_THRESHOLD * safeScrollPerPage
        if (adjustedScroll < pageToLeftScroll + scrollThreshold) {
            // Scroll berada di dalam ambang halaman kiri
            animateToPosition(pageToLeft.toFloat())
            if (mShouldAutoHide) {
                hideAfterDelay()
            }
        } else if (adjustedScroll > pageToRightScroll - scrollThreshold) {
            // Scroll cukup jauh dari halaman kiri untuk beralih ke halaman kanan
            animateToPosition((pageToLeft + 1).coerceAtMost(mNumPages - 1).toFloat())
            if (mShouldAutoHide) {
                hideAfterDelay()
            }
        } else {
            // Scroll berada di antara transisi dua halaman
            animateToPosition(pageToLeft + SHIFT_PER_ANIMATION)
            if (mShouldAutoHide) {
                mDelayedPaginationFadeHandler.removeCallbacksAndMessages(null)
            }
        }
    }

    override fun setShouldAutoHide(shouldAutoHide: Boolean) {
        mShouldAutoHide = shouldAutoHide
        if (shouldAutoHide && mPaginationPaint.alpha > INVISIBLE_ALPHA) {
            hideAfterDelay()
        } else if (!shouldAutoHide) {
            mDelayedPaginationFadeHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun setPaintColor(color: Int) {
        mPaginationPaint.color = color
        invalidate()
    }

    private fun hideAfterDelay() {
        mDelayedPaginationFadeHandler.removeCallbacksAndMessages(null)
        mDelayedPaginationFadeHandler.postDelayed(mHidePaginationRunnable, PAGINATION_FADE_DELAY)
    }

    private fun animatePaginationToAlpha(alpha: Int) {
        if (alpha == mToAlpha) return

        mAlphaAnimator?.cancel()
        mAlphaAnimator = ObjectAnimator.ofInt(this, PAGINATION_ALPHA, alpha).apply {
            duration = if (alpha < mToAlpha) PAGINATION_FADE_OUT_DURATION else PAGINATION_FADE_IN_DURATION
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mAlphaAnimator = null
                }
            })
            start()
        }
        mToAlpha = alpha
    }

    override fun pauseAnimations() {
        mAlphaAnimator?.pause()
        mAnimator?.pause()
    }

    override fun skipAnimationsToEnd() {
        mAlphaAnimator?.end()
        mAnimator?.end()
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicatorDots
    // [Penjelasan]: Menjalankan animasi perpindahan bertahap posisi titik bulat dengan pergeseran SHIFT_PER_ANIMATION.
    private fun animateToPosition(position: Float) {
        mFinalPosition = position
        if (abs(mCurrentPosition - mFinalPosition) < SHIFT_THRESHOLD) {
            mCurrentPosition = mFinalPosition
        }
        if (mAnimator == null && mCurrentPosition.compareTo(mFinalPosition) != 0) {
            val positionForThisAnim = if (mCurrentPosition > mFinalPosition) {
                mCurrentPosition - SHIFT_PER_ANIMATION
            } else {
                mCurrentPosition + SHIFT_PER_ANIMATION
            }
            mAnimator = ObjectAnimator.ofFloat(this, CURRENT_POSITION, positionForThisAnim).apply {
                addListener(AnimationCycleListener())
                duration = ANIMATION_DURATION
                start()
            }
        }
    }

    fun stopAllAnimations() {
        mAnimator?.cancel()
        mAnimator = null
        mFinalPosition = mActivePage.toFloat()
        CURRENT_POSITION.set(this, mFinalPosition)
    }

    /**
     * Mempersiapkan penunjuk halaman untuk menjalankan animasi kemunculan awal (entry animation).
     */
    fun prepareEntryAnimation() {
        mEntryAnimationRadiusFactors = FloatArray(mNumPages)
        invalidate()
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicatorDots
    // [Penjelasan]: Menjalankan sekuens animasi masuk bertahap (staggered) dengan efek pantulan overshoot pada tiap titik dot halaman.
    fun playEntryAnimation() {
        val factors = mEntryAnimationRadiusFactors ?: return
        val count = factors.size
        if (count == 0) {
            mEntryAnimationRadiusFactors = null
            invalidate()
            return
        }

        val interpolator = OvershootInterpolator(ENTER_ANIMATION_OVERSHOOT_TENSION)
        val animSet = AnimatorSet()
        for (i in 0 until count) {
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = ENTER_ANIMATION_DURATION
                val index = i
                addUpdateListener { animation ->
                    mEntryAnimationRadiusFactors?.let {
                        if (index < it.size) {
                            it[index] = animation.animatedValue as Float
                            invalidate()
                        }
                    }
                }
                setInterpolator(interpolator)
                startDelay = ENTER_ANIMATION_START_DELAY + ENTER_ANIMATION_STAGGERED_DELAY * i
            }
            animSet.play(anim)
        }

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mEntryAnimationRadiusFactors = null
                invalidateOutline()
                invalidate()
            }
        })
        animSet.start()
    }

    override fun setActiveMarker(activePage: Int) {
        if (mActivePage != activePage) {
            mActivePage = activePage
            mCurrentPosition = activePage.toFloat()
            invalidate()
        }
    }

    override fun setMarkersCount(numMarkers: Int) {
        mNumPages = numMarkers
        requestLayout()
        invalidate()
    }

    fun getMarkersCount(): Int = mNumPages

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(widthMeasureSpec)
        } else {
            ((mNumPages * 3 + 2) * mDotRadius).toInt()
        }
        val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            (4 * mDotRadius).toInt()
        }
        setMeasuredDimension(width, height)
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicatorDots
    // [Penjelasan]: Menggambar titik-titik bulat halaman inaktif dan bentuk kapsul/persegi membulat penunjuk halaman aktif yang mulus.
    override fun onDraw(canvas: Canvas) {
        if (mNumPages < 2) {
            return
        }

        if (mShouldAutoHide && mTotalScroll == 0) {
            mPaginationPaint.alpha = INVISIBLE_ALPHA
            return
        }

        var circleGap = mCircleGap
        val startX = (width - (mNumPages * circleGap) + mDotRadius) / 2f
        var x = startX + mDotRadius
        val y = height / 2f

        val entryFactors = mEntryAnimationRadiusFactors
        if (entryFactors != null) {
            if (mIsRtl) {
                x = width - x
                circleGap = -circleGap
            }
            for (i in entryFactors.indices) {
                mPaginationPaint.alpha = if (i == mActivePage) PAGE_INDICATOR_ALPHA else DOT_ALPHA
                canvas.drawCircle(x, y, mDotRadius * entryFactors[i], mPaginationPaint)
                x += circleGap
            }
        } else {
            val alpha = mPaginationPaint.alpha

            // Gambar titik tidak aktif
            mPaginationPaint.alpha = (alpha * DOT_ALPHA_FRACTION).toInt()
            for (i in 0 until mNumPages) {
                canvas.drawCircle(x, y, mDotRadius, mPaginationPaint)
                x += circleGap
            }

            // Gambar penunjuk kapsul halaman aktif
            mPaginationPaint.alpha = alpha
            val activeRect = getActiveRect()
            canvas.drawRoundRect(activeRect, mDotRadius, mDotRadius, mPaginationPaint)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicatorDots
    // [Penjelasan]: Menghitung koordinat Rect dinamis untuk kapsul halaman aktif yang meregang ke arah halaman tujuan selama usapan layar.
    private fun getActiveRect(): RectF {
        val startCircle = mCurrentPosition.toInt()
        var delta = mCurrentPosition - startCircle
        val diameter = 2 * mDotRadius

        val startX = (width - (mNumPages * mCircleGap) + mDotRadius) / 2f
        mTempRect.top = (height * 0.5f) - mDotRadius
        mTempRect.bottom = (height * 0.5f) + mDotRadius
        mTempRect.left = startX + (startCircle * mCircleGap)
        mTempRect.right = mTempRect.left + diameter

        if (delta < SHIFT_PER_ANIMATION) {
            // Dot sedang menangkap lingkaran di sebelah kanan
            mTempRect.right += delta * mCircleGap * 2
        } else {
            // Dot meninggalkan lingkaran kiri menuju lingkaran kanan
            mTempRect.right += mCircleGap
            delta -= SHIFT_PER_ANIMATION
            mTempRect.left += delta * mCircleGap * 2
        }

        if (mIsRtl) {
            val rectWidth = mTempRect.width()
            mTempRect.right = width - mTempRect.left
            mTempRect.left = mTempRect.right - rectWidth
        }

        return mTempRect
    }

    private inner class MyOutlineProvider : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            if (mEntryAnimationRadiusFactors == null) {
                val activeRect = getActiveRect()
                outline.setRoundRect(
                    activeRect.left.toInt(),
                    activeRect.top.toInt(),
                    activeRect.right.toInt(),
                    activeRect.bottom.toInt(),
                    mDotRadius
                )
            }
        }
    }

    private inner class AnimationCycleListener : AnimatorListenerAdapter() {
        private var mCancelled = false

        override fun onAnimationCancel(animation: Animator) {
            mCancelled = true
        }

        override fun onAnimationEnd(animation: Animator) {
            if (!mCancelled) {
                if (mShouldAutoHide) {
                    hideAfterDelay()
                }
                mAnimator = null
                animateToPosition(mFinalPosition)
            }
        }
    }
}
