package com.silauncer.cepat.database.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

// [Jalur Class]: com.silauncer.cepat.database.entity.WorkspaceItemDao
// [Penjelasan]: Data Access Object untuk operasi CRUD pada workspace item dan keanggotaan folder. Memastikan abstraksi akses database yang bersih (SRP).

@Dao
interface WorkspaceItemDao {
    @Query("SELECT * FROM workspace_items ORDER BY rank ASC")
    fun getAllItems(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items ORDER BY rank ASC")
    suspend fun getAllItemsSync(): List<WorkspaceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<WorkspaceItemEntity>)

    @Update
    suspend fun updateItem(item: WorkspaceItemEntity)

    @Update
    suspend fun updateItems(items: List<WorkspaceItemEntity>)

    @Delete
    suspend fun deleteItem(item: WorkspaceItemEntity)

    @Query("DELETE FROM workspace_items")
    suspend fun clearAll()

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workspace_items WHERE container_uid = :folderUid")
    suspend fun deleteByContainer(folderUid: String)
}
