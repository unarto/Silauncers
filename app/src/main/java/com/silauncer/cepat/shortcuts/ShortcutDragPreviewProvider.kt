package com.silauncer.cepat.shortcuts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import com.silauncer.cepat.graphics.DragPreviewProvider

/**
 * ShortcutDragPreviewProvider
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutDragPreviewProvider
 * // [Penjelasan]: Utilitas pembuat pratinjau bitmap/drawable saat deep shortcut diseret (drag and drop) ke workspace yang meng-extend DragPreviewProvider (adaptasi dari AOSP Launcher3 ShortcutDragPreviewProvider).
 */
class ShortcutDragPreviewProvider(
    private val iconView: View,
    private val positionShift: Point = Point(0, 0)
) : DragPreviewProvider(iconView) {

    /**
     * Menghasilkan [Drawable] pratinjau yang telah di-render dan di-scale sesuai ukuran icon target.
     */
    fun createDrawable(targetIconSizePx: Int): Drawable {
        val totalSize = targetIconSizePx + blurSizeOutline
        val bitmap = Bitmap.createBitmap(
            totalSize.coerceAtLeast(1),
            totalSize.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val background = iconView.background
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutDragPreviewProvider
        // [Penjelasan]: Mencegah pemanggilan draw pada RippleDrawable di atas canvas software untuk menghindari error RippleDrawable.STYLE_PATTERNED
        if (background != null && background !is android.graphics.drawable.RippleDrawable) {
            val bounds = getDrawableBounds(background)
            canvas.save()
            canvas.translate((blurSizeOutline / 2).toFloat(), (blurSizeOutline / 2).toFloat())
            val scaleX = targetIconSizePx.toFloat() / bounds.width().coerceAtLeast(1)
            val scaleY = targetIconSizePx.toFloat() / bounds.height().coerceAtLeast(1)
            canvas.scale(scaleX, scaleY, 0f, 0f)
            background.draw(canvas)
            canvas.restore()
        } else {
            // Gambar langsung dari view jika background tidak tersedia atau merupakan RippleDrawable
            canvas.save()
            canvas.translate((blurSizeOutline / 2).toFloat(), (blurSizeOutline / 2).toFloat())
            val wasPressed = iconView.isPressed
            try {
                if (wasPressed) iconView.isPressed = false
                background?.jumpToCurrentState()
                if (background is android.graphics.drawable.RippleDrawable) {
                    iconView.background = null
                    iconView.draw(canvas)
                    iconView.background = background
                } else {
                    iconView.draw(canvas)
                }
            } finally {
                if (wasPressed) iconView.isPressed = wasPressed
            }
            canvas.restore()
        }

        return BitmapDrawable(iconView.resources, bitmap)
    }

    /**
     * Menghitung offset posisi dan skala untuk transisi drag layer.
     */
    fun calculatePositionAndScale(
        targetSizePx: Int,
        outLocation: IntArray
    ): Float {
        iconView.getLocationOnScreen(outLocation)
        outLocation[0] += positionShift.x
        outLocation[1] += positionShift.y
        val viewWidth = iconView.width.coerceAtLeast(1)
        return targetSizePx.toFloat() / viewWidth
    }
}
