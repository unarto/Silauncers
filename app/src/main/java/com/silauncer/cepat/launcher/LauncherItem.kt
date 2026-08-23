package com.silauncer.cepat.launcher

import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo

/**
 * LauncherItem
 *
 * Single Responsibility:
 * Model representasi item tingkat atas di Workspace Grid (bisa berupa AppInfo individual, FolderInfo, atau Shortcut).
 */
sealed class LauncherItem {
    abstract val id: String
    abstract val title: String

    data class App(val appInfo: AppInfo) : LauncherItem() {
        override val id: String get() = appInfo.cacheKey
        override val title: String get() = appInfo.name
    }

    data class Folder(val folderInfo: FolderInfo) : LauncherItem() {
        override val id: String get() = folderInfo.id
        override val title: String get() = folderInfo.title
    }

    data class Shortcut(val shortcutInfo: WorkspaceShortcutInfo) : LauncherItem() {
        override val id: String get() = shortcutInfo.cacheKey
        override val title: String get() = shortcutInfo.title.toString()
    }
}
