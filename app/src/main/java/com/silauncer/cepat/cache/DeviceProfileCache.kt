package com.silauncer.cepat.cache

import com.silauncer.cepat.deviceprofile.DeviceProfile
import com.silauncer.cepat.deviceprofile.DisplayMetricsResolver
import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import com.silauncer.cepat.deviceprofile.ProfileConfig

// [Jalur Class]: com.silauncer.cepat.cache.DeviceProfileCache
// [Penjelasan]: Cache terkomputasi untuk menampung hasil perhitungan runtime DeviceProfile berdasarkan InvariantDeviceProfile, DisplaySpec, dan ProfileConfig.
object DeviceProfileCache {
    private var cachedProfile: DeviceProfile? = null
    private var lastKey: String? = null

    /**
     * Mengambil DeviceProfile yang sudah terhitung jika konfigurasi cocok, atau menghitung ulang dan menyimpannya ke cache.
     */
    fun getOrCalculate(
        inv: InvariantDeviceProfile,
        displaySpec: DisplayMetricsResolver.DisplaySpec,
        config: ProfileConfig = ProfileConfig()
    ): DeviceProfile {
        val key = "${inv.hashCode()}_${displaySpec.widthPx}_${displaySpec.heightPx}_${displaySpec.density}_${displaySpec.fontScale}_${displaySpec.isLandscape}_${config.hashCode()}"
        val existing = cachedProfile
        if (existing != null && lastKey == key) {
            return existing
        }
        val calculated = DeviceProfile(inv, displaySpec, config)
        cachedProfile = calculated
        lastKey = key
        return calculated
    }

    /**
     * Menghapus cache profil saat terjadi perubahan preferensi grid, ukuran ikon, atau orientasi layar.
     */
    fun invalidate() {
        cachedProfile = null
        lastKey = null
    }

    fun clear() {
        invalidate()
    }
}
