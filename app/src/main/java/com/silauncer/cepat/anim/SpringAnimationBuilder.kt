package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.FloatProperty
import androidx.annotation.FloatRange
import androidx.dynamicanimation.animation.SpringForce
import com.silauncer.cepat.anim.Interpolators.LINEAR

/**
 * SpringAnimationBuilder
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.SpringAnimationBuilder
 * // [Penjelasan]: Utilitas perumus kurva fisika pegas teredam (underdamped spring equations) untuk menghitung durasi ekuilibrium dan membangun ValueAnimator yang mengikuti lintasan gaya pegas (adaptasi dari AOSP Launcher3 SpringAnimationBuilder).
 */
class SpringAnimationBuilder(private val context: Context) {

    private var startValue: Float = 0f
    private var endValue: Float = 0f
    private var velocity: Float = 0f

    private var stiffness: Float = SpringForce.STIFFNESS_MEDIUM
    private var dampingRatio: Float = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
    private var minVisibleChange: Float = 1f

    private var beta: Double = 0.0
    private var gamma: Double = 0.0
    private var a: Double = 0.0
    private var b: Double = 0.0
    private var va: Double = 0.0
    private var vb: Double = 0.0

    private var valueThreshold: Double = 0.0
    private var velocityThreshold: Double = 0.0

    private var duration: Float = 0f

    fun setEndValue(value: Float): SpringAnimationBuilder {
        this.endValue = value
        return this
    }

    fun setStartValue(value: Float): SpringAnimationBuilder {
        this.startValue = value
        return this
    }

    fun setValues(vararg values: Float): SpringAnimationBuilder {
        if (values.size > 1) {
            startValue = values[0]
            endValue = values[values.size - 1]
        } else if (values.isNotEmpty()) {
            endValue = values[0]
        }
        return this
    }

    fun setStiffness(
        @FloatRange(from = 0.0, fromInclusive = false) stiffness: Float
    ): SpringAnimationBuilder {
        require(stiffness > 0f) { "Spring stiffness constant must be positive." }
        this.stiffness = stiffness
        return this
    }

    fun setDampingRatio(
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false) dampingRatio: Float
    ): SpringAnimationBuilder {
        require(dampingRatio > 0f && dampingRatio < 1f) { "Damping ratio must be between 0 and 1" }
        this.dampingRatio = dampingRatio
        return this
    }

    fun setMinimumVisibleChange(
        @FloatRange(from = 0.0, fromInclusive = false) minimumVisibleChange: Float
    ): SpringAnimationBuilder {
        require(minimumVisibleChange > 0f) { "Minimum visible change must be positive." }
        this.minVisibleChange = minimumVisibleChange
        return this
    }

    fun setStartVelocity(startVelocity: Float): SpringAnimationBuilder {
        this.velocity = startVelocity
        return this
    }

    fun getInterpolatedValue(fraction: Float): Float {
        return getValue(duration * fraction)
    }

    private fun getValue(time: Float): Float {
        return (exponentialComponent(time.toDouble()) * cosSinX(time.toDouble())).toFloat() + endValue
    }

    fun computeParams(): SpringAnimationBuilder {
        val singleFrameMs = getSingleFrameMs(context)
        val naturalFreq = Math.sqrt(stiffness.toDouble())
        val dampedFreq = naturalFreq * Math.sqrt(1.0 - (dampingRatio * dampingRatio).toDouble())

        beta = 2.0 * dampingRatio * naturalFreq
        gamma = dampedFreq
        a = (startValue - endValue).toDouble()
        b = beta * a / (2.0 * gamma) + (velocity / gamma).toDouble()

        va = a * beta / 2.0 - b * gamma
        vb = a * gamma + beta * b / 2.0

        valueThreshold = (minVisibleChange * THRESHOLD_MULTIPLIER).toDouble()
        velocityThreshold = valueThreshold * 1000.0 / singleFrameMs

        var dur = Math.atan2(-a, b) / gamma
        val piByG = Math.PI / gamma
        while (dur < 0.0 || Math.abs(exponentialComponent(dur) * cosSinV(dur)) >= velocityThreshold) {
            dur += piByG
        }

        val edgeTime = Math.max(0.0, dur - piByG / 2.0)
        var minEdge = edgeTime
        val minDiff = singleFrameMs / 2000.0

        var currentDur = dur
        do {
            if ((currentDur - minEdge) < minDiff) {
                break
            }
            val mid = (minEdge + currentDur) / 2.0
            if (isAtEquilibrium(mid)) {
                currentDur = mid
            } else {
                minEdge = mid
            }
        } while (true)

        this.duration = currentDur.toFloat()
        return this
    }

    fun getDuration(): Long {
        return (1000.0 * duration).toLong()
    }

    fun <T> build(target: T, property: FloatProperty<T>): ValueAnimator {
        computeParams()

        val animator = ValueAnimator.ofFloat(0f, duration)
        animator.duration = getDuration()
        animator.interpolator = LINEAR
        animator.addUpdateListener { anim ->
            property.setValue(target, getInterpolatedValue(anim.animatedFraction))
        }
        animator.addListener(object : AnimationSuccessListener() {
            // [Jalur Class]: com.silauncer.cepat.anim.SpringAnimationBuilder
            // [Penjelasan]: Menyelaraskan nama parameter 'animator' dengan deklarasi pada kelas induk AnimationSuccessListener untuk menghindari warning incompatibility.
            override fun onAnimationSuccess(animator: Animator) {
                property.setValue(target, endValue)
            }
        })
        return animator
    }

    private fun isAtEquilibrium(t: Double): Boolean {
        val ec = exponentialComponent(t)
        if (Math.abs(ec * cosSinX(t)) >= valueThreshold) {
            return false
        }
        return Math.abs(ec * cosSinV(t)) < velocityThreshold
    }

    private fun exponentialComponent(t: Double): Double {
        return Math.pow(Math.E, -beta * t / 2.0)
    }

    private fun cosSinX(t: Double): Double {
        return cosSin(t, a, b)
    }

    private fun cosSinV(t: Double): Double {
        return cosSin(t, va, vb)
    }

    private fun cosSin(t: Double, cosFactor: Double, sinFactor: Double): Double {
        val angle = t * gamma
        return cosFactor * Math.cos(angle) + sinFactor * Math.sin(angle)
    }

    companion object {
        private const val THRESHOLD_MULTIPLIER = 0.65f

        @JvmStatic
        fun getSingleFrameMs(context: Context): Int {
            val refreshRate = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    context.display?.refreshRate ?: 60f
                } else {
                    60f
                }
            } catch (e: Exception) {
                60f
            }
            return (1000f / if (refreshRate > 0f) refreshRate else 60f).toInt().coerceAtLeast(1)
        }
    }
}
