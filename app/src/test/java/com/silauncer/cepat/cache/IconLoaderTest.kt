package com.silauncer.cepat.cache

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.IconLoaderTest
// [Penjelasan]: Pengujian unit untuk IconLoader guna memverifikasi alur L1 Memory Cache Hit, L2 Disk Cache Hit (tanpa kedipan placeholder default icon), Fallback Icon saat missing, dan in-flight deduplication.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class IconLoaderTest {

    private lateinit var context: Context

    private class SimpleTestDrawable : Drawable() {
        override fun draw(canvas: Canvas) {}
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        IconCache.clear()
        kotlinx.coroutines.runBlocking {
            DiskIconCache.clear(context)
        }
    }

    @Test
    fun testLoadIconAsync_L1CacheHit_InvokesImmediately() = runTest {
        // [Jalur Class]: com.silauncer.cepat.cache.IconLoaderTest
        // [Penjelasan]: Memverifikasi bahwa jika ikon sudah ada di L1 Memory Cache, onLoaded langsung dipanggil secara sinkron tanpa menunggu coroutine dispatcher.
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val iconLoader = IconLoader(this, ioDispatcher = testDispatcher, mainDispatcher = testDispatcher)
        val appInfo = AppInfo(
            name = "Test App",
            componentName = ComponentName("com.sample.test", "com.sample.test.MainActivity"),
            packageName = "com.sample.test",
            user = Process.myUserHandle()
        )
        val cachedDrawable = SimpleTestDrawable()
        IconCache.put(appInfo.cacheKey, cachedDrawable)

        var loadedDrawable: Drawable? = null
        var loadedKey: String? = null
        var callbackCount = 0

        iconLoader.loadIconAsync(context, appInfo, 48) { drawable, key ->
            loadedDrawable = drawable
            loadedKey = key
            callbackCount++
        }

        assertEquals(1, callbackCount)
        assertSame(cachedDrawable, loadedDrawable)
        assertEquals(appInfo.cacheKey, loadedKey)
    }

    @Test
    fun testLoadIconAsync_L2DiskCacheHit_LoadsWithoutDefaultIconFlash() = runTest {
        // [Jalur Class]: com.silauncer.cepat.cache.IconLoaderTest
        // [Penjelasan]: Memverifikasi bahwa jika ikon berada di L2 Disk Cache, onLoaded HANYA dipanggil satu kali dengan ikon disk yang sebenarnya (tanpa kedipan default green icon).
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val iconLoader = IconLoader(this, ioDispatcher = testDispatcher, mainDispatcher = testDispatcher)
        val appInfo = AppInfo(
            name = "Disk App",
            componentName = ComponentName("com.disk.test", "com.disk.test.MainActivity"),
            packageName = "com.disk.test",
            user = Process.myUserHandle()
        )

        val diskBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        DiskIconCache.put(context, appInfo.cacheKey, diskBitmap)

        val receivedDrawables = mutableListOf<Drawable>()
        val receivedKeys = mutableListOf<String>()

        iconLoader.loadIconAsync(context, appInfo, 48) { drawable, key ->
            receivedDrawables.add(drawable)
            receivedKeys.add(key)
        }

        // Sebelum coroutine dieksekusi, callback tidak boleh dipanggil (tidak ada flash default icon prematur)
        assertEquals(0, receivedDrawables.size)

        testScheduler.advanceUntilIdle()

        // Setelah disk cache terbaca via dispatcher, tepat 1 callback terpanggil dengan hasil disk cache
        assertEquals(1, receivedDrawables.size)
        assertEquals(appInfo.cacheKey, receivedKeys[0])
        assertNotNull(IconCache.get(appInfo.cacheKey))
    }

    @Test
    fun testLoadIconAsync_CacheMiss_FallbackToDefaultActivityIcon() = runTest {
        // [Jalur Class]: com.silauncer.cepat.cache.IconLoaderTest
        // [Penjelasan]: Memverifikasi bahwa jika ikon tidak ada di L1 maupun L2 dan paket tidak ditemukan di PackageManager, iconLoader menghasilkan default activity icon yang dinormalisasi dan menyimpannya ke cache.
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val iconLoader = IconLoader(this, ioDispatcher = testDispatcher, mainDispatcher = testDispatcher)
        val appInfo = AppInfo(
            name = "Unknown App",
            componentName = ComponentName("com.unknown.nonexistent", "com.unknown.nonexistent.MainActivity"),
            packageName = "com.unknown.nonexistent",
            user = Process.myUserHandle()
        )

        var loadedDrawable: Drawable? = null
        var loadedKey: String? = null

        iconLoader.loadIconAsync(context, appInfo, 48) { drawable, key ->
            loadedDrawable = drawable
            loadedKey = key
        }

        testScheduler.advanceUntilIdle()

        assertNotNull("Loaded drawable must not be null on fallback", loadedDrawable)
        assertEquals(appInfo.cacheKey, loadedKey)
        // Terisi di L1 Memory Cache
        assertNotNull(IconCache.get(appInfo.cacheKey))
    }

    @Test
    fun testLoadIconAsync_InFlightDeduplication() = runTest {
        // [Jalur Class]: com.silauncer.cepat.cache.IconLoaderTest
        // [Penjelasan]: Memverifikasi bahwa beberapa pemanggilan serentak untuk cacheKey yang sama menggunakan in-flight request deduplication sehingga keduanya menerima callback hasil yang sama.
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val iconLoader = IconLoader(this, ioDispatcher = testDispatcher, mainDispatcher = testDispatcher)
        val appInfo = AppInfo(
            name = "Shared App",
            componentName = ComponentName("com.shared.test", "com.shared.test.MainActivity"),
            packageName = "com.shared.test",
            user = Process.myUserHandle()
        )

        val diskBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        DiskIconCache.put(context, appInfo.cacheKey, diskBitmap)

        val loadedKeys1 = mutableListOf<String>()
        val loadedKeys2 = mutableListOf<String>()

        iconLoader.loadIconAsync(context, appInfo, 48) { _, key ->
            loadedKeys1.add(key)
        }
        iconLoader.loadIconAsync(context, appInfo, 48) { _, key ->
            loadedKeys2.add(key)
        }

        testScheduler.advanceUntilIdle()

        assertEquals(1, loadedKeys1.size)
        assertEquals(1, loadedKeys2.size)
        assertEquals(appInfo.cacheKey, loadedKeys1[0])
        assertEquals(appInfo.cacheKey, loadedKeys2[0])
    }
}
