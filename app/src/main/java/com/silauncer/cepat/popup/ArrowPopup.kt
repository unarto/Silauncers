package com.silauncer.cepat.popup

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.silauncer.cepat.R
import com.silauncer.cepat.anim.Interpolators

/**
 * ArrowPopup
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
 * // [Penjelasan]: Kelas abstrak pembungkus popup floating berujung panah (caret arrow). Mengelola pembuatan panah penunjuk (RoundedArrowDrawable), kalkulasi pivot transformasi, serta animasi pembuka dan penutup. Diadaptasi dari AOSP Launcher3.
 */
abstract class ArrowPopup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    protected var mIsOpen: Boolean = false
    protected var mDeferContainerRemoval: Boolean = false
    protected var mIsAboveIcon: Boolean = true
    protected var mIsLeftAligned: Boolean = true

    protected var mArrowWidth: Int = 0
    protected var mArrowHeight: Int = 0
    protected var mArrowOffsetHorizontal: Int = 0

    protected lateinit var mArrow: ImageView

    // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
    // [Penjelasan]: Helper eksternal/subclass untuk memeriksa apakah mArrow telah diinisialisasi
    fun isArrowInitialized(): Boolean = ::mArrow.isInitialized

    // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
    // [Penjelasan]: Helper eksternal/subclass untuk mengambil referensi mArrow secara aman
    fun getArrowView(): ImageView = mArrow

    protected var mOpenCloseAnimator: AnimatorSet? = null
    private val mOnCloseCallbacks = mutableListOf<Runnable>()

    init {
        orientation = VERTICAL
        clipToOutline = true
        elevation = resources.getDimension(R.dimen.deep_shortcuts_elevation)
        
        mArrowWidth = resources.getDimensionPixelSize(R.dimen.folder_preview_item_size)
        mArrowHeight = (mArrowWidth * 0.5f).toInt()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
     * // [Penjelasan]: Mengonfigurasi tampilan panah penunjuk (caret arrow) dan memasangkannya pada container popup.
     */
    protected fun setupArrow(
        isPointingUp: Boolean,
        leftAligned: Boolean,
        arrowColor: Int,
        popupRadius: Float,
        popupWidth: Float,
        popupHeight: Float,
        arrowOffsetX: Float,
        arrowOffsetY: Float
    ) {
        mIsAboveIcon = !isPointingUp
        mIsLeftAligned = leftAligned

        if (!::mArrow.isInitialized) {
            mArrow = ImageView(context)
            val lp = LayoutParams(mArrowWidth, mArrowHeight)
            mArrow.layoutParams = lp
        }

        val drawable = RoundedArrowDrawable(
            width = mArrowWidth.toFloat(),
            height = mArrowHeight.toFloat(),
            radius = popupRadius * 0.5f,
            popupRadius = popupRadius,
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            arrowOffsetX = arrowOffsetX,
            arrowOffsetY = arrowOffsetY,
            isPointingUp = isPointingUp,
            leftAligned = leftAligned,
            color = arrowColor
        )
        mArrow.setImageDrawable(drawable)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
     * // [Penjelasan]: Menentukan titik acuan (pivot) transformasi untuk animasi skala popup berdasarkan letak ikon target.
     */
    protected fun setPivotForIcon(anchorCenterX: Float, anchorCenterY: Float) {
        val arrowCenter = mArrowOffsetHorizontal + (mArrowWidth / 2f)
        pivotX = if (mIsLeftAligned) arrowCenter else width - arrowCenter
        pivotY = if (mIsAboveIcon) height.toFloat() else 0f
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
     * // [Penjelasan]: Membuat AnimatorSet untuk animasi pembukaan atau penutupan popup dengan transisi alpha, scale, dan fade anak.
     */
    protected fun getOpenCloseAnimator(isOpening: Boolean, duration: Long): AnimatorSet {
        val animatorSet = AnimatorSet()
        val alphaValues = if (isOpening) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f)
        val scaleValues = if (isOpening) floatArrayOf(0.75f, 1f) else floatArrayOf(1f, 0.75f)

        val alphaAnim = ObjectAnimator.ofFloat(this, View.ALPHA, *alphaValues).apply {
            this.duration = duration
            interpolator = Interpolators.LINEAR
        }

        val scaleXAnim = ObjectAnimator.ofFloat(this, View.SCALE_X, *scaleValues).apply {
            this.duration = duration
            interpolator = if (isOpening) Interpolators.AGGRESSIVE_EASE else Interpolators.ACCEL
        }

        val scaleYAnim = ObjectAnimator.ofFloat(this, View.SCALE_Y, *scaleValues).apply {
            this.duration = duration
            interpolator = if (isOpening) Interpolators.AGGRESSIVE_EASE else Interpolators.ACCEL
        }

        animatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim)
        return animatorSet
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
     * // [Penjelasan]: Menjalankan animasi pembukaan popup container.
     */
    open fun animateOpen() {
        if (mIsOpen) return
        mIsOpen = true

        mOpenCloseAnimator?.cancel()
        val anim = getOpenCloseAnimator(isOpening = true, duration = 200)
        mOpenCloseAnimator = anim

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mOpenCloseAnimator = null
            }
        })
        anim.start()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
     * // [Penjelasan]: Menjalankan animasi penutupan popup container dan memanggil callback penyelesaian.
     */
    open fun animateClose() {
        if (!mIsOpen) return
        mIsOpen = false

        mOpenCloseAnimator?.cancel()
        val anim = getOpenCloseAnimator(isOpening = false, duration = 150)
        mOpenCloseAnimator = anim

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mOpenCloseAnimator = null
                if (!mDeferContainerRemoval) {
                    closeComplete()
                }
            }
        })
        anim.start()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
     * // [Penjelasan]: Menutup popup secara penuh tanpa animasi dan mengeksekusi callback penutupan.
     */
    open fun closeComplete() {
        mOpenCloseAnimator?.cancel()
        mOpenCloseAnimator = null
        mIsOpen = false
        mDeferContainerRemoval = false

        for (callback in mOnCloseCallbacks) {
            callback.run()
        }
        mOnCloseCallbacks.clear()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.ArrowPopup
     * // [Penjelasan]: Menambahkan callback Runnable yang dieksekusi saat popup ditutup secara lengkap.
     */
    fun addOnCloseCallback(callback: Runnable) {
        mOnCloseCallbacks.add(callback)
    }

    fun isOpen(): Boolean = mIsOpen
}
