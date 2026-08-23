package com.silauncer.cepat.folder

import android.graphics.Point

/**
 * FolderGridOrganizer
 *
 * // [Jalur Class]: com.silauncer.cepat.folder.FolderGridOrganizer
 * // [Penjelasan]: Pengorganisir posisi grid 3 kolom untuk item-item di dalam folder terbuka (adaptasi dari AOSP Launcher3 FolderGridOrganizer).
 * Mengatur mapping peringkat (rank) ke koordinat sel (cellX, cellY) dan pemisah halaman jika melebihi batas.
 */
class FolderGridOrganizer(
    val countX: Int = 3,
    val maxCountY: Int = 4
) {
    val maxItemsPerPage: Int = countX * maxCountY

    /**
     * Mengonversi index peringkat item menjadi koordinat Point(col, row).
     */
    fun getPosForRank(rank: Int, outPoint: Point): Point {
        val pageRank = rank % maxItemsPerPage
        outPoint.x = pageRank % countX
        outPoint.y = pageRank / countX
        return outPoint
    }

    /**
     * Mendapatkan nomor halaman untuk rank tertentu.
     */
    fun getPageForRank(rank: Int): Int {
        return rank / maxItemsPerPage
    }

    /**
     * Menghitung total halaman yang dibutuhkan untuk menampung totalItem.
     */
    fun getNumPages(totalItem: Int): Int {
        if (totalItem <= 0) return 1
        return (totalItem + maxItemsPerPage - 1) / maxItemsPerPage
    }
}
