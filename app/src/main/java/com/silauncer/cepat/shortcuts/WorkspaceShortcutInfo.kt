package com.silauncer.cepat.shortcuts

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import com.silauncer.cepat.apps.AppInfo

/**
 * WorkspaceShortcutInfo
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo
 * // [Penjelasan]: Model representasi pintasan (shortcut) di workspace, terpisah dari AppInfo. Menyimpan referensi ShortcutInfo dari OS.
 */
data class WorkspaceShortcutInfo(
    val shortcutId: String,
    val packageName: String,
    val user: UserHandle,
    val title: CharSequence,
    val shortcutInfo: ShortcutInfo?
) {
    val isEnabled: Boolean
        get() = shortcutInfo?.isEnabled ?: true

    val disabledMessage: CharSequence?
        get() = shortcutInfo?.disabledMessage

    val intent: Intent?
        get() = shortcutInfo?.intent

    val cacheKey: String
        get() = "${packageName}_${shortcutId}_${user.hashCode()}"
        
    companion object {
        fun fromShortcutInfo(shortcutInfo: ShortcutInfo): WorkspaceShortcutInfo {
            return WorkspaceShortcutInfo(
                shortcutId = shortcutInfo.id,
                packageName = shortcutInfo.getPackage() ?: "",
                user = shortcutInfo.userHandle,
                title = shortcutInfo.shortLabel ?: shortcutInfo.longLabel ?: "",
                shortcutInfo = shortcutInfo
            )
        }
    }
}
