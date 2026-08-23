package com.silauncer.cepat.launcher

import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.folder.FolderInfo

/**
 * FolderManager
 *
 * [Jalur Class]: com.silauncer.cepat.launcher.FolderManager
 * [Penjelasan]: Mengekstraksi logika bisnis pembentukan, pembubaran (dissolve), dan modifikasi item
 * dari AppAdapter. Hal ini memastikan AppAdapter murni bertugas mem-binding view (SRP).
 */
class FolderManager {

    /**
     * Membuat folder baru atau menambahkan app ke dalam folder yang sudah ada.
     * Mengembalikan list baru jika berhasil, null jika gagal.
     */
    fun createFolder(items: List<LauncherItem>, targetPosition: Int, sourcePosition: Int, folderTitle: String = ""): List<LauncherItem>? {
        if (targetPosition < 0 || sourcePosition < 0 ||
            targetPosition >= items.size || sourcePosition >= items.size ||
            targetPosition == sourcePosition
        ) {
            return null
        }

        val mutableItems = items.toMutableList()
        val targetItem = mutableItems[targetPosition]
        val sourceItem = mutableItems[sourcePosition]

        // We can create a folder from App + App, App + Shortcut, Shortcut + App, Shortcut + Shortcut.
        // We will pass them generically to FolderInfo
        val targetApp = (targetItem as? LauncherItem.App)?.appInfo
        val targetShortcut = (targetItem as? LauncherItem.Shortcut)?.shortcutInfo
        
        val sourceApp = (sourceItem as? LauncherItem.App)?.appInfo
        val sourceShortcut = (sourceItem as? LauncherItem.Shortcut)?.shortcutInfo
        
        val isTargetFolder = targetItem is LauncherItem.Folder

        if (!isTargetFolder && (targetApp != null || targetShortcut != null) && (sourceApp != null || sourceShortcut != null)) {
            // [Jalur Class]: com.silauncer.cepat.launcher.FolderManager
            // [Penjelasan]: Saat melahirkan folder baru, gabungkan App atau Shortcut.
            val finalTitle = if (folderTitle.isEmpty()) {
                val context = com.silauncer.cepat.launcher.LauncherApplication.appContext
                // Just pass targetApp and sourceApp if they exist to NameProvider. Shortcuts won't have standard package names but NameProvider will fallback
                // [Jalur Class]: com.silauncer.cepat.launcher.FolderManager
                // [Penjelasan]: Menggunakan nama representatif (itemsToGroup) untuk menghindari penamaan dummy dan mock code
                val itemsToGroup = listOfNotNull(targetApp, sourceApp)
                if (itemsToGroup.isNotEmpty()) {
                    com.silauncer.cepat.folder.FolderNameProvider(context).getSuggestedFolderName(itemsToGroup)
                } else {
                    context.getString(com.silauncer.cepat.R.string.folder_unnamed)
                }
            } else {
                folderTitle
            }

            val folderInfo = FolderInfo(
                initialTitle = finalTitle,
                initialContents = listOfNotNull(targetApp, sourceApp),
                initialShortcuts = listOfNotNull(targetShortcut, sourceShortcut)
            )
            // [Jalur Class]: com.silauncer.cepat.launcher.FolderManager
            // [Penjelasan]: Menyimpan FolderInfo yang baru dibuat ke FolderCache
            com.silauncer.cepat.cache.FolderCache.put(folderInfo.id, folderInfo)

            // Ganti targetItem dengan Folder
            mutableItems[targetPosition] = LauncherItem.Folder(folderInfo)
            // Hapus sourceItem
            mutableItems.removeAt(sourcePosition)
            return mutableItems
        } else if (isTargetFolder && (sourceApp != null || sourceShortcut != null)) {
            val folderInfo = (targetItem as LauncherItem.Folder).folderInfo
            if (sourceApp != null) folderInfo.add(sourceApp)
            if (sourceShortcut != null) folderInfo.addShortcut(sourceShortcut)
            // [Jalur Class]: com.silauncer.cepat.launcher.FolderManager
            // [Penjelasan]: Memperbarui state FolderInfo di FolderCache saat ada penambahan konten
            com.silauncer.cepat.cache.FolderCache.put(folderInfo.id, folderInfo)
            mutableItems.removeAt(sourcePosition)
            return mutableItems
        }

        return null
    }

    /**
     * Menghapus item dari folder dan meletakkannya di Workspace grid.
     */
    fun removeAppFromFolder(
        items: List<LauncherItem>,
        folderInfo: FolderInfo,
        itemToRemove: LauncherItem,
        targetWorkspacePosition: Int
    ): List<LauncherItem>? {
        val removed = when(itemToRemove) {
            is LauncherItem.App -> folderInfo.remove(itemToRemove.appInfo)
            is LauncherItem.Shortcut -> folderInfo.removeShortcut(itemToRemove.shortcutInfo)
            else -> false
        }
        
        if (!removed) return null

        val mutableItems = items.toMutableList()
        val insertPos = targetWorkspacePosition.coerceIn(0, mutableItems.size)
        
        mutableItems.add(insertPos, itemToRemove)

        // Periksa apakah folder perlu di-dissolve otomatis
        if (folderInfo.shouldAutoDissolve()) {
            // [Jalur Class]: com.silauncer.cepat.launcher.FolderManager
            // [Penjelasan]: Menghapus folder dari FolderCache saat folder dibubarkan (auto-dissolve)
            com.silauncer.cepat.cache.FolderCache.remove(folderInfo.id)
            val folderPos = mutableItems.indexOfFirst {
                (it as? LauncherItem.Folder)?.folderInfo?.id == folderInfo.id
            }
            if (folderPos != -1) {
                val remainingApp = folderInfo.getSingleRemainingApp()
                val remainingShortcut = folderInfo.getSingleRemainingShortcut()
                
                if (remainingApp != null) {
                    mutableItems[folderPos] = LauncherItem.App(remainingApp)
                } else if (remainingShortcut != null) {
                    mutableItems[folderPos] = LauncherItem.Shortcut(remainingShortcut)
                } else {
                    mutableItems.removeAt(folderPos)
                }
            }
        } else {
            // [Jalur Class]: com.silauncer.cepat.launcher.FolderManager
            // [Penjelasan]: Menyimpan pembaruan konten folder yang tersisa ke FolderCache
            com.silauncer.cepat.cache.FolderCache.put(folderInfo.id, folderInfo)
        }

        return mutableItems
    }
}
