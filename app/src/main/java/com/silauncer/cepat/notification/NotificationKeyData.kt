package com.silauncer.cepat.notification

import android.app.Notification
import android.app.Person
import android.service.notification.StatusBarNotification
import java.util.Objects

/**
 * NotificationKeyData
 *
 * // [Jalur Class]: com.silauncer.cepat.notification.NotificationKeyData
 * // [Penjelasan]: Model kunci identifikasi notifikasi yang berisi ID notifikasi, ID shortcut, jumlah count, dan person keys (adaptasi dari AOSP Launcher3 NotificationKeyData).
 */
data class NotificationKeyData(
    val notificationKey: String,
    val shortcutId: String? = null,
    var count: Int = 1,
    val personKeysFromNotification: Array<String> = emptyArray()
) {
    init {
        count = maxOf(1, count)
    }

    companion object {
        val EMPTY_STRING_ARRAY: Array<String> = emptyArray()

        /**
         * Mengekstraksi NotificationKeyData dari StatusBarNotification.
         */
        @JvmStatic
        fun fromNotification(sbn: StatusBarNotification): NotificationKeyData {
            val notif = sbn.notification
            // [Jalur Class]: com.silauncer.cepat.notification.NotificationKeyData
            // [Penjelasan]: Menggunakan percabangan SDK (API 33+) untuk membaca Parcelable ArrayList dengan type safety yang baru dan mencegah deprecation warning.
            val people = if (android.os.Build.VERSION.SDK_INT >= 33) {
                notif.extras?.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST, Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                notif.extras?.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)
            }
            val personKeys = extractPersonKeyOnly(people)
            return NotificationKeyData(
                notificationKey = sbn.key,
                shortcutId = notif.shortcutId,
                count = notif.number,
                personKeysFromNotification = personKeys
            )
        }

        /**
         * Mengekstrak daftar String keys dari kumpulan NotificationKeyData.
         */
        @JvmStatic
        fun extractKeysOnly(notificationKeys: List<NotificationKeyData>): List<String> {
            return notificationKeys.map { it.notificationKey }
        }

        private fun extractPersonKeyOnly(people: ArrayList<Person>?): Array<String> {
            if (people.isNullOrEmpty()) {
                return EMPTY_STRING_ARRAY
            }
            return people.mapNotNull { it.key }.sorted().toTypedArray()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationKeyData) return false
        // Hanya membandingkan notificationKey sesuai standar Launcher3
        return notificationKey == other.notificationKey
    }

    override fun hashCode(): Int {
        return notificationKey.hashCode()
    }
}
