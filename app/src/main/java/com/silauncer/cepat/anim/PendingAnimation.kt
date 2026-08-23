package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.util.FloatProperty
import com.silauncer.cepat.anim.AnimatorPlaybackController.Companion.addAnimationHoldersRecur
import com.silauncer.cepat.anim.AnimatorPlaybackController.Holder

/**
 * PendingAnimation
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.PendingAnimation
 * // [Penjelasan]: Pembungkus AnimatedPropertySetter yang melacak durasi global, child animation holder, serta konfigurasi pegas untuk menghasilkan AnimatorPlaybackController (adaptasi dari AOSP Launcher3 PendingAnimation).
 */
class PendingAnimation(val duration: Long) : AnimatedPropertySetter() {

    private val mAnimHolders = ArrayList<Holder>()

    fun add(anim: Animator, interpolator: TimeInterpolator, springProperty: SpringProperty) {
        anim.interpolator = interpolator
        add(anim, springProperty)
    }

    override fun add(animator: Animator) {
        add(animator, SpringProperty.DEFAULT)
    }

    fun add(animator: Animator, springProperty: SpringProperty) {
        animator.duration = duration
        mAnim.play(animator)
        addAnimationHoldersRecur(animator, duration, springProperty, mAnimHolders)
    }

    fun setInterpolator(interpolator: TimeInterpolator) {
        mAnim.interpolator = interpolator
    }

    fun <T> addFloat(
        target: T,
        property: FloatProperty<T>,
        from: Float,
        to: Float,
        interpolator: TimeInterpolator
    ) {
        val anim = ObjectAnimator.ofFloat(target, property, from, to)
        anim.interpolator = interpolator
        add(anim)
    }

    override fun buildAnim(): AnimatorSet {
        if (mAnimHolders.isEmpty()) {
            add(ValueAnimator.ofFloat(0f, 1f).apply { duration = this@PendingAnimation.duration })
        }
        return super.buildAnim()
    }

    fun createPlaybackController(): AnimatorPlaybackController {
        return AnimatorPlaybackController(buildAnim(), duration, mAnimHolders)
    }
}
