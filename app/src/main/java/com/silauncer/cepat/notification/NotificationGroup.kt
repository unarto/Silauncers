package com.silauncer.cepat.notification

import java.util.HashSet

/**
 * NotificationGroup
 *
 * // [Jalur Class]: com.silauncer.cepat.notification.NotificationGroup
 * // [Penjelasan]: Menyimpan data grup notifikasi (group summary key dan himpunan child keys) untuk menangani dismiss grup dan anak notifikasi (adaptasi AOSP Launcher3 NotificationGroup).
 */
class NotificationGroup {

    var groupSummaryKey: String? = null
        private set

    private val childKeys: MutableSet<String> = HashSet()

    fun setGroupSummaryKey(key: String?) {
        this.groupSummaryKey = key
    }

    fun addChildKey(childKey: String) {
        childKeys.add(childKey)
    }

    fun removeChildKey(childKey: String) {
        childKeys.remove(childKey)
    }

    fun getChildKeys(): Set<String> {
        return HashSet(childKeys)
    }

    fun isEmpty(): Boolean {
        return childKeys.isEmpty()
    }
}
