package com.silauncer.cepat.cache

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.cache.DiskIconCacheTest
// [Penjelasan]: Pengujian unit untuk memverifikasi penyimpanan, pembacaan, penghapusan per-package (removePackage & hapusPaket), dan pembersihan total pada DiskIconCache.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class DiskIconCacheTest {

    private lateinit var app: Application

    @Before
    fun setup() = kotlinx.coroutines.runBlocking {
        app = ApplicationProvider.getApplicationContext()
        DiskIconCache.clear(app)
    }

    @Test
    fun testPutAndGet() = kotlinx.coroutines.runBlocking {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val key = "com.sample.test/MainActivity_0"

        DiskIconCache.put(app, key, bitmap)
        val retrieved = DiskIconCache.get(app, key)

        assertNotNull(retrieved)
    }

    @Test
    fun testRemovePackageDeletesTargetPackageFilesOnly() = kotlinx.coroutines.runBlocking {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val key1 = "com.pkg.target/MainActivity_0"
        val key2 = "com.pkg.target/DetailsActivity_0"
        val keyOther = "com.pkg.other/MainActivity_0"

        DiskIconCache.put(app, key1, bitmap)
        DiskIconCache.put(app, key2, bitmap)
        DiskIconCache.put(app, keyOther, bitmap)

        assertNotNull(DiskIconCache.get(app, key1))
        assertNotNull(DiskIconCache.get(app, key2))
        assertNotNull(DiskIconCache.get(app, keyOther))

        DiskIconCache.removePackage(app, "com.pkg.target")

        assertNull(DiskIconCache.get(app, key1))
        assertNull(DiskIconCache.get(app, key2))
        assertNotNull(DiskIconCache.get(app, keyOther))
    }

    @Test
    fun testHapusPaketAlias() = kotlinx.coroutines.runBlocking {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val key = "com.test.alias/MainActivity_0"

        DiskIconCache.put(app, key, bitmap)
        assertNotNull(DiskIconCache.get(app, key))

        DiskIconCache.hapusPaket(app, "com.test.alias")
        assertNull(DiskIconCache.get(app, key))
    }

    // [Jalur Class]: com.silauncer.cepat.cache.DiskIconCacheTest
    // [Penjelasan]: Memverifikasi bahwa proses invalidasi pada update/replace paket menghapus icon lama dari L2 Disk Cache dan memungkinkan penulisan bitmap baru secara bersih tanpa stale icon.
    @Test
    fun testPackageUpdateAndReplacedInvalidation() = kotlinx.coroutines.runBlocking {
        val oldBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val newBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val key = "com.updated.app/MainActivity_0"

        DiskIconCache.put(app, key, oldBitmap)
        assertNotNull(DiskIconCache.get(app, key))

        // Invalidate package saat event PACKAGE_CHANGED/PACKAGE_REPLACED
        DiskIconCache.removePackage(app, "com.updated.app")
        assertNull(DiskIconCache.get(app, key))

        // Simpan icon baru setelah update
        DiskIconCache.put(app, key, newBitmap)
        assertNotNull(DiskIconCache.get(app, key))
    }

    // [Jalur Class]: com.silauncer.cepat.cache.DiskIconCacheTest
    // [Penjelasan]: Memverifikasi bahwa pemanggilan removePackage berulang kali (idempotent) aman dan tidak menyebabkan error maupun file corruption.
    @Test
    fun testInvalidatePackageIdempotency() = kotlinx.coroutines.runBlocking {
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val key = "com.idempotent.pkg/MainActivity_0"

        DiskIconCache.put(app, key, bitmap)
        assertNotNull(DiskIconCache.get(app, key))

        // Panggilan pertama
        DiskIconCache.removePackage(app, "com.idempotent.pkg")
        assertNull(DiskIconCache.get(app, key))

        // Panggilan berulang (idempotent)
        DiskIconCache.removePackage(app, "com.idempotent.pkg")
        assertNull(DiskIconCache.get(app, key))
    }
}
