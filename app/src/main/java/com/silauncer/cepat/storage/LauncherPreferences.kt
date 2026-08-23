package com.silauncer.cepat.storage

import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import com.tencent.mmkv.MMKV

/**
 * LauncherPreferences
 *
 * Single Responsibility:
 * Mengelola persistensi data preferensi pengguna Silauncer ke MMKV storage.
 * Menggunakan konstanta acuan dari [InvariantDeviceProfile] sebagai Single Source of Truth
 * untuk seluruh nilai default tata letak grid dan ukuran ikon.
 */
// [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
// [Penjelasan]: Mengoptimalkan akses preferensi dengan menyediakan singleton accessor getInstance() dan caching MMKV instance guna mengeliminasi overhead alokasi objek GC berulang dan pemanggilan native MMKV berulang tanpa merusak SRP atau perilaku fallback.
class LauncherPreferences {
    private val kv: MMKV? get() = cachedMMKV

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Menambahkan penekanan peringatan UNCHECKED_CAST pada enkoding MMKV untuk tipe Set<String> karena type erasure di JVM.
    @Suppress("UNCHECKED_CAST")
    private fun encode(key: String, value: Any) {
        val store = kv
        if (store != null) {
            when (value) {
                is Int -> store.encode(key, value)
                is String -> store.encode(key, value)
                is Boolean -> store.encode(key, value)
                is Float -> store.encode(key, value)
                is Set<*> -> store.encode(key, value as Set<String>)
            }
        } else {
            fallbackMap[key] = value
        }
    }

    private fun decodeInt(key: String, defaultValue: Int): Int {
        return kv?.decodeInt(key, defaultValue) ?: (fallbackMap[key] as? Int ?: defaultValue)
    }

    private fun decodeString(key: String, defaultValue: String): String {
        return kv?.decodeString(key, defaultValue) ?: (fallbackMap[key] as? String ?: defaultValue)
    }

    private fun decodeBool(key: String, defaultValue: Boolean): Boolean {
        return kv?.decodeBool(key, defaultValue) ?: (fallbackMap[key] as? Boolean ?: defaultValue)
    }

    private fun decodeFloat(key: String, defaultValue: Float): Float {
        return kv?.decodeFloat(key, defaultValue) ?: (fallbackMap[key] as? Float ?: defaultValue)
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeStringSet(key: String, defaultValue: Set<String>): Set<String> {
        return kv?.decodeStringSet(key, defaultValue) ?: (fallbackMap[key] as? Set<String> ?: defaultValue)
    }

    var gridColumns: Int
        get() = decodeInt(KEY_GRID_COLUMNS, InvariantDeviceProfile.DEFAULT_COLUMNS)
        set(value) { encode(KEY_GRID_COLUMNS, value) }

    var gridRows: Int
        get() = decodeInt(KEY_GRID_ROWS, InvariantDeviceProfile.DEFAULT_ROWS)
        set(value) { encode(KEY_GRID_ROWS, value) }

    var iconSize: Int
        get() = decodeInt(KEY_ICON_SIZE, InvariantDeviceProfile.DEFAULT_ICON_SIZE_DP.toInt())
        set(value) { encode(KEY_ICON_SIZE, value) }

    var sortMode: String
        get() = decodeString(KEY_SORT_MODE, DEFAULT_SORT_MODE) ?: DEFAULT_SORT_MODE
        set(value) { encode(KEY_SORT_MODE, value) }

    var showAppLabel: Boolean
        get() = decodeBool(KEY_SHOW_APP_LABEL, InvariantDeviceProfile.DEFAULT_SHOW_LABEL)
        set(value) { encode(KEY_SHOW_APP_LABEL, value) }

    var labelSize: Float
        get() = decodeFloat(KEY_LABEL_SIZE, InvariantDeviceProfile.DEFAULT_LABEL_SIZE_SP)
        set(value) { encode(KEY_LABEL_SIZE, value) }

    var iconSpacing: Int
        get() = decodeInt(KEY_ICON_SPACING, InvariantDeviceProfile.DEFAULT_ICON_SPACING_DP.toInt())
        set(value) { encode(KEY_ICON_SPACING, value) }

    var hiddenApps: Set<String>
        get() = decodeStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
        set(value) { encode(KEY_HIDDEN_APPS, value) }

    var appOrder: List<String>
        get() = decodeString(KEY_APP_ORDER, "")?.split(APP_ORDER_SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) { encode(KEY_APP_ORDER, value.joinToString(APP_ORDER_SEPARATOR)) }

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Menyimpan nama paket Icon Pack yang aktif ("system_aosp" untuk icon sistem default).
    var iconPack: String
        get() = decodeString(KEY_ICON_PACK, DEFAULT_ICON_PACK) ?: DEFAULT_ICON_PACK
        set(value) { encode(KEY_ICON_PACK, value) }

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Menyimpan tag bahasa aplikasi yang dipilih pengguna ("system", "id", "en").
    var appLanguage: String
        get() = decodeString(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE) ?: DEFAULT_APP_LANGUAGE
        set(value) { encode(KEY_APP_LANGUAGE, value) }

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Mendapatkan frekuensi peluncuran aplikasi tertentu berdasarkan nama paket unik untuk penghitungan prediksi.
    fun getAppLaunchCount(packageName: String): Int {
        return decodeInt("launch_count_$packageName", 0)
    }

    // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
    // [Penjelasan]: Meningkatkan frekuensi peluncuran aplikasi setiap kali diluncurkan untuk melatih model saran pintar.
    fun incrementAppLaunchCount(packageName: String) {
        val current = getAppLaunchCount(packageName)
        encode("launch_count_$packageName", current + 1)
    }

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

        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
        // [Penjelasan]: Cache lazy untuk MMKV instance agar tidak melakukan pemanggilan JNI/native mmkvWithID berulang.
        private val cachedMMKV: MMKV? by lazy {
            try {
                MMKV.mmkvWithID(MMKV_ID)
            } catch (e: Throwable) {
                null
            }
        }

        // [Jalur Class]: com.silauncer.cepat.storage.LauncherPreferences
        // [Penjelasan]: Map thread-safe global penampung fallback in-memory untuk menduplikasi fungsionalitas MMKV pada lingkungan JVM/unit test.
        private val fallbackMap = java.util.concurrent.ConcurrentHashMap<String, Any>()

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
