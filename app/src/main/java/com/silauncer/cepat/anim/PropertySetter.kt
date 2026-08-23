package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.util.FloatProperty
import android.util.IntProperty
import android.view.View

/**
 * PropertySetter
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.PropertySetter
 * // [Penjelasan]: Abstraksi setter untuk mengubah properti View (alpha, background color, properti float/int/color) baik secara langsung tanpa animasi maupun melalui pendaftaran animasi (adaptasi dari AOSP Launcher3 PropertySetter).
 */
abstract class PropertySetter {

    companion object {
        @JvmField
        protected val NO_OP: AnimatorSet = AnimatorSet()

        @JvmField
        val NO_ANIM_PROPERTY_SETTER: PropertySetter = object : PropertySetter() {
            override fun add(animator: Animator) {
                animator.duration = 0
                animator.start()
                animator.end()
            }
        }
    }

    open fun setViewAlpha(view: View?, alpha: Float, interpolator: TimeInterpolator): Animator {
        if (view != null) {
            view.alpha = alpha
            AlphaUpdateListener.updateVisibility(view)
        }
        return NO_OP
    }

    open fun setViewBackgroundColor(view: View?, color: Int, interpolator: TimeInterpolator): Animator {
        view?.setBackgroundColor(color)
        return NO_OP
    }

    open fun <T> setFloat(
        target: T,
        property: FloatProperty<T>,
        value: Float,
        interpolator: TimeInterpolator
    ): Animator {
        property.setValue(target, value)
        return NO_OP
    }

    open fun <T> setInt(
        target: T,
        property: IntProperty<T>,
        value: Int,
        interpolator: TimeInterpolator
    ): Animator {
        property.setValue(target, value)
        return NO_OP
    }

    open fun <T> setColor(
        target: T,
        property: IntProperty<T>,
        value: Int,
        interpolator: TimeInterpolator
    ): Animator {
        property.setValue(target, value)
        return NO_OP
    }

    abstract fun add(animator: Animator)

    open fun addEndListener(listener: (Boolean) -> Unit) {
        listener(true)
    }

    open fun buildAnim(): AnimatorSet {
        return NO_OP
    }
}
