package com.silauncer.cepat.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import com.silauncer.cepat.R

/**
 * DragPreviewProvider
 *
 * // [Jalur Class]: com.silauncer.cepat.graphics.DragPreviewProvider
 * // [Penjelasan]: Utilitas pembuat bitmap dan drawable pratinjau saat item diseret (drag preview), serta perhitungan posisi dan skala transisi (adaptasi dari AOSP Launcher3 DragPreviewProvider).
 */
open class DragPreviewProvider(
    protected val view: View,
    context: Context = view.context
) {

    private val tempRect = Rect()

    val blurSizeOutline: Int = context.resources.getDimensionPixelSize(
        R.dimen.folder_preview_padding
    )
    val previewPadding: Int = blurSizeOutline

    /**
     * Menggambar view ke dalam kanvas tujuan dengan skala tertentu.
     * // [Jalur Class]: com.silauncer.cepat.graphics.DragPreviewProvider
     * // [Penjelasan]: Menangani penggambaran view secara aman pada kanvas software tanpa memicu error RippleDrawable STYLE_PATTERNED
     */
    protected open fun drawDragView(destCanvas: Canvas, scale: Float) {
        val saveCount = destCanvas.save()
        destCanvas.scale(scale, scale)
        destCanvas.translate(blurSizeOutline / 2f, blurSizeOutline / 2f)

        val background = view.background
        val wasPressed = view.isPressed
        val wasSelected = view.isSelected
        val wasActivated = view.isActivated
        try {
            if (wasPressed) view.isPressed = false
            if (wasSelected) view.isSelected = false
            if (wasActivated) view.isActivated = false
            background?.jumpToCurrentState()

            if (background is android.graphics.drawable.RippleDrawable) {
                view.background = null
                view.draw(destCanvas)
                view.background = background
            } else {
                view.draw(destCanvas)
            }
        } finally {
            if (wasPressed) view.isPressed = wasPressed
            if (wasSelected) view.isSelected = wasSelected
            if (wasActivated) view.isActivated = wasActivated
        }

        destCanvas.restoreToCount(saveCount)
    }

    /**
     * Menghasilkan drawable pratinjau dari view yang sedang diseret.
     */
    open fun createDrawable(): Drawable {
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        val scale = view.scaleX

        val totalWidth = width + blurSizeOutline
        val totalHeight = height + blurSizeOutline

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawDragView(canvas, scale)

        return BitmapDrawable(view.resources, bitmap)
    }

    /**
     * Menghitung skala dan titik posisi view di layar.
     */
    open fun getScaleAndPosition(preview: Drawable, outPos: IntArray): Float {
        view.getLocationOnScreen(outPos)
        val scale = view.scaleX
        outPos[0] = Math.round(outPos[0] - (preview.intrinsicWidth - scale * view.width * view.scaleX) / 2f)
        outPos[1] = Math.round(outPos[1] - (1 - scale) * preview.intrinsicHeight / 2f - previewPadding / 2f)
        return scale
    }

    /**
     * Menghitung skala dan posisi target view.
     */
    open fun getScaleAndPosition(targetView: View, outPos: IntArray): Float {
        targetView.getLocationOnScreen(outPos)
        val scale = targetView.scaleX
        outPos[0] = Math.round(outPos[0] - (targetView.width - scale * targetView.width * targetView.scaleX) / 2f)
        outPos[1] = Math.round(outPos[1] - (1 - scale) * targetView.height / 2f - previewPadding / 2f)
        return scale
    }

    /**
     * Mengonversi bitmap preview menjadi alpha-only bitmap mask.
     */
    fun convertPreviewToAlphaBitmap(preview: Bitmap): Bitmap {
        return preview.copy(Bitmap.Config.ALPHA_8, true)
    }

    companion object {
        fun getDrawableBounds(d: Drawable?): Rect {
            val bounds = Rect()
            if (d == null) return bounds
            d.copyBounds(bounds)
            if (bounds.width() == 0 || bounds.height() == 0) {
                bounds.set(0, 0, d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1))
            } else {
                bounds.offsetTo(0, 0)
            }
            return bounds
        }
    }
}
