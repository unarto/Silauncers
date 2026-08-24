package com.silauncer.cepat.apps

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.storage.LauncherPreferences
import com.silauncer.cepat.launcher.LauncherAppController
import com.silauncer.cepat.dot.DotInfo
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
 * AppsPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
 * // [Penjelasan]: Pengujian unit komprehensif untuk memvalidasi model-like fungsionalitas pengumpulan, penyimpanan, penyaringan, dan pengurutan daftar aplikasi di Silauncer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = com.silauncer.cepat.launcher.LauncherApplication::class)
class AppsPackageTest {

    private lateinit var context: Context
    private lateinit var prefs: LauncherPreferences

    @Before
    fun setUp() {
        // // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
        // // [Penjelasan]: Menginisialisasi context aplikasi, in-memory fallback preferences, dan preferences aplikasi sebelum tes dijalankan.
        context = ApplicationProvider.getApplicationContext()
        prefs = LauncherPreferences()
        prefs.resetToDefaults()
    }

    @Test
    fun testAppInfo_and_LaunchIntent() {
        // // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
        // // [Penjelasan]: Memeriksa pembentukan model AppInfo, dot notification, serta pembuatan Launch Intent-nya.
        val component = ComponentName("com.test.app", "com.test.app.MainActivity")
        val appInfo = AppInfo(
            name = "Test App",
            componentName = component,
            packageName = "com.test.app",
            user = Process.myUserHandle()
        )

        assertEquals("Test App", appInfo.name)
        assertEquals("com.test.app", appInfo.packageName)
        assertFalse(appInfo.isDotted)

        val intent = appInfo.launchIntent()
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertEquals(component, intent.component)
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)

        // Test notification dot
        val appInfo2 = appInfo.copy(hasNotification = true)
        assertTrue(appInfo2.isDotted)
    }

    @Test
    fun testAppStateHolder_ThreadSafeCache() = runTest {
        // // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
        // // [Penjelasan]: Memverifikasi penambahan, pembersihan, dan manipulasi daftar aplikasi dalam AppStateHolder secara asinkron.
        val holder = AppStateHolder()
        val app1 = AppInfo("App A", ComponentName("pkg.a", "cls.a"), "pkg.a")
        val app2 = AppInfo("App B", ComponentName("pkg.b", "cls.b"), "pkg.b")

        holder.setApps(listOf(app1, app2))
        assertEquals(2, holder.getApps().size)

        // Menambahkan aplikasi duplikat (seharusnya disaring)
        val added = holder.addApps(listOf(app1, AppInfo("App C", ComponentName("pkg.c", "cls.c"), "pkg.c")))
        assertEquals(1, added.size)
        assertEquals("App C", added[0].name)
        assertEquals(3, holder.getApps().size)

        // Hapus paket
        holder.removePackage("pkg.b", Process.myUserHandle())
        assertEquals(2, holder.getApps().size)
    }

    @Test
    fun testAppStateHolder_AdvancedDeduplicationAndRemoval() = runTest {
        // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
        // [Penjelasan]: Memverifikasi secara mendalam deduplikasi cacheKey pada setApps dan addApps, penghapusan multiple components per package, re-addition, dan isolasi multi-user.
        val holder = AppStateHolder()
        val user0 = Process.myUserHandle()

        // 1. Validasi deduplikasi saat setApps dengan item duplikat dalam input
        val app1a = AppInfo("App 1", ComponentName("pkg.one", "cls.Main"), "pkg.one", user = user0)
        val app1b = AppInfo("App 1 Dup", ComponentName("pkg.one", "cls.Main"), "pkg.one", user = user0)
        val app2 = AppInfo("App 2", ComponentName("pkg.two", "cls.Main"), "pkg.two", user = user0)

        holder.setApps(listOf(app1a, app1b, app2))
        val initialApps = holder.getApps()
        assertEquals(2, initialApps.size)
        assertEquals("App 1", initialApps[0].name)
        assertEquals("App 2", initialApps[1].name)

        // 2. Validasi addApps dengan input berisi duplikat internal dan duplikat dari apps eksisting
        val app3a = AppInfo("App 3", ComponentName("pkg.three", "cls.A"), "pkg.three", user = user0)
        val app3b = AppInfo("App 3 Duplicate", ComponentName("pkg.three", "cls.A"), "pkg.three", user = user0)
        val app4 = AppInfo("App 4 (Second Activity)", ComponentName("pkg.one", "cls.Second"), "pkg.one", user = user0)

        val added = holder.addApps(listOf(app1a, app3a, app3b, app4))
        // app1a sudah ada -> dilewati
        // app3a baru -> ditambahkan
        // app3b duplikat dari app3a -> dilewati
        // app4 activity kedua dari pkg.one -> ditambahkan
        assertEquals(2, added.size)
        assertEquals("App 3", added[0].name)
        assertEquals("App 4 (Second Activity)", added[1].name)
        assertEquals(4, holder.getApps().size)

        // 3. Validasi removePackage menghapus seluruh komponen dari paket target (cls.Main & cls.Second)
        holder.removePackage("pkg.one", user0)
        val remainingApps = holder.getApps()
        assertEquals(2, remainingApps.size)
        assertTrue(remainingApps.none { it.packageName == "pkg.one" })

        // 4. Validasi re-adding paket yang baru saja dihapus berhasil dimasukkan kembali
        val readded = holder.addApps(listOf(app1a))
        assertEquals(1, readded.size)
        assertEquals(3, holder.getApps().size)
        assertTrue(holder.getApps().any { it.componentName.className == "cls.Main" && it.packageName == "pkg.one" })
    }

    @Test
    fun testAppSorter_AtoZ_and_ZtoA() {
        // // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
        // // [Penjelasan]: Memverifikasi keakuratan algoritma pengurutan abjad dari A ke Z, Z ke A, dan pengurutan khusus (custom order).
        val appA = AppInfo("Apple", ComponentName("pkg.a", "cls.a"), "pkg.a")
        val appB = AppInfo("Banana", ComponentName("pkg.b", "cls.b"), "pkg.b")
        val appC = AppInfo("Cherry", ComponentName("pkg.c", "cls.c"), "pkg.c")
        val apps = listOf(appC, appA, appB)

        // Urutan A-Z (Default)
        val sortedAz = AppSorter.sort(apps, "a_z")
        assertEquals("Apple", sortedAz[0].name)
        assertEquals("Banana", sortedAz[1].name)
        assertEquals("Cherry", sortedAz[2].name)

        // Urutan Z-A
        val sortedZa = AppSorter.sort(apps, "z_a")
        assertEquals("Cherry", sortedZa[0].name)
        assertEquals("Banana", sortedZa[1].name)
        assertEquals("Apple", sortedZa[2].name)

        // Urutan Kustom
        val customOrder = listOf(
            ComponentName("pkg.b", "cls.b").flattenToString(),
            ComponentName("pkg.c", "cls.c").flattenToString(),
            ComponentName("pkg.a", "cls.a").flattenToString()
        )
        val sortedCustom = AppSorter.sort(apps, "custom", customOrder)
        assertEquals("Banana", sortedCustom[0].name)
        assertEquals("Cherry", sortedCustom[1].name)
        assertEquals("Apple", sortedCustom[2].name)
    }

    @Test
    fun testAppActionHandler_RequestUninstall_WithApplicationContext_SetsFlagsAndExtras() {
        // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
        // [Penjelasan]: Memvalidasi bahwa pemanggilan requestUninstall menggunakan ApplicationContext berhasil menyematkan flag FLAG_ACTIVITY_NEW_TASK dan EXTRA_USER tanpa runtime crash.
        val actionHandler = AppActionHandler(context)
        val testApp = AppInfo(
            name = "Uninstall Target",
            componentName = ComponentName("com.target.pkg", "com.target.pkg.MainActivity"),
            packageName = "com.target.pkg",
            user = Process.myUserHandle()
        )

        actionHandler.requestUninstall(testApp)

        val shadowApp = org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("Started intent should not be null", startedIntent)
        assertEquals(Intent.ACTION_DELETE, startedIntent.action)
        assertEquals(android.net.Uri.parse("package:com.target.pkg"), startedIntent.data)
        assertTrue(
            "FLAG_ACTIVITY_NEW_TASK must be set when launching from ApplicationContext",
            (startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
        assertEquals(Process.myUserHandle(), startedIntent.getParcelableExtra<UserHandle>(Intent.EXTRA_USER))
    }

    @Test
    fun testAppActionHandler_LaunchApp_IncrementsUsageCountAndStartsIntent() = kotlinx.coroutines.test.runTest {
        // [Jalur Class]: com.silauncer.cepat.apps.AppsPackageTest
        // [Penjelasan]: Memverifikasi bahwa launchApp mencatat frekuensi peluncuran aplikasi via AppStatsRepository (Room) dan meluncurkan LaunchIntent dengan flag yang tepat.
        val actionHandler = AppActionHandler(context)
        val appStatsRepo = com.silauncer.cepat.database.AppStatsRepository(context)
        val testApp = AppInfo(
            name = "Launch Target",
            componentName = ComponentName("com.launch.target", "com.launch.target.MainActivity"),
            packageName = "com.launch.target",
            user = Process.myUserHandle()
        )

        val initialCount = appStatsRepo.getLaunchCount("com.launch.target")
        actionHandler.launchApp(testApp)
        // Tunggu coroutine asinkron IO selesai
        Thread.sleep(500)
        val updatedCount = appStatsRepo.getLaunchCount("com.launch.target")
        assertEquals(initialCount + 1, updatedCount)

        val shadowApp = org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_MAIN, startedIntent.action)
        assertEquals(testApp.componentName, startedIntent.component)
        assertTrue((startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }
}
