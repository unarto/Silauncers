package com.silauncer.cepat.database

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Process
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.FolderCache
import com.silauncer.cepat.cache.WorkspaceCache
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.launcher.LauncherItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
// [Penjelasan]: Pengujian unit komprehensif untuk WorkspaceRepository guna memvalidasi perilaku Cache Hit, Cache Miss, Rekonsiliasi Snapshot Aplikasi (tambah/hapus aplikasi), penanganan isi Folder, serta konsistensi metode getCachedWorkspace, simpanWorkspace, dan muatWorkspace.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class WorkspaceRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: WorkspaceRepository
    private val myUser = Process.myUserHandle()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkspaceCache.clear()
        FolderCache.clear()
        repository = WorkspaceRepository(context)
    }

    @After
    fun tearDown() {
        WorkspaceCache.clear()
        FolderCache.clear()
    }

    private fun createApp(name: String, pkg: String, cls: String): AppInfo {
        return AppInfo(
            name = name,
            componentName = ComponentName(pkg, cls),
            packageName = pkg,
            user = myUser
        )
    }

    @Test
    fun testCacheHitWhenInstalledAppsSnapshotUnchanged() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa ketika snapshot aplikasi tidak berubah, WorkspaceRepository langsung mengembalikan item dari cache memori tanpa melakukan rekonstruksi ulang.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val app2 = createApp("App 2", "com.example.app2", "com.example.app2.MainActivity")
        val initialItems = listOf(LauncherItem.App(app1), LauncherItem.App(app2))

        repository.saveWorkspace(initialItems)

        val loadedFirst = repository.loadWorkspace(listOf(app1, app2))
        assertEquals(2, loadedFirst.size)

        // Verifikasi cache memori terisi
        val cached = repository.getCachedWorkspace()
        assertNotNull(cached)
        assertEquals(2, cached?.size)

        // Panggilan kedua dengan snapshot yang sama persis (Cache Hit)
        val loadedSecond = repository.loadWorkspace(listOf(app1, app2))
        assertEquals(2, loadedSecond.size)
        assertEquals(loadedFirst, loadedSecond)
    }

    @Test
    fun testCacheMissWhenCacheInvalidated() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi pemuatan kembali dari Room Database saat WorkspaceCache dalam keadaan kosong (cache miss).
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        repository.saveWorkspace(listOf(LauncherItem.App(app1)))

        WorkspaceCache.invalidate()
        assertNull(repository.getCachedWorkspace())

        val reloaded = repository.loadWorkspace(listOf(app1))
        assertEquals(1, reloaded.size)
        assertEquals("com.example.app1", (reloaded[0] as LauncherItem.App).appInfo.packageName)
        assertNotNull(repository.getCachedWorkspace())
    }

    @Test
    fun testSnapshotChange_NewAppInstalled_TriggersReconciliation() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa penambahan aplikasi baru pada snapshot memicu deteksi stale cache, rekonsiliasi ulang dengan DB, dan penambahan orphan app ke workspace.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val app2 = createApp("App 2", "com.example.app2", "com.example.app2.MainActivity")
        val newApp = createApp("App 3", "com.example.app3", "com.example.app3.MainActivity")

        repository.saveWorkspace(listOf(LauncherItem.App(app1), LauncherItem.App(app2)))
        repository.loadWorkspace(listOf(app1, app2))

        // Cache memori saat ini hanya punya app1 & app2
        assertEquals(2, repository.getCachedWorkspace()?.size)

        // Snapshot baru menyertakan newApp (App 3)
        val reloadedWithNewApp = repository.loadWorkspace(listOf(app1, app2, newApp))
        assertEquals(3, reloadedWithNewApp.size)
        assertTrue(reloadedWithNewApp.any { (it as? LauncherItem.App)?.appInfo?.packageName == "com.example.app3" })
        assertEquals(3, repository.getCachedWorkspace()?.size)
    }

    @Test
    fun testSnapshotChange_AppUninstalled_TriggersReconciliation() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa penghapusan aplikasi dari snapshot memicu deteksi stale cache dan rekonsiliasi ulang sehingga aplikasi terhapus dihilangkan dari hasil workspace.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val app2 = createApp("App 2", "com.example.app2", "com.example.app2.MainActivity")

        repository.saveWorkspace(listOf(LauncherItem.App(app1), LauncherItem.App(app2)))
        repository.loadWorkspace(listOf(app1, app2))

        // Cache memori memiliki 2 app
        assertEquals(2, repository.getCachedWorkspace()?.size)

        // Snapshot baru hanya memiliki app1 (app2 di-uninstall)
        val reloadedAfterUninstall = repository.loadWorkspace(listOf(app1))
        assertEquals(1, reloadedAfterUninstall.size)
        assertEquals("com.example.app1", (reloadedAfterUninstall[0] as LauncherItem.App).appInfo.packageName)
        assertEquals(1, repository.getCachedWorkspace()?.size)
    }

    @Test
    fun testSnapshotChange_FolderAppUninstalled_DissolvesFolderProperly() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa ketika salah satu aplikasi di dalam folder di-uninstall dari snapshot, folder yang tersisa 1 aplikasi otomatis dibubarkan (auto-dissolve) dan workspace cache diperbarui.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val app2 = createApp("App 2", "com.example.app2", "com.example.app2.MainActivity")

        val folderInfo = FolderInfo(id = "folder_test", initialTitle = "Tools", initialContents = listOf(app1, app2))
        repository.saveWorkspace(listOf(LauncherItem.Folder(folderInfo)))

        val loaded = repository.loadWorkspace(listOf(app1, app2))
        assertEquals(1, loaded.size)
        assertTrue(loaded[0] is LauncherItem.Folder)

        // app2 di-uninstall, folder tersisa 1 app (app1) -> auto dissolve
        val reloaded = repository.loadWorkspace(listOf(app1))
        assertEquals(1, reloaded.size)
        assertTrue(reloaded[0] is LauncherItem.App)
        assertEquals("com.example.app1", (reloaded[0] as LauncherItem.App).appInfo.packageName)
    }

    @Test
    fun testAliases_simpanWorkspace_and_muatWorkspace() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa alias simpanWorkspace dan muatWorkspace berfungsi identik dengan saveWorkspace dan loadWorkspace.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        repository.simpanWorkspace(listOf(LauncherItem.App(app1)))

        val loaded = repository.muatWorkspace(listOf(app1))
        assertEquals(1, loaded.size)
        assertEquals("com.example.app1", (loaded[0] as LauncherItem.App).appInfo.packageName)
    }

    @Test
    fun testIncrementalSave_PreservesPrimaryKeysOnReorder() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa pengurutan ulang item tidak menghapus seluruh baris DB, melainkan memperbarui (update) baris eksisting dengan mempertahankan ID primary key SQLite.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val app2 = createApp("App 2", "com.example.app2", "com.example.app2.MainActivity")
        val dao = LauncherDatabase.getDatabase(context).workspaceItemDao()

        // Simpan urutan awal: app1 (rank 0), app2 (rank 1)
        repository.saveWorkspace(listOf(LauncherItem.App(app1), LauncherItem.App(app2)))
        val initialEntities = dao.getAllItemsSync()
        assertEquals(2, initialEntities.size)
        val app1InitialId = initialEntities.first { it.componentName == app1.componentName.flattenToString() }.id
        val app2InitialId = initialEntities.first { it.componentName == app2.componentName.flattenToString() }.id

        // Simpan urutan baru: app2 (rank 0), app1 (rank 1)
        repository.saveWorkspace(listOf(LauncherItem.App(app2), LauncherItem.App(app1)))
        val reorderedEntities = dao.getAllItemsSync()
        assertEquals(2, reorderedEntities.size)

        val app1Reordered = reorderedEntities.first { it.componentName == app1.componentName.flattenToString() }
        val app2Reordered = reorderedEntities.first { it.componentName == app2.componentName.flattenToString() }

        // Primary key IDs harus dipertahankan (bukan dihapus lalu di-generate baru)
        assertEquals(app1InitialId, app1Reordered.id)
        assertEquals(app2InitialId, app2Reordered.id)
        assertEquals(1, app1Reordered.rank)
        assertEquals(0, app2Reordered.rank)
    }

    @Test
    fun testIncrementalSave_AddsNewItemAndDeletesRemovedItem() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi diff update: item yang dipertahankan tidak berubah ID-nya, item baru di-insert, dan item yang dibuang di-delete.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val app2 = createApp("App 2", "com.example.app2", "com.example.app2.MainActivity")
        val app3 = createApp("App 3", "com.example.app3", "com.example.app3.MainActivity")
        val dao = LauncherDatabase.getDatabase(context).workspaceItemDao()

        repository.saveWorkspace(listOf(LauncherItem.App(app1), LauncherItem.App(app2)))
        val initialApp1Id = dao.getAllItemsSync().first { it.componentName == app1.componentName.flattenToString() }.id

        // app2 dibuang, app3 ditambahkan
        repository.saveWorkspace(listOf(LauncherItem.App(app1), LauncherItem.App(app3)))
        val updatedEntities = dao.getAllItemsSync()
        assertEquals(2, updatedEntities.size)

        // app1 tetap memiliki ID yang sama
        val currentApp1 = updatedEntities.first { it.componentName == app1.componentName.flattenToString() }
        assertEquals(initialApp1Id, currentApp1.id)

        // app2 sudah terhapus
        assertTrue(updatedEntities.none { it.componentName == app2.componentName.flattenToString() })

        // app3 berhasil ditambahkan
        assertTrue(updatedEntities.any { it.componentName == app3.componentName.flattenToString() })
    }

    @Test
    fun testIncrementalSave_FolderTitleUpdate() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi pembaruan judul folder secara in-place mempertahankan ID folder tanpa menghapus child items.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val dao = LauncherDatabase.getDatabase(context).workspaceItemDao()

        val folderInfo = FolderInfo(id = "folder_edit", initialTitle = "Social", initialContents = listOf(app1))
        repository.saveWorkspace(listOf(LauncherItem.Folder(folderInfo)))

        val initialFolderEntity = dao.getAllItemsSync().first { it.itemUid == "folder_edit" }
        assertEquals("Social", initialFolderEntity.title)
        val initialFolderId = initialFolderEntity.id

        // Ubah judul folder menjadi "Work"
        folderInfo.setTitle("Work")
        repository.saveWorkspace(listOf(LauncherItem.Folder(folderInfo)))

        val updatedFolderEntity = dao.getAllItemsSync().first { it.itemUid == "folder_edit" }
        assertEquals("Work", updatedFolderEntity.title)
        assertEquals(initialFolderId, updatedFolderEntity.id)
    }

    @Test
    fun testSaveEmptyWorkspace_ClearsDatabase() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa menyimpan daftar kosong memicu pembersihan menyeluruh pada tabel Room.
        val app1 = createApp("App 1", "com.example.app1", "com.example.app1.MainActivity")
        val dao = LauncherDatabase.getDatabase(context).workspaceItemDao()

        repository.saveWorkspace(listOf(LauncherItem.App(app1)))
        assertEquals(1, dao.getAllItemsSync().size)

        repository.saveWorkspace(emptyList())
        assertTrue(dao.getAllItemsSync().isEmpty())
    }
}
