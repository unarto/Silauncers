package com.silauncer.cepat.cache

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache

/**
 * IconCache
 *
 * Single Responsibility:
 * Mengelola Level-1 In-Memory LRU Cache untuk icon aplikasi.
 * Menggunakan batas ukuran dinamis berbasis persentase alokasi Heap RAM (12.5% maxMemory),
 * serta mengukur ukuran entri berdasarkan byte alokasi bitmap aktual.
 */
object IconCache {
    private const val HEAP_FRACTION = 8 // 1/8 (~12.5%) dari RAM Heap
    private const val MIN_CACHE_SIZE_KB = 4096 // 4MB
    private const val MAX_CACHE_SIZE_KB = 65536 // 64MB
    private const val FALLBACK_ENTRY_SIZE_KB = 4 // 4KB untuk non-bitmap drawable

    // Hitung alokasi memori heap dalam Kilobytes
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    private val calculatedCacheSizeKb = (maxMemoryKb / HEAP_FRACTION).coerceIn(MIN_CACHE_SIZE_KB, MAX_CACHE_SIZE_KB)

    private val cache = object : LruCache<String, Drawable>(calculatedCacheSizeKb) {
        override fun sizeOf(key: String, value: Drawable): Int {
            return if (value is BitmapDrawable && value.bitmap != null && !value.bitmap.isRecycled) {
                (value.bitmap.allocationByteCount / 1024).coerceAtLeast(1)
            } else {
                FALLBACK_ENTRY_SIZE_KB
            }
        }
    }

    fun get(key: String): Drawable? {
        return cache.get(key)
    }

    fun put(key: String, drawable: Drawable) {
        cache.put(key, drawable)
    }

    /**
     * Menghapus cache icon dari package tertentu dengan prefix matching efisien tanpa GC thrashing.
     */
    fun removePackage(packageName: String) {
        val prefix = "$packageName/"
        val keysToRemove = cache.snapshot().keys.filter { it.startsWith(prefix) || it.startsWith(packageName) }
        for (key in keysToRemove) {
            cache.remove(key)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}
