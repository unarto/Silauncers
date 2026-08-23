package com.silauncer.cepat.dot

import com.silauncer.cepat.notification.NotificationKeyData

/**
 * DotInfo
 *
 * // [Jalur Class]: com.silauncer.cepat.dot.DotInfo
 * // [Penjelasan]: Data container yang mengelola informasi notification dot (daftar kunci notifikasi dan total count akumulasi) untuk icon aplikasi (adaptasi dari AOSP Launcher3 DotInfo).
 */
open class DotInfo {

    companion object {
        const val MAX_COUNT = 999
    }

    /**
     * Kunci notifikasi yang diwakili oleh dot ini.
     */
    private val notificationKeys: MutableList<NotificationKeyData> = ArrayList()

    /**
     * Total jumlah notifikasi akumulasi dari seluruh keys.
     */
    private var totalCount: Int = 0

    /**
     * Menambahkan atau memperbarui NotificationKeyData.
     * Mengembalikan true jika notifikasi baru ditambahkan atau jumlah count berubah.
     */
    open fun addOrUpdateNotificationKey(notificationKey: NotificationKeyData): Boolean {
        val indexOfPrevKey = notificationKeys.indexOf(notificationKey)
        val prevKey = if (indexOfPrevKey == -1) null else notificationKeys[indexOfPrevKey]
        if (prevKey != null) {
            if (prevKey.count == notificationKey.count) {
                return false
            }
            totalCount -= prevKey.count
            totalCount += notificationKey.count
            prevKey.count = notificationKey.count
            return true
        }
        val added = notificationKeys.add(notificationKey)
        if (added) {
            totalCount += notificationKey.count
        }
        return added
    }

    /**
     * Menghapus notification key jika ada.
     * Mengembalikan true jika key berhasil dihapus.
     */
    open fun removeNotificationKey(notificationKey: NotificationKeyData): Boolean {
        val indexOf = notificationKeys.indexOf(notificationKey)
        if (indexOf != -1) {
            val removedKey = notificationKeys.removeAt(indexOf)
            totalCount -= removedKey.count
            return true
        }
        return false
    }

    /**
     * Mengembalikan daftar salinan kunci notifikasi.
     */
    fun getNotificationKeys(): List<NotificationKeyData> {
        return ArrayList(notificationKeys)
    }

    /**
     * Mengembalikan total hitungan notifikasi dibatasi maksimal [MAX_COUNT].
     */
    open fun getNotificationCount(): Int {
        return minOf(totalCount, MAX_COUNT)
    }

    /**
     * Memeriksa apakah terdapat notifikasi aktif.
     */
    open fun hasDot(): Boolean {
        return getNotificationCount() > 0
    }

    /**
     * Membersihkan seluruh data notifikasi.
     */
    open fun clear() {
        notificationKeys.clear()
        totalCount = 0
    }

    override fun toString(): String {
        return totalCount.toString()
    }
}
