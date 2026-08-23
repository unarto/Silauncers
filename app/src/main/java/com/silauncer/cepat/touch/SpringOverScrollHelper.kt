package com.silauncer.cepat.touch

import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView

/**
 * SpringOverScrollHelper
 *
 * // [Jalur Class]: com.silauncer.cepat.touch.SpringOverScrollHelper
 * // [Penjelasan]: Utilitas over-scroll dinamis berbasis DynamicAnimation SpringAnimation untuk RecyclerView.
 * Menggantikan efek glow biru platform dengan pergeseran translasi fisik pegas yang mulus.
 */
object SpringOverScrollHelper {

    // [Jalur Class]: com.silauncer.cepat.touch.SpringOverScrollHelper
    // [Penjelasan]: Memasang custom EdgeEffectFactory pada RecyclerView untuk mensimulasikan overscroll spring yang reaktif
    fun setup(recyclerView: RecyclerView) {
        recyclerView.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return object : EdgeEffect(view.context) {
                    
                    override fun onPull(deltaDistance: Float) {
                        super.onPull(deltaDistance)
                        handlePull(deltaDistance)
                    }

                    override fun onPull(deltaDistance: Float, displacement: Float) {
                        super.onPull(deltaDistance, displacement)
                        handlePull(deltaDistance)
                    }

                    private fun handlePull(deltaDistance: Float) {
                        val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                        val translationYDelta = sign * view.height * deltaDistance * 0.2f
                        view.translationY += translationYDelta
                    }

                    override fun onRelease() {
                        super.onRelease()
                        if (view.translationY != 0f) {
                            val anim = SpringAnimation(view, SpringAnimation.TRANSLATION_Y, 0f)
                            anim.spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                            anim.spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                            anim.start()
                        }
                    }

                    override fun onAbsorb(velocity: Int) {
                        super.onAbsorb(velocity)
                        val sign = if (direction == DIRECTION_BOTTOM) -1 else 1
                        val translationVelocity = sign * velocity * 0.5f
                        val anim = SpringAnimation(view, SpringAnimation.TRANSLATION_Y, 0f)
                        anim.setStartVelocity(translationVelocity)
                        anim.spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                        anim.spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                        anim.start()
                    }

                    override fun draw(canvas: Canvas?): Boolean {
                        return false // Jangan gambar efek glow biru bawaan
                    }
                }
            }
        }
    }
}
