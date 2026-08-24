package com.silauncer.cepat.database

import android.content.Context
import com.silauncer.cepat.database.entity.AppStatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// [Jalur Class]: com.silauncer.cepat.database.AppStatsRepository
// [Penjelasan]: Repositori Room untuk mengelola statistik penggunaan aplikasi (launch count) secara terisolasi tanpa mencemari konfigurasi preferensi MMKV.
class AppStatsRepository(context: Context) {
    private val dao = LauncherDatabase.getDatabase(context).appStatsDao()

    suspend fun getLaunchCount(packageName: String): Int = withContext(Dispatchers.IO) {
        dao.getLaunchCount(packageName) ?: 0
    }

    suspend fun getAllStatsMap(): Map<String, Int> = withContext(Dispatchers.IO) {
        dao.getAllStats().associate { it.packageName to it.launchCount }
    }

    suspend fun incrementLaunchCount(packageName: String) = withContext(Dispatchers.IO) {
        val updatedRows = dao.incrementLaunchCount(packageName)
        if (updatedRows == 0) {
            dao.insertOrUpdate(AppStatsEntity(packageName = packageName, launchCount = 1))
        }
    }

    suspend fun clearAllStats() = withContext(Dispatchers.IO) {
        dao.clearAllStats()
    }
}
