package com.silauncer.cepat.cache

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.silauncer.cepat.apps.AppInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * IconLoader
 *
 * Single Responsibility:
 * Mengkoordinasikan pemuatan icon aplikasi secara asinkron dengan strategi 2-Level Cache
 * (L1 Memory Cache -> L2 Disk Cache -> System PackageManager IPC),
 * membatasi antrean I/O dengan Dispatchers.IO.limitedParallelism(4) untuk stabilitas 60/120 FPS,
 * dan melakukan pre-scaling bitmap ke targetIconSizePx sebelum disimpan ke cache.
 */
// [Jalur Class]: com.silauncer.cepat.cache.IconLoader
// [Penjelasan]: Mendukung konstruktor dengan CoroutineDispatcher (ioDispatcher dan mainDispatcher) yang dapat diinjeksikan secara fleksibel dengan default Dispatchers.IO.limitedParallelism dan Dispatchers.Main.immediate untuk produksi serta TestDispatcher untuk unit testing.
class IconLoader(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(IO_PARALLELISM),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) {
    private var defaultIcon: Drawable? = null
    
    // In-Flight request deduplication
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<Drawable>>()

    private fun getDefaultIcon(context: Context): Drawable {
        if (defaultIcon == null) {
            defaultIcon = context.packageManager.defaultActivityIcon
        }
        return defaultIcon!!
    }

    /**
     * Memuat icon secara asinkron dengan strategi 2-level cache:
     * 1. L1 Memory Cache (0ms instant)
     * 2. L2 Disk Cache (~2-5ms)
     * 3. System PackageManager IPC call (Fallback paling lambat)
     */
    fun loadIconAsync(
        context: Context,
        appInfo: AppInfo,
        targetIconSizePx: Int = 0,
        onLoaded: (Drawable, String) -> Unit
    ) {
        val cacheKey = appInfo.cacheKey
        
        // 1. Level 1: Cek Memory Cache (0ms)
        val memCached = IconCache.get(cacheKey)
        if (memCached != null) {
            onLoaded(memCached, cacheKey)
            return
        }

        // [Jalur Class]: com.silauncer.cepat.cache.IconLoader
        // [Penjelasan]: Menghilangkan pemanggilan prematur getDefaultIcon sebelum L2 Disk Cache dicek, mencegah kedipan (flickering/placeholder flash) ke ikon default hijau saat ikon sebenarnya sudah tersedia di disk cache.

        val appContext = context.applicationContext

        scope.launch {
            val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
                scope.async(ioDispatcher) {
                    // 2. Level 2: Cek Disk Cache
                    val diskCached = DiskIconCache.get(appContext, cacheKey)
                    if (diskCached != null) {
                        IconCache.put(cacheKey, diskCached)
                        return@async diskCached
                    }

                    // 3. Level 3: Ambil dari Icon Pack atau Fallback IPC Call ke PackageManager
                    // [Jalur Class]: com.silauncer.cepat.cache.IconLoader
                    // [Penjelasan]: Memeriksa ketersediaan ikon dari Icon Pack aktif via IconPackManager menggunakan singleton LauncherPreferences sebelum fallback ke System AOSP PackageManager.
                    val pm = appContext.packageManager
                    val activeIconPack = com.silauncer.cepat.storage.LauncherPreferences.getInstance().iconPack
                    val packIcon = if (activeIconPack != com.silauncer.cepat.icons.IconPackManager.SYSTEM_AOSP) {
                        com.silauncer.cepat.icons.IconPackManager.getInstance().getIcon(appContext, appInfo.componentName, activeIconPack)
                    } else {
                        null
                    }

                    val rawIcon = packIcon ?: try {
                        pm.getActivityIcon(appInfo.componentName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        pm.defaultActivityIcon
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        pm.defaultActivityIcon
                    }

                    // 4. Pre-scaling / Normalisasi Bitmap
                    val scaledBitmap = normalizeToBitmap(rawIcon, targetIconSizePx)
                    val resultDrawable = BitmapDrawable(appContext.resources, scaledBitmap)

                    // Simpan ke L1 (Memory) & L2 (Disk)
                    IconCache.put(cacheKey, resultDrawable)
                    DiskIconCache.put(appContext, cacheKey, scaledBitmap)

                    resultDrawable
                }
            }

            try {
                val icon = deferred.await()
                withContext(mainDispatcher) {
                    onLoaded(icon, cacheKey)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(mainDispatcher) {
                    onLoaded(getDefaultIcon(context), cacheKey)
                }
            } finally {
                inFlightRequests.remove(cacheKey, deferred)
            }
        }
    }

    fun loadShortcutIconAsync(
        context: Context,
        shortcut: com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo,
        targetIconSizePx: Int = 0,
        onLoaded: (Drawable, String) -> Unit
    ) {
        val cacheKey = shortcut.cacheKey
        
        // 1. Level 1: Cek Memory Cache (0ms)
        val memCached = IconCache.get(cacheKey)
        if (memCached != null) {
            onLoaded(memCached, cacheKey)
            return
        }

        // [Jalur Class]: com.silauncer.cepat.cache.IconLoader
        // [Penjelasan]: Menghilangkan kedipan placeholder default icon pada shortcut dengan langsung memeriksa L2 Disk Cache dan hanya memberikan default fallback jika gagal.

        val appContext = context.applicationContext
        scope.launch {
            val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
                scope.async(ioDispatcher) {
                    // 2. Level 2: Cek Disk Cache
                    val diskCached = DiskIconCache.get(appContext, cacheKey)
                    if (diskCached != null) {
                        IconCache.put(cacheKey, diskCached)
                        return@async diskCached
                    }

                    // 3. Level 3: IPC Call ke LauncherApps
                    val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
                    val rawIcon = try {
                        val shortcutInfo = shortcut.shortcutInfo
                        if (shortcutInfo != null) {
                            launcherApps.getShortcutIconDrawable(shortcutInfo, appContext.resources.displayMetrics.densityDpi) ?: getDefaultIcon(context)
                        } else {
                            getDefaultIcon(context)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        getDefaultIcon(context)
                    }

                    // 4. Pre-scaling / Normalisasi Bitmap
                    val scaledBitmap = normalizeToBitmap(rawIcon, targetIconSizePx)
                    val resultDrawable = BitmapDrawable(appContext.resources, scaledBitmap)

                    // Simpan ke L1 (Memory) & L2 (Disk)
                    IconCache.put(cacheKey, resultDrawable)
                    DiskIconCache.put(appContext, cacheKey, scaledBitmap)
                    
                    resultDrawable
                }
            }

            try {
                val icon = deferred.await()
                withContext(mainDispatcher) {
                    onLoaded(icon, cacheKey)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(mainDispatcher) {
                    onLoaded(getDefaultIcon(context), cacheKey)
                }
            } finally {
                inFlightRequests.remove(cacheKey, deferred)
            }
        }
    }

    /**
     * Mengubah dan melakukan pre-scale drawable menjadi Bitmap dengan ukuran presisi targetIconSizePx.
     */
    private fun normalizeToBitmap(drawable: Drawable, targetSizePx: Int): Bitmap {
        val width = if (targetSizePx > 0) targetSizePx else drawable.intrinsicWidth.coerceAtLeast(MIN_DEFAULT_ICON_PX)
        val height = if (targetSizePx > 0) targetSizePx else drawable.intrinsicHeight.coerceAtLeast(MIN_DEFAULT_ICON_PX)

        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val srcBmp = drawable.bitmap
            if (srcBmp.width == width && srcBmp.height == height) {
                return srcBmp
            }
            return Bitmap.createScaledBitmap(srcBmp, width, height, true)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private const val IO_PARALLELISM = 4
        private const val MIN_DEFAULT_ICON_PX = 48
    }
}
