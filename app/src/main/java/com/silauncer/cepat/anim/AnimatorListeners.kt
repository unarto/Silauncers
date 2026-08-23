package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator

/**
 * AnimatorListeners
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.AnimatorListeners
 * // [Penjelasan]: Utilitas pembuat listener animasi umum (success callback, end callback dengan status boolean, atau standard end callback) (adaptasi dari AOSP Launcher3 AnimatorListeners).
 */
object AnimatorListeners {

    private const val SUCCESS_TRANSITION_PROGRESS = 0.5f

    /**
     * Mengembalikan [AnimatorListener] yang menjalankan callback saat animasi selesai dengan sukses tanpa dibatalkan.
     */
    @JvmStatic
    fun forSuccessCallback(callback: Runnable): AnimatorListener {
        return object : AnimationSuccessListener() {
            override fun onAnimationSuccess(animator: Animator) {
                callback.run()
            }
        }
    }

    /**
     * Mengembalikan [AnimatorListener] yang mengeksekusi callback dengan boolean bernilai true jika animasi sukses mencapai target.
     */
    @JvmStatic
    fun forEndCallback(callback: (Boolean) -> Unit): AnimatorListener {
        return object : AnimatorListenerAdapter() {
            private var listenerCalled = false

            override fun onAnimationCancel(animation: Animator) {
                if (!listenerCalled) {
                    listenerCalled = true
                    callback(false)
                }
            }

            override fun onAnimationEnd(anim: Animator) {
                if (!listenerCalled) {
                    listenerCalled = true
                    val success = if (anim is ValueAnimator) {
                        anim.animatedFraction > SUCCESS_TRANSITION_PROGRESS
                    } else {
                        true
                    }
                    callback(success)
                }
            }
        }
    }

    /**
     * Mengembalikan [AnimatorListener] yang selalu mengeksekusi runnable callback saat animasi berakhir (baik dibatalkan atau selesai).
     */
    @JvmStatic
    fun forEndCallback(callback: Runnable): AnimatorListener {
        return object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                callback.run()
            }
        }
    }
}
