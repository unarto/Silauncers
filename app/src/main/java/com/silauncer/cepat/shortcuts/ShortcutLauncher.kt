package com.silauncer.cepat.shortcuts

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.util.Log
import android.view.View

/**
 * ShortcutLauncher
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutLauncher
 * // [Penjelasan]: Menangani eksekusi peluncuran shortcut aplikasi secara aman melalui LauncherApps dengan menyertakan area animasi sumber (source bounds).
 */
class ShortcutLauncher(private val context: Context) {

    /**
     * Meluncurkan shortcut dengan [ShortcutInfo] dan referensi [sourceView] untuk animasi bounds.
     */
    fun startShortcut(shortcutInfo: ShortcutInfo, sourceView: View? = null): Boolean {
        val bounds = Rect()
        sourceView?.getGlobalVisibleRect(bounds)
        return startShortcut(
            packageName = shortcutInfo.`package`,
            shortcutId = shortcutInfo.id,
            sourceBounds = if (bounds.isEmpty) null else bounds,
            startActivityOptions = null,
            user = shortcutInfo.userHandle
        )
    }

    /**
     * Meluncurkan shortcut dengan parameter lengkap.
     */
    fun startShortcut(
        packageName: String,
        shortcutId: String,
        sourceBounds: Rect?,
        startActivityOptions: Bundle?,
        user: UserHandle
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }

        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                ?: return false

            val options = startActivityOptions ?: ActivityOptions.makeBasic().toBundle()
            launcherApps.startShortcut(packageName, shortcutId, sourceBounds, options, user)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Activity tidak ditemukan untuk shortcut $shortcutId di $packageName", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Tidak memiliki izin meluncurkan shortcut $shortcutId", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "LauncherApps dalam status tidak valid saat meluncurkan shortcut $shortcutId", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Gagal meluncurkan shortcut $shortcutId", e)
            false
        }
    }

    companion object {
        private const val TAG = "ShortcutLauncher"
    }
}
