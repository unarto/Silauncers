package com.silauncer.cepat.folder

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.launcher.FolderManager
import com.silauncer.cepat.launcher.LauncherItem
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

// [Jalur Class]: com.silauncer.cepat.folder.FolderDragOutAndDissolveTest
// [Penjelasan]: Unit test untuk auto-dissolve folder saat item dikeluarkan via FolderManager.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class FolderDragOutAndDissolveTest {

    private lateinit var context: Context
    private lateinit var app1: AppInfo
    private lateinit var app2: AppInfo
    private lateinit var app3: AppInfo
    private val folderManager = FolderManager()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        app1 = AppInfo(
            name = "App One",
            componentName = ComponentName("com.example.app1", "com.example.app1.MainActivity"),
            packageName = "com.example.app1"
        )
        app2 = AppInfo(
            name = "App Two",
            componentName = ComponentName("com.example.app2", "com.example.app2.MainActivity"),
            packageName = "com.example.app2"
        )
        app3 = AppInfo(
            name = "App Three",
            componentName = ComponentName("com.example.app3", "com.example.app3.MainActivity"),
            packageName = "com.example.app3"
        )
    }

    @Test
    fun testFolderInfoShouldAutoDissolveCondition() {
        val folder = FolderInfo(initialTitle = "Games", initialContents = listOf(app1, app2, app3))
        assertFalse(folder.shouldAutoDissolve())
        assertNull(folder.getSingleRemainingApp())

        folder.remove(app3)
        assertEquals(2, folder.itemCount())
        assertFalse(folder.shouldAutoDissolve())
        assertNull(folder.getSingleRemainingApp())

        folder.remove(app2)
        assertEquals(1, folder.itemCount())
        assertTrue(folder.shouldAutoDissolve())
        assertEquals(app1, folder.getSingleRemainingApp())

        folder.remove(app1)
        assertEquals(0, folder.itemCount())
        assertTrue(folder.shouldAutoDissolve())
        assertNull(folder.getSingleRemainingApp())
    }

    @Test
    fun testDragOutFromTwoItemFolderTriggersAutoDissolve() {
        val folderInfo = FolderInfo(initialTitle = "Work", initialContents = listOf(app1, app2))
        val initialItems = listOf(LauncherItem.Folder(folderInfo), LauncherItem.App(app3))

        // Drag app2 out of folder to workspace position 1
        val updatedItems = folderManager.removeAppFromFolder(
            items = initialItems,
            folderInfo = folderInfo,
            itemToRemove = com.silauncer.cepat.launcher.LauncherItem.App(app2),
            targetWorkspacePosition = 1
        )

        assertNotNull(updatedItems)
        // After drag-out: folder had 2 items -> 1 item left -> auto-dissolves into App(app1)
        // List should contain: [App(app2), App(app1), App(app3)] or placed at target position
        assertEquals(3, updatedItems?.size)

        val item1 = updatedItems?.get(1)
        assertTrue(item1 is LauncherItem.App)
        assertEquals(app2, (item1 as LauncherItem.App).appInfo)
    }

    @Test
    fun testDragOutFromThreeItemFolderPreservesFolder() {
        val folderInfo = FolderInfo(initialTitle = "Tools", initialContents = listOf(app1, app2, app3))
        val initialItems = listOf(LauncherItem.Folder(folderInfo))

        // Drag out app3 to workspace position 1
        val updatedItems = folderManager.removeAppFromFolder(
            items = initialItems,
            folderInfo = folderInfo,
            itemToRemove = com.silauncer.cepat.launcher.LauncherItem.App(app3),
            targetWorkspacePosition = 1
        )

        assertNotNull(updatedItems)
        assertEquals(2, updatedItems?.size)

        val item0 = updatedItems?.get(0)
        assertTrue(item0 is LauncherItem.Folder)
        assertEquals(2, (item0 as LauncherItem.Folder).folderInfo.itemCount())
        assertEquals(listOf(app1, app2), (item0 as LauncherItem.Folder).folderInfo.getItems())

        val item1 = updatedItems?.get(1)
        assertTrue(item1 is LauncherItem.App)
        assertEquals(app3, (item1 as LauncherItem.App).appInfo)
    }

    @Test
    fun testAutoDissolveEmptyFolderRemovesFolderFromWorkspace() {
        val folderInfo = FolderInfo(initialTitle = "EmptySoon", initialContents = listOf(app1))
        val initialItems = listOf(LauncherItem.Folder(folderInfo), LauncherItem.App(app2))

        // Drag out the only item (app1)
        val updatedItems = folderManager.removeAppFromFolder(
            items = initialItems,
            folderInfo = folderInfo,
            itemToRemove = com.silauncer.cepat.launcher.LauncherItem.App(app1),
            targetWorkspacePosition = 0
        )

        assertNotNull(updatedItems)
        // Folder had 1 item, now 0 items -> folder is removed completely, app1 is placed at pos 0
        assertEquals(2, updatedItems?.size)
        val item0 = updatedItems?.get(0)
        assertTrue(item0 is LauncherItem.App)
        assertEquals(app1, (item0 as LauncherItem.App).appInfo)
    }

    @Test
    fun testUnpackFolderRestoresAllItemsToWorkspace() {
        val folderInfo = FolderInfo(initialTitle = "UnpackTest", initialContents = listOf(app1, app2, app3))
        val initialItems = mutableListOf<LauncherItem>(LauncherItem.Folder(folderInfo))

        val allItems = folderInfo.getAllItems()
        assertEquals(3, allItems.size)

        val folderIndex = initialItems.indexOfFirst { (it as? LauncherItem.Folder)?.folderInfo?.id == folderInfo.id }
        assertEquals(0, folderIndex)

        initialItems.removeAt(folderIndex)
        initialItems.addAll(folderIndex, allItems)

        assertEquals(3, initialItems.size)
        assertTrue(initialItems[0] is LauncherItem.App)
        assertEquals(app1, (initialItems[0] as LauncherItem.App).appInfo)
        assertEquals(app2, (initialItems[1] as LauncherItem.App).appInfo)
        assertEquals(app3, (initialItems[2] as LauncherItem.App).appInfo)
    }

    @Test
    fun testWallpaperBlurControllerFastStackBlur() {
        val controller = FolderWallpaperBlurController(context)
        val bitmap = android.graphics.Bitmap.createBitmap(50, 50, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)

        val blurred = controller.fastStackBlur(bitmap, radius = 10)
        assertNotNull(blurred)
        assertEquals(50, blurred.width)
        assertEquals(50, blurred.height)
    }

    @Test
    fun testFolderDragDropController_360DegreeDragAndExit() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderDragDropController
        // [Penjelasan]: Memverifikasi bahwa FolderDragDropController mendukung drag bebas 360 derajat dan mendeteksi keluarnya item di segala arah
        val cardContainer = android.view.View(context).apply {
            layout(100, 100, 300, 300)
        }
        val scrimView = android.view.View(context)
        val itemView = android.view.View(context)

        val controller = FolderDragDropController(
            cardContainer = cardContainer,
            scrimView = scrimView,
            dragOutThreshold = 20f
        )

        var dragOutCalled = false
        var draggedAppResult: LauncherItem? = null
        var dropXResult = 0f
        var dropYResult = 0f

        controller.onDragOutListener = { item, x, y ->
            dragOutCalled = true
            draggedAppResult = item
            dropXResult = x
            dropYResult = y
        }

        val appItem = LauncherItem.App(app1)
        controller.startDragOutTracking(appItem, itemView)
        assertTrue(controller.isDraggingItem)
        assertEquals(appItem, controller.activeDragApp)

        // Test 1: Gerakan drag bebas ke kanan bawah (diagonal)
        val moveEvent = android.view.MotionEvent.obtain(0L, 10L, android.view.MotionEvent.ACTION_MOVE, 200f, 200f, 0)
        val handled = controller.onTouchEvent(moveEvent)
        assertTrue(handled)

        // Test 2: Trigger forceDragExit (menyeret ke luar batas ke arah kiri / atas / bawah / kanan)
        controller.forceDragExit(50f, 50f)
        val upEvent = android.view.MotionEvent.obtain(0L, 20L, android.view.MotionEvent.ACTION_UP, 50f, 50f, 0)
        controller.onTouchEvent(upEvent)

        assertTrue(dragOutCalled)
        assertEquals(appItem, draggedAppResult)
        assertEquals(50f, dropXResult, 0.01f)
        assertEquals(50f, dropYResult, 0.01f)
        assertFalse(controller.isDraggingItem)
    }
}

