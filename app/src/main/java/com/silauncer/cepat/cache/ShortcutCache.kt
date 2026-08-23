package com.silauncer.cepat.cache

import android.util.LruCache
import com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo

// [Jalur Class]: com.silauncer.cepat.cache.ShortcutCache
// [Penjelasan]: Memory cache berbatas (bounded LRU Cache) untuk menyimpan metadata shortcut dan deep shortcut guna menghindari IPC/query berulang ke LauncherApps system service.
object ShortcutCache {
    private const val MAX_SHORTCUTS = 100
    private val cache = LruCache<String, WorkspaceShortcutInfo>(MAX_SHORTCUTS)

    fun get(key: String): WorkspaceShortcutInfo? = cache.get(key)

    fun put(key: String, shortcut: WorkspaceShortcutInfo) {
        cache.put(key, shortcut)
    }

    /**
     * Menghapus cache shortcut dari package tertentu saat shortcut di-update atau aplikasi di-uninstall.
     */
    fun removePackage(packageName: String) {
        val keysToRemove = cache.snapshot().keys.filter { it.startsWith(packageName) }
        for (key in keysToRemove) {
            cache.remove(key)
        }
    }

    fun invalidatePackage(packageName: String) = removePackage(packageName)

    fun clear() {
        cache.evictAll()
    }
}
