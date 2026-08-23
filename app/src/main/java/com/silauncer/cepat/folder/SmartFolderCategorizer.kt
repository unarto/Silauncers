package com.silauncer.cepat.folder

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.silauncer.cepat.apps.AppInfo
import java.util.Locale

// [Jalur Class]: com.silauncer.cepat.folder.SmartFolderCategorizer
// [Penjelasan]: Kelas utilitas cerdas untuk mendeteksi kategori bawaan aplikasi (PackageManager) dan meluncurkan deteksi heuristik berbasis kata kunci nama paket serta nama aplikasi untuk penamaan otomatis folder yang intuitif (SRP).
class SmartFolderCategorizer(private val context: Context) {

    /**
     * Menentukan kategori terbaik/mayoritas untuk sekumpulan aplikasi di dalam folder.
     */
    fun getCategoryName(items: List<AppInfo>): String {
        if (items.isEmpty()) return "Folder"

        val categoryCounts = mutableMapOf<String, Int>()

        for (item in items) {
            val category = getAppCategory(item)
            categoryCounts[category] = categoryCounts.getOrDefault(category, 0) + 1
        }

        // Cari kategori terbanyak selain "Lainnya" atau "Alat" (jika ada yang lebih spesifik)
        val sortedCategories = categoryCounts.entries.sortedByDescending { it.value }
        if (sortedCategories.isNotEmpty()) {
            val topCategory = sortedCategories.first().key
            if (topCategory != "Lainnya" || sortedCategories.size == 1) {
                return topCategory
            }
            // Jika terbanyak adalah "Lainnya", tapi ada kategori spesifik lainnya, gunakan yang spesifik tersebut
            val alternative = sortedCategories.firstOrNull { it.key != "Lainnya" }
            if (alternative != null) {
                return alternative.key
            }
            return topCategory
        }

        return "Folder"
    }

    private fun getAppCategory(app: AppInfo): String {
        // Coba ambil kategori formal dari PackageManager
        val pm = context.packageManager
        try {
            val appInfo = pm.getApplicationInfo(app.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val formalCategory = when (appInfo.category) {
                    ApplicationInfo.CATEGORY_GAME -> "Game"
                    ApplicationInfo.CATEGORY_SOCIAL -> "Sosial"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Produktivitas"
                    ApplicationInfo.CATEGORY_AUDIO -> "Musik & Audio"
                    ApplicationInfo.CATEGORY_VIDEO -> "Video & Film"
                    ApplicationInfo.CATEGORY_IMAGE -> "Foto & Video"
                    ApplicationInfo.CATEGORY_MAPS -> "Peta & Navigasi"
                    ApplicationInfo.CATEGORY_NEWS -> "Berita & Bacaan"
                    else -> null
                }
                if (formalCategory != null) {
                    return formalCategory
                }
            }
        } catch (e: Exception) {
            // Abaikan jika package tidak ditemukan
        }

        // Jika tidak terdeteksi secara formal, gunakan pencocokan kata kunci (heuristic) berbasis nama paket dan nama aplikasi
        val pkg = app.packageName.lowercase(Locale.ROOT)
        val name = app.name.lowercase(Locale.ROOT)

        return when {
            // Game
            pkg.contains("game") || pkg.contains("arcade") || pkg.contains("toy") || pkg.contains("play") || pkg.contains("pubg") || pkg.contains("subway") || name.contains("game") || name.contains("play") -> "Game"
            // Sosial & Chatting
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("facebook") || pkg.contains("instagram") || pkg.contains("messenger") || pkg.contains("twitter") || pkg.contains("tiktok") || pkg.contains("social") || pkg.contains("chat") || name.contains("chat") || name.contains("sosial") -> "Sosial"
            // Media, Musik & Video
            pkg.contains("music") || pkg.contains("spotify") || pkg.contains("audio") || pkg.contains("player") || pkg.contains("youtube") || pkg.contains("video") || pkg.contains("gallery") || pkg.contains("camera") || pkg.contains("photo") || pkg.contains("editor") || name.contains("musik") || name.contains("video") || name.contains("foto") -> "Media"
            // Internet & Browsing
            pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") || pkg.contains("opera") || pkg.contains("internet") || name.contains("browser") || name.contains("internet") -> "Internet"
            // Peta & Perjalanan
            pkg.contains("map") || pkg.contains("navigation") || pkg.contains("gps") || pkg.contains("uber") || pkg.contains("grab") || pkg.contains("gojek") || name.contains("peta") || name.contains("navigasi") -> "Peta & Navigasi"
            // Produktivitas & Pekerjaan
            pkg.contains("office") || pkg.contains("excel") || pkg.contains("word") || pkg.contains("pdf") || pkg.contains("document") || pkg.contains("drive") || pkg.contains("email") || pkg.contains("mail") || pkg.contains("note") || name.contains("catatan") || name.contains("surat") || name.contains("kantor") -> "Produktivitas"
            // Belanja & Finansial
            pkg.contains("shop") || pkg.contains("store") || pkg.contains("pay") || pkg.contains("bank") || pkg.contains("wallet") || pkg.contains("finance") || name.contains("belanja") || name.contains("bank") || name.contains("dompet") -> "Belanja & Finansial"
            // Alat / Utility (Default Heuristic)
            pkg.contains("tool") || pkg.contains("calculator") || pkg.contains("clock") || pkg.contains("calendar") || pkg.contains("file") || pkg.contains("settings") || pkg.contains("cleaner") || pkg.contains("antivirus") || name.contains("alat") || name.contains("kalkulator") || name.contains("jam") -> "Alat"
            // Lainnya / Fallback kontekstual
            else -> "Lainnya"
        }
    }
}
