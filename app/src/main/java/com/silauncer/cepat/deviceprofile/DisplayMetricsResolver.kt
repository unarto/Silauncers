package com.silauncer.cepat.deviceprofile

import android.content.Context
import android.content.res.Configuration
import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager

/**
 * DisplayMetricsResolver
 *
 * Single Responsibility:
 * Bertanggung jawab secara eksklusif untuk mengekstraksi spesifikasi fisik dan metrik layar aktual
 * (width, height, DPI, density, orientasi, serta system insets) dari Android System Context.
 * Tidak berisi business logic atau tata letak launcher.
 */
class DisplayMetricsResolver {

    data class DisplaySpec(
        val widthPx: Int,
        val heightPx: Int,
        val densityDpi: Int,
        val density: Float,
        val fontScale: Float,
        val isLandscape: Boolean,
        val screenWidthDp: Float,
        val screenHeightDp: Float,
        val smallestScreenWidthDp: Int,
        val insetTopPx: Int,
        val insetBottomPx: Int,
        val insetLeftPx: Int,
        val insetRightPx: Int
    ) {
        val availableWidthPx: Int
            get() = (widthPx - insetLeftPx - insetRightPx).coerceAtLeast(0)

        val availableHeightPx: Int
            get() = (heightPx - insetTopPx - insetBottomPx).coerceAtLeast(0)
    }

    /**
     * Mengekstrak detail resolusi dan dimensi layar secara kompatibel (Android 7.0 s/d Android 14+).
     */
    fun resolve(context: Context): DisplaySpec {
        val resources = context.resources
        val config = resources.configuration
        val metrics: DisplayMetrics = resources.displayMetrics

        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        val fontScale = config.fontScale
        val density = metrics.density
        val densityDpi = metrics.densityDpi
        val smallestScreenWidthDp = config.smallestScreenWidthDp

        var widthPx = metrics.widthPixels
        var heightPx = metrics.heightPixels
        var insetTopPx = 0
        var insetBottomPx = 0
        var insetLeftPx = 0
        var insetRightPx = 0

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            val currentWindowMetrics = windowManager.currentWindowMetrics
            val bounds = currentWindowMetrics.bounds
            widthPx = bounds.width()
            heightPx = bounds.height()

            val windowInsets = currentWindowMetrics.windowInsets
            val insets = windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            insetTopPx = insets.top
            insetBottomPx = insets.bottom
            insetLeftPx = insets.left
            insetRightPx = insets.right
        } else {
            // Fallback untuk API < 30
            val statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (statusBarResId > 0) {
                insetTopPx = resources.getDimensionPixelSize(statusBarResId)
            }
            val navBarResId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (navBarResId > 0) {
                insetBottomPx = resources.getDimensionPixelSize(navBarResId)
            }
        }

        val screenWidthDp = widthPx / density
        val screenHeightDp = heightPx / density

        return DisplaySpec(
            widthPx = widthPx,
            heightPx = heightPx,
            densityDpi = densityDpi,
            density = density,
            fontScale = fontScale,
            isLandscape = isLandscape,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            smallestScreenWidthDp = smallestScreenWidthDp,
            insetTopPx = insetTopPx,
            insetBottomPx = insetBottomPx,
            insetLeftPx = insetLeftPx,
            insetRightPx = insetRightPx
        )
    }

    companion object {
        fun dpToPx(dp: Float, density: Float): Int = (dp * density + 0.5f).toInt()
        fun pxToDp(px: Int, density: Float): Float = if (density > 0) px / density else 0f
        fun spToPx(sp: Float, fontScale: Float, density: Float): Float = sp * fontScale * density
    }
}
