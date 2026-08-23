package com.silauncer.cepat.folder

import android.app.Application
import android.content.ComponentName
import com.silauncer.cepat.apps.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class FolderInfoTest {

    private lateinit var app1: AppInfo
    private lateinit var app2: AppInfo
    private lateinit var app3: AppInfo

    @Before
    fun setup() {
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
    fun testInitialFolderState() {
        val folder = FolderInfo(initialTitle = "Work", initialContents = listOf(app1, app2))

        assertEquals("Work", folder.title)
        assertEquals(2, folder.itemCount())
        assertFalse(folder.isEmpty())
        assertTrue(folder.contains(app1))
        assertTrue(folder.contains(app2))
        assertFalse(folder.contains(app3))
    }

    @Test
    fun testAddItemAndListener() {
        val folder = FolderInfo(initialTitle = "Tools")
        var itemsChangedCalled = false
        var addedItem: AppInfo? = null

        val listener = object : FolderInfo.FolderListener {
            override fun onItemsChanged() {
                itemsChangedCalled = true
            }

            override fun onItemAdded(item: AppInfo, rank: Int) {
                addedItem = item
            }
        }
        folder.addListener(listener)

        folder.add(app1)

        assertEquals(1, folder.itemCount())
        assertTrue(itemsChangedCalled)
        assertEquals(app1, addedItem)
        assertEquals(app1, folder.getItem(0))
    }

    @Test
    fun testRemoveItemAndListener() {
        val folder = FolderInfo(initialContents = listOf(app1, app2))
        var removedItem: AppInfo? = null

        val listener = object : FolderInfo.FolderListener {
            override fun onItemRemoved(item: AppInfo) {
                removedItem = item
            }
        }
        folder.addListener(listener)

        val success = folder.remove(app1)
        assertTrue(success)
        assertEquals(1, folder.itemCount())
        assertEquals(app1, removedItem)
        assertEquals(app2, folder.getItem(0))
    }

    @Test
    fun testReorderItems() {
        val folder = FolderInfo(initialContents = listOf(app1, app2, app3))

        folder.reorder(0, 2)

        val items = folder.getItems()
        assertEquals(app2, items[0])
        assertEquals(app3, items[1])
        assertEquals(app1, items[2])
    }

    @Test
    fun testSetTitleAndListener() {
        val folder = FolderInfo(initialTitle = "Games")
        var updatedTitle: String? = null

        folder.addListener(object : FolderInfo.FolderListener {
            override fun onTitleChanged(newTitle: String) {
                updatedTitle = newTitle
            }
        })

        folder.setTitle("Favorites")

        assertEquals("Favorites", folder.title)
        assertEquals("Favorites", updatedTitle)
    }

    @Test
    fun testRemovePackage() {
        val folder = FolderInfo(initialContents = listOf(app1, app2, app3))

        val changed = folder.removePackage("com.example.app2")

        assertTrue(changed)
        assertEquals(2, folder.itemCount())
        assertFalse(folder.hasPackage("com.example.app2"))
        assertTrue(folder.hasPackage("com.example.app1"))
        assertTrue(folder.hasPackage("com.example.app3"))
    }
}
