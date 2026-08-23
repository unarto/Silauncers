package com.silauncer.cepat.notification

import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class AppNotificationState(
    val packageName: String,
    val notificationCount: Int,
    val hasNotification: Boolean = notificationCount > 0
)

// [Jalur Class]: com.silauncer.cepat.notification.NotificationRepository
// [Penjelasan]: Menghubungkan pelacakan notifikasi dengan NotificationCache terpusat untuk mengelola titik notifikasi (Notification Dots).
class NotificationRepository private constructor() {

    private val _notificationState = MutableStateFlow<Map<String, AppNotificationState>>(emptyMap())
    val notificationState: StateFlow<Map<String, AppNotificationState>> = _notificationState.asStateFlow()

    fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val key = sbn.key
        
        // [Jalur Class]: com.silauncer.cepat.notification.NotificationRepository
        // [Penjelasan]: Memasukkan notifikasi aktif ke NotificationCache dan memperbarui StateFlow jika ada perubahan.
        val changed = com.silauncer.cepat.cache.NotificationCache.addNotificationKey(packageName, key)
        if (changed) {
            publishState()
        }
    }

    fun onNotificationRemoved(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val key = sbn.key
        
        // [Jalur Class]: com.silauncer.cepat.notification.NotificationRepository
        // [Penjelasan]: Menghapus key notifikasi dari NotificationCache dan mempublikasikan status terbaru jika berubah.
        val changed = com.silauncer.cepat.cache.NotificationCache.removeNotificationKey(packageName, key)
        if (changed) {
            publishState()
        }
    }

    fun onNotificationFullRefresh(activeNotifications: List<StatusBarNotification>) {
        // [Jalur Class]: com.silauncer.cepat.notification.NotificationRepository
        // [Penjelasan]: Menggunakan replaceAll() untuk pembaruan atomik (volatile swap) daripada kombinasi clear() dan add(),
        // sehingga UI atau observer state flow tidak akan pernah menerima map kosong secara tidak sengaja di tengah proses refresh.
        val newMap = mutableMapOf<String, MutableSet<String>>()
        for (sbn in activeNotifications) {
            newMap.getOrPut(sbn.packageName) { mutableSetOf() }.add(sbn.key)
        }
        com.silauncer.cepat.cache.NotificationCache.replaceAll(newMap)
        publishState()
    }

    private fun publishState() {
        val allNotifications = com.silauncer.cepat.cache.NotificationCache.getAll()
        val newState = allNotifications.mapValues { (packageName, keys) ->
            AppNotificationState(packageName, keys.size)
        }
        _notificationState.value = newState
    }

    companion object {
        @Volatile
        private var instance: NotificationRepository? = null

        fun getInstance(): NotificationRepository {
            return instance ?: synchronized(this) {
                instance ?: NotificationRepository().also { instance = it }
            }
        }
    }
}
