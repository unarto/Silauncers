package com.silauncer.cepat.touch

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * OverScroll
 *
 * // [Jalur Class]: com.silauncer.cepat.touch.OverScroll
 * // [Penjelasan]: Utilitas matematis untuk meredam pergeseran gestur usap yang melebihi batas tampilan (overscroll damping) pada Launcher3.
 */
object OverScroll {

    const val OVERSCROLL_DAMP_FACTOR = 0.07f

    /**
     * Kurva pengaruh peredaman overscroll.
     */
    private fun overScrollInfluenceCurve(f: Float): Float {
        val t = f - 1.0f
        return t * t * t + 1.0f
    }

    /**
     * Menghitung nilai perpindahan yang diredam berdasarkan jumlah tarikan dan batas maksimum.
     */
    @JvmStatic
    fun dampedScroll(amount: Float, max: Int): Float {
        if (amount == 0f || max == 0) return 0f

        var f = amount / max.toFloat()
        val absF = abs(f)
        f = (f / absF) * overScrollInfluenceCurve(absF)

        if (abs(f) >= 1f) {
            f /= abs(f)
        }

        return (OVERSCROLL_DAMP_FACTOR * f * max).roundToInt().toFloat()
    }
}
