package com.silauncer.cepat.pageindicators

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.ViewConfiguration
import com.silauncer.cepat.R

// [Jalur Class]: com.silauncer.cepat.pageindicators.WorkspacePageIndicator
// [Penjelasan]: Indikator halaman garis horizontal (Workspace Line Scrubber) teradaptasi dari AOSP Launcher3. Ditampilkan secara halus dengan animasi fade-in saat pengguna berpindah halaman workspace dan otomatis memudar (fade-out) setelah jeda scroll selesai.
class WorkspacePageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PageIndicator {

    companion object {
        val LINE_ANIMATE_DURATION = ViewConfiguration.getScrollBarFadeDuration().toLong()
        val LINE_FADE_DELAY = ViewConfiguration.getScrollDefaultDelay().toLong()
        const val WHITE_ALPHA = (0.70f * 255).toInt()
        const val BLACK_ALPHA = (0.65f * 255).toInt()

        private const val LINE_ALPHA_ANIMATOR_INDEX = 0
        private const val NUM_PAGES_ANIMATOR_INDEX = 1
        private const val TOTAL_SCROLL_ANIMATOR_INDEX = 2
        private const val ANIMATOR_COUNT = 3

        private val PAINT_ALPHA = object : Property<WorkspacePageIndicator, Int>(Int::class.java, "paint_alpha") {
            override fun get(obj: WorkspacePageIndicator): Int = obj.mLinePaint.alpha

            override fun set(obj: WorkspacePageIndicator, value: Int) {
                obj.mLinePaint.alpha = value
                obj.invalidate()
            }
        }

        private val NUM_PAGES = object : Property<WorkspacePageIndicator, Float>(Float::class.java, "num_pages") {
            override fun get(obj: WorkspacePageIndicator): Float = obj.mNumPagesFloat

            override fun set(obj: WorkspacePageIndicator, value: Float) {
                obj.mNumPagesFloat = value
                obj.invalidate()
            }
        }

        private val TOTAL_SCROLL = object : Property<WorkspacePageIndicator, Int>(Int::class.java, "total_scroll") {
            override fun get(obj: WorkspacePageIndicator): Int = obj.mTotalScroll

            override fun set(obj: WorkspacePageIndicator, value: Int) {
                obj.mTotalScroll = value
                obj.invalidate()
            }
        }
    }

    private val mAnimators = arrayOfNulls<ValueAnimator>(ANIMATOR_COUNT)
    private val mDelayedLineFadeHandler = Handler(Looper.getMainLooper())
    private var mShouldAutoHide = true

    private var mActiveAlpha = WHITE_ALPHA
    private var mToAlpha = 0
    private var mNumPagesFloat = 0f
    private var mCurrentScroll = 0
    private var mTotalScroll = 0
    private val mLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 0
        color = Color.WHITE
    }
    private val mLineHeight: Int

    private val mHideLineRunnable = Runnable { animateLineToAlpha(0) }

    init {
        mLineHeight = context.resources.getDimensionPixelSize(R.dimen.workspace_page_indicator_line_height)
        mActiveAlpha = WHITE_ALPHA
        mLinePaint.color = Color.WHITE
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.WorkspacePageIndicator
    // [Penjelasan]: Menggambar garis scrub horizontal dengan panjang proporsional (availableWidth / numPages) dan posisi terinterpolasi sesuai kemajuan scroll.
    override fun onDraw(canvas: Canvas) {
        if (mTotalScroll == 0 || mNumPagesFloat <= 0f) {
            return
        }

        val progress = (mCurrentScroll.toFloat() / mTotalScroll).coerceIn(0f, 1f)
        val availableWidth = width
        val lineWidth = (availableWidth / mNumPagesFloat).toInt()
        val lineLeft = (progress * (availableWidth - lineWidth)).toInt()
        val lineRight = lineLeft + lineWidth

        val top = height / 2f - mLineHeight / 2f
        val bottom = height / 2f + mLineHeight / 2f
        val radius = mLineHeight.toFloat()

        canvas.drawRoundRect(
            lineLeft.toFloat(),
            top,
            lineRight.toFloat(),
            bottom,
            radius,
            radius,
            mLinePaint
        )
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.WorkspacePageIndicator
    // [Penjelasan]: Memperbarui scroll saat perpindahan halaman berlangsung, memunculkan garis dengan animasi alpha, dan mengatur timer auto-fade.
    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        if (alpha == 0f) {
            return
        }
        animateLineToAlpha(mActiveAlpha)

        mCurrentScroll = currentScroll
        if (mTotalScroll == 0) {
            mTotalScroll = totalScroll
        } else if (mTotalScroll != totalScroll) {
            animateToTotalScroll(totalScroll)
        } else {
            invalidate()
        }

        if (mShouldAutoHide) {
            hideAfterDelay()
        }
    }

    private fun hideAfterDelay() {
        mDelayedLineFadeHandler.removeCallbacksAndMessages(null)
        mDelayedLineFadeHandler.postDelayed(mHideLineRunnable, LINE_FADE_DELAY)
    }

    override fun setActiveMarker(activePage: Int) {
        // Line indicator menghitung posisi berbasis scroll progress secara kontinu
    }

    // [Jalur Class]: com.silauncer.cepat.pageindicators.WorkspacePageIndicator
    // [Penjelasan]: Memperbarui jumlah total halaman dengan animasi transisi panjang garis indikator yang mulus.
    override fun setMarkersCount(numMarkers: Int) {
        val target = numMarkers.toFloat()
        if (mNumPagesFloat.compareTo(target) != 0) {
            setupAndRunAnimation(
                ObjectAnimator.ofFloat(this, NUM_PAGES, target),
                NUM_PAGES_ANIMATOR_INDEX
            )
        } else {
            mAnimators[NUM_PAGES_ANIMATOR_INDEX]?.let {
                it.cancel()
                mAnimators[NUM_PAGES_ANIMATOR_INDEX] = null
            }
        }
    }

    override fun setShouldAutoHide(shouldAutoHide: Boolean) {
        mShouldAutoHide = shouldAutoHide
        if (shouldAutoHide && mLinePaint.alpha > 0) {
            hideAfterDelay()
        } else if (!shouldAutoHide) {
            mDelayedLineFadeHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun setPaintColor(color: Int) {
        mLinePaint.color = color
        invalidate()
    }

    private fun animateLineToAlpha(alpha: Int) {
        if (alpha == mToAlpha) {
            return
        }
        mToAlpha = alpha
        setupAndRunAnimation(
            ObjectAnimator.ofInt(this, PAINT_ALPHA, alpha),
            LINE_ALPHA_ANIMATOR_INDEX
        )
    }

    private fun animateToTotalScroll(totalScroll: Int) {
        setupAndRunAnimation(
            ObjectAnimator.ofInt(this, TOTAL_SCROLL, totalScroll),
            TOTAL_SCROLL_ANIMATOR_INDEX
        )
    }

    private fun setupAndRunAnimation(animator: ValueAnimator, animatorIndex: Int) {
        mAnimators[animatorIndex]?.cancel()
        mAnimators[animatorIndex] = animator
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mAnimators[animatorIndex] = null
            }
        })
        animator.duration = LINE_ANIMATE_DURATION
        animator.start()
    }

    override fun pauseAnimations() {
        for (i in 0 until ANIMATOR_COUNT) {
            mAnimators[i]?.pause()
        }
    }

    override fun skipAnimationsToEnd() {
        for (i in 0 until ANIMATOR_COUNT) {
            mAnimators[i]?.end()
        }
    }
}
