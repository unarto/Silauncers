package com.silauncer.cepat.pm

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * // [Jalur Class]: com.silauncer.cepat.pm.PinRequestHelper
 * // [Penjelasan]: Pembantu penanganan PinItemRequest dari sistem Android O+ (API 26+)
 * ketika aplikasi eksternal meminta launcher untuk menyematkan pintasan (shortcut) ke workspace.
 */
object PinRequestHelper {

    /**
     * // [Jalur Class]: com.silauncer.cepat.pm.PinRequestHelper
     * // [Penjelasan]: Mengekstraksi objek PinItemRequest dari Intent yang dikirimkan oleh sistem Android.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun getPinItemRequest(intent: Intent): PinItemRequest? {
        // [Jalur Class]: com.silauncer.cepat.pm.PinRequestHelper
        // [Penjelasan]: Menggunakan IntentCompat.getParcelableExtra untuk menggantikan API getParcelableExtra yang deprecated pada API 33+.
        return androidx.core.content.IntentCompat.getParcelableExtra(intent, LauncherApps.EXTRA_PIN_ITEM_REQUEST, PinItemRequest::class.java)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.pm.PinRequestHelper
     * // [Penjelasan]: Membuat objek PinItemRequest dari ShortcutInfo yang diberikan via LauncherApps dan ShortcutManager.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun createRequestForShortcut(context: Context, info: ShortcutInfo): PinItemRequest? {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return null
        val resultIntent = shortcutManager.createShortcutResultIntent(info)
        return launcherApps.getPinItemRequest(resultIntent)
    }
}
