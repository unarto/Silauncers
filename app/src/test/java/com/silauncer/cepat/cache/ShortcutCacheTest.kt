package com.silauncer.cepat.cache

import android.app.Application
import android.os.Process
import com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.ShortcutCacheTest
// [Penjelasan]: Pengujian unit untuk memverifikasi penyimpanan, pengambilan, dan invalidasi metadata shortcut pada ShortcutCache.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class ShortcutCacheTest {

    @Before
    fun setup() {
        ShortcutCache.clear()
    }

    @Test
    fun testPutAndGet() {
        val shortcut = WorkspaceShortcutInfo(
            shortcutId = "shortcut_new_chat",
            packageName = "com.chat.app",
            user = Process.myUserHandle(),
            title = "New Chat",
            shortcutInfo = null
        )
        val key = "com.chat.app_shortcut_new_chat_0"
        ShortcutCache.put(key, shortcut)

        val retrieved = ShortcutCache.get(key)
        assertNotNull(retrieved)
        assertEquals("New Chat", retrieved?.title)
    }

    @Test
    fun testRemovePackage() {
        val s1 = WorkspaceShortcutInfo("s1", "com.pkg.one", Process.myUserHandle(), "Shortcut 1", null)
        val s2 = WorkspaceShortcutInfo("s2", "com.pkg.two", Process.myUserHandle(), "Shortcut 2", null)

        ShortcutCache.put("com.pkg.one_s1_0", s1)
        ShortcutCache.put("com.pkg.two_s2_0", s2)

        ShortcutCache.removePackage("com.pkg.one")

        assertNull(ShortcutCache.get("com.pkg.one_s1_0"))
        assertNotNull(ShortcutCache.get("com.pkg.two_s2_0"))
    }
}
