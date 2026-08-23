package com.silauncer.cepat.shortcuts

import android.content.Context
import android.os.UserHandle
import com.silauncer.cepat.pm.UserCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ShortcutRepository
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutRepository
 * // [Penjelasan]: Menangani pengambilan ShortcutInfo dari Android LauncherApps (Pinned/Dynamic) ke dalam WorkspaceShortcutInfo.
 */
class ShortcutRepository(private val context: Context) {

    suspend fun getPinnedShortcuts(
        packageName: String,
        shortcutIds: List<String>,
        user: UserHandle
    ): List<WorkspaceShortcutInfo> = withContext(Dispatchers.IO) {
        // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutRepository
        // [Penjelasan]: Mengecek shortcut di cache terlebih dahulu sebelum memanggil API query sistem
        val cachedShortcuts = mutableListOf<WorkspaceShortcutInfo>()
        val missingIds = mutableListOf<String>()
        val userSerial = UserCache.getInstance(context).getSerialNumberForUser(user)

        for (id in shortcutIds) {
            val key = "${packageName}_${id}_${userSerial}"
            val cached = com.silauncer.cepat.cache.ShortcutCache.get(key)
            if (cached != null) {
                cachedShortcuts.add(cached)
            } else {
                missingIds.add(id)
            }
        }

        if (missingIds.isEmpty()) {
            return@withContext cachedShortcuts
        }

        val queryResult = ShortcutRequest(context, user)
            .forPackage(packageName, missingIds)
            .query(ShortcutRequest.PINNED or ShortcutRequest.PUBLISHED)
        
        val newShortcuts = queryResult.map { WorkspaceShortcutInfo.fromShortcutInfo(it) }
        
        for (shortcut in newShortcuts) {
            val key = "${packageName}_${shortcut.shortcutId}_${userSerial}"
            com.silauncer.cepat.cache.ShortcutCache.put(key, shortcut)
        }
        
        cachedShortcuts.addAll(newShortcuts)
        return@withContext cachedShortcuts
    }
}
