package com.silauncer.cepat.touch

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

// [Jalur Class]: com.silauncer.cepat.touch.CheckLongPressHelper
// [Penjelasan]: Mengadaptasi mekanisme deteksi long-press Launcher3 presisi dengan mengelola status isPressed, menghitung touch slop, dan memicu haptic feedback adaptif langsung saat long-press terpicu.
class CheckLongPressHelper(
    private val view: View,
    private val listener: View.OnLongClickListener? = null
) {
    private val slop: Float = ViewConfiguration.get(view.context).scaledTouchSlop.toFloat()
    private var longPressTimeoutFactor = 0.75f
    private var hasPerformedLongPress = false
    private var pendingCheckForLongPress: Runnable? = null

    // [Jalur Class]: com.silauncer.cepat.touch.CheckLongPressHelper
    // [Penjelasan]: Menangani aliran sentuhan (MotionEvent) untuk mendeteksi penekanan tombol jangka panjang dengan translasi koordinat lokal (offsetX, offsetY) agar tidak batal oleh jitter atau koordinat parent.
    fun onTouchEvent(ev: MotionEvent, offsetX: Float = 0f, offsetY: Float = 0f) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelLongPress()
                view.isPressed = true
                postCheckForLongPress()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                view.isPressed = false
                cancelLongPress()
            }
            MotionEvent.ACTION_MOVE -> {
                val localX = ev.x - offsetX
                val localY = ev.y - offsetY
                if (!pointInView(view, localX, localY, slop)) {
                    view.isPressed = false
                    cancelLongPress()
                }
            }
        }
    }

    private fun postCheckForLongPress() {
        hasPerformedLongPress = false
        if (pendingCheckForLongPress == null) {
            pendingCheckForLongPress = Runnable { triggerLongPress() }
        }
        view.postDelayed(pendingCheckForLongPress, (ViewConfiguration.getLongPressTimeout() * longPressTimeoutFactor).toLong())
    }

    fun cancelLongPress() {
        hasPerformedLongPress = false
        clearCallbacks()
    }

    fun hasPerformedLongPress(): Boolean {
        return hasPerformedLongPress
    }

    private fun triggerLongPress() {
        if (view.parent != null && view.hasWindowFocus() && !hasPerformedLongPress) {
            // [Jalur Class]: com.silauncer.cepat.touch.CheckLongPressHelper
            // [Penjelasan]: Memberikan umpan balik haptic (HapticFeedbackConstants) yang responsif tepat ketika long-press terpicu.
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val handled = listener?.onLongClick(view) ?: view.performLongClick()
            if (handled) {
                view.isPressed = false
                hasPerformedLongPress = true
            }
            clearCallbacks()
        }
    }

    private fun clearCallbacks() {
        pendingCheckForLongPress?.let {
            view.removeCallbacks(it)
            pendingCheckForLongPress = null
        }
    }

    private fun pointInView(v: View, localX: Float, localY: Float, slop: Float): Boolean {
        // [Jalur Class]: com.silauncer.cepat.touch.CheckLongPressHelper
        // [Penjelasan]: Menggunakan koordinat lokal view (0 hingga width/height) alih-alih koordinat parent (left/top)
        return localX >= -slop && localY >= -slop && localX < (v.width + slop) && localY < (v.height + slop)
    }
}
