package com.silauncer.cepat.launcher

import android.app.Application
import com.tencent.mmkv.MMKV

class LauncherApplication : Application() {
    companion object {
        lateinit var appContext: android.content.Context
            private set
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.LauncherApplication
    // [Penjelasan]: Menginisialisasi MMKV storage engine pada startup aplikasi dan menerapkan konfigurasi preferensi bahasa pengguna secara langsung.
    override fun onCreate() {
        super.onCreate()
        appContext = this
        MMKV.initialize(this)

        val savedLang = com.silauncer.cepat.storage.LauncherPreferences.getInstance().appLanguage
        com.silauncer.cepat.utils.LanguageHelper.applyLanguage(this, savedLang)
    }
}
