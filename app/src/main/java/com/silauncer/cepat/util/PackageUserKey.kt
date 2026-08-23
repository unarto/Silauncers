package com.silauncer.cepat.util

import android.os.Process
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import java.util.Objects

/**
 * PackageUserKey
 *
 * // [Jalur Class]: com.silauncer.cepat.util.PackageUserKey
 * // [Penjelasan]: Kunci komposit unik berbasis nama paket (packageName), UserHandle profil Android, dan kategori widget (adaptasi AOSP Launcher3 PackageUserKey).
 */
data class PackageUserKey(
    var packageName: String,
    var user: UserHandle? = Process.myUserHandle(),
    var widgetCategory: Int = NO_CATEGORY
) {
    companion object {
        const val NO_CATEGORY = -1

        /**
         * Membuat PackageUserKey dari StatusBarNotification.
         */
        fun fromNotification(notification: StatusBarNotification): PackageUserKey {
            return PackageUserKey(notification.packageName, notification.user, NO_CATEGORY)
        }
    }

    fun update(pkgName: String, userHandle: UserHandle?, category: Int = NO_CATEGORY) {
        this.packageName = pkgName
        this.user = userHandle
        this.widgetCategory = category
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageUserKey) return false
        return packageName == other.packageName &&
                widgetCategory == other.widgetCategory &&
                Objects.equals(user, other.user)
    }

    override fun hashCode(): Int {
        return Objects.hash(packageName, widgetCategory, user)
    }

    override fun toString(): String {
        return "$packageName#$user,category=$widgetCategory"
    }
}
