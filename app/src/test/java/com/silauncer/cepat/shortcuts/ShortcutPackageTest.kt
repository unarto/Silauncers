package com.silauncer.cepat.shortcuts

import android.content.ComponentName
import android.os.Process
import android.os.UserHandle
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.home.LauncherItemDiffCallback
import com.silauncer.cepat.launcher.FolderManager
import com.silauncer.cepat.launcher.LauncherItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
// [Penjelasan]: Pengujian unit komprehensif untuk modul Pintasan (Shortcut) Launcher3 yang diadaptasikan ke Silauncer
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShortcutPackageTest {

    private val userHandle = Process.myUserHandle()

    @Test
    fun testShortcutKey_EqualityAndHash() {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
        // [Penjelasan]: Menguji kesetaraan dan identifikasi unik ShortcutKey berdasarkan packageName, id shortcut, dan UserHandle
        val key1 = ShortcutKey("com.example.app", userHandle, "shortcut_compose")
        val key2 = ShortcutKey("com.example.app", userHandle, "shortcut_compose")
        val key3 = ShortcutKey("com.example.app", userHandle, "shortcut_search")

        assertEquals(key1, key2)
        assertEquals(key1.hashCode(), key2.hashCode())
        assertNotEquals(key1, key3)
        assertEquals("com.example.app", key1.packageName)
        assertEquals("shortcut_compose", key1.id)
        assertEquals(userHandle, key1.user)
    }

    @Test
    fun testWorkspaceShortcutInfo_PropertiesAndCacheKey() {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
        // [Penjelasan]: Menguji integritas model data WorkspaceShortcutInfo, evaluasi default isEnabled, dan pembentukan cacheKey
        val shortcut = WorkspaceShortcutInfo(
            shortcutId = "sc_compose_mail",
            packageName = "com.google.android.gm",
            user = userHandle,
            title = "Tulis Email",
            shortcutInfo = null
        )

        assertEquals("sc_compose_mail", shortcut.shortcutId)
        assertEquals("com.google.android.gm", shortcut.packageName)
        assertEquals("Tulis Email", shortcut.title)
        assertTrue(shortcut.isEnabled)
        assertNull(shortcut.disabledMessage)
        assertNull(shortcut.intent)
        assertTrue(shortcut.cacheKey.contains("com.google.android.gm"))
        assertTrue(shortcut.cacheKey.contains("sc_compose_mail"))
    }

    @Test
    fun testLauncherItem_TypesAndSealedClasses() {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
        // [Penjelasan]: Menguji representasi sealed class LauncherItem untuk App, Folder, dan Shortcut
        val app = AppInfo(
            name = "Kamera",
            componentName = ComponentName("com.android.camera", "com.android.camera.CameraActivity"),
            packageName = "com.android.camera",
            user = userHandle
        )
        val shortcut = WorkspaceShortcutInfo(
            shortcutId = "sc_selfie",
            packageName = "com.android.camera",
            user = userHandle,
            title = "Selfie",
            shortcutInfo = null
        )
        val folder = FolderInfo(initialTitle = "Media", initialContents = listOf(app))

        val itemApp = LauncherItem.App(app)
        val itemShortcut = LauncherItem.Shortcut(shortcut)
        val itemFolder = LauncherItem.Folder(folder)

        assertTrue(itemApp is LauncherItem)
        assertTrue(itemShortcut is LauncherItem)
        assertTrue(itemFolder is LauncherItem)

        assertEquals(app, itemApp.appInfo)
        assertEquals(shortcut, itemShortcut.shortcutInfo)
        assertEquals(folder, itemFolder.folderInfo)
    }

    @Test
    fun testFolderInfo_ShortcutAdditionAndRemoval() {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
        // [Penjelasan]: Menguji penambahan dan penghapusan WorkspaceShortcutInfo di dalam FolderInfo, serta verifikasi auto-dissolve
        val app = AppInfo(
            name = "Galeri",
            componentName = ComponentName("com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity"),
            packageName = "com.android.gallery3d",
            user = userHandle
        )
        val shortcut = WorkspaceShortcutInfo(
            shortcutId = "sc_albums",
            packageName = "com.android.gallery3d",
            user = userHandle,
            title = "Album Foto",
            shortcutInfo = null
        )

        val folderInfo = FolderInfo(initialTitle = "Foto", initialContents = listOf(app))
        assertEquals(1, folderInfo.itemCount())
        assertTrue(folderInfo.isShortcutsEmpty())

        folderInfo.addShortcut(shortcut)
        assertEquals(2, folderInfo.itemCount())
        assertFalse(folderInfo.shouldAutoDissolve())
        assertEquals(1, folderInfo.getShortcuts().size)
        assertFalse(folderInfo.isShortcutsEmpty())

        val allItems = folderInfo.getAllItems()
        assertEquals(2, allItems.size)
        assertTrue(allItems[0] is LauncherItem.App)
        assertTrue(allItems[1] is LauncherItem.Shortcut)

        // Hapus shortcut
        val removed = folderInfo.removeShortcut(shortcut)
        assertTrue(removed)
        assertEquals(1, folderInfo.itemCount())
        assertTrue(folderInfo.shouldAutoDissolve())
        assertEquals(app, folderInfo.getSingleRemainingApp())
    }

    @Test
    fun testFolderManager_CreateFolderWithShortcut() {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
        // [Penjelasan]: Menguji kemampuan FolderManager untuk menggabungkan LauncherItem.App dan LauncherItem.Shortcut menjadi sebuah Folder
        val app = AppInfo(
            name = "Catatan",
            componentName = ComponentName("com.example.notes", "com.example.notes.MainActivity"),
            packageName = "com.example.notes",
            user = userHandle
        )
        val shortcut = WorkspaceShortcutInfo(
            shortcutId = "new_note",
            packageName = "com.example.notes",
            user = userHandle,
            title = "Catatan Baru",
            shortcutInfo = null
        )

        val items = listOf(LauncherItem.App(app), LauncherItem.Shortcut(shortcut))
        val manager = FolderManager()

        val newItems = manager.createFolder(items, targetPosition = 0, sourcePosition = 1, folderTitle = "Produktivitas")
        assertNotNull(newItems)
        assertEquals(1, newItems!!.size)
        assertTrue(newItems[0] is LauncherItem.Folder)

        val createdFolder = (newItems[0] as LauncherItem.Folder).folderInfo
        assertEquals("Produktivitas", createdFolder.title)
        assertEquals(2, createdFolder.itemCount())
        assertEquals(1, createdFolder.getItems().size)
        assertEquals(1, createdFolder.getShortcuts().size)
    }

    @Test
    fun testFolderManager_RemoveShortcutFromFolder() {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
        // [Penjelasan]: Menguji pelepasan Shortcut dari Folder dan auto-dissolve jika sisa item tinggal 1
        val app = AppInfo(
            name = "Musik",
            componentName = ComponentName("com.example.music", "com.example.music.PlayerActivity"),
            packageName = "com.example.music",
            user = userHandle
        )
        val shortcut = WorkspaceShortcutInfo(
            shortcutId = "play_favorites",
            packageName = "com.example.music",
            user = userHandle,
            title = "Favorit",
            shortcutInfo = null
        )

        val folderInfo = FolderInfo(
            initialTitle = "Audio",
            initialContents = listOf(app),
            initialShortcuts = listOf(shortcut)
        )
        val workspaceItems = listOf(LauncherItem.Folder(folderInfo))
        val manager = FolderManager()

        val updatedItems = manager.removeAppFromFolder(
            items = workspaceItems,
            folderInfo = folderInfo,
            itemToRemove = LauncherItem.Shortcut(shortcut),
            targetWorkspacePosition = 1
        )

        assertNotNull(updatedItems)
        // Folder auto-dissolved because only 1 app was left, so workspace now has App + Shortcut
        assertEquals(2, updatedItems!!.size)
        assertTrue(updatedItems.any { it is LauncherItem.App && it.appInfo.packageName == "com.example.music" })
        assertTrue(updatedItems.any { it is LauncherItem.Shortcut && it.shortcutInfo.shortcutId == "play_favorites" })
    }

    @Test
    fun testLauncherItemDiffCallback_Accuracy() {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutPackageTest
        // [Penjelasan]: Menguji DiffUtil Callback dalam mendeteksi kesamaan item dan perubahan konten antar varian LauncherItem
        val shortcut1 = WorkspaceShortcutInfo(
            shortcutId = "sc1",
            packageName = "com.app.a",
            user = userHandle,
            title = "Pintasan A",
            shortcutInfo = null
        )
        val shortcut2 = WorkspaceShortcutInfo(
            shortcutId = "sc1",
            packageName = "com.app.a",
            user = userHandle,
            title = "Pintasan A Baru",
            shortcutInfo = null
        )

        val oldList = listOf(LauncherItem.Shortcut(shortcut1))
        val newList = listOf(LauncherItem.Shortcut(shortcut2))

        val diff = LauncherItemDiffCallback(oldList, newList)
        assertTrue(diff.areItemsTheSame(0, 0))
        assertFalse(diff.areContentsTheSame(0, 0))
    }
}
