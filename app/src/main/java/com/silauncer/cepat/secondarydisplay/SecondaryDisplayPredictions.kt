package com.silauncer.cepat.secondarydisplay

import android.content.Context
import android.view.View
import com.silauncer.cepat.apps.AppInfo

/**
 * SecondaryDisplayPredictions
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPredictions
 * // [Penjelasan]: Pengelola rekomendasi/prediksi aplikasi dan pembatas (app divider) pada laci aplikasi di antarmuka layar sekunder (Secondary Display).
 */
class SecondaryDisplayPredictions(private val context: Context) {

    private val predictedApps = ArrayList<AppInfo>()

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
     * // [Penjelasan]: Mengatur daftar aplikasi terprediksi pada bagian atas laci aplikasi dengan mengurutkannya secara pintar berdasarkan frekuensi penggunaan dari singleton LauncherPreferences.
     */
    fun setPredictedApps(apps: List<AppInfo>) {
        val prefs = com.silauncer.cepat.storage.LauncherPreferences.getInstance()
        val sortedApps = apps.sortedWith(compareByDescending<AppInfo> {
            prefs.getAppLaunchCount(it.packageName)
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
