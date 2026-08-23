package com.silauncer.cepat.keyboard

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import com.silauncer.cepat.R

/**
 * FocusIndicatorHelper
 *
 * // [Jalur Class]: com.silauncer.cepat.keyboard.FocusIndicatorHelper
 * // [Penjelasan]: Kelas pembantu untuk menggambar latar belakang fokus dari sebuah View (adaptasi dari AOSP Launcher3 FocusIndicatorHelper).
 * Mengimplementasikan OnFocusChangeListener untuk langsung merespons perpindahan fokus keyboard/D-pad.
 */
abstract class FocusIndicatorHelper(
    container: View,
    color: Int = ContextCompat.getColor(container.context, R.color.focused_background)
) : ItemFocusIndicatorHelper<View>(container, color), View.OnFocusChangeListener {

    init {
        radius = container.resources.getDimension(R.dimen.focus_indicator_corner_radius)
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (v != null) {
            changeFocus(v, hasFocus)
        }
    }

    override fun shouldDraw(item: View): Boolean {
        return item.isAttachedToWindow && item.visibility == View.VISIBLE
    }

    /**
     * Subclass sederhana yang mengasumsikan view target adalah anak langsung dari kontainer.
     */
    open class SimpleFocusIndicatorHelper(container: View) : FocusIndicatorHelper(container) {
        override fun viewToRect(item: View, outRect: Rect) {
            outRect.set(item.left, item.top, item.right, item.bottom)
        }
    }
}
