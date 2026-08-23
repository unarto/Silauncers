package com.silauncer.cepat.deviceprofile

import android.content.res.Resources
import com.silauncer.cepat.R

/**
 * ProfileConfig
 *
 * Single Responsibility:
 * Mengkapsulasi seluruh parameter tata letak konstan, margin, padding pengaman,
 * rasio perkiraan teks, serta ambang batas (threshold) form-factor.
 *
 * Mendukung pembacaan dinamis berbasis Android Resources (`dimens.xml`) sehingga
 * konfigurasi dapat diubah tanpa mengubah logika kalkulasi di [DeviceProfile].
 */
data class ProfileConfig(
    val workspacePaddingHorizontalDp: Float = DEFAULT_PADDING_PHONE_HORIZONTAL_DP,
    val workspacePaddingVerticalDp: Float = DEFAULT_PADDING_PHONE_VERTICAL_DP,
    val cellSafetyMarginDp: Float = DEFAULT_CELL_SAFETY_MARGIN_DP,
    val minSafetyIconSizeDp: Float = DEFAULT_MIN_SAFETY_ICON_SIZE_DP,
    val lineHeightMultiplier: Float = DEFAULT_LINE_HEIGHT_MULTIPLIER,
    val swThresholdFoldableDp: Int = DEFAULT_SW_FOLDABLE_DP,
    val swThresholdTabletDp: Int = DEFAULT_SW_TABLET_DP
) {
    companion object {
        // Default / Fallback Constants
        const val DEFAULT_PADDING_PHONE_HORIZONTAL_DP = 8f
        const val DEFAULT_PADDING_PHONE_VERTICAL_DP = 8f
        const val DEFAULT_PADDING_LANDSCAPE_HORIZONTAL_DP = 16f
        const val DEFAULT_PADDING_LANDSCAPE_VERTICAL_DP = 8f
        const val DEFAULT_PADDING_FOLDABLE_HORIZONTAL_DP = 16f
        const val DEFAULT_PADDING_FOLDABLE_VERTICAL_DP = 16f
        const val DEFAULT_PADDING_TABLET_HORIZONTAL_DP = 24f
        const val DEFAULT_PADDING_TABLET_VERTICAL_DP = 24f

        const val DEFAULT_CELL_SAFETY_MARGIN_DP = 4f
        const val DEFAULT_MIN_SAFETY_ICON_SIZE_DP = 24f
        const val DEFAULT_LINE_HEIGHT_MULTIPLIER = 1.5f

        const val DEFAULT_SW_FOLDABLE_DP = 600
        const val DEFAULT_SW_TABLET_DP = 720

        /**
         * Membaca konfigurasi langsung dari Android Resources (res/values/dimens.xml).
         */
        fun fromResources(
            resources: Resources,
            isLandscape: Boolean,
            isTablet: Boolean,
            isFoldable: Boolean
        ): ProfileConfig {
            val density = resources.displayMetrics.density
            val toDp: (Int) -> Float = { resId ->
                try {
                    resources.getDimension(resId) / if (density > 0) density else 1f
                } catch (e: Exception) {
                    0f
                }
            }

            val cellSafetyMargin = toDp(R.dimen.cell_safety_margin).let { if (it > 0) it else DEFAULT_CELL_SAFETY_MARGIN_DP }
            val minSafetyIconSize = toDp(R.dimen.min_safety_icon_size).let { if (it > 0) it else DEFAULT_MIN_SAFETY_ICON_SIZE_DP }

            val swFoldable = try { resources.getInteger(R.integer.sw_threshold_foldable_dp) } catch (e: Exception) { DEFAULT_SW_FOLDABLE_DP }
            val swTablet = try { resources.getInteger(R.integer.sw_threshold_tablet_dp) } catch (e: Exception) { DEFAULT_SW_TABLET_DP }

            val (horizontalDp, verticalDp) = when {
                isTablet -> Pair(
                    toDp(R.dimen.workspace_padding_horizontal_tablet).let { if (it > 0) it else DEFAULT_PADDING_TABLET_HORIZONTAL_DP },
                    toDp(R.dimen.workspace_padding_vertical_tablet).let { if (it > 0) it else DEFAULT_PADDING_TABLET_VERTICAL_DP }
                )
                isFoldable -> Pair(
                    toDp(R.dimen.workspace_padding_horizontal_foldable).let { if (it > 0) it else DEFAULT_PADDING_FOLDABLE_HORIZONTAL_DP },
                    toDp(R.dimen.workspace_padding_vertical_foldable).let { if (it > 0) it else DEFAULT_PADDING_FOLDABLE_VERTICAL_DP }
                )
                isLandscape -> Pair(
                    toDp(R.dimen.workspace_padding_horizontal_landscape).let { if (it > 0) it else DEFAULT_PADDING_LANDSCAPE_HORIZONTAL_DP },
                    toDp(R.dimen.workspace_padding_vertical_landscape).let { if (it > 0) it else DEFAULT_PADDING_LANDSCAPE_VERTICAL_DP }
                )
                else -> Pair(
                    toDp(R.dimen.workspace_padding_horizontal_phone).let { if (it > 0) it else DEFAULT_PADDING_PHONE_HORIZONTAL_DP },
                    toDp(R.dimen.workspace_padding_vertical_phone).let { if (it > 0) it else DEFAULT_PADDING_PHONE_VERTICAL_DP }
                )
            }

            return ProfileConfig(
                workspacePaddingHorizontalDp = horizontalDp,
                workspacePaddingVerticalDp = verticalDp,
                cellSafetyMarginDp = cellSafetyMargin,
                minSafetyIconSizeDp = minSafetyIconSize,
                lineHeightMultiplier = DEFAULT_LINE_HEIGHT_MULTIPLIER,
                swThresholdFoldableDp = swFoldable,
                swThresholdTabletDp = swTablet
            )
        }
    }
}
