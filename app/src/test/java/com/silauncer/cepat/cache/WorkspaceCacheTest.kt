package com.silauncer.cepat.cache

import android.app.Application
import android.content.ComponentName
import android.os.Process
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.launcher.LauncherItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.WorkspaceCacheTest
// [Penjelasan]: Pengujian unit untuk memverifikasi penyimpanan, pengambilan, dan invalidasi tata letak workspace pada WorkspaceCache.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class WorkspaceCacheTest {

    @Before
    fun setup() {
        WorkspaceCache.clear()
    }

    @Test
    fun testSetAndGet() {
        val app = AppInfo("Browser", ComponentName("com.browser", "com.browser.Main"), "com.browser", Process.myUserHandle())
        val items = listOf(LauncherItem.App(app))

        WorkspaceCache.set(items)

        val retrieved = WorkspaceCache.get()
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.size)
    }

    @Test
    fun testInvalidate() {
        val app = AppInfo("Calculator", ComponentName("com.calc", "com.calc.Main"), "com.calc", Process.myUserHandle())
        WorkspaceCache.set(listOf(LauncherItem.App(app)))

        WorkspaceCache.invalidate()

        assertNull(WorkspaceCache.get())
    }
}
