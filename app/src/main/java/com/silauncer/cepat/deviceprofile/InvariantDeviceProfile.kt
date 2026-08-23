package com.silauncer.cepat.deviceprofile

import android.content.res.Resources
import com.silauncer.cepat.R
import com.silauncer.cepat.storage.LauncherPreferences

/**
 * InvariantDeviceProfile
 *
 * Single Responsibility:
 * Memegang dan mengelola data acuan konfigurasi grid dasar yang bersifat imutabel.
 * Berisi spesifikasi baseline Silauncer (5x6 grid, 48dp icon size, 10sp label text size, 4dp icon spacing)
 * serta katalog preset acuan untuk berbagai form factor (Phone, Tablet, Foldable).
 *
 * Murni data holder/konfigurasi acuan — tidak mengandung logika kalkulasi runtime layar dinamis.
 */
data class InvariantDeviceProfile(
    val name: String = NAME_DEFAULT,
    val numColumns: Int = DEFAULT_COLUMNS,
    val numRows: Int = DEFAULT_ROWS,
    val iconSizeDp: Float = DEFAULT_ICON_SIZE_DP,
    val iconTextSizeSp: Float = DEFAULT_LABEL_SIZE_SP,
    val iconSpacingDp: Float = DEFAULT_ICON_SPACING_DP,
    val showAppLabel: Boolean = DEFAULT_SHOW_LABEL
) {
    companion object {
        const val NAME_DEFAULT = "Phone_Portrait_5x6"
        const val NAME_LANDSCAPE = "Phone_Landscape_6x4"
        const val NAME_FOLDABLE = "Foldable_6x5"
        const val NAME_TABLET = "Tablet_7x6"
        const val NAME_USER_CUSTOM = "User_Custom_Profile"

        // Baseline Constants Silauncer (Single Source of Truth)
        const val DEFAULT_COLUMNS = 5
        const val DEFAULT_ROWS = 6
        const val DEFAULT_ICON_SIZE_DP = 48f
        const val DEFAULT_LABEL_SIZE_SP = 10.0f
        const val DEFAULT_ICON_SPACING_DP = 4.0f
        const val DEFAULT_SHOW_LABEL = true

        // Preset Phone Landscape
        const val LANDSCAPE_COLUMNS = 6
        const val LANDSCAPE_ROWS = 4
        const val LANDSCAPE_ICON_SIZE_DP = 44f
        const val LANDSCAPE_LABEL_SIZE_SP = 9.5f
        const val LANDSCAPE_ICON_SPACING_DP = 4.0f

        // Preset Foldable
        const val FOLDABLE_COLUMNS = 6
        const val FOLDABLE_ROWS = 5
        const val FOLDABLE_ICON_SIZE_DP = 52f
        const val FOLDABLE_LABEL_SIZE_SP = 11.0f
        const val FOLDABLE_ICON_SPACING_DP = 6.0f

        // Preset Tablet
        const val TABLET_COLUMNS = 7
        const val TABLET_ROWS = 6
        const val TABLET_ICON_SIZE_DP = 56f
        const val TABLET_LABEL_SIZE_SP = 12.0f
        const val TABLET_ICON_SPACING_DP = 8.0f

        /**
         * Preset Default Smartphone Portrait (Silauncer Baseline)
         */
        val PRESET_PHONE_PORTRAIT = InvariantDeviceProfile(
            name = NAME_DEFAULT,
            numColumns = DEFAULT_COLUMNS,
            numRows = DEFAULT_ROWS,
            iconSizeDp = DEFAULT_ICON_SIZE_DP,
            iconTextSizeSp = DEFAULT_LABEL_SIZE_SP,
            iconSpacingDp = DEFAULT_ICON_SPACING_DP,
            showAppLabel = DEFAULT_SHOW_LABEL
        )

        /**
         * Preset Smartphone Landscape
         */
        val PRESET_PHONE_LANDSCAPE = InvariantDeviceProfile(
            name = NAME_LANDSCAPE,
            numColumns = LANDSCAPE_COLUMNS,
            numRows = LANDSCAPE_ROWS,
            iconSizeDp = LANDSCAPE_ICON_SIZE_DP,
            iconTextSizeSp = LANDSCAPE_LABEL_SIZE_SP,
            iconSpacingDp = LANDSCAPE_ICON_SPACING_DP,
            showAppLabel = DEFAULT_SHOW_LABEL
        )

        /**
         * Preset Layar Lebar / Foldable (sw >= 600dp)
         */
        val PRESET_FOLDABLE = InvariantDeviceProfile(
            name = NAME_FOLDABLE,
            numColumns = FOLDABLE_COLUMNS,
            numRows = FOLDABLE_ROWS,
            iconSizeDp = FOLDABLE_ICON_SIZE_DP,
            iconTextSizeSp = FOLDABLE_LABEL_SIZE_SP,
            iconSpacingDp = FOLDABLE_ICON_SPACING_DP,
            showAppLabel = DEFAULT_SHOW_LABEL
        )

        /**
         * Preset Tablet (sw >= 720dp)
         */
        val PRESET_TABLET = InvariantDeviceProfile(
            name = NAME_TABLET,
            numColumns = TABLET_COLUMNS,
            numRows = TABLET_ROWS,
            iconSizeDp = TABLET_ICON_SIZE_DP,
            iconTextSizeSp = TABLET_LABEL_SIZE_SP,
            iconSpacingDp = TABLET_ICON_SPACING_DP,
            showAppLabel = DEFAULT_SHOW_LABEL
        )

        /**
         * Factory method: Membuat InvariantDeviceProfile dinamis dari preferensi tersimpan di MMKV.
         */
        fun fromPreferences(
            prefs: LauncherPreferences,
            name: String = NAME_USER_CUSTOM
        ): InvariantDeviceProfile {
            return InvariantDeviceProfile(
                name = name,
                numColumns = prefs.gridColumns,
                numRows = prefs.gridRows,
                iconSizeDp = prefs.iconSize.toFloat(),
                iconTextSizeSp = prefs.labelSize,
                iconSpacingDp = prefs.iconSpacing.toFloat(),
                showAppLabel = prefs.showAppLabel
            )
        }

        /**
         * Mendapatkan profil acuan berdasarkan form factor perangkat.
         */
        fun getProfileForFormFactor(
            smallestWidthDp: Int,
            isLandscape: Boolean,
            config: ProfileConfig = ProfileConfig()
        ): InvariantDeviceProfile {
            return when {
                smallestWidthDp >= config.swThresholdTabletDp -> PRESET_TABLET
                smallestWidthDp >= config.swThresholdFoldableDp -> PRESET_FOLDABLE
                isLandscape -> PRESET_PHONE_LANDSCAPE
                else -> PRESET_PHONE_PORTRAIT
            }
        }

        /**
         * Membaca konfigurasi profil acuan langsung dari XML Resources (`dimens.xml`).
         */
        fun fromResources(
            resources: Resources,
            smallestWidthDp: Int,
            isLandscape: Boolean,
            config: ProfileConfig = ProfileConfig()
        ): InvariantDeviceProfile {
            val density = resources.displayMetrics.density
            val toDp: (Int, Float) -> Float = { resId, fallback ->
                try {
                    resources.getDimension(resId) / if (density > 0) density else 1f
                } catch (e: Exception) {
                    fallback
                }
            }
            val toSp: (Int, Float) -> Float = { resId, fallback ->
                try {
                    val px = resources.getDimension(resId)
                    val fontScale = resources.configuration.fontScale
                    val scaledDensity = density * if (fontScale > 0) fontScale else 1f
                    if (scaledDensity > 0) px / scaledDensity else fallback
                } catch (e: Exception) {
                    fallback
                }
            }
            val toInt: (Int, Int) -> Int = { resId, fallback ->
                try {
                    resources.getInteger(resId)
                } catch (e: Exception) {
                    fallback
                }
            }

            return when {
                smallestWidthDp >= config.swThresholdTabletDp -> InvariantDeviceProfile(
                    name = NAME_TABLET,
                    numColumns = toInt(R.integer.preset_tablet_columns, TABLET_COLUMNS),
                    numRows = toInt(R.integer.preset_tablet_rows, TABLET_ROWS),
                    iconSizeDp = toDp(R.dimen.preset_tablet_icon_size, TABLET_ICON_SIZE_DP),
                    iconTextSizeSp = toSp(R.dimen.preset_tablet_label_size, TABLET_LABEL_SIZE_SP),
                    iconSpacingDp = toDp(R.dimen.preset_tablet_icon_spacing, TABLET_ICON_SPACING_DP),
                    showAppLabel = DEFAULT_SHOW_LABEL
                )
                smallestWidthDp >= config.swThresholdFoldableDp -> InvariantDeviceProfile(
                    name = NAME_FOLDABLE,
                    numColumns = toInt(R.integer.preset_foldable_columns, FOLDABLE_COLUMNS),
                    numRows = toInt(R.integer.preset_foldable_rows, FOLDABLE_ROWS),
                    iconSizeDp = toDp(R.dimen.preset_foldable_icon_size, FOLDABLE_ICON_SIZE_DP),
                    iconTextSizeSp = toSp(R.dimen.preset_foldable_label_size, FOLDABLE_LABEL_SIZE_SP),
                    iconSpacingDp = toDp(R.dimen.preset_foldable_icon_spacing, FOLDABLE_ICON_SPACING_DP),
                    showAppLabel = DEFAULT_SHOW_LABEL
                )
                isLandscape -> InvariantDeviceProfile(
                    name = NAME_LANDSCAPE,
                    numColumns = toInt(R.integer.preset_phone_landscape_columns, LANDSCAPE_COLUMNS),
                    numRows = toInt(R.integer.preset_phone_landscape_rows, LANDSCAPE_ROWS),
                    iconSizeDp = toDp(R.dimen.preset_phone_landscape_icon_size, LANDSCAPE_ICON_SIZE_DP),
                    iconTextSizeSp = toSp(R.dimen.preset_phone_landscape_label_size, LANDSCAPE_LABEL_SIZE_SP),
                    iconSpacingDp = toDp(R.dimen.preset_phone_landscape_icon_spacing, LANDSCAPE_ICON_SPACING_DP),
                    showAppLabel = DEFAULT_SHOW_LABEL
                )
                else -> InvariantDeviceProfile(
                    name = NAME_DEFAULT,
                    numColumns = toInt(R.integer.default_grid_columns, DEFAULT_COLUMNS),
                    numRows = toInt(R.integer.default_grid_rows, DEFAULT_ROWS),
                    iconSizeDp = toDp(R.dimen.default_icon_size, DEFAULT_ICON_SIZE_DP),
                    iconTextSizeSp = toSp(R.dimen.default_label_size, DEFAULT_LABEL_SIZE_SP),
                    iconSpacingDp = toDp(R.dimen.default_icon_spacing, DEFAULT_ICON_SPACING_DP),
                    showAppLabel = DEFAULT_SHOW_LABEL
                )
            }
        }
    }
}
