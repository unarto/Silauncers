package com.silauncer.cepat.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// [Jalur Class]: com.silauncer.cepat.database.entity.AppStatsEntity
// [Penjelasan]: Entitas Room Database untuk mencatat statistik penggunaan aplikasi (frekuensi peluncuran/launch count) secara terstruktur menggantikan dynamic key MMKV.
@Entity(tableName = "app_stats")
data class AppStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "launch_count")
    val launchCount: Int = 0
)
