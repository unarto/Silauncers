package com.silauncer.cepat.util

/**
 * CellAndSpan
 *
 * // [Jalur Class]: com.silauncer.cepat.util.CellAndSpan
 * // [Penjelasan]: Kelas data penampung area sel pada grid workspace (cellX, cellY, spanX, spanY) adaptasi AOSP Launcher3.
 */
data class CellAndSpan(
    var cellX: Int = -1,
    var cellY: Int = -1,
    var spanX: Int = 1,
    var spanY: Int = 1
) {
    /**
     * // [Jalur Class]: com.silauncer.cepat.util.CellAndSpan
     * // [Penjelasan]: Menyalin koordinat sel dan ukuran span dari objek CellAndSpan lain.
     */
    fun copyFrom(copy: CellAndSpan) {
        cellX = copy.cellX
        cellY = copy.cellY
        spanX = copy.spanX
        spanY = copy.spanY
    }

    override fun toString(): String {
        return "($cellX, $cellY: $spanX, $spanY)"
    }
}
