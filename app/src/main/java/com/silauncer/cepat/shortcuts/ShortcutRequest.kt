package com.silauncer.cepat.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.UserHandle
import android.util.Log

/**
 * ShortcutRequest
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutRequest
 * // [Penjelasan]: Utilitas fluent query builder untuk melakukan query shortcut aplikasi melalui LauncherApps secara terstruktur dan aman (adaptasi dari AOSP Launcher3 ShortcutRequest).
 */
class ShortcutRequest(
    private val context: Context,
    private val userHandle: UserHandle
) {

    private var packageName: String? = null
    private var shortcutIds: List<String>? = null
    private var activity: ComponentName? = null
    private var failed: Boolean = false

    /**
     * Membatasi query shortcut untuk package tertentu.
     */
    fun forPackage(packageName: String): ShortcutRequest {
        return forPackage(packageName, null as List<String>?)
    }

    /**
     * Membatasi query shortcut untuk package dan ID shortcut tertentu.
     */
    fun forPackage(packageName: String, vararg shortcutIds: String): ShortcutRequest {
        return forPackage(packageName, shortcutIds.toList())
    }

    /**
     * Membatasi query shortcut untuk package dan daftar ID shortcut.
     */
    fun forPackage(packageName: String, shortcutIds: List<String>?): ShortcutRequest {
        this.packageName = packageName
        this.shortcutIds = shortcutIds
        return this
    }

    /**
     * Membatasi query berdasarkan target Activity component.
     */
    fun withContainer(activity: ComponentName?): ShortcutRequest {
        if (activity == null) {
            failed = true
        } else {
            this.activity = activity
        }
        return this
    }

    /**
     * Mengeksekusi query dengan flags yang ditentukan.
     */
    fun query(flags: Int): QueryResult {
        if (failed || Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return QueryResult.DEFAULT
        }

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                ?: return QueryResult.DEFAULT

            if (!launcherApps.hasShortcutHostPermission()) {
                return QueryResult.DEFAULT
            }

            val query = LauncherApps.ShortcutQuery()
            query.setQueryFlags(flags)
            packageName?.let { query.setPackage(it) }
            shortcutIds?.let { query.setShortcutIds(it) }
            activity?.let { query.setActivity(it) }

            val shortcuts = launcherApps.getShortcuts(query, userHandle)
            return QueryResult(shortcuts ?: emptyList(), true)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException saat query shortcuts: ${e.message}")
            return QueryResult.DEFAULT
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException saat query shortcuts: ${e.message}")
            return QueryResult.DEFAULT
        } catch (e: Exception) {
            Log.e(TAG, "Exception tidak terduga saat query shortcuts: ${e.message}")
            return QueryResult.DEFAULT
        }
    }

    /**
     * QueryResult
     *
     * // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutRequest.QueryResult
     * // [Penjelasan]: Pembungkus daftar ShortcutInfo hasil query beserta indikator status keberhasilan (wasSuccess).
     */
    class QueryResult(
        private val list: List<ShortcutInfo>,
        val wasSuccess: Boolean
    ) : List<ShortcutInfo> by list {

        companion object {
            val DEFAULT = QueryResult(emptyList(), false)
        }
    }

    companion object {
        private const val TAG = "ShortcutRequest"

        val ALL: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        } else {
            0
        }

        val PUBLISHED: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
        } else {
            0
        }

        val PINNED: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        } else {
            0
        }
    }
}
