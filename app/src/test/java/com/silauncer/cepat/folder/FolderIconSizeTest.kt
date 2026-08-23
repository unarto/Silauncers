package com.silauncer.cepat.folder

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.deviceprofile.DeviceProfile
import com.silauncer.cepat.deviceprofile.DisplayMetricsResolver
import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import com.silauncer.cepat.home.AppAdapter
import com.silauncer.cepat.launcher.LauncherItem
import com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
// [Penjelasan]: Unit test untuk memverifikasi konsistensi propagasi iconSizePx pada FolderIcon dan AppAdapter sesuai audit LOW #2.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class FolderIconSizeTest {

    private lateinit var context: Context
    private lateinit var testScope: CoroutineScope
    private lateinit var iconLoader: IconLoader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = CoroutineScope(Dispatchers.Unconfined)
        iconLoader = IconLoader(testScope)
    }

    private fun createDummyApp(name: String, pkg: String): AppInfo {
        return AppInfo(
            name = name,
            componentName = ComponentName(pkg, "$pkg.MainActivity"),
            packageName = pkg
        )
    }

    @Test
    fun testFolderIcon_smallIconSizePx() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
        // [Penjelasan]: Memastikan FolderIcon menggunakan ukuran kecil ketika iconSizePx kecil diberikan secara eksplisit
        val folderIcon = FolderIcon(context)
        val folderInfo = FolderInfo(initialTitle = "Small Folder", initialContents = listOf(createDummyApp("App1", "com.test.app1")))
        val expectedSizePx = 96

        folderIcon.bind(
            info = folderInfo,
            loader = iconLoader,
            deviceProfile = null,
            iconSizePx = expectedSizePx,
            onClick = {}
        )

        val previewContainer = folderIcon.findViewById<FrameLayout>(R.id.folder_preview_container)
        assertNotNull(previewContainer)
        assertEquals(expectedSizePx, previewContainer.layoutParams.width)
        assertEquals(expectedSizePx, previewContainer.layoutParams.height)
    }

    @Test
    fun testFolderIcon_largeIconSizePx() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
        // [Penjelasan]: Memastikan FolderIcon menggunakan ukuran besar ketika iconSizePx besar diberikan secara eksplisit
        val folderIcon = FolderIcon(context)
        val folderInfo = FolderInfo(initialTitle = "Large Folder", initialContents = listOf(createDummyApp("App1", "com.test.app1")))
        val expectedSizePx = 250

        folderIcon.bind(
            info = folderInfo,
            loader = iconLoader,
            deviceProfile = null,
            iconSizePx = expectedSizePx,
            onClick = {}
        )

        val previewContainer = folderIcon.findViewById<FrameLayout>(R.id.folder_preview_container)
        assertNotNull(previewContainer)
        assertEquals(expectedSizePx, previewContainer.layoutParams.width)
        assertEquals(expectedSizePx, previewContainer.layoutParams.height)
    }

    @Test
    fun testFolderIcon_zeroOrNegativeIconSizePx_usesDeviceProfileFallback() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
        // [Penjelasan]: Memastikan saat iconSizePx <= 0, FolderIcon tetap menggunakan fallback actualIconSizePx dari DeviceProfile
        val folderIcon = FolderIcon(context)
        val folderInfo = FolderInfo(initialTitle = "Fallback Profile", initialContents = listOf(createDummyApp("App1", "com.test.app1")))

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
        val deviceProfile = DeviceProfile(inv, displaySpec)
        val expectedProfileSizePx = deviceProfile.actualIconSizePx
        assertTrue("DeviceProfile actualIconSizePx harus > 0", expectedProfileSizePx > 0)

        // Binding dengan iconSizePx = 0
        folderIcon.bind(
            info = folderInfo,
            loader = iconLoader,
            deviceProfile = deviceProfile,
            iconSizePx = 0,
            onClick = {}
        )

        val previewContainer = folderIcon.findViewById<FrameLayout>(R.id.folder_preview_container)
        assertEquals(expectedProfileSizePx, previewContainer.layoutParams.width)
        assertEquals(expectedProfileSizePx, previewContainer.layoutParams.height)

        // Binding ulang dengan iconSizePx = -1 (negatif)
        folderIcon.bind(
            info = folderInfo,
            loader = iconLoader,
            deviceProfile = deviceProfile,
            iconSizePx = -1,
            onClick = {}
        )
        assertEquals(expectedProfileSizePx, previewContainer.layoutParams.width)
        assertEquals(expectedProfileSizePx, previewContainer.layoutParams.height)
    }

    @Test
    fun testFolderIcon_nullDeviceProfileAndZeroIconSize_usesDimenFallback() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
        // [Penjelasan]: Memastikan saat iconSizePx <= 0 dan deviceProfile null, ukuran jatuh ke default R.dimen.folder_preview_size
        val folderIcon = FolderIcon(context)
        val folderInfo = FolderInfo(initialTitle = "Dimen Fallback", initialContents = listOf(createDummyApp("App1", "com.test.app1")))
        val expectedDimenSize = context.resources.getDimensionPixelSize(R.dimen.folder_preview_size)

        folderIcon.bind(
            info = folderInfo,
            loader = iconLoader,
            deviceProfile = null,
            iconSizePx = 0,
            onClick = {}
        )

        val previewContainer = folderIcon.findViewById<FrameLayout>(R.id.folder_preview_container)
        assertEquals(expectedDimenSize, previewContainer.layoutParams.width)
        assertEquals(expectedDimenSize, previewContainer.layoutParams.height)
    }

    @Test
    fun testAppAdapter_consistentIconSizeAcrossAppShortcutAndFolder() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
        // [Penjelasan]: Memastikan AppAdapter mengaplikasikan iconSizePx secara konsisten pada App, Shortcut, dan Folder
        val iconSizePx = 135
        var clickedFolder: FolderInfo? = null

        val adapter = AppAdapter(
            coroutineScope = testScope,
            iconSizePx = iconSizePx,
            showAppLabel = true,
            labelSizeSp = 12f,
            iconSpacingPx = 8,
            gridRows = 5,
            onClick = {},
            onShortcutClick = {},
            onFolderClick = { clickedFolder = it }
        )

        val app = createDummyApp("App Test", "com.test.app")
        val shortcut = WorkspaceShortcutInfo(
            shortcutId = "sc_1",
            packageName = "com.test.shortcut",
            user = Process.myUserHandle(),
            title = "Shortcut Test",
            shortcutInfo = null
        )
        val folder = FolderInfo(
            initialTitle = "Folder Test",
            initialContents = listOf(app)
        )

        val items = listOf(
            LauncherItem.App(app),
            LauncherItem.Shortcut(shortcut),
            LauncherItem.Folder(folder)
        )
        adapter.submitLauncherItems(items)

        val parent = FrameLayout(context)

        // 1. Bind App Item (position 0)
        val appHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0)) as AppAdapter.AppViewHolder
        adapter.onBindViewHolder(appHolder, 0)
        val appIconView = appHolder.itemView.findViewById<ImageView>(R.id.app_icon)
        assertEquals("App icon width harus sama dengan iconSizePx", iconSizePx, appIconView.layoutParams.width)
        assertEquals("App icon height harus sama dengan iconSizePx", iconSizePx, appIconView.layoutParams.height)

        // 2. Bind Shortcut Item (position 1)
        val shortcutHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(1)) as AppAdapter.ShortcutViewHolder
        adapter.onBindViewHolder(shortcutHolder, 1)
        val shortcutIconView = shortcutHolder.itemView.findViewById<ImageView>(R.id.app_icon)
        assertEquals("Shortcut icon width harus sama dengan iconSizePx", iconSizePx, shortcutIconView.layoutParams.width)
        assertEquals("Shortcut icon height harus sama dengan iconSizePx", iconSizePx, shortcutIconView.layoutParams.height)

        // 3. Bind Folder Item (position 2)
        val folderHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(2)) as AppAdapter.FolderViewHolder
        adapter.onBindViewHolder(folderHolder, 2)
        val folderPreview = folderHolder.folderIcon.findViewById<FrameLayout>(R.id.folder_preview_container)
        assertEquals("Folder preview width harus sama dengan iconSizePx", iconSizePx, folderPreview.layoutParams.width)
        assertEquals("Folder preview height harus sama dengan iconSizePx", iconSizePx, folderPreview.layoutParams.height)
    }

    @Test
    fun testAppAdapter_updateConfig_updatesFolderIconSizeDynamically() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
        // [Penjelasan]: Memastikan updateConfig() pada AppAdapter mengubah iconSizePx dan memicu pembaruan ukuran folder yang sedang di-bind
        val initialIconSizePx = 100
        val updatedIconSizePx = 175

        val adapter = AppAdapter(
            coroutineScope = testScope,
            iconSizePx = initialIconSizePx,
            showAppLabel = true,
            labelSizeSp = 12f,
            iconSpacingPx = 8,
            gridRows = 5,
            onClick = {},
            onShortcutClick = {},
            onFolderClick = {}
        )

        val folder = FolderInfo(
            initialTitle = "Dynamic Folder",
            initialContents = listOf(createDummyApp("App1", "com.test.app1"))
        )
        adapter.submitLauncherItems(listOf(LauncherItem.Folder(folder)))

        val parent = FrameLayout(context)
        val folderHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0)) as AppAdapter.FolderViewHolder
        adapter.onBindViewHolder(folderHolder, 0)

        val folderPreview = folderHolder.folderIcon.findViewById<FrameLayout>(R.id.folder_preview_container)
        assertEquals(initialIconSizePx, folderPreview.layoutParams.width)
        assertEquals(initialIconSizePx, folderPreview.layoutParams.height)

        // Panggil updateConfig dengan ukuran baru
        adapter.updateConfig(
            newIconSizePx = updatedIconSizePx,
            newShowLabel = true,
            newLabelSizeSp = 14f,
            newIconSpacingPx = 10,
            newGridRows = 6
        )

        // Re-bind view holder
        adapter.onBindViewHolder(folderHolder, 0)
        assertEquals("Folder preview width harus terbarui mengikuti updatedIconSizePx", updatedIconSizePx, folderPreview.layoutParams.width)
        assertEquals("Folder preview height harus terbarui mengikuti updatedIconSizePx", updatedIconSizePx, folderPreview.layoutParams.height)
    }

    @Test
    fun testFolderIcon_bindingNoRegression() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIconSizeTest
        // [Penjelasan]: Memastikan fungsionalitas binding judul, callback klik, dan unbind bekerja tanpa regresi
        val folderIcon = FolderIcon(context)
        val app = createDummyApp("App Test", "com.test.app")
        val folderInfo = FolderInfo(initialTitle = "Custom Name", initialContents = listOf(app))

        var clicked = false
        folderIcon.bind(
            info = folderInfo,
            loader = iconLoader,
            deviceProfile = null,
            iconSizePx = 120,
            showAppLabel = true,
            labelSizeSp = 14f,
            iconSpacingPx = 12,
            onClick = { clicked = true }
        )

        val titleView = folderIcon.findViewById<TextView>(R.id.folder_name)
        assertEquals(View.VISIBLE, titleView.visibility)
        assertEquals("Custom Name", titleView.text.toString())

        folderIcon.performClick()
        assertTrue("Callback klik folder harus terpanggil", clicked)

        folderIcon.unbind()
    }
}
