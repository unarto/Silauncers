// [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferencesTest
// [Penjelasan]: Pengujian unit untuk memvalidasi lifecycle, singleton instance reuse, keselarasan nilai baca/tulis, dan default value LauncherPreferences.
package com.silauncer.cepat.storage

import android.app.Application
import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class LauncherPreferencesTest {

    private lateinit var prefs: LauncherPreferences

    @Before
    fun setUp() {
        prefs = LauncherPreferences.getInstance()
        prefs.resetToDefaults()
    }

    @Test
    fun testSingletonInstanceIdentity() {
        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferencesTest
        // [Penjelasan]: Memverifikasi bahwa LauncherPreferences.getInstance() dan get() selalu mengembalikan instance singleton yang sama persis di memori.
        val instance1 = LauncherPreferences.getInstance()
        val instance2 = LauncherPreferences.getInstance()
        val instance3 = LauncherPreferences.get()

        assertNotNull(instance1)
        assertSame(instance1, instance2)
        assertSame(instance1, instance3)
    }

    @Test
    fun testDefaultValues() {
        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferencesTest
        // [Penjelasan]: Memverifikasi seluruh default value sesuai dengan InvariantDeviceProfile.
        assertEquals(InvariantDeviceProfile.DEFAULT_COLUMNS, prefs.gridColumns)
        assertEquals(InvariantDeviceProfile.DEFAULT_ROWS, prefs.gridRows)
        assertEquals(InvariantDeviceProfile.DEFAULT_ICON_SIZE_DP.toInt(), prefs.iconSize)
        assertEquals(InvariantDeviceProfile.DEFAULT_SHOW_LABEL, prefs.showAppLabel)
        assertEquals(InvariantDeviceProfile.DEFAULT_LABEL_SIZE_SP, prefs.labelSize, 0.01f)
        assertEquals(InvariantDeviceProfile.DEFAULT_ICON_SPACING_DP.toInt(), prefs.iconSpacing)
        assertEquals(LauncherPreferences.DEFAULT_SORT_MODE, prefs.sortMode)
        assertEquals(LauncherPreferences.DEFAULT_ICON_PACK, prefs.iconPack)
        assertEquals(LauncherPreferences.DEFAULT_APP_LANGUAGE, prefs.appLanguage)
        assertTrue(prefs.hiddenApps.isEmpty())
        assertTrue(prefs.appOrder.isEmpty())
    }

    @Test
    fun testReadWritePersistenceAcrossInstances() {
        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferencesTest
        // [Penjelasan]: Memverifikasi penulisan nilai pada satu instance langsung terbaca secara konsisten pada instansiasi baru.
        val instance1 = LauncherPreferences.getInstance()
        instance1.gridColumns = 6
        instance1.gridRows = 7
        instance1.iconSize = 64
        instance1.showAppLabel = false
        instance1.iconPack = "com.custom.iconpack"
        instance1.appLanguage = "id"
        instance1.hiddenApps = setOf("com.hidden.one", "com.hidden.two")
        instance1.appOrder = listOf("pkg.a", "pkg.b")

        val instance2 = LauncherPreferences()
        assertEquals(6, instance2.gridColumns)
        assertEquals(7, instance2.gridRows)
        assertEquals(64, instance2.iconSize)
        assertEquals(false, instance2.showAppLabel)
        assertEquals("com.custom.iconpack", instance2.iconPack)
        assertEquals("id", instance2.appLanguage)
        assertEquals(setOf("com.hidden.one", "com.hidden.two"), instance2.hiddenApps)
        assertEquals(listOf("pkg.a", "pkg.b"), instance2.appOrder)
    }
}
