package com.silauncer.cepat.launcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AppOrderPersistence
 * 
 * [Jalur Class]: com.silauncer.cepat.launcher.AppOrderPersistence
 * [Penjelasan]: Mengekstraksi logika penyimpanan urutan workspace dari GridDragAndDropHandler untuk memenuhi SRP.
 * Disesuaikan untuk menyimpan List<LauncherItem> ke Database Room (bukan sekadar AppInfo).
 */
class AppOrderPersistence(
    private val appController: LauncherAppController,
    private val coroutineScope: CoroutineScope
) {
    // [Jalur Class]: com.silauncer.cepat.launcher.AppOrderPersistence
    // [Penjelasan]: Menjalankan penyimpanan urutan workspace dengan NonCancellable agar operasi penulisan database Room tidak terputus/batal saat Activity/lifecycle dihancurkan mendadak.
    fun saveOrder(items: List<LauncherItem>) {
        coroutineScope.launch {
            withContext(NonCancellable) {
                appController.saveCustomWorkspaceOrder(items)
            }
        }
    }
}

