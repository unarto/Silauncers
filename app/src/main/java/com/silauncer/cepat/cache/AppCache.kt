package com.silauncer.cepat.cache

import android.util.LruCache
import com.silauncer.cepat.apps.AppInfo

// [Jalur Class]: com.silauncer.cepat.cache.AppCache
// [Penjelasan]: Memory cache berbatas (bounded LRU Cache) untuk menyimpan objek AppInfo guna mencegah alokasi berulang saat pemindaian aplikasi.
object AppCache {
    private const val MAX_APP_ENTRIES = 500
    private val cache = LruCache<String, AppInfo>(MAX_APP_ENTRIES)

    fun get(key: String): AppInfo? = cache.get(key)

    fun put(key: String, appInfo: AppInfo) {
        cache.put(key, appInfo)
    }

    /**
     * Menghapus cache AppInfo dari package tertentu saat aplikasi di-update atau di-uninstall.
     */
    // [Jalur Class]: com.silauncer.cepat.cache.AppCache
    // [Penjelasan]: Menghapus seluruh entri AppInfo untuk packageName spesifik dengan batasan pemisah presisi (/ atau _) agar tidak salah menghapus paket berawalan serupa.
    fun removePackage(packageName: String) {
        val prefixSlash = "$packageName/"
        val prefixUnderscore = "${packageName}_"
        val keysToRemove = cache.snapshot().keys.filter { 
            it.startsWith(prefixSlash) || it.startsWith(prefixUnderscore) || it == packageName
        }
        for (key in keysToRemove) {
            cache.remove(key)
        }
    }

    fun invalidatePackage(packageName: String) = removePackage(packageName)

    fun clear() {
        cache.evictAll()
    }
}
