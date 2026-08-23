// [Jalur Class]: com.silauncer.cepat.settings.SettingsWallpaperHelper
// [Tanggung Jawab SRP]: Khusus menangani drawable wallpaper dinamis perangkat, transparansi window, dan penerapan efek blur/scrim transparan.
package com.silauncer.cepat.settings

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.silauncer.cepat.R

/**
 * SettingsWallpaperHelper
 *
 * Mengelola integrasi tampilan Wallpaper perangkat secara dinamis,
 * transparansi window bar sistem, serta lapisan efek blur dan scrim.
 */
class SettingsWallpaperHelper {

    /**
     * Mengatur transparansi penuh pada Window Activity agar wallpaper perangkat
     * dapat terekspos secara alami dan bebas batas hitam/putih pekat.
     */
    fun applyWindowTransparency(activity: Activity) {
        val window = activity.window

        // [Jalur Class]: com.silauncer.cepat.settings.SettingsWallpaperHelper
        // [Penjelasan]: Mengaktifkan flag FLAG_SHOW_WALLPAPER dan window background transparan agar wallpaper sistem terlihat langsung di belakang window.
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    /**
     * Memuat drawable wallpaper perangkat yang sedang aktif secara realtime,
     * menyetelnya pada wallpaperImageView, menerapkan efek blur dinamis (Android 12+),
     * serta mengonfigurasi scrim transparan 20% untuk keterbacaan teks yang optimal tanpa menutup wallpaper.
     */
    fun setupDynamicWallpaper(
        activity: Activity,
        wallpaperImageView: ImageView,
        scrimView: View
    ) {
        try {
            // [Jalur Class]: com.silauncer.cepat.settings.SettingsWallpaperHelper
            // [Penjelasan]: Memastikan latar belakang ImageView selalu transparan agar tidak ada blok warna pekat yang menutupi wallpaper.
            wallpaperImageView.setBackgroundColor(Color.TRANSPARENT)

            val wallpaperManager = WallpaperManager.getInstance(activity.applicationContext)
            val wallpaperDrawable: Drawable? = try {
                wallpaperManager.drawable ?: wallpaperManager.fastDrawable
            } catch (se: SecurityException) {
                // Izin akses wallpaper dibatasi pada beberapa OEM/level API, gunakan fastDrawable fallback
                wallpaperManager.fastDrawable
            } catch (t: Throwable) {
                null
            }

            if (wallpaperDrawable != null) {
                wallpaperImageView.setImageDrawable(wallpaperDrawable)
                wallpaperImageView.visibility = View.VISIBLE

                // Terapkan efek Blur perangkat keras (Hardware RenderEffect) pada Android 12+ (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurEffect = RenderEffect.createBlurEffect(
                        BLUR_RADIUS_PX,
                        BLUR_RADIUS_PX,
                        Shader.TileMode.CLAMP
                    )
                    wallpaperImageView.setRenderEffect(blurEffect)
                }
            } else {
                // [Jalur Class]: com.silauncer.cepat.settings.SettingsWallpaperHelper
                // [Penjelasan]: Jika drawable tidak dapat diekstrak langsung, biarkan ImageView transparan agar wallpaper window manager (FLAG_SHOW_WALLPAPER) terekspos langsung.
                wallpaperImageView.setImageDrawable(null)
                wallpaperImageView.setBackgroundColor(Color.TRANSPARENT)
            }

            // [Jalur Class]: com.silauncer.cepat.settings.SettingsWallpaperHelper
            // [Penjelasan]: Lapisan scrim overlay transparan (20% opacity) dari resource agar wallpaper tetap tampak dinamis dan kartu TreeView tetap kontras.
            val scrimColor = ContextCompat.getColor(scrimView.context, R.color.scrim_overlay_color)
            scrimView.setBackgroundColor(scrimColor)

        } catch (e: Throwable) {
            // Fallback aman untuk lingkungan unit test / Robolectric
            wallpaperImageView.setImageDrawable(null)
            wallpaperImageView.setBackgroundColor(Color.TRANSPARENT)
            val scrimColor = ContextCompat.getColor(scrimView.context, R.color.scrim_overlay_color)
            scrimView.setBackgroundColor(scrimColor)
        }
    }

    /**
     * Membersihkan referensi drawable saat Activity dihancurkan untuk mencegah memory leak.
     */
    fun clear(wallpaperImageView: ImageView?) {
        wallpaperImageView?.setImageDrawable(null)
    }

    companion object {
        private const val BLUR_RADIUS_PX = 20f
    }
}
