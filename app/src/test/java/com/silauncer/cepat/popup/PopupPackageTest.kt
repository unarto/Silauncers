package com.silauncer.cepat.popup

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.dot.DotInfo
import com.silauncer.cepat.notification.NotificationKeyData
import com.silauncer.cepat.util.PackageUserKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PopupPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.PopupPackageTest
 * // [Penjelasan]: Pengujian unit komprehensif untuk package com.silauncer.cepat.popup (ArrowPopup, PopupContainerWithArrow, PopupDataProvider, PopupPopulator, PopupLiveUpdateHandler, SystemShortcut).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class PopupPackageTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testPopupDataProvider_notificationPostingAndTrimming() {
        var notifiedKey: PackageUserKey? = null
        val provider = PopupDataProvider { key ->
            notifiedKey = key
        }

        val packageUserKey = PackageUserKey("com.example.test", android.os.Process.myUserHandle())
        val notificationKey = NotificationKeyData("notif_key_1", "shortcut_1", 1, emptyArray())

        // 1. Post notification
        provider.onNotificationPosted(packageUserKey, notificationKey)
        assertEquals(packageUserKey, notifiedKey)

        val appInfo = AppInfo(
            name = "Test App",
            componentName = android.content.ComponentName("com.example.test", "com.example.test.MainActivity"),
            packageName = "com.example.test",
            user = android.os.Process.myUserHandle()
        )

        val dotInfo = provider.getDotInfoForItem(appInfo)
        assertNotNull(dotInfo)
        assertTrue(dotInfo?.hasDot() == true)

        // 2. Attach PopupDataChangeListener
        var liveUpdatedHeader = false
        var trimmedNotifs = false

        provider.setChangeListener(object : PopupDataProvider.PopupDataChangeListener {
            override fun onNotificationDotsUpdated(packageUserKey: PackageUserKey) {
                liveUpdatedHeader = true
            }

            override fun trimNotifications(updatedDots: Map<PackageUserKey, DotInfo>) {
                trimmedNotifs = true
            }
        })

        // 3. Remove notification
        provider.onNotificationRemoved(packageUserKey, notificationKey)
        assertTrue(liveUpdatedHeader)
        assertTrue(trimmedNotifs)

        val dotInfoAfterRemove = provider.getDotInfoForItem(appInfo)
        assertNull(dotInfoAfterRemove)
    }

    @Test
    fun testPopupPopulator_sortAndFilterShortcuts() {
        val shortcuts = emptyList<android.content.pm.ShortcutInfo>()
        val filtered = PopupPopulator.sortAndFilterShortcuts(shortcuts, PopupPopulator.MAX_SHORTCUTS)
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun testSystemShortcuts_appInfoAndUninstallCreation() {
        val appInfo = AppInfo(
            name = "Test App",
            componentName = android.content.ComponentName("com.example.test", "com.example.test.MainActivity"),
            packageName = "com.example.test",
            user = android.os.Process.myUserHandle()
        )

        val appInfoShortcut = SystemShortcut.AppInfoShortcut(context, appInfo)
        assertEquals("com.example.test", appInfoShortcut.appInfo.packageName)
        assertTrue(appInfoShortcut.isEnabled)

        val uninstallShortcut = SystemShortcut.UninstallShortcut(context, appInfo)
        assertEquals("com.example.test", uninstallShortcut.appInfo.packageName)
        assertTrue(uninstallShortcut.isEnabled)
    }
}
