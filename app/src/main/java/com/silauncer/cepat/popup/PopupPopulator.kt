package com.silauncer.cepat.popup

import android.content.Context
import android.content.pm.ShortcutInfo
import android.os.Handler
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.notification.NotificationInfo
import com.silauncer.cepat.notification.NotificationKeyData
import com.silauncer.cepat.shortcuts.ShortcutRequest
import java.util.Collections
import java.util.Comparator

/**
 * PopupPopulator
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.PopupPopulator
 * // [Penjelasan]: Logika penyortiran dan pemfilteran pintasan (shortcuts) yang ditampilkan pada popup container, memastikan prioritas manifest vs dynamic shortcuts sesuai standar AOSP Launcher3.
 */
object PopupPopulator {

    const val MAX_SHORTCUTS = 4
    const val NUM_DYNAMIC = 2
    const val MAX_SHORTCUTS_IF_NOTIFICATIONS = 2

    // [Jalur Class]: com.silauncer.cepat.popup.PopupPopulator
    // [Penjelasan]: Komparator ranking shortcut: manifest shortcut diutamakan sebelum dynamic shortcut, lalu diurutkan berdasarkan getRank().
    private val SHORTCUT_RANK_COMPARATOR = Comparator<ShortcutInfo> { a, b ->
        if (a.isDeclaredInManifest && !b.isDeclaredInManifest) {
            return@Comparator -1
        }
        if (!a.isDeclaredInManifest && b.isDeclaredInManifest) {
            return@Comparator 1
        }
        a.rank.compareTo(b.rank)
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupPopulator
    // [Penjelasan]: Memfilter dan mengurutkan daftar ShortcutInfo agar jumlahnya tidak melebihi batas maksimal yang diizinkan pada kartu popup.
    fun sortAndFilterShortcuts(
        shortcuts: List<ShortcutInfo>,
        maxCount: Int = MAX_SHORTCUTS,
        shortcutIdToRemoveFirst: String? = null
    ): List<ShortcutInfo> {
        val workingList = ArrayList(shortcuts)

        if (shortcutIdToRemoveFirst != null) {
            val iterator = workingList.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().id == shortcutIdToRemoveFirst) {
                    iterator.remove()
                    break
                }
            }
        }

        Collections.sort(workingList, SHORTCUT_RANK_COMPARATOR)
        if (workingList.size <= maxCount) {
            return workingList
        }

        val filteredShortcuts = ArrayList<ShortcutInfo>(maxCount)
        var numDynamic = 0

        for (shortcut in workingList) {
            val currentFilteredSize = filteredShortcuts.size
            if (currentFilteredSize < maxCount) {
                filteredShortcuts.add(shortcut)
                if (shortcut.isDynamic) {
                    numDynamic++
                }
                continue
            }

            if (shortcut.isDynamic && numDynamic < NUM_DYNAMIC) {
                numDynamic++
                val lastStaticIndex = currentFilteredSize - numDynamic
                if (lastStaticIndex in 0 until filteredShortcuts.size) {
                    filteredShortcuts.removeAt(lastStaticIndex)
                    filteredShortcuts.add(shortcut)
                }
            }
        }

        return filteredShortcuts
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupPopulator
     * // [Penjelasan]: Membuat Runnable asinkron untuk mengambil pintasan deep shortcut dan notifikasi aktif secara terpisah di thread terpisah lalu memperbarui UI.
     */
    fun createUpdateRunnable(
        context: Context,
        appInfo: AppInfo,
        uiHandler: Handler,
        popupDataProvider: PopupDataProvider,
        onUpdateCallback: (shortcuts: List<ShortcutInfo>, notifications: List<NotificationInfo>) -> Unit
    ): Runnable {
        return Runnable {
            val rawShortcuts = ShortcutRequest(context, appInfo.user)
                .forPackage(appInfo.packageName)
                .query(ShortcutRequest.ALL)
            
            val notifications = popupDataProvider.getNotificationsForItem(context, appInfo)
            val filteredShortcuts = sortAndFilterShortcuts(
                rawShortcuts,
                if (notifications.isNotEmpty()) MAX_SHORTCUTS_IF_NOTIFICATIONS else MAX_SHORTCUTS
            )

            uiHandler.post {
                onUpdateCallback(filteredShortcuts, notifications)
            }
        }
    }
}
