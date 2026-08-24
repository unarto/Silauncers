package com.silauncer.cepat.database.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// [Jalur Class]: com.silauncer.cepat.database.entity.AppStatsDao
// [Penjelasan]: Data Access Object untuk operasi pembacaan, perolehan seluruh data, dan penambahan launch count aplikasi pada database Room.
@Dao
interface AppStatsDao {
    @Query("SELECT launch_count FROM app_stats WHERE package_name = :packageName")
    suspend fun getLaunchCount(packageName: String): Int?

    @Query("SELECT * FROM app_stats")
    suspend fun getAllStats(): List<AppStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: AppStatsEntity)

    @Query("UPDATE app_stats SET launch_count = launch_count + 1 WHERE package_name = :packageName")
    suspend fun incrementLaunchCount(packageName: String): Int

    @Query("DELETE FROM app_stats")
    suspend fun clearAllStats()
}
