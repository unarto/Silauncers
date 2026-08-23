package com.silauncer.cepat.dragndrop

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import com.silauncer.cepat.R
import com.silauncer.cepat.launcher.LauncherItem

/**
 * [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
 * [Penjelasan]: Pengontrol modular yang bertanggung jawab mengelola status, animasi, deteksi hit-test hover,
 * serta aksi drop target bar di bagian atas layar ("Hapus" dan "Info aplikasi").
 */
class DropTargetBarController(
    private val parentProvider: () -> ViewGroup?
) {

    private var dropTargetBarView: View? = null
    private var targetRemoveView: View? = null
    private var targetInfoView: View? = null

    var isHoveringRemove: Boolean = false
        private set

    var isHoveringInfo: Boolean = false
        private set

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
    // [Penjelasan]: Menginisialisasi referensi view drop target bar secara lazy dari root layout
    private fun ensureViewsInitialized() {
        if (dropTargetBarView == null) {
            val parent = parentProvider()
            dropTargetBarView = parent?.findViewById(R.id.drop_target_bar_layout)
            targetRemoveView = dropTargetBarView?.findViewById(R.id.target_remove)
            targetInfoView = dropTargetBarView?.findViewById(R.id.target_info)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
    // [Penjelasan]: Menampilkan drop target bar dengan animasi meluncur turun dari atas (slide-down)
    fun show(isApp: Boolean) {
        ensureViewsInitialized()
        targetInfoView?.visibility = if (isApp) View.VISIBLE else View.GONE

        dropTargetBarView?.let { bar ->
            bar.visibility = View.VISIBLE
            bar.alpha = 0f
            bar.translationY = -bar.height.toFloat().coerceAtLeast(100f)
            bar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
    // [Penjelasan]: Menyembunyikan drop target bar dengan animasi meluncur naik ke atas (slide-up)
    fun hide() {
        dropTargetBarView?.let { bar ->
            bar.animate()
                .alpha(0f)
                .translationY(-bar.height.toFloat().coerceAtLeast(100f))
                .setDuration(200)
                .withEndAction {
                    bar.visibility = View.GONE
                }
                .start()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
    // [Penjelasan]: Mengevaluasi deteksi tabrakan koordinat kursor terhadap zona Hapus dan Info aplikasi
    fun updateHoverState(rawX: Float, rawY: Float, onHapticFeedback: (Int) -> Unit): Boolean {
        ensureViewsInitialized()
        val bar = dropTargetBarView ?: return false
        if (bar.visibility != View.VISIBLE) return false

        var hoverRemove = false
        var hoverInfo = false

        targetRemoveView?.let { removeView ->
            val loc = IntArray(2)
            removeView.getLocationOnScreen(loc)
            val rect = Rect(loc[0], loc[1], loc[0] + removeView.width, loc[1] + removeView.height)
            if (rect.contains(rawX.toInt(), rawY.toInt())) {
                hoverRemove = true
            }
        }

        if (targetInfoView?.visibility == View.VISIBLE) {
            targetInfoView?.let { infoView ->
                val loc = IntArray(2)
                infoView.getLocationOnScreen(loc)
                val rect = Rect(loc[0], loc[1], loc[0] + infoView.width, loc[1] + infoView.height)
                if (rect.contains(rawX.toInt(), rawY.toInt())) {
                    hoverInfo = true
                }
            }
        }

        if (hoverRemove != isHoveringRemove) {
            isHoveringRemove = hoverRemove
            targetRemoveView?.setBackgroundColor(if (hoverRemove) 0x33FF3B30.toInt() else Color.TRANSPARENT)
            if (hoverRemove) {
                onHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        if (hoverInfo != isHoveringInfo) {
            isHoveringInfo = hoverInfo
            targetInfoView?.setBackgroundColor(if (hoverInfo) 0x33007AFF.toInt() else Color.TRANSPARENT)
            if (hoverInfo) {
                onHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        return isHoveringRemove || isHoveringInfo
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
    // [Penjelasan]: Mengembalikan status warna latar belakang dan hover ke kondisi awal transparan
    fun resetHoverState() {
        isHoveringRemove = false
        isHoveringInfo = false
        targetRemoveView?.setBackgroundColor(Color.TRANSPARENT)
        targetInfoView?.setBackgroundColor(Color.TRANSPARENT)
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
    // [Penjelasan]: Membuka halaman Pengaturan Info Aplikasi sistem untuk paket aplikasi target
    fun openAppInfo(context: Context, item: LauncherItem) {
        val app = (item as? LauncherItem.App)?.appInfo ?: return
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SILAUNCER", "Gagal membuka Info Aplikasi: ${e.message}")
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DropTargetBarController
    // [Penjelasan]: Membersihkan referensi View untuk mencegah memory leak saat siklus hidup Activity berakhir
    fun cleanup() {
        resetHoverState()
        dropTargetBarView = null
        targetRemoveView = null
        targetInfoView = null
    }
}
