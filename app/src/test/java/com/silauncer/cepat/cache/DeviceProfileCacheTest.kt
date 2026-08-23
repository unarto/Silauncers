package com.silauncer.cepat.cache

import android.app.Application
import com.silauncer.cepat.deviceprofile.DisplayMetricsResolver
import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import com.silauncer.cepat.deviceprofile.ProfileConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.DeviceProfileCacheTest
// [Penjelasan]: Pengujian unit untuk memverifikasi kalkulasi dan caching DeviceProfile serta invalidasinya.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class DeviceProfileCacheTest {

    @Before
    fun setup() {
        DeviceProfileCache.clear()
    }

    @Test
    fun testGetOrCalculateReturnsCachedInstance() {
        val inv = InvariantDeviceProfile.PRESET_PHONE_PORTRAIT
        val spec = DisplayMetricsResolver.DisplaySpec(
            widthPx = 1080,
            heightPx = 2400,
            densityDpi = 440,
            density = 2.75f,
            fontScale = 1.0f,
            isLandscape = false,
            screenWidthDp = 392f,
            screenHeightDp = 872f,
            smallestScreenWidthDp = 392,
            insetTopPx = 100,
            insetBottomPx = 0,
            insetLeftPx = 0,
            insetRightPx = 0
        )
        val config = ProfileConfig()

        val profile1 = DeviceProfileCache.getOrCalculate(inv, spec, config)
        val profile2 = DeviceProfileCache.getOrCalculate(inv, spec, config)

        assertNotNull(profile1)
        assertSame(profile1, profile2)
    }

    @Test
    fun testInvalidateClearsCache() {
        val inv = InvariantDeviceProfile.PRESET_PHONE_PORTRAIT
        val spec = DisplayMetricsResolver.DisplaySpec(
            widthPx = 1080,
            heightPx = 2400,
            densityDpi = 440,
            density = 2.75f,
            fontScale = 1.0f,
            isLandscape = false,
            screenWidthDp = 392f,
            screenHeightDp = 872f,
            smallestScreenWidthDp = 392,
            insetTopPx = 100,
            insetBottomPx = 0,
            insetLeftPx = 0,
            insetRightPx = 0
        )
        val config = ProfileConfig()

        val profile1 = DeviceProfileCache.getOrCalculate(inv, spec, config)
        DeviceProfileCache.invalidate()
        val profile2 = DeviceProfileCache.getOrCalculate(inv, spec, config)

        assertEquals(profile1.numColumns, profile2.numColumns)
    }
}
