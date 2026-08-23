package com.silauncer.cepat.keyboard

import android.graphics.Rect
import android.view.View

/**
 * SpatialFocusNavigationHelper
 *
 * // [Jalur Class]: com.silauncer.cepat.keyboard.SpatialFocusNavigationHelper
 * // [Penjelasan]: Algoritma penemuan fokus spasial 2D untuk navigasi D-pad dan Keyboard
 * (adaptasi dari AOSP Launcher3 KeyboardDragAndDropView.getNextSelection).
 * Menggunakan perbandingan jarak berbobot pada sumbu utama (major axis) dan sumbu minor (minor axis)
 * untuk menemukan node/elemen berikutnya secara deterministik.
 */
class SpatialFocusNavigationHelper {

    /**
     * Menemukan target berikutnya dalam daftar elemen berdasarkan arah navigasi (FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD).
     *
     * @param items Daftar elemen
     * @param currentItem Elemen saat ini
     * @param boundsProvider Lambda untuk mengambil Rect bounding box dari elemen
     * @param direction Arah navigasi fokus dari [View]
     */
    fun <T> findNextFocus(
        items: List<T>,
        currentItem: T?,
        boundsProvider: (T) -> Rect,
        direction: Int
    ): T? {
        if (items.isEmpty()) return null
        val totalNodes = items.size
        val currentIndex = if (currentItem != null) items.indexOf(currentItem) else -1

        if (direction == View.FOCUS_FORWARD) {
            return if (currentIndex < 0) items.first() else items[(currentIndex + 1) % totalNodes]
        }
        if (direction == View.FOCUS_BACKWARD) {
            return if (currentIndex < 0) items.last() else items[(currentIndex + totalNodes - 1) % totalNodes]
        }

        if (currentItem == null || currentIndex < 0) {
            return items.firstOrNull()
        }

        val currentRect = boundsProvider(currentItem)

        val majorAxis: (Rect, Rect) -> Int
        val minorAxis: (Rect) -> Int

        when (direction) {
            View.FOCUS_RIGHT -> {
                majorAxis = { source, dest -> dest.left - source.left }
                minorAxis = { it.centerY() }
            }
            View.FOCUS_LEFT -> {
                majorAxis = { source, dest -> source.left - dest.left }
                minorAxis = { it.centerY() }
            }
            View.FOCUS_UP -> {
                majorAxis = { source, dest -> source.top - dest.top }
                minorAxis = { it.centerX() }
            }
            View.FOCUS_DOWN -> {
                majorAxis = { source, dest -> dest.top - source.top }
                minorAxis = { it.centerX() }
            }
            else -> return null
        }

        var minWeight = Float.MAX_VALUE
        var match: T? = null

        for (i in 0 until totalNodes) {
            val candidate = items[i]
            if (candidate == currentItem) continue

            val destRect = boundsProvider(candidate)
            val majorAxisWeight = majorAxis(currentRect, destRect)
            if (majorAxisWeight <= 0) {
                continue
            }
            val minorAxisWeight = minorAxis(destRect) - minorAxis(currentRect)
            val weight = (majorAxisWeight * majorAxisWeight) +
                    (minorAxisWeight * minorAxisWeight * MINOR_AXIS_WEIGHT)

            if (weight < minWeight) {
                minWeight = weight
                match = candidate
            }
        }

        return match
    }

    companion object {
        const val MINOR_AXIS_WEIGHT = 13f
    }
}
