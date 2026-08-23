package com.silauncer.cepat.cache

import android.util.LruCache
import com.silauncer.cepat.folder.FolderInfo

// [Jalur Class]: com.silauncer.cepat.cache.FolderCache
// [Penjelasan]: Memory cache berbatas (bounded LRU Cache) untuk menyimpan instance FolderInfo aktif berdasarkan Folder ID guna mencegah re-instansiasi.
object FolderCache {
    private const val MAX_FOLDERS = 50
    private val cache = LruCache<String, FolderInfo>(MAX_FOLDERS)

    fun get(folderId: String): FolderInfo? = cache.get(folderId)

    fun put(folderId: String, folderInfo: FolderInfo) {
        cache.put(folderId, folderInfo)
    }

    fun remove(folderId: String) {
        cache.remove(folderId)
    }

    fun clear() {
        cache.evictAll()
    }
}
