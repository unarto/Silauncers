package com.silauncer.cepat.workspace

import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.hypot

/**
 * CellLayout
 *
 * [Jalur Class]: com.silauncer.cepat.workspace.CellLayout
 * [Penjelasan]: Pengelola spasial grid workspace ala Launcher3 AOSP. Bertanggung jawab atas:
 * 1. Menentukan indeks sel target (findMatchingCellToTarget) saat koordinat sentuhan bergeser.
 * 2. Menggerakkan ikon tetangga secara dinamis dan mulus (realtimeReorder) menggunakan animasi pergeseran halus
 *    sehingga membuka ruang bagi ikon yang diseret sebelum dilepaskan.
 * 3. Menghitung titik koordinat presisi (cellToPoint) untuk animasi SNAP balik saat drop terjadi.
 * 4. Mereset translasi ikon tetangga (resetReorder) ke posisi resting semula saat drag selesai/batal.
 */
class CellLayout {

    private var currentTargetPos: Int = RecyclerView.NO_POSITION
    private val interpolator = DecelerateInterpolator(1.5f)

    // [Jalur Class]: com.silauncer.cepat.workspace.CellLayout
    // [Penjelasan]: Mencari indeks posisi cell adapter yang paling cocok dengan titik tengah ikon yang diseret
    fun findMatchingCellToTarget(
        dragCenterX: Float,
        dragCenterY: Float,
        recyclerView: RecyclerView,
        columns: Int,
        itemCount: Int
    ): Int {
        if (itemCount <= 0) return 0

        val location = IntArray(2)
        recyclerView.getLocationOnScreen(location)
        val relX = dragCenterX - location[0]
        val relY = dragCenterY - location[1]

        // 1. Coba cari view di bawah koordinat sentuhan
        val childUnder = recyclerView.findChildViewUnder(relX, relY)
        if (childUnder != null) {
            val vh = recyclerView.getChildViewHolder(childUnder)
            if (vh != null && vh.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                return vh.bindingAdapterPosition.coerceIn(0, itemCount - 1)
            }
        }

        // 2. Jika tidak persis di atas view, cari cell terdekat berdasarkan jarak Euclidean
        val childCount = recyclerView.childCount
        var closestPos = RecyclerView.NO_POSITION
        var minDistance = Float.MAX_VALUE

        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val vh = recyclerView.getChildViewHolder(child) ?: continue
            val pos = vh.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) continue

            val centerX = child.left + child.width / 2f
            val centerY = child.top + child.height / 2f
            val dist = hypot(relX - centerX, relY - centerY)

            if (dist < minDistance) {
                minDistance = dist
                closestPos = pos
            }
        }

        if (closestPos != RecyclerView.NO_POSITION) {
            return closestPos.coerceIn(0, itemCount - 1)
        }

        // 3. Fallback matematika grid jika belum ada child ter-render
        val itemWidth = if (recyclerView.width > 0 && columns > 0) recyclerView.width / columns else 1
        val itemHeight = itemWidth // Asumsi rasio 1:1
        val col = (relX / itemWidth).toInt().coerceIn(0, columns - 1)
        val row = (relY / itemHeight).toInt().coerceAtLeast(0)
        val estimatedIndex = (row * columns + col).coerceIn(0, itemCount - 1)
        return estimatedIndex
    }

    // [Jalur Class]: com.silauncer.cepat.workspace.CellLayout
    // [Penjelasan]: Menggeser posisi view tetangga secara real-time untuk membuka ruang kosong pada slot target
    fun realtimeReorder(
        fromPos: Int,
        targetPos: Int,
        recyclerView: RecyclerView,
        columns: Int
    ) {
        if (targetPos == currentTargetPos || fromPos == RecyclerView.NO_POSITION) return
        currentTargetPos = targetPos

        val safeCols = if (columns > 0) columns else 4
        val childCount = recyclerView.childCount

        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val vh = recyclerView.getChildViewHolder(child) ?: continue
            val pos = vh.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION || pos == fromPos) continue

            // Hitung slot tujuan untuk tetangga ini
            val destSlot = when {
                fromPos < targetPos && pos in (fromPos + 1)..targetPos -> pos - 1
                fromPos > targetPos && pos in targetPos until fromPos -> pos + 1
                else -> pos
            }

            val targetDx: Float
            val targetDy: Float

            if (destSlot == pos) {
                targetDx = 0f
                targetDy = 0f
            } else {
                val colDiff = (destSlot % safeCols) - (pos % safeCols)
                val rowDiff = (destSlot / safeCols) - (pos / safeCols)
                val cellW = child.width.toFloat()
                val cellH = child.height.toFloat()
                targetDx = colDiff * cellW
                targetDy = rowDiff * cellH
            }

            // Animasi translasi mulus menuju target offset
            if (child.translationX != targetDx || child.translationY != targetDy) {
                child.animate()
                    .translationX(targetDx)
                    .translationY(targetDy)
                    .setDuration(150)
                    .setInterpolator(interpolator)
                    .start()
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.workspace.CellLayout
    // [Penjelasan]: Menghitung posisi koordinat layar (screen X, Y) dari titik tengah atau asal cell target untuk animasi SNAP balik
    fun cellToPoint(
        targetPos: Int,
        recyclerView: RecyclerView,
        columns: Int,
        rootView: ViewGroup
    ): PointF {
        val rootLoc = IntArray(2)
        rootView.getLocationOnScreen(rootLoc)

        val vh = recyclerView.findViewHolderForAdapterPosition(targetPos)
        if (vh != null && vh.itemView.width > 0) {
            val viewLoc = IntArray(2)
            vh.itemView.getLocationOnScreen(viewLoc)
            // Kurangi efek translasi sementara dari animasi reflow agar snapping mendarat di posisi asli layout
            val originX = viewLoc[0] - vh.itemView.translationX - rootLoc[0]
            val originY = viewLoc[1] - vh.itemView.translationY - rootLoc[1]
            return PointF(originX, originY)
        }

        // Fallback hitung posisi grid jika view belum diikat
        val rvLoc = IntArray(2)
        recyclerView.getLocationOnScreen(rvLoc)
        val safeCols = if (columns > 0) columns else 4
        val col = targetPos % safeCols
        val row = targetPos / safeCols
        val cellW = if (recyclerView.width > 0) recyclerView.width / safeCols else 150
        val cellH = cellW

        val x = rvLoc[0] + col * cellW - rootLoc[0]
        val y = rvLoc[1] + row * cellH - rootLoc[1]
        return PointF(x.toFloat(), y.toFloat())
    }

    // [Jalur Class]: com.silauncer.cepat.workspace.CellLayout
    // [Penjelasan]: Mengembalikan semua translasi tetangga ke 0 (posisi awal) saat drag berakhir atau dibatalkan
    fun resetReorder(recyclerView: RecyclerView, animate: Boolean = false) {
        currentTargetPos = RecyclerView.NO_POSITION
        val childCount = recyclerView.childCount
        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            child.animate().cancel()
            if (animate) {
                child.animate()
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(120)
                    .setInterpolator(interpolator)
                    .start()
            } else {
                child.translationX = 0f
                child.translationY = 0f
            }
        }
    }
}
