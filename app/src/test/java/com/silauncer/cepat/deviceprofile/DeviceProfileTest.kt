package com.silauncer.cepat.deviceprofile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProfileTest {

    @Test
    fun testDefaultInvariantDeviceProfileValues() {
        val inv = InvariantDeviceProfile.PRESET_PHONE_PORTRAIT
        assertEquals(5, inv.numColumns)
        assertEquals(6, inv.numRows)
        assertEquals(48f, inv.iconSizeDp, 0.001f)
        assertEquals(10.0f, inv.iconTextSizeSp, 0.001f)
        assertEquals(4.0f, inv.iconSpacingDp, 0.001f)
        assertTrue(inv.showAppLabel)
    }

    @Test
    fun testDeviceProfileCalculationsWithDefaultConfig() {
        val inv = InvariantDeviceProfile.PRESET_PHONE_PORTRAIT
        val displaySpec = DisplayMetricsResolver.DisplaySpec(
            widthPx = 1080,
            heightPx = 2400,
            densityDpi = 420,
            density = 2.625f,
            fontScale = 1.0f,
            isLandscape = false,
            screenWidthDp = 411.4f,
            screenHeightDp = 914.3f,
            smallestScreenWidthDp = 411,
            insetTopPx = 64,
            insetBottomPx = 120,
            insetLeftPx = 0,
            insetRightPx = 0
        )

        val profile = DeviceProfile(inv, displaySpec)

        assertEquals(5, profile.numColumns)
        assertEquals(6, profile.numRows)
        assertTrue(profile.cellWidthPx > 0)
        assertTrue(profile.cellHeightPx > 0)
        assertTrue(profile.actualIconSizePx > 0)
        assertEquals(10.0f * 1.0f * 2.625f, profile.actualLabelSizePx, 0.01f)
    }

    @Test
    fun testDeviceProfileWithInjectedCustomConfig() {
        val inv = InvariantDeviceProfile.PRESET_PHONE_PORTRAIT
        val displaySpec = DisplayMetricsResolver.DisplaySpec(
            widthPx = 1080,
            heightPx = 2400,
            densityDpi = 420,
            density = 2.625f,
            fontScale = 1.0f,
            isLandscape = false,
            screenWidthDp = 411.4f,
            screenHeightDp = 914.3f,
            smallestScreenWidthDp = 411,
            insetTopPx = 0,
            insetBottomPx = 0,
            insetLeftPx = 0,
            insetRightPx = 0
        )

        val customConfig = ProfileConfig(
            workspacePaddingHorizontalDp = 12f,
            workspacePaddingVerticalDp = 12f,
            cellSafetyMarginDp = 6f,
            minSafetyIconSizeDp = 20f,
            lineHeightMultiplier = 1.4f
        )

        val profile = DeviceProfile(inv, displaySpec, customConfig)
        assertEquals(5, profile.numColumns)
        assertTrue(profile.cellWidthPx > 0)
        assertTrue(profile.cellHeightPx > 0)
    }
}
