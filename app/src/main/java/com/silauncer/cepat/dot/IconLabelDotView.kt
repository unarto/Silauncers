package com.silauncer.cepat.dot

import android.view.View

/**
 * IconLabelDotView
 *
 * // [Jalur Class]: com.silauncer.cepat.dot.IconLabelDotView
 * // [Penjelasan]: Interface standar untuk view yang memiliki icon, label, dan notification dot (adaptasi dari AOSP Launcher3 IconLabelDotView).
 */
interface IconLabelDotView {
    fun setIconVisible(visible: Boolean)
    fun setForceHideDot(hide: Boolean)

    companion object {
        /**
         * Mengatur visibilitas icon dan dot secara bersamaan.
         */
        fun setIconAndDotVisible(view: View, visible: Boolean) {
            if (view is IconLabelDotView) {
                view.setIconVisible(visible)
                view.setForceHideDot(!visible)
            } else {
                view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
            }
        }
    }
}
