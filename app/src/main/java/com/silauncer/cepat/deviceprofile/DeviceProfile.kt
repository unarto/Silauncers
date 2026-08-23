package com.silauncer.cepat.deviceprofile

import kotlin.math.min

/**
 * DeviceProfile
 *
 * Single Responsibility:
 * Menghitung dan menyimpan variabel dimensi tata letak dinamis (runtime layout metrics)
 * saat aplikasi berjalan berdasarkan konfigurasi acuan [InvariantDeviceProfile],
 * spesifikasi fisik layar [DisplayMetricsResolver.DisplaySpec], dan aturan konfigurasi [ProfileConfig].
 *
 * Mengkalkulasi: cellWidthPx, cellHeightPx, actualIconSizePx, textVisibility/size,
 * workspacePadding, dan margin grid TANPA menggunakan magic number di dalam logika kalkulasi.
 */
class DeviceProfile(
    val inv: InvariantDeviceProfile,
    val displaySpec: DisplayMetricsResolver.DisplaySpec,
    val config: ProfileConfig = ProfileConfig()
) {
    // Flag form-factor & orientasi
    val isLandscape: Boolean = displaySpec.isLandscape
    val isTablet: Boolean = displaySpec.smallestScreenWidthDp >= config.swThresholdTabletDp
    val isFoldable: Boolean = displaySpec.smallestScreenWidthDp in config.swThresholdFoldableDp until config.swThresholdTabletDp

    // Dimensi Layar
    val widthPx: Int = displaySpec.widthPx
    val heightPx: Int = displaySpec.heightPx
    val availableWidthPx: Int = displaySpec.availableWidthPx
    val availableHeightPx: Int = displaySpec.availableHeightPx

    // Grid configuration
    val numColumns: Int = inv.numColumns
    val numRows: Int = inv.numRows

    // Padding workspace (dalam pixel)
    val workspacePaddingLeftPx: Int
    val workspacePaddingRightPx: Int
    val workspacePaddingTopPx: Int
    val workspacePaddingBottomPx: Int

    // Ukuran Spacing antar item (dalam pixel)
    val iconSpacingPx: Int

    // Dimensi Cell Grid (dalam pixel)
    val cellWidthPx: Int
    val cellHeightPx: Int

    // Ukuran Ikon & Teks Terkalkulasi (dalam pixel)
    val actualIconSizePx: Int
    val actualLabelSizePx: Float
    val isLabelVisible: Boolean

    // Skala spring load workspace untuk state edit/drag
    val workspaceSpringLoadScale: Float = 0.85f

    init {
        val density = displaySpec.density

        // 1. Ekstraksi base padding dari config
        val baseHorizontalPaddingPx = DisplayMetricsResolver.dpToPx(config.workspacePaddingHorizontalDp, density)
        val baseVerticalPaddingPx = DisplayMetricsResolver.dpToPx(config.workspacePaddingVerticalDp, density)

        workspacePaddingLeftPx = baseHorizontalPaddingPx + displaySpec.insetLeftPx
        workspacePaddingRightPx = baseHorizontalPaddingPx + displaySpec.insetRightPx
        workspacePaddingTopPx = baseVerticalPaddingPx + displaySpec.insetTopPx
        workspacePaddingBottomPx = baseVerticalPaddingPx + displaySpec.insetBottomPx

        // 2. Hitung spacing antar item
        iconSpacingPx = DisplayMetricsResolver.dpToPx(inv.iconSpacingDp, density)

        // 3. Kalkulasi ruang bersih yang tersedia untuk grid sel
        val totalUsableWidth = (widthPx - workspacePaddingLeftPx - workspacePaddingRightPx).coerceAtLeast(0)
        val totalUsableHeight = (heightPx - workspacePaddingTopPx - workspacePaddingBottomPx).coerceAtLeast(0)

        // 4. Hitung lebar dan tinggi cell
        val totalHorizontalSpacing = (numColumns - 1).coerceAtLeast(0) * iconSpacingPx
        val totalVerticalSpacing = (numRows - 1).coerceAtLeast(0) * iconSpacingPx

        val rawCellWidth = if (numColumns > 0) {
            (totalUsableWidth - totalHorizontalSpacing) / numColumns
        } else {
            0
        }

        val rawCellHeight = if (numRows > 0) {
            (totalUsableHeight - totalVerticalSpacing) / numRows
        } else {
            0
        }

        cellWidthPx = rawCellWidth.coerceAtLeast(0)
        cellHeightPx = rawCellHeight.coerceAtLeast(0)

        // 5. Hitung ukuran teks label
        isLabelVisible = inv.showAppLabel
        actualLabelSizePx = DisplayMetricsResolver.spToPx(
            inv.iconTextSizeSp,
            displaySpec.fontScale,
            density
        )

        // 6. Hitung ukuran ikon aktual dengan safety clamping berbasis config
        val safetyMarginPx = DisplayMetricsResolver.dpToPx(config.cellSafetyMarginDp, density)
        val minSafetyIconSizePx = DisplayMetricsResolver.dpToPx(config.minSafetyIconSizeDp, density)

        val targetIconSizePx = DisplayMetricsResolver.dpToPx(inv.iconSizeDp, density)
        val maxAllowedIconWidth = (cellWidthPx - safetyMarginPx).coerceAtLeast(0)
        val textHeightEstimatePx = if (isLabelVisible) (actualLabelSizePx * config.lineHeightMultiplier).toInt() else 0
        val maxAllowedIconHeight = (cellHeightPx - textHeightEstimatePx - safetyMarginPx).coerceAtLeast(0)

        val maxAllowedIconSize = min(maxAllowedIconWidth, maxAllowedIconHeight)

        actualIconSizePx = if (maxAllowedIconSize in 1 until targetIconSizePx) {
            maxAllowedIconSize
        } else {
            targetIconSizePx
        }.coerceAtLeast(minSafetyIconSizePx)
    }

    /**
     * Helper kalkulator posisi bounds icon dalam sebuah cell.
     */
    fun calculateIconTopOffset(): Int {
        val totalContentHeight = actualIconSizePx + (if (isLabelVisible) actualLabelSizePx.toInt() else 0)
        return ((cellHeightPx - totalContentHeight) / 2).coerceAtLeast(0)
    }

    companion object {
        // [Jalur Class]: com.silauncer.cepat.deviceprofile.DeviceProfile
        // [Penjelasan]: Mengambil atau mengkalkulasi DeviceProfile melalui DeviceProfileCache guna mencegah penghitungan ulang layout metrik yang identik.
        fun get(
            inv: InvariantDeviceProfile,
            displaySpec: DisplayMetricsResolver.DisplaySpec,
            config: ProfileConfig = ProfileConfig()
        ): DeviceProfile {
            return com.silauncer.cepat.cache.DeviceProfileCache.getOrCalculate(inv, displaySpec, config)
        }
    }
}
