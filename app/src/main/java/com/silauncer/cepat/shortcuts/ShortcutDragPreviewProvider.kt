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
        if (background != null) {
            val bounds = getDrawableBounds(background)
            canvas.save()
            canvas.translate((blurSizeOutline / 2).toFloat(), (blurSizeOutline / 2).toFloat())
            val scaleX = targetIconSizePx.toFloat() / bounds.width().coerceAtLeast(1)
            val scaleY = targetIconSizePx.toFloat() / bounds.height().coerceAtLeast(1)
            canvas.scale(scaleX, scaleY, 0f, 0f)
            background.draw(canvas)
            canvas.restore()
        } else {
            // Gambar langsung dari view jika background tidak tersedia
            canvas.save()
            canvas.translate((blurSizeOutline / 2).toFloat(), (blurSizeOutline / 2).toFloat())
            iconView.draw(canvas)
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
