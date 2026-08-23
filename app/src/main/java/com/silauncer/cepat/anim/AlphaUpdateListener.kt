package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup

/**
 * AlphaUpdateListener
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.AlphaUpdateListener
 * // [Penjelasan]: Listener animasi yang secara otomatis memperbarui status visibilitas view (VISIBLE / INVISIBLE) dan focusability ViewGroup saat nilai alpha berubah melintasi threshold cutoff (adaptasi dari AOSP Launcher3 AlphaUpdateListener).
 */
class AlphaUpdateListener(private val view: View) : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {

    override fun onAnimationUpdate(animation: ValueAnimator) {
        updateVisibility(view)
    }

    override fun onAnimationEnd(animation: Animator) {
        updateVisibility(view)
    }

    override fun onAnimationStart(animation: Animator) {
        view.visibility = View.VISIBLE
    }

    companion object {
        const val ALPHA_CUTOFF_THRESHOLD = 0.01f

        /**
         * Memperbarui visibilitas view berdasarkan nilai alpha saat ini.
         */
        @JvmStatic
        fun updateVisibility(view: View) {
            if (view.alpha < ALPHA_CUTOFF_THRESHOLD && view.visibility != View.INVISIBLE) {
                view.visibility = View.INVISIBLE
            } else if (view.alpha > ALPHA_CUTOFF_THRESHOLD && view.visibility != View.VISIBLE) {
                if (view is ViewGroup) {
                    val oldFocusability = view.descendantFocusability
                    view.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    view.visibility = View.VISIBLE
                    view.descendantFocusability = oldFocusability
                } else {
                    view.visibility = View.VISIBLE
                }
            }
        }
    }
}
