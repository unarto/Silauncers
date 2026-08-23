package com.silauncer.cepat.popup

import android.content.Context
import android.service.notification.StatusBarNotification
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.dot.DotInfo
import com.silauncer.cepat.notification.NotificationInfo
import com.silauncer.cepat.notification.NotificationKeyData
import com.silauncer.cepat.notification.NotificationListener
import com.silauncer.cepat.util.PackageUserKey
import java.util.concurrent.ConcurrentHashMap

/**
 * PopupDataProvider
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
 * // [Penjelasan]: Penyedia data untuk popup dan notification dot. Mengimplementasikan NotificationsChangedListener untuk mendengarkan perubahan notifikasi sistem secara langsung dan memperbarui status badge/dot pada aplikasi. Diadaptasi dari AOSP Launcher3.
 */
class PopupDataProvider(
    private val notificationDotsChangeListener: ((PackageUserKey) -> Unit)? = null
) : NotificationListener.NotificationsChangedListener {

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider.PopupDataChangeListener
     * // [Penjelasan]: Interface pendengar perubahan data notifikasi dan pintasan sistem saat popup container sedang ditampilkan secara aktif.
     */
    interface PopupDataChangeListener {
        fun onNotificationDotsUpdated(packageUserKey: PackageUserKey) {}
        fun trimNotifications(updatedDots: Map<PackageUserKey, DotInfo>) {}
        fun onSystemShortcutsUpdated() {}
    }

    private val packageUserToDotInfos = ConcurrentHashMap<PackageUserKey, DotInfo>()
    private var mChangeListener: PopupDataChangeListener? = null

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
     * // [Penjelasan]: Mendaftarkan atau melepaskan listener perubahan data popup.
     */
    fun setChangeListener(listener: PopupDataChangeListener?) {
        mChangeListener = listener
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
    // [Penjelasan]: Callback saat ada notifikasi baru diposting oleh sistem untuk paket tertentu.
    override fun onNotificationPosted(
        postedPackageUserKey: PackageUserKey,
        notificationKey: NotificationKeyData
    ) {
        val dotInfo = packageUserToDotInfos.getOrPut(postedPackageUserKey) { DotInfo() }
        if (dotInfo.addOrUpdateNotificationKey(notificationKey)) {
            notificationDotsChangeListener?.invoke(postedPackageUserKey)
            mChangeListener?.onNotificationDotsUpdated(postedPackageUserKey)
            mChangeListener?.trimNotifications(packageUserToDotInfos)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
    // [Penjelasan]: Callback saat notifikasi dihapus atau di-dismiss.
    override fun onNotificationRemoved(
        removedPackageUserKey: PackageUserKey,
        notificationKey: NotificationKeyData
    ) {
        val dotInfo = packageUserToDotInfos[removedPackageUserKey]
        if (dotInfo != null && dotInfo.removeNotificationKey(notificationKey)) {
            if (dotInfo.getNotificationKeys().isEmpty()) {
                packageUserToDotInfos.remove(removedPackageUserKey)
            }
            notificationDotsChangeListener?.invoke(removedPackageUserKey)
            mChangeListener?.onNotificationDotsUpdated(removedPackageUserKey)
            mChangeListener?.trimNotifications(packageUserToDotInfos)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
    // [Penjelasan]: Callback saat terjadi refresh menyeluruh terhadap semua notifikasi aktif sistem.
    override fun onNotificationFullRefresh(activeNotifications: List<StatusBarNotification>) {
        val previousKeys = HashSet(packageUserToDotInfos.keys)
        packageUserToDotInfos.clear()

        for (sbn in activeNotifications) {
            val packageUserKey = PackageUserKey.fromNotification(sbn)
            val dotInfo = packageUserToDotInfos.getOrPut(packageUserKey) { DotInfo() }
            dotInfo.addOrUpdateNotificationKey(NotificationKeyData.fromNotification(sbn))
        }

        val allAffectedKeys = HashSet(previousKeys).apply { addAll(packageUserToDotInfos.keys) }
        for (key in allAffectedKeys) {
            notificationDotsChangeListener?.invoke(key)
            mChangeListener?.onNotificationDotsUpdated(key)
        }
        mChangeListener?.trimNotifications(packageUserToDotInfos)
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
    // [Penjelasan]: Mengambil DotInfo untuk AppInfo tertentu jika ada notifikasi aktif.
    fun getDotInfoForItem(appInfo: AppInfo): DotInfo? {
        val key = PackageUserKey(appInfo.packageName, appInfo.user)
        return packageUserToDotInfos[key]
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
    // [Penjelasan]: Mengambil daftar kunci notifikasi aktif untuk AppInfo tertentu.
    fun getNotificationKeysForItem(appInfo: AppInfo): List<NotificationKeyData> {
        val dotInfo = getDotInfoForItem(appInfo) ?: return emptyList()
        return dotInfo.getNotificationKeys()
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupDataProvider
    // [Penjelasan]: Mengambil daftar objek NotificationInfo terurai dari notifikasi aktif untuk ditampilkan pada popup container.
    fun getNotificationsForItem(context: Context, appInfo: AppInfo): List<NotificationInfo> {
        val notificationKeys = getNotificationKeysForItem(appInfo)
        if (notificationKeys.isEmpty()) return emptyList()

        val listener = NotificationListener.getInstanceIfConnected() ?: return emptyList()
        val sbnList = listener.getNotificationsForKeys(notificationKeys)
        return sbnList.map { sbn -> NotificationInfo(context, sbn) }
    }
}
