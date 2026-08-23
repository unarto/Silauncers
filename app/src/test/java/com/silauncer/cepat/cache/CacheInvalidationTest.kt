package com.silauncer.cepat.cache

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.deviceprofile.DisplayMetricsResolver
import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import com.silauncer.cepat.deviceprofile.ProfileConfig
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.launcher.LauncherItem
import com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.CacheInvalidationTest
// [Penjelasan]: Pengujian unit komprehensif untuk memastikan seluruh lapisan cache (App, Icon L1, DiskIcon L2, Folder, Workspace, DeviceProfile, Notification, Shortcut) melakukan invalidasi dengan benar sesuai aturan sistem.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class CacheInvalidationTest {

    private lateinit var app: Application

    private class TestDrawable : Drawable() {
        override fun draw(canvas: Canvas) {}
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    @Before
    fun setup() = kotlinx.coroutines.runBlocking {
        app = ApplicationProvider.getApplicationContext()
        AppCache.clear()
        IconCache.clear()
        DiskIconCache.clear(app)
        FolderCache.clear()
        WorkspaceCache.clear()
        DeviceProfileCache.clear()
        NotificationCache.clear()
        ShortcutCache.clear()
    }

    @Test
    fun testAppUninstallInvalidation() = kotlinx.coroutines.runBlocking {
        val packageName = "com.sample.uninstall"
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)

        // 1. Setup cache entries
        AppCache.put("${packageName}_Main_0", AppInfo("App", ComponentName(packageName, "$packageName.Main"), packageName, Process.myUserHandle()))
        IconCache.put("$packageName/Main_0", TestDrawable())
        DiskIconCache.put(app, "$packageName/Main_0", bitmap)
        ShortcutCache.put("${packageName}_s1_0", WorkspaceShortcutInfo("s1", packageName, Process.myUserHandle(), "Short", null))
        NotificationCache.addNotificationKey(packageName, "notif_key_1")
        WorkspaceCache.set(listOf(LauncherItem.App(AppInfo("App", ComponentName(packageName, "$packageName.Main"), packageName, Process.myUserHandle()))))

        // 2. Simulate uninstall invalidation
        AppCache.removePackage(packageName)
        IconCache.removePackage(packageName)
        DiskIconCache.removePackage(app, packageName)
        ShortcutCache.removePackage(packageName)
        NotificationCache.removePackage(packageName)
        WorkspaceCache.invalidate()

        // 3. Assert all invalidated (including L2 Disk Cache)
        assertNull(AppCache.get("${packageName}_Main_0"))
        assertNull(IconCache.get("$packageName/Main_0"))
        assertNull(DiskIconCache.get(app, "$packageName/Main_0"))
        assertNull(ShortcutCache.get("${packageName}_s1_0"))
        assertNull(NotificationCache.getAll()[packageName])
        assertNull(WorkspaceCache.get())
    }

    // [Jalur Class]: com.silauncer.cepat.cache.CacheInvalidationTest
    // [Penjelasan]: Memverifikasi bahwa invalidasi saat update/replace aplikasi membersihkan L1 IconCache dan L2 DiskIconCache pada paket target tanpa mengganggu cache paket lain, serta operasi bersifat aman/idempotent.
    @Test
    fun testPackageUpdateInvalidatesL1AndL2DiskCacheWithoutAffectingOtherPackages() = kotlinx.coroutines.runBlocking {
        val targetPkg = "com.target.updateapp"
        val otherPkg = "com.other.app"
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)

        val targetKey = "$targetPkg/MainActivity_0"
        val otherKey = "$otherPkg/MainActivity_0"

        // 1. Setup L1 and L2 for both packages
        IconCache.put(targetKey, TestDrawable())
        DiskIconCache.put(app, targetKey, bitmap)
        IconCache.put(otherKey, TestDrawable())
        DiskIconCache.put(app, otherKey, bitmap)

        assertNotNull(IconCache.get(targetKey))
        assertNotNull(DiskIconCache.get(app, targetKey))
        assertNotNull(IconCache.get(otherKey))
        assertNotNull(DiskIconCache.get(app, otherKey))

        // 2. Simulate package update invalidation
        IconCache.removePackage(targetPkg)
        DiskIconCache.removePackage(app, targetPkg)

        // 3. Verify target package L1 & L2 are cleared
        assertNull(IconCache.get(targetKey))
        assertNull(DiskIconCache.get(app, targetKey))

        // 4. Verify other package L1 & L2 remain completely intact
        assertNotNull(IconCache.get(otherKey))
        assertNotNull(DiskIconCache.get(app, otherKey))

        // 5. Verify idempotency: calling removePackage multiple times does not crash or corrupt other packages
        IconCache.removePackage(targetPkg)
        DiskIconCache.removePackage(app, targetPkg)
        assertNull(DiskIconCache.get(app, targetKey))
        assertNotNull(DiskIconCache.get(app, otherKey))
    }

    @Test
    fun testFolderDissolveInvalidation() = kotlinx.coroutines.runBlocking {
        val folder = FolderInfo(id = "folder_to_dissolve", initialTitle = "Utilities")
        FolderCache.put(folder.id, folder)
        assertNotNull(FolderCache.get(folder.id))

        FolderCache.remove(folder.id)
        assertNull(FolderCache.get(folder.id))
    }

    @Test
    fun testDeviceProfileInvalidation() = kotlinx.coroutines.runBlocking {
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

        val p1 = DeviceProfileCache.getOrCalculate(inv, spec, config)
        val p2 = DeviceProfileCache.getOrCalculate(inv, spec, config)
        assertSame(p1, p2)

        DeviceProfileCache.invalidate()
        val p3 = DeviceProfileCache.getOrCalculate(inv, spec, config)
        assertNotNull(p3)
    }
}
