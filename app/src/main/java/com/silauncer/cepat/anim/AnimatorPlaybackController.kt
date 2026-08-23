package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import com.silauncer.cepat.anim.Interpolators.LINEAR
import com.silauncer.cepat.anim.Interpolators.clampToProgress
import com.silauncer.cepat.anim.Interpolators.scrollInterpolatorForVelocity
import java.util.Collections

/**
 * AnimatorPlaybackController
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.AnimatorPlaybackController
 * // [Penjelasan]: Pengendali playback animasi (AnimatorSet) yang mendukung scrubbing manual, physics-driven velocity fling, pause, reverse, dan integrasi animasi pegas (adaptasi dari AOSP Launcher3 AnimatorPlaybackController).
 */
class AnimatorPlaybackController internal constructor(
    private val mAnim: AnimatorSet,
    private val mDuration: Long,
    childAnims: List<Holder>
) : ValueAnimator.AnimatorUpdateListener {

    private val mAnimationPlayer: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        interpolator = LINEAR
        addListener(OnAnimationEndDispatcher())
        addUpdateListener(this@AnimatorPlaybackController)
    }

    private val mChildAnimations: Array<Holder> = childAnims.toTypedArray()
    protected var mCurrentFraction: Float = 0f
    private var mEndAction: Runnable? = null
    protected var mTargetCancelled: Boolean = false

    init {
        mAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                mTargetCancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                mTargetCancelled = false
            }

            override fun onAnimationStart(animation: Animator) {
                mTargetCancelled = false
            }
        })
    }

    val target: AnimatorSet
        get() = mAnim

    val duration: Long
        get() = mDuration

    val interpolator: TimeInterpolator
        get() = mAnim.interpolator ?: LINEAR

    val animationPlayer: ValueAnimator
        get() = mAnimationPlayer

    val progressFraction: Float
        get() = mCurrentFraction

    val interpolatedProgress: Float
        get() = interpolator.getInterpolation(mCurrentFraction)

    fun start() {
        mAnimationPlayer.setFloatValues(mCurrentFraction, 1f)
        mAnimationPlayer.duration = clampDuration(1f - mCurrentFraction)
        mAnimationPlayer.start()
    }

    fun reverse() {
        mAnimationPlayer.setFloatValues(mCurrentFraction, 0f)
        mAnimationPlayer.duration = clampDuration(mCurrentFraction)
        mAnimationPlayer.start()
    }

    fun startWithVelocity(
        context: Context,
        goingToEnd: Boolean,
        velocityPxPerMs: Float,
        endDistance: Float,
        animationDuration: Long
    ) {
        val distanceInverse = 1f / Math.abs(endDistance).coerceAtLeast(1f)
        val velocityProgressPerMs = velocityPxPerMs * distanceInverse
        val singleFrameMs = SpringAnimationBuilder.getSingleFrameMs(context)
        val oneFrameProgress = velocityProgressPerMs * singleFrameMs
        val nextFrameProgress = (progressFraction + oneFrameProgress).coerceIn(0f, 1f)

        val springFlag = if (goingToEnd) {
            SpringProperty.FLAG_CAN_SPRING_ON_END
        } else {
            SpringProperty.FLAG_CAN_SPRING_ON_START
        }

        var springDuration = animationDuration
        for (h in mChildAnimations) {
            if ((h.springProperty.flags and springFlag) != 0) {
                val s = SpringAnimationBuilder(context)
                    .setStartValue(mCurrentFraction)
                    .setEndValue(if (goingToEnd) 1f else 0f)
                    .setStartVelocity(velocityProgressPerMs)
                    .setMinimumVisibleChange(distanceInverse)
                    .setDampingRatio(h.springProperty.dampingRatio)
                    .setStiffness(h.springProperty.stiffness)
                    .computeParams()

                val expectedDurationL = s.getDuration()
                springDuration = Math.max(expectedDurationL, springDuration)
                val expectedDuration = expectedDurationL.toFloat()

                h.mapper = ProgressMapper { _, _ ->
                    if (expectedDuration <= 0f || oneFrameProgress >= 1f) {
                        1f
                    } else {
                        val playProgress = (mAnimationPlayer.currentPlayTime / expectedDuration).coerceIn(0f, 1f)
                        val fromLow = Math.abs(oneFrameProgress)
                        fromLow + playProgress * (1f - fromLow)
                    }
                }
                h.anim.interpolator = TimeInterpolator { fraction -> s.getInterpolatedValue(fraction) }
            }
        }

        mAnimationPlayer.setFloatValues(nextFrameProgress, if (goingToEnd) 1f else 0f)

        if (springDuration <= animationDuration) {
            mAnimationPlayer.duration = animationDuration
            mAnimationPlayer.interpolator = scrollInterpolatorForVelocity(velocityPxPerMs)
        } else {
            mAnimationPlayer.duration = springDuration
            val cutOff = animationDuration / springDuration.toFloat()
            mAnimationPlayer.interpolator = clampToProgress(scrollInterpolatorForVelocity(velocityPxPerMs), 0f, cutOff)
        }
        mAnimationPlayer.start()
    }

    fun forceFinishIfCloseToEnd() {
        if (mAnimationPlayer.isRunning && mAnimationPlayer.animatedFraction > ANIMATION_COMPLETE_THRESHOLD) {
            mAnimationPlayer.end()
        }
    }

    fun pause() {
        for (h in mChildAnimations) {
            h.reset()
        }
        mAnimationPlayer.cancel()
    }

    fun setPlayFraction(fraction: Float) {
        mCurrentFraction = fraction
        if (mTargetCancelled) return

        val progress = fraction.coerceIn(0f, 1f)
        for (holder in mChildAnimations) {
            holder.setProgress(progress)
        }
    }

    fun setEndAction(runnable: Runnable?) {
        mEndAction = runnable
    }

    override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
        setPlayFraction(valueAnimator.animatedValue as Float)
    }

    protected fun clampDuration(fraction: Float): Long {
        val playPos = mDuration * fraction
        return if (playPos <= 0) 0L else Math.min(playPos.toLong(), mDuration)
    }

    fun dispatchOnStart(): AnimatorPlaybackController {
        callListenerCommandRecursively(mAnim) { listener, a -> listener.onAnimationStart(a) }
        return this
    }

    fun dispatchOnCancel(): AnimatorPlaybackController {
        callListenerCommandRecursively(mAnim) { listener, a -> listener.onAnimationCancel(a) }
        return this
    }

    fun dispatchOnEnd(): AnimatorPlaybackController {
        callListenerCommandRecursively(mAnim) { listener, a -> listener.onAnimationEnd(a) }
        return this
    }

    fun dispatchSetInterpolator(interpolator: TimeInterpolator) {
        callAnimatorCommandRecursively(mAnim) { a -> a.interpolator = interpolator }
    }

    private inner class OnAnimationEndDispatcher : AnimationSuccessListener() {
        private var mDispatched = false

        override fun onAnimationStart(animation: Animator) {
            mCancelled = false
            mDispatched = false
        }

        override fun onAnimationSuccess(animator: Animator) {
            if (!mDispatched) {
                dispatchOnEnd()
                mEndAction?.run()
                mDispatched = true
            }
        }
    }

    fun interface ProgressMapper {
        fun getProgress(progress: Float, globalProgress: Float): Float

        companion object {
            val DEFAULT = ProgressMapper { progress, globalEndProgress ->
                if (progress > globalEndProgress) 1f else (progress / globalEndProgress)
            }
        }
    }

    class Holder(
        val anim: ValueAnimator,
        globalDuration: Float,
        val springProperty: SpringProperty
    ) {
        val interpolator: TimeInterpolator? = anim.interpolator
        val globalEndProgress: Float = anim.duration / globalDuration
        var mapper: ProgressMapper = ProgressMapper.DEFAULT

        fun setProgress(progress: Float) {
            anim.setCurrentFraction(mapper.getProgress(progress, globalEndProgress))
        }

        fun reset() {
            anim.interpolator = interpolator
            mapper = ProgressMapper.DEFAULT
        }
    }

    companion object {
        private const val ANIMATION_COMPLETE_THRESHOLD = 0.95f

        @JvmStatic
        fun wrap(anim: AnimatorSet, duration: Long): AnimatorPlaybackController {
            val childAnims = ArrayList<Holder>()
            addAnimationHoldersRecur(anim, duration, SpringProperty.DEFAULT, childAnims)
            return AnimatorPlaybackController(anim, duration, childAnims)
        }

        @JvmStatic
        fun callListenerCommandRecursively(
            anim: Animator,
            command: (AnimatorListener, Animator) -> Unit
        ) {
            callAnimatorCommandRecursively(anim) { a ->
                val listeners = a.listeners ?: Collections.emptyList()
                for (l in listeners) {
                    command(l, a)
                }
            }
        }

        @JvmStatic
        fun callAnimatorCommandRecursively(anim: Animator, command: (Animator) -> Unit) {
            command(anim)
            if (anim is AnimatorSet) {
                val children = anim.childAnimations ?: Collections.emptyList()
                for (child in children) {
                    callAnimatorCommandRecursively(child, command)
                }
            }
        }

        @JvmStatic
        fun addAnimationHoldersRecur(
            anim: Animator,
            globalDuration: Long,
            springProperty: SpringProperty,
            out: ArrayList<Holder>
        ) {
            val forceDuration = anim.duration
            val forceInterpolator = anim.interpolator
            if (anim is ValueAnimator) {
                out.add(Holder(anim, globalDuration.toFloat(), springProperty))
            } else if (anim is AnimatorSet) {
                for (child in anim.childAnimations) {
                    if (forceDuration > 0) {
                        child.duration = forceDuration
                    }
                    if (forceInterpolator != null) {
                        child.interpolator = forceInterpolator
                    }
                    addAnimationHoldersRecur(child, globalDuration, springProperty, out)
                }
            } else {
                throw IllegalArgumentException("Tipe animasi tidak dikenal: $anim")
            }
        }
    }
}
