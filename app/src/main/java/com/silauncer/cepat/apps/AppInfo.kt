package com.silauncer.cepat.apps

import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.os.UserHandle

import com.silauncer.cepat.dot.DotInfo

/**
 * Minimal app model untuk Silauncer.
 *
 * Hanya menyimpan identitas aplikasi dan informasi untuk launch.
 * Icon dikelola terpisah oleh IconCache dan DiskIconCache.
 */
data class AppInfo(
    val name: String,
    val componentName: ComponentName,
    val packageName: String,
    val user: UserHandle = Process.myUserHandle(),
    val hasNotification: Boolean = false,
    // [Jalur Class]: com.silauncer.cepat.apps.AppInfo
    // [Penjelasan]: Menyimpan data detail notification dot (DotInfo) untuk integrasi Launcher3 dot system
    val dotInfo: DotInfo? = null
) {

    // [Jalur Class]: com.silauncer.cepat.apps.AppInfo
    // [Penjelasan]: Memeriksa apakah aplikasi memiliki dot notifikasi aktif baik via boolean flag maupun DotInfo
    val isDotted: Boolean
        get() = hasNotification || (dotInfo?.hasDot() == true)

    fun launchIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = componentName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
    }

    companion object {
        // [Jalur Class]: com.silauncer.cepat.apps.AppInfo
        // [Penjelasan]: Menyediakan metode terpusat pembuatan cache key agar format identitas komponen konsisten di seluruh lapisan sistem (AppDataSource, IconLoader, AppCache, DiskIconCache).
        fun createCacheKey(packageName: String, className: String, user: UserHandle): String {
            val context = try {
                com.silauncer.cepat.launcher.LauncherApplication.appContext
            } catch (e: Throwable) {
                null
            }
            val serial = if (context != null) {
                try {
                    com.silauncer.cepat.pm.UserCache.getInstance(context).getSerialNumberForUser(user)
                } catch (e: Throwable) {
                    user.hashCode().toLong()
                }
            } else {
                user.hashCode().toLong()
            }
            return "$packageName/${className}_$serial"
        }

        // [Jalur Class]: com.silauncer.cepat.apps.AppInfo
        // [Penjelasan]: Overload helper pembuatan cache key berbasis ComponentName dan UserHandle.
        fun createCacheKey(componentName: ComponentName, user: UserHandle): String {
            return createCacheKey(componentName.packageName, componentName.className, user)
        }
    }

    /**
     * Kunci unik untuk cache aplikasi/icon.
     * Format: packageName/className_serial agar memudahkan prefix matching.
     */
    // [Jalur Class]: com.silauncer.cepat.apps.AppInfo
    // [Penjelasan]: Menggunakan metode terpusat createCacheKey agar identitas model konsisten dengan sumber data dan loader.
    val cacheKey: String
        get() = createCacheKey(packageName, componentName.className, user)
}
