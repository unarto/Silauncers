package com.silauncer.cepat.secondarydisplay

import android.content.Context
import android.view.View
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.database.AppStatsRepository
import kotlinx.coroutines.runBlocking

/**
 * SecondaryDisplayPredictions
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
 * // [Penjelasan]: Pengelola rekomendasi/prediksi aplikasi dan pembatas (app divider) pada laci aplikasi di antarmuka layar sekunder (Secondary Display).
 * Menggunakan Room AppStatsRepository untuk mengambil frekuensi penggunaan aplikasi secara terstruktur.
 */
class SecondaryDisplayPredictions(private val context: Context) {

    private val predictedApps = ArrayList<AppInfo>()
    private val appStatsRepo = AppStatsRepository(context)

    companion object {
        /**
         * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
         * // [Penjelasan]: Membuat instansi SecondaryDisplayPredictions untuk konteks tampilan sekunder.
         */
        fun newInstance(context: Context): SecondaryDisplayPredictions {
            return SecondaryDisplayPredictions(context)
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
     * // [Penjelasan]: Memperbarui tampilan pembatas (divider) aplikasi yang memisahkan aplikasi terprediksi dan seluruh aplikasi.
     */
    fun updateAppDivider(dividerView: View?) {
        dividerView?.visibility = if (predictedApps.isNotEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
     * // [Penjelasan]: Mengatur daftar aplikasi terprediksi secara asinkron dari Room Database berdasarkan statistik peluncuran nyata.
     */
    suspend fun setPredictedAppsSuspend(apps: List<AppInfo>) {
        val statsMap = appStatsRepo.getAllStatsMap()
        applyRanking(apps, statsMap)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
     * // [Penjelasan]: Mengatur daftar aplikasi terprediksi pada bagian atas laci aplikasi dengan mengurutkannya secara pintar berdasarkan frekuensi penggunaan dari Room Database.
     */
    fun setPredictedApps(apps: List<AppInfo>) {
        val statsMap = runBlocking { appStatsRepo.getAllStatsMap() }
        applyRanking(apps, statsMap)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
     * // [Penjelasan]: Menerapkan pengurutan aplikasi berdasarkan map statistik frekuensi penggunaan aplikasi.
     */
    fun setPredictedAppsWithStats(apps: List<AppInfo>, statsMap: Map<String, Int>) {
        applyRanking(apps, statsMap)
    }

    private fun applyRanking(apps: List<AppInfo>, statsMap: Map<String, Int>) {
        val sortedApps = apps.sortedWith(compareByDescending<AppInfo> {
            statsMap[it.packageName] ?: 0
        }.thenBy { it.name.lowercase() })

        predictedApps.clear()
        predictedApps.addAll(sortedApps.take(4))
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
     * // [Penjelasan]: Mengembalikan daftar aplikasi terprediksi saat ini.
     */
    fun getPredictedApps(): List<AppInfo> = ArrayList(predictedApps)
}
