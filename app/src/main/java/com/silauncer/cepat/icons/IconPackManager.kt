// [Jalur Class]: com.silauncer.cepat.icons.IconPackManager
// [Tanggung Jawab SRP]: Khusus menangani pemindaian Icon Pack terinstal, pemuatan drawable dari Icon Pack (appfilter.xml & resources), dan penyediaan fallback ke icon sistem/AOSP.
package com.silauncer.cepat.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

/**
 * Metadata paket ikon yang terinstal pada sistem perangkat.
 */
data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null
)

/**
 * IconPackManager
 *
 * Mengelola integrasi Third-Party Icon Pack dan Default System/AOSP Icon:
 * 1. Memindai aplikasi Icon Pack terinstal via Intent Launcher Theme filters.
 * 2. Memuat dan meng-cache pemetaan icon dari file appfilter.xml di dalam paket ikon.
 * 3. Menyediakan fallback aman ke AOSP/System Icon jika aplikasi tidak didukung oleh Icon Pack.
 */
class IconPackManager private constructor() {

    private var currentLoadedPackage: String? = null
    private val componentDrawableMap = ConcurrentHashMap<String, String>()
    private val packageDrawableMap = ConcurrentHashMap<String, String>()

    /**
     * Memindai seluruh aplikasi Icon Pack yang terinstal pada perangkat.
     */
    // [Jalur Class]: com.silauncer.cepat.icons.IconPackManager
    // [Penjelasan]: Memindai aplikasi icon pack pihak ketiga via queryIntentActivities dengan action tema launcher standar industri (Nova, ADW, Go Launcher, Apex, dll).
    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val iconPacks = mutableMapOf<String, IconPackInfo>()

        for (action in ICON_PACK_INTENT_ACTIONS) {
            val intent = Intent(action)
            val activities = try {
                pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            } catch (e: Throwable) {
                emptyList()
            }

            for (resolveInfo in activities) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (!iconPacks.containsKey(pkgName)) {
                    val label = resolveInfo.loadLabel(pm).toString()
                    val icon = try {
                        resolveInfo.loadIcon(pm)
                    } catch (e: Throwable) {
                        null
                    }
                    iconPacks[pkgName] = IconPackInfo(
                        packageName = pkgName,
                        label = label,
                        icon = icon
                    )
                }
            }
        }

        return iconPacks.values.sortedBy { it.label }
    }

    /**
     * Mendapatkan nama tampilan Icon Pack (misal: "Default Sistem / AOSP" atau label aplikasi Icon Pack).
     */
    // [Jalur Class]: com.silauncer.cepat.icons.IconPackManager
    // [Penjelasan]: Mengembalikan label ramah pengguna untuk konfigurasi icon pack yang aktif.
    fun getIconPackLabel(context: Context, iconPackPackage: String): String {
        if (iconPackPackage == SYSTEM_AOSP || iconPackPackage.isEmpty()) {
            return "Default Sistem / AOSP"
        }
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(iconPackPackage, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Throwable) {
            iconPackPackage
        }
    }

    /**
     * Memuat drawable ikon aplikasi dari Icon Pack aktif.
     * Mengembalikan null jika ikon tidak ditemukan di dalam paket (sehingga pemanggil menggunakan Fallback System/AOSP).
     */
    // [Jalur Class]: com.silauncer.cepat.icons.IconPackManager
    // [Penjelasan]: Mengambil drawable spesifik dari icon pack pihak ketiga berdasarkan ComponentName atau PackageName dengan parsing appfilter.
    fun getIcon(
        context: Context,
        componentName: ComponentName,
        iconPackPackage: String
    ): Drawable? {
        if (iconPackPackage == SYSTEM_AOSP || iconPackPackage.isEmpty()) {
            return null
        }

        try {
            ensureIconPackLoaded(context, iconPackPackage)

            val compKey = "ComponentInfo{${componentName.packageName}/${componentName.className}}"
            val shortKey = "${componentName.packageName}/${componentName.className}"
            val pkgKey = componentName.packageName

            val drawableName = componentDrawableMap[compKey]
                ?: componentDrawableMap[shortKey]
                ?: packageDrawableMap[pkgKey]

            val pm = context.packageManager
            val iconPackRes = pm.getResourcesForApplication(iconPackPackage)

            if (drawableName != null) {
                val resId = iconPackRes.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (resId != 0) {
                    return ResourcesCompat.getDrawable(iconPackRes, resId, null)
                }
            }

            // Coba lookup berdasarkan konvensi nama class/package umum
            val directName1 = componentName.className.substringAfterLast(".").lowercase()
            val directId1 = iconPackRes.getIdentifier(directName1, "drawable", iconPackPackage)
            if (directId1 != 0) {
                return ResourcesCompat.getDrawable(iconPackRes, directId1, null)
            }

            val directName2 = componentName.packageName.replace(".", "_").lowercase()
            val directId2 = iconPackRes.getIdentifier(directName2, "drawable", iconPackPackage)
            if (directId2 != 0) {
                return ResourcesCompat.getDrawable(iconPackRes, directId2, null)
            }

        } catch (e: Throwable) {
            // Fallback gracefully jika terjadi error I/O atau resources missing
        }

        return null
    }

    /**
     * Memuat dan meng-cache appfilter.xml dari icon pack package jika belum dimuat.
     */
    @Synchronized
    private fun ensureIconPackLoaded(context: Context, iconPackPackage: String) {
        if (currentLoadedPackage == iconPackPackage) return

        clearCache()
        currentLoadedPackage = iconPackPackage

        try {
            val pm = context.packageManager
            val iconPackRes = pm.getResourcesForApplication(iconPackPackage)
            val resId = iconPackRes.getIdentifier("appfilter", "xml", iconPackPackage)

            if (resId != 0) {
                val parser = iconPackRes.getXml(resId)
                var eventType = parser.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")

                        if (component != null && drawable != null) {
                            componentDrawableMap[component] = drawable
                            val extractedPkg = extractPackageFromComponent(component)
                            if (extractedPkg != null) {
                                packageDrawableMap[extractedPkg] = drawable
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Throwable) {
            // Abaikan error parsing agar tidak crash
        }
    }

    private fun extractPackageFromComponent(componentString: String): String? {
        val start = componentString.indexOf("{")
        val slash = componentString.indexOf("/")
        return if (start != -1 && slash != -1 && slash > start) {
            componentString.substring(start + 1, slash)
        } else {
            null
        }
    }

    /**
     * Membersihkan cache XML appfilter saat icon pack diubah.
     */
    // [Jalur Class]: com.silauncer.cepat.icons.IconPackManager
    // [Penjelasan]: Mengosongkan cache in-memory pemetaan nama icon pack saat preferensi paket ikon diubah atau direset.
    fun clearCache() {
        currentLoadedPackage = null
        componentDrawableMap.clear()
        packageDrawableMap.clear()
    }

    companion object {
        const val SYSTEM_AOSP = "system_aosp"

        private val ICON_PACK_INTENT_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.fede.launcher.THEME_ICONPACK",
            "com.teslacoilsw.launcher.THEME",
            "com.anddoes.launcher.THEME",
            "com.dlto.atom.launcher.THEME"
        )

        @Volatile
        private var instance: IconPackManager? = null

        fun getInstance(): IconPackManager {
            return instance ?: synchronized(this) {
                instance ?: IconPackManager().also { instance = it }
            }
        }
    }
}
