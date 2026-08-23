package com.silauncer.cepat.cache

import android.app.Application
import com.silauncer.cepat.folder.FolderInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.FolderCacheTest
// [Penjelasan]: Pengujian unit untuk memverifikasi penyimpanan, pengambilan, dan penghapusan folder pada FolderCache.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class FolderCacheTest {

    @Before
    fun setup() {
        FolderCache.clear()
    }

    @Test
    fun testPutAndGet() {
        val folder = FolderInfo(id = "folder_123", initialTitle = "Games")
        FolderCache.put(folder.id, folder)

        val retrieved = FolderCache.get("folder_123")
        assertNotNull(retrieved)
        assertEquals("Games", retrieved?.title)
    }

    @Test
    fun testRemove() {
        val folder = FolderInfo(id = "folder_abc", initialTitle = "Social")
        FolderCache.put(folder.id, folder)

        FolderCache.remove("folder_abc")
        assertNull(FolderCache.get("folder_abc"))
    }
}
