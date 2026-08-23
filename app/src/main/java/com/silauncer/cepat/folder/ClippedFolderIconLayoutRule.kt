package com.silauncer.cepat.folder

import android.graphics.PointF
import android.graphics.Rect

/**
 * ClippedFolderIconLayoutRule
 *
 * // [Jalur Class]: com.silauncer.cepat.folder.ClippedFolderIconLayoutRule
 * // [Penjelasan]: Aturan tata letak thumbnail item di dalam ikon folder workspace (adaptasi dari AOSP Launcher3 ClippedFolderIconLayoutRule).
 * Mengatur batas maksimal pratinjau (9 item untuk grid 3x3) serta kalkulasi posisi relatif mini icon.
 */
class ClippedFolderIconLayoutRule(
    val numColumns: Int = 3,
    val numRows: Int = 3
) {
    val maxNumItemsInPreview: Int = numColumns * numRows

    /**
     * Menghitung posisi relatif (x, y dalam koordinat 0..1 atau piksel) untuk item indeks ke-i di pratinjau.
     */
    fun getPreviewItemPosition(index: Int, bounds: Rect, outPoint: PointF): Boolean {
        if (index < 0 || index >= maxNumItemsInPreview) {
            return false
        }
        val col = index % numColumns
        val row = index / numColumns

        val cellWidth = bounds.width().toFloat() / numColumns
        val cellHeight = bounds.height().toFloat() / numRows

        outPoint.x = bounds.left + col * cellWidth + cellWidth / 2f
        outPoint.y = bounds.top + row * cellHeight + cellHeight / 2f
        return true
    }

    companion object {
        const val MAX_NUM_ITEMS_IN_PREVIEW = 9
    }
}
