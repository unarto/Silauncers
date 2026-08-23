package com.silauncer.cepat.anim

import androidx.dynamicanimation.animation.SpringForce

/**
 * SpringProperty
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.SpringProperty
 * // [Penjelasan]: Model konfigurasi parameter fisika pegas (damping ratio, stiffness, dan flag arah pemutaran pegas) (adaptasi dari AOSP Launcher3 SpringProperty).
 */
class SpringProperty @JvmOverloads constructor(
    @JvmField val flags: Int = 0
) {

    @JvmField
    var dampingRatio: Float = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY

    @JvmField
    var stiffness: Float = SpringForce.STIFFNESS_MEDIUM

    fun setDampingRatio(dampingRatio: Float): SpringProperty {
        this.dampingRatio = dampingRatio
        return this
    }

    fun setStiffness(stiffness: Float): SpringProperty {
        this.stiffness = stiffness
        return this
    }

    companion object {
        const val FLAG_CAN_SPRING_ON_END = 1 shl 0
        const val FLAG_CAN_SPRING_ON_START = 1 shl 1

        @JvmField
        val DEFAULT = SpringProperty()
    }
}
