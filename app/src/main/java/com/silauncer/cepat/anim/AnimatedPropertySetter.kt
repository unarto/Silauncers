package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.FloatProperty
import android.util.IntProperty
import android.view.View

/**
 * AnimatedPropertySetter
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.AnimatedPropertySetter
 * // [Penjelasan]: Implementasi PropertySetter yang membangun dan mengelompokkan animasi properti ke dalam AnimatorSet dengan pembaruan visibilitas alpha otomatis dan listener frame/selesai (adaptasi dari AOSP Launcher3 AnimatedPropertySetter).
 */
open class AnimatedPropertySetter : PropertySetter() {

    @JvmField
    protected val mAnim: AnimatorSet = AnimatorSet()

    @JvmField
    protected var mProgressAnimator: ValueAnimator? = null

    override fun setViewAlpha(view: View?, alpha: Float, interpolator: TimeInterpolator): Animator {
        if (view == null) return NO_OP

        if (java.lang.Float.compare(view.alpha, alpha) == 0) {
            AlphaUpdateListener.updateVisibility(view)
            return NO_OP
        }

        val anim = ObjectAnimator.ofFloat(view, View.ALPHA, alpha)
        anim.addListener(AlphaUpdateListener(view))
        anim.interpolator = interpolator
        add(anim)
        return anim
    }

    override fun setViewBackgroundColor(view: View?, color: Int, interpolator: TimeInterpolator): Animator {
        if (view == null) return NO_OP
        val bg = view.background
        if (bg is ColorDrawable && bg.color == color) {
            return NO_OP
        }
        val anim = ObjectAnimator.ofArgb(view, VIEW_BACKGROUND_COLOR, color)
        anim.interpolator = interpolator
        add(anim)
        return anim
    }

    override fun <T> setFloat(
        target: T,
        property: FloatProperty<T>,
        value: Float,
        interpolator: TimeInterpolator
    ): Animator {
        if (property.get(target) == value) {
            return NO_OP
        }
        val anim = ObjectAnimator.ofFloat(target, property, value)
        anim.interpolator = interpolator
        add(anim)
        return anim
    }

    override fun <T> setInt(
        target: T,
        property: IntProperty<T>,
        value: Int,
        interpolator: TimeInterpolator
    ): Animator {
        if (property.get(target) == value) {
            return NO_OP
        }
        val anim = ObjectAnimator.ofInt(target, property, value)
        anim.interpolator = interpolator
        add(anim)
        return anim
    }

    override fun <T> setColor(
        target: T,
        property: IntProperty<T>,
        value: Int,
        interpolator: TimeInterpolator
    ): Animator {
        if (property.get(target) == value) {
            return NO_OP
        }
        val anim = ObjectAnimator.ofArgb(target, property, value)
        anim.interpolator = interpolator
        add(anim)
        return anim
    }

    fun addOnFrameCallback(runnable: Runnable) {
        addOnFrameListener { runnable.run() }
    }

    fun addOnFrameListener(listener: ValueAnimator.AnimatorUpdateListener) {
        if (mProgressAnimator == null) {
            mProgressAnimator = ValueAnimator.ofFloat(0f, 1f)
        }
        mProgressAnimator?.addUpdateListener(listener)
    }

    override fun addEndListener(listener: (Boolean) -> Unit) {
        if (mProgressAnimator == null) {
            mProgressAnimator = ValueAnimator.ofFloat(0f, 1f)
        }
        mProgressAnimator?.addListener(AnimatorListeners.forEndCallback(listener))
    }

    fun addListener(listener: AnimatorListener) {
        mAnim.addListener(listener)
    }

    override fun add(animator: Animator) {
        mAnim.play(animator)
    }

    override fun buildAnim(): AnimatorSet {
        val progressAnim = mProgressAnimator
        if (progressAnim != null) {
            add(progressAnim)
            mProgressAnimator = null
        }
        return mAnim
    }

    companion object {
        @JvmField
        val VIEW_BACKGROUND_COLOR: IntProperty<View> = object : IntProperty<View>("backgroundColor") {
            override fun setValue(view: View, color: Int) {
                view.setBackgroundColor(color)
            }

            override fun get(view: View): Int {
                val bg = view.background
                return if (bg is ColorDrawable) bg.color else Color.TRANSPARENT
            }
        }
    }
}
