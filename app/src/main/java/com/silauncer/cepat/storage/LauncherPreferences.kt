package com.silauncer.cepat.storage

import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import com.tencent.mmkv.MMKV

/**
 * LauncherPreferences
 *
 * Single Responsibility:
 * Mengelola persistensi data preferensi/konfigurasi pengguna Silauncer ke MMKV storage.
 * Menggunakan konstanta acuan dari [InvariantDeviceProfile] sebagai Single Source of Truth
 * untuk seluruh nilai default tata letak grid dan ukuran ikon.
 */
// [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
// [Penjelasan]: Menyediakan akses persistensi preferensi pengguna murni berbasis MMKV tanpa fallbackMap atau penyamaran kegagalan memori. MMKV menjadi single source of truth untuk konfigurasi preferensi ringan.
class LauncherPreferences {
    private val kv: MMKV get() = MMKV.mmkvWithID(MMKV_ID)

    var gridColumns: Int
        get() = kv.decodeInt(KEY_GRID_COLUMNS, InvariantDeviceProfile.DEFAULT_COLUMNS)
        set(value) { kv.encode(KEY_GRID_COLUMNS, value) }

    var gridRows: Int
        get() = kv.decodeInt(KEY_GRID_ROWS, InvariantDeviceProfile.DEFAULT_ROWS)
        set(value) { kv.encode(KEY_GRID_ROWS, value) }

    var iconSize: Int
        get() = kv.decodeInt(KEY_ICON_SIZE, InvariantDeviceProfile.DEFAULT_ICON_SIZE_DP.toInt())
        set(value) { kv.encode(KEY_ICON_SIZE, value) }

    var sortMode: String
        get() = kv.decodeString(KEY_SORT_MODE, DEFAULT_SORT_MODE) ?: DEFAULT_SORT_MODE
        set(value) { kv.encode(KEY_SORT_MODE, value) }

    var showAppLabel: Boolean
        get() = kv.decodeBool(KEY_SHOW_APP_LABEL, InvariantDeviceProfile.DEFAULT_SHOW_LABEL)
        set(value) { kv.encode(KEY_SHOW_APP_LABEL, value) }

    var labelSize: Float
        get() = kv.decodeFloat(KEY_LABEL_SIZE, InvariantDeviceProfile.DEFAULT_LABEL_SIZE_SP)
        set(value) { kv.encode(KEY_LABEL_SIZE, value) }

    var iconSpacing: Int
        get() = kv.decodeInt(KEY_ICON_SPACING, InvariantDeviceProfile.DEFAULT_ICON_SPACING_DP.toInt())
        set(value) { kv.encode(KEY_ICON_SPACING, value) }

    var hiddenApps: Set<String>
        get() = kv.decodeStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
        set(value) { kv.encode(KEY_HIDDEN_APPS, value) }

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Legacy storage untuk migrasi satu-kali ke Room DB (LauncherAppController). Tidak digunakan untuk penyimpanan aktif baru.
    var appOrder: List<String>
        get() = kv.decodeString(KEY_APP_ORDER, "")?.split(APP_ORDER_SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) { kv.encode(KEY_APP_ORDER, value.joinToString(APP_ORDER_SEPARATOR)) }

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Menyimpan nama paket Icon Pack yang aktif ("system_aosp" untuk icon sistem default).
    var iconPack: String
        get() = kv.decodeString(KEY_ICON_PACK, DEFAULT_ICON_PACK) ?: DEFAULT_ICON_PACK
        set(value) { kv.encode(KEY_ICON_PACK, value) }

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Menyimpan tag bahasa aplikasi yang dipilih pengguna ("system", "id", "en").
    var appLanguage: String
        get() = kv.decodeString(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE) ?: DEFAULT_APP_LANGUAGE
        set(value) { kv.encode(KEY_APP_LANGUAGE, value) }

    fun resetToDefaults() {
        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
        // [Penjelasan]: Mereset seluruh konfigurasi preferensi ke nilai default bawaan profil perangkat dan sistem.
        gridColumns = InvariantDeviceProfile.DEFAULT_COLUMNS
        gridRows = InvariantDeviceProfile.DEFAULT_ROWS
        iconSize = InvariantDeviceProfile.DEFAULT_ICON_SIZE_DP.toInt()
        sortMode = DEFAULT_SORT_MODE
        showAppLabel = InvariantDeviceProfile.DEFAULT_SHOW_LABEL
        labelSize = InvariantDeviceProfile.DEFAULT_LABEL_SIZE_SP
        iconSpacing = InvariantDeviceProfile.DEFAULT_ICON_SPACING_DP.toInt()
        hiddenApps = emptySet()
        appOrder = emptyList()
        iconPack = DEFAULT_ICON_PACK
        appLanguage = DEFAULT_APP_LANGUAGE
    }

    companion object {
        private const val MMKV_ID = "silauncer_launcher"

        @Volatile
        private var instance: LauncherPreferences? = null

        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
        // [Penjelasan]: Singleton instance provider thread-safe untuk LauncherPreferences guna mencegah alokasi objek GC berulang di berbagai caller.
        fun getInstance(): LauncherPreferences {
            return instance ?: synchronized(this) {
                instance ?: LauncherPreferences().also { instance = it }
            }
        }

        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
        // [Penjelasan]: Alias ringkas get() untuk mendapatkan instance bersama LauncherPreferences.
        fun get(): LauncherPreferences = getInstance()

        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_GRID_ROWS = "grid_rows"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_SHOW_APP_LABEL = "show_app_label"
        private const val KEY_LABEL_SIZE = "label_size"
        private const val KEY_ICON_SPACING = "icon_spacing"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_APP_ORDER = "app_order"
        private const val KEY_ICON_PACK = "icon_pack"
        private const val KEY_APP_LANGUAGE = "app_language"

        const val DEFAULT_SORT_MODE = "a_z"
        const val DEFAULT_ICON_PACK = "system_aosp"
        const val DEFAULT_APP_LANGUAGE = "system"
        private const val APP_ORDER_SEPARATOR = ","
    }
}
