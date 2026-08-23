package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider

/**
 * RevealOutlineAnimation
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.RevealOutlineAnimation
 * // [Penjelasan]: Base ViewOutlineProvider untuk efek animasi clipping reveal shape yang memodifikasi outline radius dan bounding rect secara dinamis sepanjang animasi (adaptasi dari AOSP Launcher3 RevealOutlineAnimation).
 */
abstract class RevealOutlineAnimation : ViewOutlineProvider() {

    @JvmField
    protected val mOutline: Rect = Rect()

    @JvmField
    protected var mOutlineRadius: Float = 0f

    abstract fun shouldRemoveElevationDuringAnimation(): Boolean
    abstract fun setProgress(progress: Float)

    @JvmOverloads
    fun createRevealAnimator(
        revealView: View,
        isReversed: Boolean,
        startProgress: Float = 0f
    ): ValueAnimator {
        val va = if (isReversed) {
            ValueAnimator.ofFloat(1f - startProgress, 0f)
        } else {
            ValueAnimator.ofFloat(startProgress, 1f)
        }
        val elevation = revealView.elevation

        va.addListener(object : AnimatorListenerAdapter() {
            private var isClippedToOutline = false
            private var oldOutlineProvider: ViewOutlineProvider? = null

            override fun onAnimationStart(animation: Animator) {
                isClippedToOutline = revealView.clipToOutline
                oldOutlineProvider = revealView.outlineProvider

                revealView.outlineProvider = this@RevealOutlineAnimation
                revealView.clipToOutline = true
                if (shouldRemoveElevationDuringAnimation()) {
                    revealView.translationZ = -elevation
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                revealView.outlineProvider = oldOutlineProvider
                revealView.clipToOutline = isClippedToOutline
                if (shouldRemoveElevationDuringAnimation()) {
                    revealView.translationZ = 0f
                }
            }
        })

        va.addUpdateListener { v ->
            val progress = v.animatedValue as Float
            setProgress(progress)
            revealView.invalidateOutline()
        }
        return va
    }

    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(mOutline, mOutlineRadius)
    }

    fun getRadius(): Float = mOutlineRadius

    fun getOutline(out: Rect) {
        out.set(mOutline)
    }
}
