package com.silauncer.cepat.database

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.database.AppStatsRepositoryTest
// [Penjelasan]: Pengujian unit untuk memvalidasi operasi CRUD statistik peluncuran aplikasi (launch count) pada AppStatsRepository berbasis Room DB.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class AppStatsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: AppStatsRepository

    @Before
    fun setUp() {
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            repository = AppStatsRepository(context)
            repository.clearAllStats()
        }
    }

    @Test
    fun testIncrementAndGetLaunchCount() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.AppStatsRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa pemanggilan incrementLaunchCount menambah nilai secara atomik dan getLaunchCount mengembalikan hitungan yang tepat.
        val pkg = "com.test.app"
        assertEquals(0, repository.getLaunchCount(pkg))

        repository.incrementLaunchCount(pkg)
        assertEquals(1, repository.getLaunchCount(pkg))

        repository.incrementLaunchCount(pkg)
        assertEquals(2, repository.getLaunchCount(pkg))
    }

    @Test
    fun testGetAllStatsMap() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.AppStatsRepositoryTest
        // [Penjelasan]: Memverifikasi bahwa getAllStatsMap mengembalikan mapping lengkap seluruh paket aplikasi dan frekuensinya.
        repository.incrementLaunchCount("com.app.one")
        repository.incrementLaunchCount("com.app.one")
        repository.incrementLaunchCount("com.app.two")

        val map = repository.getAllStatsMap()
        assertEquals(2, map.size)
        assertEquals(2, map["com.app.one"])
        assertEquals(1, map["com.app.two"])
    }

    @Test
    fun testClearAllStats() = runBlocking {
        // [Jalur Class]: com.silauncer.cepat.database.AppStatsRepositoryTest
        // [Penjelasan]: Memverifikasi pembersihan seluruh data statistik aplikasi.
        repository.incrementLaunchCount("com.app.one")
        assertTrue(repository.getAllStatsMap().isNotEmpty())

        repository.clearAllStats()
        assertTrue(repository.getAllStatsMap().isEmpty())
    }
}
