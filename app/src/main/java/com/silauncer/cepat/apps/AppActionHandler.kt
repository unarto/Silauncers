package com.silauncer.cepat.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.widget.Toast
import com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo

class AppActionHandler(private val context: Context) {

    fun launchApp(app: AppInfo) {
        // [Jalur Class]: com.silauncer.cepat.apps.AppActionHandler
        // [Penjelasan]: Mencatat frekuensi peluncuran aplikasi ke preferensi pengguna via singleton LauncherPreferences untuk melatih model saran pintar sebelum memulai aktivitas aplikasi.
        com.silauncer.cepat.storage.LauncherPreferences.getInstance().incrementAppLaunchCount(app.packageName)
        val intent = app.launchIntent()
        startActivitySafely(intent, app.name)
    }

    fun launchShortcut(shortcut: WorkspaceShortcutInfo) {
        // [Jalur Class]: com.silauncer.cepat.apps.AppActionHandler
        // [Penjelasan]: Memulai eksekusi pintasan melalui LauncherApps (standar OS), memeriksa apakah pintasan dinonaktifkan dan memberikan pesan toast jika tidak dapat diluncurkan.
        if (!shortcut.isEnabled) {
            val disabledMessage = shortcut.disabledMessage?.takeIf { it.isNotBlank() }
                ?: context.getString(com.silauncer.cepat.R.string.shortcut_disabled)
            Toast.makeText(context, disabledMessage, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startShortcut(
                shortcut.packageName,
                shortcut.shortcutId,
                null,
                null,
                shortcut.user
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(com.silauncer.cepat.R.string.activity_not_found), Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Cannot open shortcut", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            Toast.makeText(context, "Shortcut error", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestUninstall(app: AppInfo) {
        // [Jalur Class]: com.silauncer.cepat.apps.AppActionHandler
        // [Penjelasan]: Menyiapkan intent ACTION_DELETE dengan data URI package, user profile ekstra, dan flag FLAG_ACTIVITY_NEW_TASK agar dialog uninstall sistem dapat berjalan aman pada Activity maupun non-Activity context.
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${app.packageName}")
            putExtra(Intent.EXTRA_USER, app.user)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivitySafely(intent, app.name)
    }

    private fun startActivitySafely(intent: Intent, appName: String) {
        // [Jalur Class]: com.silauncer.cepat.apps.AppActionHandler
        // [Penjelasan]: Memastikan flag FLAG_ACTIVITY_NEW_TASK terpasang jika konteks yang digunakan bukan Activity untuk mencegah AndroidRuntimeException saat startActivity dipanggil dari ApplicationContext.
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "App not found: $appName", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Cannot open: $appName", Toast.LENGTH_SHORT).show()
        }
    }
}
