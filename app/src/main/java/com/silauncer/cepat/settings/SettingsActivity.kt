// [Jalur Class]: com.silauncer.cepat.settings.SettingsActivity
// [Tanggung Jawab SRP]: Khusus menangani inisialisasi UI, lifecycle, dan event binding layar Pengaturan.
package com.silauncer.cepat.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.GetInstalledAppsUseCase
import com.silauncer.cepat.storage.LauncherPreferences
import kotlinx.coroutines.launch

/**
 * SettingsActivity
 *
 * Aktivitas utama antarmuka Pengaturan (Launcher Settings).
 * Berfungsi sebagai orchestrator UI tipis yang mengoordinasikan:
 * - Wallpaper dinamis & transparansi window ([SettingsWallpaperHelper])
 * - Pembangunan hierarki node konfigurasi ([SettingsNodeFactory])
 * - Rendering TreeView bertingkat ([SettingsTreeAdapter])
 * - Persistensi data preferensi ([LauncherPreferences])
 */
class SettingsActivity : AppCompatActivity() {

    private val wallpaperHelper = SettingsWallpaperHelper()
    // [Jalur Class]: com.silauncer.cepat.settings.SettingsActivity
    // [Penjelasan]: Mengakses instance preferensi bersama melalui singleton LauncherPreferences.getInstance().
    private val prefs: LauncherPreferences by lazy { LauncherPreferences.getInstance() }

    private lateinit var wallpaperBackground: ImageView
    private lateinit var wallpaperScrim: View
    private lateinit var btnBack: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var treeAdapter: SettingsTreeAdapter
    private lateinit var nodeFactory: SettingsNodeFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Terapkan transparansi window bar sistem
        wallpaperHelper.applyWindowTransparency(this)

        setContentView(R.layout.activity_settings)

        // 2. Inisialisasi referensi view UI
        wallpaperBackground = findViewById(R.id.wallpaperBackground)
        wallpaperScrim = findViewById(R.id.wallpaperScrim)
        btnBack = findViewById(R.id.btnBack)
        recyclerView = findViewById(R.id.settingsRecyclerView)

        // 3. Konfigurasi Dynamic Wallpaper & Efek Scrim Blur
        wallpaperHelper.setupDynamicWallpaper(this, wallpaperBackground, wallpaperScrim)

        // 4. Tombol Navigasi Kembali
        btnBack.setOnClickListener {
            finish()
        }

        // 5. Inisialisasi Factory Node dan Adapter TreeView
        treeAdapter = SettingsTreeAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = treeAdapter

        nodeFactory = SettingsNodeFactory(
            context = this,
            prefs = prefs,
            onManageHiddenApps = { openManageHiddenAppsDialog() },
            onResetLayout = { showResetConfirmDialog() },
            onSettingChanged = { refreshSettingsTree() }
        )

        // 6. Muat data awal TreeView
        refreshSettingsTree()
    }

    /**
     * Memperbarui seluruh item hierarki TreeView saat terjadi perubahan nilai preference.
     */
    private fun refreshSettingsTree() {
        val treeNodes = nodeFactory.createTreeNodes()
        treeAdapter.setNodes(treeNodes)
    }

    /**
     * Menampilkan dialog pemilihan aplikasi yang ingin disembunyikan dari peluncur.
     */
    private fun openManageHiddenAppsDialog() {
        lifecycleScope.launch {
            val useCase = GetInstalledAppsUseCase(this@SettingsActivity)
            val apps = useCase()
            if (!isFinishing && !isDestroyed) {
                HiddenAppsDialog.show(this@SettingsActivity, apps, prefs)
            }
        }
    }

    /**
     * Menampilkan dialog konfirmasi sebelum mereset pengaturan tata letak ke default.
     */
    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_folder_title)
            .setMessage("Apakah Anda yakin ingin mengembalikan seluruh tata letak dan ukuran ikon ke default?")
            .setPositiveButton("Reset") { _, _ ->
                prefs.resetToDefaults()
                refreshSettingsTree()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        wallpaperHelper.clear(wallpaperBackground)
    }
}
