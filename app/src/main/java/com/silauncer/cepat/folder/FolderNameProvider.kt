package com.silauncer.cepat.folder

import android.content.Context
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo

/**
 * FolderNameProvider
 *
 * // [Jalur Class]: com.silauncer.cepat.folder.FolderNameProvider
 * // [Penjelasan]: Penyedia nama default dan penamaan cerdas untuk folder aplikasi (adaptasi dari AOSP Launcher3 FolderNameProvider).
 */
class FolderNameProvider(private val context: Context) {
    // [Jalur Class]: com.silauncer.cepat.folder.FolderNameProvider
    // [Penjelasan]: Mendelegasikan penyediaan nama kategori cerdas ke SmartFolderCategorizer (SRP)
    private val categorizer = SmartFolderCategorizer(context)

    /**
     * Menghasilkan nama default untuk folder ("Folder" sesuai string resource).
     */
    fun getDefaultFolderName(): String {
        return context.getString(R.string.folder_unnamed)
    }

    /**
     * Menghasilkan saran nama folder berdasarkan item yang dimasukkan ke dalam folder.
     */
    fun getSuggestedFolderName(items: List<AppInfo>): String {
        if (items.isEmpty()) {
            return getDefaultFolderName()
        }
        val suggestedName = categorizer.getCategoryName(items)
        return if (suggestedName == "Lainnya" || suggestedName.isEmpty()) getDefaultFolderName() else suggestedName
    }
}
