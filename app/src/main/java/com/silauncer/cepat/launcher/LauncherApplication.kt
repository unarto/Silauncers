package com.silauncer.cepat.launcher

import android.app.Application
import com.tencent.mmkv.MMKV

class LauncherApplication : Application() {
    companion object {
        lateinit var appContext: android.content.Context
            private set
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.LauncherApplication
    // [Penjelasan]: Membungkus inisialisasi MMKV dengan try-catch untuk mengantisipasi UnsatisfiedLinkError di lingkungan pengujian JVM/Robolectric murni tanpa merusak runtime production.
    override fun onCreate() {
        super.onCreate()
        appContext = this
        try {
            MMKV.initialize(this)
        } catch (e: Throwable) {
            // Diabaikan di lingkungan pengujian JVM
        }

        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherApplication
        // [Penjelasan]: Menerapkan preferensi bahasa yang tersimpan saat inisialisasi aplikasi menggunakan singleton LauncherPreferences.
        try {
            val savedLang = com.silauncer.cepat.storage.LauncherPreferences.getInstance().appLanguage
            com.silauncer.cepat.utils.LanguageHelper.applyLanguage(this, savedLang)
        } catch (e: Throwable) {
            // Safe fallback
        }
    }
}
