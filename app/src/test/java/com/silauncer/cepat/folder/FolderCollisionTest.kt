package com.silauncer.cepat.folder

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.dragndrop.FolderCollisionHelper
import com.silauncer.cepat.home.AppAdapter
import com.silauncer.cepat.launcher.FolderManager
import com.silauncer.cepat.launcher.LauncherItem
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

// [Jalur Class]: com.silauncer.cepat.folder.FolderCollisionTest
// [Penjelasan]: Unit test untuk deteksi collision folder dan kreasi folder via FolderManager.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class FolderCollisionTest {

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
    fun testFolderManagerCreatesFolderFromTwoApps() {
        val initialItems = listOf(
            LauncherItem.App(app1),
            LauncherItem.App(app2),
            LauncherItem.App(app3)
        )

        // Drop item at position 1 (app2) onto item at position 0 (app1)
        val updatedItems = folderManager.createFolder(
            items = initialItems,
            targetPosition = 0,
            sourcePosition = 1,
            folderTitle = "Utilities"
        )

        assertNotNull(updatedItems)
        assertEquals(2, updatedItems?.size)

        val firstItem = updatedItems?.get(0)
        assertTrue(firstItem is LauncherItem.Folder)
        val folder = (firstItem as LauncherItem.Folder).folderInfo
        assertEquals("Utilities", folder.title)
        assertEquals(2, folder.itemCount())
        assertEquals(app1, folder.getItem(0))
        assertEquals(app2, folder.getItem(1))

        val secondItem = updatedItems[1]
        assertTrue(secondItem is LauncherItem.App)
        assertEquals(app3, (secondItem as LauncherItem.App).appInfo)
    }

    @Test
    fun testFolderManagerAddsAppToExistingFolder() {
        val initialFolder = FolderInfo(initialTitle = "Work", initialContents = listOf(app1, app2))
        val initialItems = listOf(
            LauncherItem.Folder(initialFolder),
            LauncherItem.App(app3)
        )

        // Drop app3 (pos 1) into Folder (pos 0)
        val updatedItems = folderManager.createFolder(
            items = initialItems,
            targetPosition = 0,
            sourcePosition = 1
        )

        assertNotNull(updatedItems)
        assertEquals(1, updatedItems?.size)

        val firstItem = updatedItems?.get(0)
        assertTrue(firstItem is LauncherItem.Folder)
        val folder = (firstItem as LauncherItem.Folder).folderInfo
        assertEquals(3, folder.itemCount())
        assertEquals(app3, folder.getItem(2))
    }

    @Test
    fun testCollisionHelperHoverStateManagement() {
        val collisionHelper = FolderCollisionHelper(context)
        val dummyView = View(context)

        // Hover enter
        val targetResult = FolderCollisionHelper.DropTargetResult(
            position = 1,
            viewHolder = object : RecyclerView.ViewHolder(dummyView) {},
            itemView = dummyView,
            distancePx = 15f
        )

        collisionHelper.updateHoverState(targetResult)
        collisionHelper.clearHover()
    }
}

