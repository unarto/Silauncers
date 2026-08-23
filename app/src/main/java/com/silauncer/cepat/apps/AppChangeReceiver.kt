package com.silauncer.cepat.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class AppChangeReceiver(
    private val onPackageEvent: (action: String?, packageName: String?, replacing: Boolean, user: android.os.UserHandle) -> Unit
) : BroadcastReceiver() {

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        // [Jalur Class]: com.silauncer.cepat.apps.AppChangeReceiver
        // [Penjelasan]: Menggunakan IntentCompat.getParcelableExtra untuk menggantikan API getParcelableExtra yang deprecated pada API 33+.
        val user = androidx.core.content.IntentCompat.getParcelableExtra(intent, Intent.EXTRA_USER, android.os.UserHandle::class.java) ?: android.os.Process.myUserHandle()
        
        onPackageEvent(action, packageName, replacing, user)
    }
}
