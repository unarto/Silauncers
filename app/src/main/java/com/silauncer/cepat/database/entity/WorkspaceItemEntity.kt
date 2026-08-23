package com.silauncer.cepat.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

// [Jalur Class]: com.silauncer.cepat.database.entity.WorkspaceItemEntity
// [Penjelasan]: Entitas Room yang menyimpan persistensi terstruktur untuk item di Workspace (App maupun Folder) serta posisinya dan relasinya.

@Entity(tableName = "workspace_items")
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    @ColumnInfo(name = "item_type")
    val itemType: Int, // 0 = App, 1 = Folder

    @ColumnInfo(name = "item_uid")
    val itemUid: String? = null, // UUID string untuk identifikasi Folder (hanya untuk itemType = 1)

    @ColumnInfo(name = "container_uid")
    val containerUid: String? = null, // null jika berada di workspace, atau Folder UUID jika di dalam folder
    
    @ColumnInfo(name = "rank")
    val rank: Int, // Order position

    @ColumnInfo(name = "title")
    val title: String? = null, // Title for Folder

    @ColumnInfo(name = "component_name")
    val componentName: String? = null, // Flattened component name for App

    @ColumnInfo(name = "user_serial")
    val userSerial: Long = 0L, // User serial number untuk App
    
    @ColumnInfo(name = "shortcut_id")
    val shortcutId: String? = null // ID shortcut (hanya untuk itemType = 2)
) {
    companion object {
        const val ITEM_TYPE_APP = 0
        const val ITEM_TYPE_FOLDER = 1
        const val ITEM_TYPE_SHORTCUT = 2
    }
}
