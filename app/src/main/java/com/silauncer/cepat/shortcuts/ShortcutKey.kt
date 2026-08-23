package com.silauncer.cepat.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.UserHandle

/**
 * ShortcutKey
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.ShortcutKey
 * // [Penjelasan]: Kunci unik untuk mengidentifikasi sebuah shortcut berdasarkan nama package, id shortcut, dan UserHandle pengguna (adaptasi dari AOSP Launcher3 ShortcutKey).
 */
data class ShortcutKey(
    val componentName: ComponentName,
    val user: UserHandle
) {

    constructor(packageName: String, user: UserHandle, id: String) : this(
        ComponentName(packageName, id),
        user
    )

    val id: String
        get() = componentName.className

    val packageName: String
        get() = componentName.packageName

    /**
     * Membangun [ShortcutRequest] yang siap dieksekusi untuk kunci shortcut ini.
     */
    fun buildRequest(context: Context): ShortcutRequest {
        return ShortcutRequest(context, user)
            .forPackage(packageName, id)
    }

    companion object {
        const val EXTRA_SHORTCUT_ID = "shortcut_id"
        private const val INTENT_CATEGORY = "com.silauncer.cepat.DEEP_SHORTCUT"

        /**
         * Membuat [ShortcutKey] dari instansi [ShortcutInfo].
         */
        fun fromInfo(shortcutInfo: ShortcutInfo): ShortcutKey {
            return ShortcutKey(
                shortcutInfo.`package`,
                shortcutInfo.userHandle,
                shortcutInfo.id
            )
        }

        /**
         * Membuat [ShortcutKey] dari [Intent] dan [UserHandle].
         */
        fun fromIntent(intent: Intent, user: UserHandle): ShortcutKey {
            val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID) ?: ""
            val pkg = intent.`package` ?: intent.component?.packageName ?: ""
            return ShortcutKey(pkg, user, shortcutId)
        }

        /**
         * Membuat [Intent] dari [ShortcutInfo].
         */
        fun makeIntent(shortcutInfo: ShortcutInfo): Intent {
            val intent = makeIntent(shortcutInfo.id, shortcutInfo.`package`)
            shortcutInfo.activity?.let { intent.component = it }
            return intent
        }

        /**
         * Membuat [Intent] dasar untuk shortcut id dan package name.
         */
        fun makeIntent(shortcutId: String, packageName: String): Intent {
            return Intent(Intent.ACTION_MAIN)
                .addCategory(INTENT_CATEGORY)
                .setPackage(packageName)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .putExtra(EXTRA_SHORTCUT_ID, shortcutId)
        }
    }
}
