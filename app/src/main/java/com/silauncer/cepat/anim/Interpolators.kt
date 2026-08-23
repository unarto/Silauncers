package com.silauncer.cepat.anim

import android.graphics.Path
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator

/**
 * Interpolators
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.Interpolators
 * // [Penjelasan]: Kumpulan kurva interpolasi animasi standar dan custom (AOSP Launcher3 Interpolators) untuk menghasilkan transisi UI yang halus, responsif, dan konsisten di seluruh komponen launcher.
 */
object Interpolators {

    @JvmField
    val LINEAR: Interpolator = LinearInterpolator()

    @JvmField
    val ACCEL: Interpolator = AccelerateInterpolator()
    @JvmField
    val ACCEL_0_5: Interpolator = AccelerateInterpolator(0.5f)
    @JvmField
    val ACCEL_0_75: Interpolator = AccelerateInterpolator(0.75f)
    @JvmField
    val ACCEL_1_5: Interpolator = AccelerateInterpolator(1.5f)
    @JvmField
    val ACCEL_2: Interpolator = AccelerateInterpolator(2f)

    @JvmField
    val DEACCEL: Interpolator = DecelerateInterpolator()
    @JvmField
    val DEACCEL_1_5: Interpolator = DecelerateInterpolator(1.5f)
    @JvmField
    val DEACCEL_1_7: Interpolator = DecelerateInterpolator(1.7f)
    @JvmField
    val DEACCEL_2: Interpolator = DecelerateInterpolator(2f)
    @JvmField
    val DEACCEL_2_5: Interpolator = DecelerateInterpolator(2.5f)
    @JvmField
    val DEACCEL_3: Interpolator = DecelerateInterpolator(3f)

    @JvmField
    val ACCEL_DEACCEL: Interpolator = AccelerateDecelerateInterpolator()

    @JvmField
    val FAST_OUT_SLOW_IN: Interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    @JvmField
    val AGGRESSIVE_EASE: Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    @JvmField
    val AGGRESSIVE_EASE_IN_OUT: Interpolator = PathInterpolator(0.6f, 0f, 0.4f, 1f)

    @JvmField
    val DECELERATED_EASE: Interpolator = PathInterpolator(0f, 0f, 0.2f, 1f)
    @JvmField
    val ACCELERATED_EASE: Interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
    @JvmField
    val PREDICTIVE_BACK_DECELERATED_EASE: Interpolator = PathInterpolator(0f, 0f, 0f, 1f)

    @JvmField
    val EMPHASIZED: Interpolator = createEmphasizedInterpolator()
    @JvmField
    val EMPHASIZED_ACCELERATE: Interpolator = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)
    @JvmField
    val EMPHASIZED_DECELERATE: Interpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

    @JvmField
    val EXAGGERATED_EASE: Interpolator = PathInterpolator(Path().apply {
        moveTo(0f, 0f)
        cubicTo(0.05f, 0f, 0.133333f, 0.08f, 0.166666f, 0.4f)
        cubicTo(0.225f, 0.94f, 0.5f, 1f, 1f, 1f)
    })

    @JvmField
    val INSTANT: Interpolator = Interpolator { 1f }

    @JvmField
    val FINAL_FRAME: Interpolator = Interpolator { t -> if (t < 1f) 0f else 1f }

    @JvmField
    val OVERSHOOT_0_75: Interpolator = OvershootInterpolator(0.75f)
    @JvmField
    val OVERSHOOT_1_2: Interpolator = OvershootInterpolator(1.2f)
    @JvmField
    val OVERSHOOT_1_7: Interpolator = OvershootInterpolator(1.7f)

    @JvmField
    val TOUCH_RESPONSE_INTERPOLATOR: Interpolator = PathInterpolator(0.3f, 0f, 0.1f, 1f)
    @JvmField
    val TOUCH_RESPONSE_INTERPOLATOR_ACCEL_DEACCEL: Interpolator =
        Interpolator { v -> ACCEL_DEACCEL.getInterpolation(TOUCH_RESPONSE_INTERPOLATOR.getInterpolation(v)) }

    @JvmField
    val ZOOM_OUT: Interpolator = object : Interpolator {
        private val FOCAL_LENGTH = 0.35f

        override fun getInterpolation(v: Float): Float {
            return (1.0f - FOCAL_LENGTH / (FOCAL_LENGTH + v)) / (1.0f - FOCAL_LENGTH / (FOCAL_LENGTH + 1.0f))
        }
    }

    @JvmField
    val ZOOM_IN: Interpolator = Interpolator { v ->
        DEACCEL_3.getInterpolation(1f - ZOOM_OUT.getInterpolation(1f - v))
    }

    @JvmField
    val SCROLL: Interpolator = Interpolator { t ->
        val x = t - 1.0f
        x * x * x * x * x + 1f
    }

    @JvmField
    val SCROLL_CUBIC: Interpolator = Interpolator { t ->
        val x = t - 1.0f
        x * x * x + 1f
    }

    private const val FAST_FLING_PX_MS = 10f

    @JvmStatic
    fun scrollInterpolatorForVelocity(velocity: Float): Interpolator {
        return if (Math.abs(velocity) > FAST_FLING_PX_MS) SCROLL else SCROLL_CUBIC
    }

    @JvmStatic
    fun overshootInterpolatorForVelocity(velocity: Float): Interpolator {
        return OvershootInterpolator(Math.min(Math.abs(velocity), 3f))
    }

    @JvmStatic
    fun clampToProgress(interpolator: Interpolator, lowerBound: Float, upperBound: Float): Interpolator {
        require(upperBound >= lowerBound) {
            "upperBound ($upperBound) must be greater than or equal to lowerBound ($lowerBound)"
        }
        return Interpolator { t -> clampToProgress(interpolator, t, lowerBound, upperBound) }
    }

    @JvmStatic
    fun clampToProgress(interpolator: Interpolator, progress: Float, lowerBound: Float, upperBound: Float): Float {
        require(upperBound >= lowerBound) {
            "upperBound ($upperBound) must be greater than or equal to lowerBound ($lowerBound)"
        }
        if (progress == lowerBound && progress == upperBound) {
            return if (progress == 0f) 0f else 1f
        }
        if (progress < lowerBound) return 0f
        if (progress > upperBound) return 1f
        return interpolator.getInterpolation((progress - lowerBound) / (upperBound - lowerBound))
    }

    @JvmStatic
    fun clampToProgress(progress: Float, lowerBound: Float, upperBound: Float): Float {
        return clampToProgress(LINEAR, progress, lowerBound, upperBound)
    }

    @JvmStatic
    fun mapToProgress(interpolator: Interpolator, lowerBound: Float, upperBound: Float): Interpolator {
        return Interpolator { t ->
            val fraction = interpolator.getInterpolation(t)
            lowerBound + fraction * (upperBound - lowerBound)
        }
    }

    /**
     * Memetakan nilai dari rentang [fromMin, fromMax] ke rentang [toMin, toMax] dengan kurva interpolator.
     */
    @JvmStatic
    fun mapToRange(
        value: Float,
        fromMin: Float,
        fromMax: Float,
        toMin: Float,
        toMax: Float,
        interpolator: Interpolator
    ): Float {
        if (fromMin == fromMax) return toMin
        val fraction = ((value - fromMin) / (fromMax - fromMin)).coerceIn(0f, 1f)
        return toMin + interpolator.getInterpolation(fraction) * (toMax - toMin)
    }

    @JvmStatic
    fun reverse(interpolator: Interpolator): Interpolator {
        return Interpolator { t -> 1f - interpolator.getInterpolation(1f - t) }
    }

    private fun createEmphasizedInterpolator(): PathInterpolator {
        val path = Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        }
        return PathInterpolator(path)
    }
}
