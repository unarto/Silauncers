package com.silauncer.cepat.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * DiskIconCache
 *
 * Single Responsibility:
 * Mengelola persistensi dan pembacaan icon bitmap di disk cache lokal aplikasi (Level-2 Cache).
 */
object DiskIconCache {
    private const val CACHE_DIR_NAME = "icons_cache"
    private const val COMPRESS_QUALITY = 100
    
    // [Jalur Class]: com.silauncer.cepat.cache.DiskIconCache
    // [Penjelasan]: Menggunakan struktur Mutex per-package untuk menyinkronkan operasi read/write secara asinkron tanpa memblokir thread IO.
    private val packageMutexes = ConcurrentHashMap<String, Mutex>()
    
    private fun getMutex(packageName: String): Mutex {
        return packageMutexes.getOrPut(packageName) { Mutex() }
    }

    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    internal fun extractPackageName(key: String): String {
        return when {
            key.contains('/') -> key.substringBefore('/')
            key.contains('_') -> key.substringBefore('_')
            else -> key
        }
    }

    internal fun sanitizePackageName(packageName: String): String {
        return packageName.replace(Regex("[^a-zA-Z0-9._]"), "_")
    }

    private fun getCacheFile(context: Context, key: String): File {
        val pkg = extractPackageName(key)
        val safePkg = sanitizePackageName(pkg)
        val hash = hashKey(key)
        return File(getCacheDir(context), "${safePkg}_$hash.png")
    }

    // [Jalur Class]: com.silauncer.cepat.cache.DiskIconCache
    // [Penjelasan]: Mengambil bitmap drawable dari disk cache dengan pengamanan Mutex untuk menghindari race condition saat operasi penulisan file sedang berjalan.
    suspend fun get(context: Context, key: String): Drawable? {
        val pkg = extractPackageName(key)
        return getMutex(pkg).withLock {
            val file = getCacheFile(context, key)
            val targetFile = if (file.exists() && file.length() > 0L) {
                file
            } else {
                // Fallback backward compatibility untuk file legacy tanpa prefix
                val legacyFile = File(getCacheDir(context), "${hashKey(key)}.png")
                if (legacyFile.exists() && legacyFile.length() > 0L) legacyFile else return@withLock null
            }
    
            try {
                val bitmap = BitmapFactory.decodeFile(targetFile.absolutePath) ?: return@withLock null
                BitmapDrawable(context.resources, bitmap)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun put(context: Context, key: String, bitmap: Bitmap) {
        val pkg = extractPackageName(key)
        getMutex(pkg).withLock {
            try {
                val file = getCacheFile(context, key)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESS_QUALITY, out)
                }
            } catch (e: Exception) {
                // Ignore disk write errors
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.cache.DiskIconCache
    // [Penjelasan]: Menghapus seluruh file disk cache ikon bitmap yang berasosiasi dengan package name tertentu dengan pengamanan Mutex.
    suspend fun removePackage(context: Context, packageName: String) {
        getMutex(packageName).withLock {
            try {
                val dir = getCacheDir(context)
                if (dir.exists()) {
                    val safePkg = sanitizePackageName(packageName)
                    val targetPrefix = "${safePkg}_"
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith(targetPrefix)) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        packageMutexes.remove(packageName)
    }

    // [Jalur Class]: com.silauncer.cepat.cache.DiskIconCache
    // [Penjelasan]: Alias metode hapusPaket sesuai konvensi lokal untuk menghapus seluruh disk cache ikon paket tertentu secara nyata.
    suspend fun hapusPaket(context: Context, packageName: String) = removePackage(context, packageName)

    suspend fun clear(context: Context) {
        // [Penjelasan]: Clear tidak mengunci semua mutex satu persatu secara eksplisit karena ini destruktif secara global.
        try {
            getCacheDir(context).deleteRecursively()
            packageMutexes.clear()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun hashKey(key: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(key.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            key.hashCode().toString()
        }
    }
}
