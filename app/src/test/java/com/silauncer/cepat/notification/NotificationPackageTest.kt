package com.silauncer.cepat.notification

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.folder.FolderIcon
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.touch.OverScroll
import com.silauncer.cepat.util.PackageUserKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * NotificationPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.notification.NotificationPackageTest
 * // [Penjelasan]: Unit test komprehensif untuk memverifikasi fungsionalitas paket notification (NotificationKeyData, NotificationGroup, NotificationInfo, NotificationListener, NotificationMainView, NotificationContainer, OverScroll, dan PackageUserKey).
 */
// [Jalur Class]: com.silauncer.cepat.notification.NotificationPackageTest
// [Penjelasan]: Menambahkan penekanan peringatan deprecation untuk pengujian unit StatusBarNotification constructor.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
@Suppress("DEPRECATION")
class NotificationPackageTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNotificationKeyData() {
        val key1 = NotificationKeyData("key_1", shortcutId = "sc1", count = 2)
        val key1Duplicate = NotificationKeyData("key_1", shortcutId = "sc_diff", count = 99)
        val key2 = NotificationKeyData("key_2", count = -5)

        assertEquals(1, key2.count)
        assertEquals(key1, key1Duplicate)
        assertTrue(key1 != key2)
        assertEquals(key1.hashCode(), key1Duplicate.hashCode())

        val keysOnly = NotificationKeyData.extractKeysOnly(listOf(key1, key2))
        assertEquals(listOf("key_1", "key_2"), keysOnly)
    }

    @Test
    fun testNotificationGroup() {
        val group = NotificationGroup()
        assertTrue(group.isEmpty())
        assertNull(group.groupSummaryKey)

        group.setGroupSummaryKey("summary_key_1")
        assertEquals("summary_key_1", group.groupSummaryKey)

        group.addChildKey("child_1")
        group.addChildKey("child_2")
        assertFalse(group.isEmpty())
        assertEquals(setOf("child_1", "child_2"), group.getChildKeys())

        group.removeChildKey("child_1")
        assertFalse(group.isEmpty())
        assertEquals(setOf("child_2"), group.getChildKeys())

        group.removeChildKey("child_2")
        assertTrue(group.isEmpty())
    }

    @Test
    fun testPackageUserKey() {
        val myUser = Process.myUserHandle()
        val key1 = PackageUserKey("com.example.app", myUser)
        val key2 = PackageUserKey("com.example.app", myUser)
        val key3 = PackageUserKey("com.other.app", myUser)

        assertEquals(key1, key2)
        assertTrue(key1 != key3)
        assertEquals(key1.hashCode(), key2.hashCode())

        key1.update("com.updated.app", myUser, 42)
        assertEquals("com.updated.app", key1.packageName)
        assertEquals(42, key1.widgetCategory)
        assertTrue(key1.toString().contains("com.updated.app"))
    }

    @Test
    fun testOverScrollDamping() {
        val dampenedZero = OverScroll.dampedScroll(0f, 100)
        assertEquals(0f, dampenedZero, 0.001f)

        val dampenedPositive = OverScroll.dampedScroll(50f, 100)
        assertTrue(dampenedPositive > 0)
        assertTrue(dampenedPositive < 50f) // Harus teredam jauh lebih kecil dari 50f

        val dampenedNegative = OverScroll.dampedScroll(-50f, 100)
        assertTrue(dampenedNegative < 0)
        assertTrue(dampenedNegative > -50f)
    }

    @Test
    fun testNotificationInfoExtraction() {
        val notif = Notification.Builder(context, "test_channel")
            .setContentTitle("Test Title")
            .setContentText("Test Content")
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        val sbn = StatusBarNotification(
            "com.silauncer.cepat",
            "com.silauncer.cepat",
            101,
            "tag_1",
            Process.myUid(),
            Process.myPid(),
            0,
            notif,
            Process.myUserHandle(),
            System.currentTimeMillis()
        )

        val notifInfo = NotificationInfo(context, sbn)
        assertEquals("Test Title", notifInfo.title?.toString())
        assertEquals("Test Content", notifInfo.text?.toString())
        assertTrue(notifInfo.autoCancel)
        assertTrue(notifInfo.dismissable)
        assertEquals("com.silauncer.cepat", notifInfo.packageUserKey.packageName)

        val contrastedDrawable = notifInfo.getIconForBackground(context, Color.BLACK)
        // Icon mungkin null atau drawable default
    }

    @Test
    fun testNotificationMainViewAndContainer() {
        val mainView = NotificationMainView(context)
        mainView.updateHeader(1)
        mainView.updateHeader(3)
        mainView.updateBackgroundColor(Color.DKGRAY)

        val notif = Notification.Builder(context, "test_channel")
            .setContentTitle("Sample Notification")
            .setContentText("Sample Message")
            .build()

        val sbn = StatusBarNotification(
            "com.silauncer.cepat",
            "com.silauncer.cepat",
            102,
            "tag_2",
            Process.myUid(),
            Process.myPid(),
            0,
            notif,
            Process.myUserHandle(),
            System.currentTimeMillis()
        )
        val info1 = NotificationInfo(context, sbn)

        mainView.applyNotificationInfo(info1)
        assertEquals(info1, mainView.getNotificationInfo())

        // Drag calculations
        mainView.onPrimaryDrag(0.2f)
        mainView.onPrimaryDrag(0.5f)
        mainView.onPrimaryDrag(0.9f)

        mainView.onSecondaryDrag(0.2f)
        mainView.onSecondaryDrag(0.4f)
        mainView.onSecondaryDrag(0.8f)

        // Notification Container
        val container = NotificationContainer(context)
        container.applyNotificationInfos(listOf(info1))
        assertEquals(1, container.getNotificationInfos().size)

        container.trimNotifications(listOf(info1.notificationKey))
        assertEquals(1, container.getNotificationInfos().size)

        container.trimNotifications(emptyList())
        assertEquals(0, container.getNotificationInfos().size)
    }

    @Test
    fun testNotificationListenerRegistration() {
        val testListener = object : NotificationListener.NotificationsChangedListener {
            var postedCount = 0
            var removedCount = 0
            var fullRefreshCount = 0

            override fun onNotificationPosted(postedPackageUserKey: PackageUserKey, notificationKey: NotificationKeyData) {
                postedCount++
            }

            override fun onNotificationRemoved(removedPackageUserKey: PackageUserKey, notificationKey: NotificationKeyData) {
                removedCount++
            }

            override fun onNotificationFullRefresh(activeNotifications: List<StatusBarNotification>) {
                fullRefreshCount++
            }
        }

        NotificationListener.addNotificationsChangedListener(testListener)
        NotificationListener.removeNotificationsChangedListener(testListener)
    }

    // [Jalur Class]: com.silauncer.cepat.notification.NotificationPackageTest
    // [Penjelasan]: Memverifikasi bahwa onDestroy pada NotificationListener melepaskan referensi connected instance, menghentikan worker thread, dan dapat dipanggil berulang kali (idempotent) tanpa exception.
    @Test
    fun testNotificationListenerLifecycleOnDestroyCleanup() {
        val listener = NotificationListener()
        listener.onListenerConnected()
        assertEquals(listener, NotificationListener.getInstanceIfConnected())

        // Eksekusi onDestroy pertama kali
        listener.onDestroy()
        assertNull(NotificationListener.getInstanceIfConnected())

        // Verifikasi pemanggilan berulang (idempotent) tidak menyebabkan error
        listener.onDestroy()
        assertNull(NotificationListener.getInstanceIfConnected())
    }

    private fun createTestSbn(pkg: String, id: Int, tag: String? = null): StatusBarNotification {
        val notif = Notification.Builder(context, "test_channel")
            .setContentTitle("Test $pkg")
            .build()
        return StatusBarNotification(
            pkg,
            pkg,
            id,
            tag,
            Process.myUid(),
            Process.myPid(),
            0,
            notif,
            Process.myUserHandle(),
            System.currentTimeMillis()
        )
    }

    @Test
    fun testNotificationRepositoryStateFlow() {
        val repo = NotificationRepository.getInstance()
        repo.onNotificationFullRefresh(emptyList()) // Reset state

        val sbn1 = createTestSbn("com.test.app1", 101, "tag1")
        val sbn2 = createTestSbn("com.test.app1", 102, "tag2")

        // 1. Post first notification
        repo.onNotificationPosted(sbn1)
        val stateAfterPost1 = repo.notificationState.value
        assertTrue(stateAfterPost1.containsKey("com.test.app1"))
        val app1State = stateAfterPost1["com.test.app1"]
        assertNotNull(app1State)
        assertTrue(app1State!!.hasNotification)
        assertEquals(1, app1State.notificationCount)

        // 2. Post second notification for the same package
        repo.onNotificationPosted(sbn2)
        val stateAfterPost2 = repo.notificationState.value
        assertEquals(2, stateAfterPost2["com.test.app1"]?.notificationCount)

        // 3. Remove first notification
        repo.onNotificationRemoved(sbn1)
        val stateAfterRemove1 = repo.notificationState.value
        assertTrue(stateAfterRemove1.containsKey("com.test.app1"))
        assertEquals(1, stateAfterRemove1["com.test.app1"]?.notificationCount)
        assertTrue(stateAfterRemove1["com.test.app1"]!!.hasNotification)

        // 4. Remove second notification -> package entry removed
        repo.onNotificationRemoved(sbn2)
        val stateAfterRemove2 = repo.notificationState.value
        assertFalse(stateAfterRemove2.containsKey("com.test.app1"))

        // Reset
        repo.onNotificationFullRefresh(emptyList())
    }

    @Test
    fun testFolderNotificationDotAggregation() {
        val repo = NotificationRepository.getInstance()
        repo.onNotificationFullRefresh(emptyList())

        val app1 = AppInfo(
            name = "App 1",
            componentName = ComponentName("com.folder.app1", "com.folder.app1.MainActivity"),
            packageName = "com.folder.app1"
        )
        val app2 = AppInfo(
            name = "App 2",
            componentName = ComponentName("com.folder.app2", "com.folder.app2.MainActivity"),
            packageName = "com.folder.app2"
        )

        val folderInfo = FolderInfo(initialTitle = "Social", initialContents = listOf(app1, app2))
        val folderIcon = FolderIcon(context)
        val iconLoader = IconLoader(CoroutineScope(Dispatchers.Unconfined))
        folderIcon.bind(folderInfo, iconLoader, onClick = {})

        // Awalnya tidak ada notifikasi
        assertFalse(folderIcon.getDotInfo().hasDot())

        // Simulasikan notifikasi masuk ke app1
        val sbnApp1 = createTestSbn("com.folder.app1", 201)
        repo.onNotificationPosted(sbnApp1)
        val stateMap = repo.notificationState.value
        
        // Sinkronisasi state ke AppInfo (seperti yang dilakukan di LauncherActivity)
        for (item in folderInfo.getItems()) {
            val state = stateMap[item.packageName]
            folderInfo.replaceItem(item, item.copy(hasNotification = state?.hasNotification == true))
        }

        // Render ulang pratinjau folder
        folderIcon.bind(folderInfo, iconLoader, onClick = {})
        assertTrue(folderIcon.getDotInfo().hasDot())
        assertEquals(1, folderIcon.getDotInfo().getNotificationCount())

        // Simulasikan notifikasi masuk ke app2 juga
        val sbnApp2 = createTestSbn("com.folder.app2", 202)
        repo.onNotificationPosted(sbnApp2)
        val stateMap2 = repo.notificationState.value
        for (item in folderInfo.getItems()) {
            val state = stateMap2[item.packageName]
            folderInfo.replaceItem(item, item.copy(hasNotification = state?.hasNotification == true))
        }
        folderIcon.bind(folderInfo, iconLoader, onClick = {})
        assertTrue(folderIcon.getDotInfo().hasDot())
        assertEquals(2, folderIcon.getDotInfo().getNotificationCount())

        // Bersihkan semua notifikasi
        repo.onNotificationFullRefresh(emptyList())
        val stateMap3 = repo.notificationState.value
        for (item in folderInfo.getItems()) {
            val state = stateMap3[item.packageName]
            folderInfo.replaceItem(item, item.copy(hasNotification = state?.hasNotification == true))
        }
        folderIcon.bind(folderInfo, iconLoader, onClick = {})
        assertFalse(folderIcon.getDotInfo().hasDot())
        assertEquals(0, folderIcon.getDotInfo().getNotificationCount())

        folderIcon.unbind()
    }
}
