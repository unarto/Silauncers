package com.silauncer.cepat.dragndrop

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import kotlin.math.hypot

/**
 * FolderCollisionHelper
 *
 * Single Responsibility:
 * Menghitung tabrakan spasial (spatial collision detection) antara pointer sentuhan/drag
 * dengan view-view item pada workspace grid, serta mengelola animasi visual feedback (hover state).
 */
class FolderCollisionHelper(private val context: Context) {

    data class DropTargetResult(
        val position: Int,
        val viewHolder: RecyclerView.ViewHolder,
        val itemView: View,
        val distancePx: Float
    )

    private val creationRadiusPx: Float = context.resources.getDimension(R.dimen.folder_creation_radius)
    private val hoverScale: Float = getFloatDimension(R.dimen.folder_hover_scale, 1.15f)
    private val animDurationMs: Long = context.resources.getInteger(R.integer.folder_hover_anim_duration_ms).toLong()

    private var currentHoverView: View? = null
    private var currentHoverPosition: Int = RecyclerView.NO_POSITION

    /**
     * Mencari target cell yang berada dalam radius toleransi tabrakan folder creation.
     */
    fun findDropTarget(
        recyclerView: RecyclerView,
        dragX: Float,
        dragY: Float,
        draggedPosition: Int
    ): DropTargetResult? {
        val childCount = recyclerView.childCount
        var closestResult: DropTargetResult? = null
        var minDistance = Float.MAX_VALUE

        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val vh = recyclerView.getChildViewHolder(child) ?: continue
            val pos = vh.bindingAdapterPosition

            if (pos == RecyclerView.NO_POSITION || pos == draggedPosition) {
                continue
            }

            // Hitung titik tengah target cell dalam koordinat RecyclerView
            val centerX = child.x + child.width / 2f
            val centerY = child.y + child.height / 2f

            val distance = hypot(dragX - centerX, dragY - centerY)

            if (distance <= creationRadiusPx && distance < minDistance) {
                minDistance = distance
                closestResult = DropTargetResult(pos, vh, child, distance)
            }
        }

        return closestResult
    }

    /**
     * Mencari posisi adapter pada workspace yang paling dekat dengan koordinat (rawX, rawY).
     * Jika tidak ada yang cocok, mengembalikan posisi akhir item count.
     */
    fun findNearestWorkspacePosition(
        recyclerView: RecyclerView,
        rawX: Float,
        rawY: Float
    ): Int {
        val location = IntArray(2)
        recyclerView.getLocationOnScreen(location)
        val relativeX = rawX - location[0]
        val relativeY = rawY - location[1]

        val childUnder = recyclerView.findChildViewUnder(relativeX, relativeY)
        if (childUnder != null) {
            val vh = recyclerView.getChildViewHolder(childUnder)
            if (vh != null && vh.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                return vh.bindingAdapterPosition
            }
        }

        val childCount = recyclerView.childCount
        var closestPos = RecyclerView.NO_POSITION
        var minDistance = Float.MAX_VALUE

        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val vh = recyclerView.getChildViewHolder(child) ?: continue
            val pos = vh.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) continue

            val centerX = child.x + child.width / 2f
            val centerY = child.y + child.height / 2f
            val distance = hypot(relativeX - centerX, relativeY - centerY)

            if (distance < minDistance) {
                minDistance = distance
                closestPos = pos
            }
        }

        val adapterItemCount = recyclerView.adapter?.itemCount ?: 0
        return if (closestPos != RecyclerView.NO_POSITION) closestPos else adapterItemCount
    }

    /**
     * Mengatur visual feedback hover saat item yang di-drag berada di atas kandidat target folder.
     */
    fun updateHoverState(target: DropTargetResult?) {
        if (target != null) {
            if (currentHoverPosition != target.position) {
                clearHover()
                currentHoverView = target.itemView
                currentHoverPosition = target.position
                animateHoverEnter(target.itemView)
            }
        } else {
            clearHover()
        }
    }

    /**
     * Mengembalikan visual feedback hover ke kondisi normal.
     */
    fun clearHover() {
        currentHoverView?.let { view ->
            animateHoverExit(view)
        }
        currentHoverView = null
        currentHoverPosition = RecyclerView.NO_POSITION
    }

    private fun getFloatDimension(resId: Int, defaultValue: Float): Float {
        val typedValue = TypedValue()
        return try {
            context.resources.getValue(resId, typedValue, true)
            typedValue.float
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun animateHoverEnter(view: View) {
        view.animate()
            .scaleX(hoverScale)
            .scaleY(hoverScale)
            .setDuration(animDurationMs)
            .start()
    }

    private fun animateHoverExit(view: View) {
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(animDurationMs)
            .start()
    }
}
