package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.annotation.CallSuper

/**
 * AnimationSuccessListener
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.AnimationSuccessListener
 * // [Penjelasan]: Listener adapter untuk animasi yang menjamin callback onAnimationSuccess hanya dipanggil jika animasi selesai secara normal tanpa dibatalkan (adaptasi dari AOSP Launcher3 AnimationSuccessListener).
 */
abstract class AnimationSuccessListener : AnimatorListenerAdapter() {

    @JvmField
    protected var mCancelled: Boolean = false

    @CallSuper
    override fun onAnimationCancel(animation: Animator) {
        mCancelled = true
    }

    override fun onAnimationEnd(animation: Animator) {
        if (!mCancelled) {
            onAnimationSuccess(animation)
        }
    }

    /**
     * Dipanggil hanya ketika animasi selesai secara penuh dan sukses tanpa pernah di-cancel sebelumnya.
     */
    abstract fun onAnimationSuccess(animator: Animator)
}
