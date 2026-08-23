// [Jalur Class]: com.silauncer.cepat.utils.LanguageHelper
// [Tanggung Jawab SRP]: Khusus menangani konfigurasi dan perubahan Locale/Bahasa aplikasi secara dinamis menggunakan AppCompatDelegate dan LocaleListCompat.
package com.silauncer.cepat.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * LanguageHelper
 *
 * Mengelola perubahan bahasa aplikasi (Locale) secara dinamis tanpa merusak state activity.
 * Mendukung opsi:
 * - Default Sistem ("system")
 * - Bahasa Indonesia ("id")
 * - English ("en")
 */
object LanguageHelper {

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_INDONESIAN = "id"
    const val LANGUAGE_ENGLISH = "en"

    // [Jalur Class]: com.silauncer.cepat.utils.LanguageHelper
    // [Penjelasan]: Menerapkan locale pilihan pengguna secara dinamis ke seluruh komponen aplikasi via AppCompatDelegate.
    fun applyLanguage(context: Context, languageTag: String) {
        try {
            val appLocales = if (languageTag == LANGUAGE_SYSTEM || languageTag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(languageTag)
            }
            AppCompatDelegate.setApplicationLocales(appLocales)
        } catch (e: Throwable) {
            // Fallback safe execution pada environment JVM/test
        }
    }

    // [Jalur Class]: com.silauncer.cepat.utils.LanguageHelper
    // [Penjelasan]: Mengembalikan daftar opsi bahasa yang tersedia (kode bahasa dan nama tampilan).
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            LANGUAGE_SYSTEM to "Default Sistem",
            LANGUAGE_INDONESIAN to "Bahasa Indonesia",
            LANGUAGE_ENGLISH to "English"
        )
    }

    // [Jalur Class]: com.silauncer.cepat.utils.LanguageHelper
    // [Penjelasan]: Mendapatkan nama tampilan bahasa berdasarkan tag ISO/kode bahasa.
    fun getLanguageDisplayName(languageTag: String): String {
        return when (languageTag) {
            LANGUAGE_INDONESIAN -> "Bahasa Indonesia"
            LANGUAGE_ENGLISH -> "English"
            else -> "Default Sistem"
        }
    }
}
