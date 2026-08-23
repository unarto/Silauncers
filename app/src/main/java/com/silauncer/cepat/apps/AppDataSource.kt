package com.silauncer.cepat.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// [Jalur Class]: com.silauncer.cepat.apps.AppDataSource
// [Penjelasan]: Membuat kelas dan metode open agar dapat di-stub/di-subclass di lingkungan pengujian unit secara efisien tanpa memerlukan shadow system service yang kompleks.
open class AppDataSource(private val context: Context) {
    private val launcherApps: LauncherApps? by lazy {
        try {
            context.getSystemService(LauncherApps::class.java)
        } catch (e: Exception) {
            null
        }
    }

    open suspend fun getInstalledApps(
        packageName: String? = null,
        user: UserHandle = Process.myUserHandle()
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val appsService = launcherApps ?: return@withContext emptyList()
            val activities = appsService.getActivityList(packageName, user) ?: return@withContext emptyList()
            activities.map { activity ->
                val component = activity.componentName
                // [Jalur Class]: com.silauncer.cepat.apps.AppDataSource
                // [Penjelasan]: Menggunakan factory method AppInfo.createCacheKey yang seragam dengan AppInfo.cacheKey untuk mencegah inkonsistensi key, cache miss, dan kegagalan invalidasi paket.
                val cacheKey = AppInfo.createCacheKey(component, user)
                
                // [Jalur Class]: com.silauncer.cepat.apps.AppDataSource
                // [Penjelasan]: Mengambil AppInfo dari AppCache untuk menghemat alokasi memori. Jika belum ada, buat baru dan masukkan ke cache.
                var cachedInfo = com.silauncer.cepat.cache.AppCache.get(cacheKey)
                if (cachedInfo == null) {
                    cachedInfo = AppInfo(
                        name = activity.label?.toString() ?: component.packageName,
                        componentName = component,
                        packageName = component.packageName,
                        user = user
                    )
                    com.silauncer.cepat.cache.AppCache.put(cacheKey, cachedInfo)
                }
                cachedInfo
            }.distinctBy { it.componentName }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
