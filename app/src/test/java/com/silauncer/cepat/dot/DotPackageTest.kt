package com.silauncer.cepat.dot

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.folder.FolderIcon
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.notification.NotificationKeyData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DotPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.dot.DotPackageTest
 * // [Penjelasan]: Unit test untuk memverifikasi fungsionalitas paket dot (NotificationKeyData, DotInfo, FolderDotInfo, IconLabelDotView, serta integrasi dengan AppInfo dan FolderIcon).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class DotPackageTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNotificationKeyDataCreationAndEquality() {
        val key1 = NotificationKeyData("key_1", shortcutId = "sc1", count = 2)
        val key1Duplicate = NotificationKeyData("key_1", shortcutId = "sc1_diff", count = 5)
        val key2 = NotificationKeyData("key_2", count = 0) // Count minimum 1

        assertEquals(1, key2.count)
        assertEquals(key1, key1Duplicate)
        assertTrue(key1 != key2)
        assertEquals(key1.hashCode(), key1Duplicate.hashCode())

        val extracted = NotificationKeyData.extractKeysOnly(listOf(key1, key2))
        assertEquals(listOf("key_1", "key_2"), extracted)
    }

    @Test
    fun testDotInfoAddUpdateRemove() {
        val dotInfo = DotInfo()
        assertFalse(dotInfo.hasDot())
        assertEquals(0, dotInfo.getNotificationCount())

        val key1 = NotificationKeyData("key_1", count = 2)
        val added = dotInfo.addOrUpdateNotificationKey(key1)
        assertTrue(added)
        assertTrue(dotInfo.hasDot())
        assertEquals(2, dotInfo.getNotificationCount())
        assertEquals(1, dotInfo.getNotificationKeys().size)

        // Update count
        val key1Updated = NotificationKeyData("key_1", count = 5)
        val updated = dotInfo.addOrUpdateNotificationKey(key1Updated)
        assertTrue(updated)
        assertEquals(5, dotInfo.getNotificationCount())

        // Same count update should return false
        val key1Same = NotificationKeyData("key_1", count = 5)
        val sameResult = dotInfo.addOrUpdateNotificationKey(key1Same)
        assertFalse(sameResult)
        assertEquals(5, dotInfo.getNotificationCount())

        // Add second key
        val key2 = NotificationKeyData("key_2", count = 3)
        dotInfo.addOrUpdateNotificationKey(key2)
        assertEquals(8, dotInfo.getNotificationCount())

        // Remove key1
        val removed = dotInfo.removeNotificationKey(key1)
        assertTrue(removed)
        assertEquals(3, dotInfo.getNotificationCount())

        // Remove non-existent key
        val removedNonExistent = dotInfo.removeNotificationKey(NotificationKeyData("key_unknown"))
        assertFalse(removedNonExistent)

        // Clear
        dotInfo.clear()
        assertFalse(dotInfo.hasDot())
        assertEquals(0, dotInfo.getNotificationCount())
    }

    @Test
    fun testDotInfoMaxCountCapping() {
        val dotInfo = DotInfo()
        val keyLarge = NotificationKeyData("key_large", count = 2000)
        dotInfo.addOrUpdateNotificationKey(keyLarge)
        assertEquals(DotInfo.MAX_COUNT, dotInfo.getNotificationCount())
    }

    @Test
    fun testFolderDotInfoAggregation() {
        val folderDot = FolderDotInfo()
        assertFalse(folderDot.hasDot())
        assertEquals(0, folderDot.getNotificationCount())

        val childDot1 = DotInfo()
        childDot1.addOrUpdateNotificationKey(NotificationKeyData("k1", count = 1))
        childDot1.addOrUpdateNotificationKey(NotificationKeyData("k2", count = 3))

        val childDot2 = DotInfo()
        childDot2.addOrUpdateNotificationKey(NotificationKeyData("k3", count = 2))

        folderDot.addDotInfo(childDot1)
        // childDot1 has 2 distinct notification keys
        assertEquals(2, folderDot.getNotificationCount())
        assertTrue(folderDot.hasDot())

        folderDot.addDotInfo(childDot2)
        // +1 from childDot2 -> total 3
        assertEquals(3, folderDot.getNotificationCount())

        folderDot.subtractDotInfo(childDot1)
        assertEquals(1, folderDot.getNotificationCount())

        folderDot.clear()
        assertEquals(0, folderDot.getNotificationCount())
        assertFalse(folderDot.hasDot())
    }

    @Test
    fun testIconLabelDotViewHelper() {
        val testView = object : View(context), IconLabelDotView {
            var iconVisibleState: Boolean = true
            var dotHiddenState: Boolean = false

            override fun setIconVisible(visible: Boolean) {
                iconVisibleState = visible
            }

            override fun setForceHideDot(hide: Boolean) {
                dotHiddenState = hide
            }
        }

        IconLabelDotView.setIconAndDotVisible(testView, false)
        assertFalse(testView.iconVisibleState)
        assertTrue(testView.dotHiddenState)

        IconLabelDotView.setIconAndDotVisible(testView, true)
        assertTrue(testView.iconVisibleState)
        assertFalse(testView.dotHiddenState)

        // Plain view
        val plainView = View(context)
        IconLabelDotView.setIconAndDotVisible(plainView, false)
        assertEquals(View.INVISIBLE, plainView.visibility)
        IconLabelDotView.setIconAndDotVisible(plainView, true)
        assertEquals(View.VISIBLE, plainView.visibility)
    }

    @Test
    fun testAppInfoAndFolderIconIntegration() {
        val dotInfo = DotInfo()
        dotInfo.addOrUpdateNotificationKey(NotificationKeyData("notif_1", count = 1))

        val app = AppInfo(
            name = "App Dot",
            componentName = ComponentName("com.dot.test", "com.dot.test.MainActivity"),
            packageName = "com.dot.test",
            dotInfo = dotInfo
        )

        assertTrue(app.isDotted)

        val folderInfo = FolderInfo(initialTitle = "Test Folder", initialContents = listOf(app))
        val folderIcon = FolderIcon(context)
        val iconLoader = IconLoader(CoroutineScope(Dispatchers.Unconfined))

        folderIcon.bind(
            info = folderInfo,
            loader = iconLoader,
            onClick = {}
        )

        assertTrue(folderIcon.getDotInfo().hasDot())

        folderIcon.setForceHideDot(true)
        folderIcon.setIconVisible(false)

        folderIcon.unbind()
    }
}
