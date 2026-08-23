package com.silauncer.cepat.cache

import android.app.Application
import android.content.ComponentName
import android.os.Process
import com.silauncer.cepat.apps.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.AppCacheTest
// [Penjelasan]: Pengujian unit untuk memverifikasi penyimpanan, pengambilan, dan invalidasi paket pada AppCache.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class AppCacheTest {

    @Before
    fun setup() {
        AppCache.clear()
    }

    @Test
    fun testPutAndGet() {
        val appInfo = AppInfo(
            name = "Test App",
            componentName = ComponentName("com.test.app", "com.test.app.MainActivity"),
            packageName = "com.test.app",
            user = Process.myUserHandle()
        )
        val key = "com.test.app_MainActivity_0"
        AppCache.put(key, appInfo)

        val retrieved = AppCache.get(key)
        assertNotNull(retrieved)
        assertEquals("Test App", retrieved?.name)
    }

    @Test
    fun testCacheKeyConsistency() {
        val component = ComponentName("com.sample.app", "com.sample.app.MainActivity")
        val user = Process.myUserHandle()
        val app = AppInfo("App", component, "com.sample.app", user)

        val factoryKey = AppInfo.createCacheKey(component, user)
        val modelKey = app.cacheKey

        assertEquals(factoryKey, modelKey)
        AppCache.put(factoryKey, app)
        assertEquals(app, AppCache.get(modelKey))

        AppCache.removePackage("com.sample.app")
        assertNull(AppCache.get(modelKey))
    }

    @Test
    fun testRemovePackage() {
        val app1 = AppInfo("App 1", ComponentName("com.sample.one", "com.sample.one.Main"), "com.sample.one", Process.myUserHandle())
        val app2 = AppInfo("App 2", ComponentName("com.sample.two", "com.sample.two.Main"), "com.sample.two", Process.myUserHandle())

        AppCache.put("com.sample.one_Main_0", app1)
        AppCache.put("com.sample.two_Main_0", app2)

        AppCache.removePackage("com.sample.one")

        assertNull(AppCache.get("com.sample.one_Main_0"))
        assertNotNull(AppCache.get("com.sample.two_Main_0"))
    }
}
