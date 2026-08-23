package com.silauncer.cepat.graphics

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils

/**
 * IconPalette
 *
 * // [Jalur Class]: com.silauncer.cepat.graphics.IconPalette
 * // [Penjelasan]: Utilitas penentuan dan perhitungan kontras warna (contrast ratio) berbasis model warna LAB & HSV untuk icon, badge, notification dot, dan teks (adaptasi dari AOSP Launcher3 IconPalette).
 */
object IconPalette {

    private const val MIN_PRELOAD_COLOR_SATURATION = 0.2f
    private const val MIN_PRELOAD_COLOR_LIGHTNESS = 0.6f
    private const val MIN_CONTRAST_RATIO = 4.5

    /**
     * Menghasilkan warna kontras yang cocok untuk indikator progres/preload berbasis warna dominan.
     */
    fun getPreloadProgressColor(dominantColor: Int, fallbackAccent: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(dominantColor, hsv)
        return if (hsv[1] < MIN_PRELOAD_COLOR_SATURATION) {
            fallbackAccent
        } else {
            hsv[2] = hsv[2].coerceAtLeast(MIN_PRELOAD_COLOR_LIGHTNESS)
            Color.HSVToColor(hsv)
        }
    }

    /**
     * Memastikan kontras warna foreground terhadap background memenuhi rasio minimum (WCAG 4.5:1).
     */
    fun resolveContrastColor(color: Int, background: Int): Int {
        return ensureTextContrast(color, background)
    }

    /**
     * Menemukan warna dengan hue yang sama namun memiliki kontras yang cukup terhadap warna background.
     */
    fun ensureTextContrast(color: Int, bg: Int): Int {
        return findContrastColor(color, bg, MIN_CONTRAST_RATIO)
    }

    /**
     * Menyesuaikan lightness (L) pada ruang warna LAB hingga mencapai target rasio kontras.
     */
    fun findContrastColor(fg: Int, bg: Int, minRatio: Double): Int {
        if (ColorUtils.calculateContrast(fg, bg) >= minRatio) {
            return fg
        }

        val labBg = DoubleArray(3)
        ColorUtils.colorToLAB(bg, labBg)
        val bgL = labBg[0]

        val labFg = DoubleArray(3)
        ColorUtils.colorToLAB(fg, labFg)
        val fgL = labFg[0]
        val isBgDark = bgL < 50.0

        var low = if (isBgDark) fgL else 0.0
        var high = if (isBgDark) 100.0 else fgL
        val a = labFg[1]
        val b = labFg[2]

        var result = fg
        for (i in 0 until 15) {
            if (high - low <= 0.00001) break
            val l = (low + high) / 2.0
            result = ColorUtils.LABToColor(l, a, b)
            if (ColorUtils.calculateContrast(result, bg) > minRatio) {
                if (isBgDark) high = l else low = l
            } else {
                if (isBgDark) low = l else high = l
            }
        }
        return ColorUtils.LABToColor(low, a, b)
    }
}
