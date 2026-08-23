package com.silauncer.cepat.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.silauncer.cepat.database.entity.WorkspaceItemDao
import com.silauncer.cepat.database.entity.WorkspaceItemEntity

// [Jalur Class]: com.silauncer.cepat.database.LauncherDatabase
// [Penjelasan]: Setup utama Room Database untuk menyimpan struktur workspace dan folder Silauncer, diinisialisasi sebagai singleton.

@Database(entities = [WorkspaceItemEntity::class], version = 2, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun workspaceItemDao(): WorkspaceItemDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "silauncer_database"
                )
                // [Jalur Class]: com.silauncer.cepat.database.LauncherDatabase
                // [Penjelasan]: Menggunakan overload fallbackToDestructiveMigration(true) yang modern dan tidak deprecated untuk menghapus tabel saat versi skema naik tanpa migrasi manual.
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
