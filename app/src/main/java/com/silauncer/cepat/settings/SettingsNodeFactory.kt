// [Jalur Class]: com.silauncer.cepat.settings.SettingsNodeFactory
// [Tanggung Jawab SRP]: Khusus membangun struktur data hierarki TreeView (Parent, Child, Switch/Toggle, dan Option items) berbasis LauncherPreferences.
package com.silauncer.cepat.settings

import android.content.Context
import com.silauncer.cepat.R
import com.silauncer.cepat.cache.DiskIconCache
import com.silauncer.cepat.cache.IconCache
import com.silauncer.cepat.icons.IconPackManager
import com.silauncer.cepat.settings.SettingsNode.ChildNode.ActionChildNode
import com.silauncer.cepat.settings.SettingsNode.ChildNode.OptionChildNode
import com.silauncer.cepat.settings.SettingsNode.ChildNode.SwitchChildNode
import com.silauncer.cepat.settings.SettingsNode.OptionItem
import com.silauncer.cepat.settings.SettingsNode.ParentNode
import com.silauncer.cepat.storage.LauncherPreferences
import com.silauncer.cepat.utils.LanguageHelper
import kotlinx.coroutines.launch

/**
 * SettingsNodeFactory
 *
 * Bertanggung jawab memetakan konfigurasi dari [LauncherPreferences]
 * menjadi struktur data hierarki TreeView yang rapi dan terstruktur:
 * 1. Grid & Tata Letak
 * 2. Ikon & Label Aplikasi (Termasuk Pilihan Paket Ikon / Icon Pack)
 * 3. Bahasa Aplikasi (Language Settings)
 * 4. Laci Aplikasi & Urutan
 * 5. Pemeliharaan & Reset
 */
class SettingsNodeFactory(
    private val context: Context,
    private val prefs: LauncherPreferences,
    private val onManageHiddenApps: () -> Unit,
    private val onResetLayout: () -> Unit,
    private val onSettingChanged: () -> Unit
) {

    /**
     * Membangun daftar lengkap Parent Nodes beserta anak-anaknya.
     */
    fun createTreeNodes(): List<ParentNode> {
        return listOf(
            buildGridLayoutParent(),
            buildIconLabelParent(),
            buildLanguageParent(),
            buildAppDrawerParent(),
            buildMaintenanceParent()
        )
    }

    // [Jalur Class]: com.silauncer.cepat.settings.SettingsNodeFactory
    // [Penjelasan]: Helper function untuk membungkus instansiasi OptionChildNode.
    // Membantu memastikan SRP di mana Factory hanya bertugas "merakit" node (wiring),
    // sementara trigger event ke callback dienkapsulasi dengan rapi.
    private inline fun createOptionNode(
        id: String,
        parentId: String,
        title: String,
        subtitle: String,
        currentValue: String,
        displayValue: String,
        options: List<OptionItem>,
        crossinline onUpdateAction: (String) -> Unit
    ): OptionChildNode {
        return OptionChildNode(
            id = id,
            parentId = parentId,
            title = title,
            subtitle = subtitle,
            currentValue = currentValue,
            displayValue = displayValue,
            options = options,
            onSelected = { selected ->
                onUpdateAction(selected.key)
                onSettingChanged()
            }
        )
    }

    // [Jalur Class]: com.silauncer.cepat.settings.SettingsNodeFactory
    // [Penjelasan]: Helper function untuk membungkus instansiasi SwitchChildNode.
    private inline fun createSwitchNode(
        id: String,
        parentId: String,
        title: String,
        subtitle: String,
        isChecked: Boolean,
        crossinline onUpdateAction: (Boolean) -> Unit
    ): SwitchChildNode {
        return SwitchChildNode(
            id = id,
            parentId = parentId,
            title = title,
            subtitle = subtitle,
            isChecked = isChecked,
            onCheckedChange = { checked ->
                onUpdateAction(checked)
                onSettingChanged()
            }
        )
    }

    private fun buildGridLayoutParent(): ParentNode {
        val currentGrid = "${prefs.gridColumns}x${prefs.gridRows}"
        val gridOptions = listOf(
            OptionItem("4x4", "4 Kolom × 4 Baris (16 Ikon)"),
            OptionItem("4x5", "4 Kolom × 5 Baris (20 Ikon)"),
            OptionItem("5x5", "5 Kolom × 5 Baris (25 Ikon)"),
            OptionItem("5x6", "5 Kolom × 6 Baris (30 Ikon - Default)"),
            OptionItem("6x6", "6 Kolom × 6 Baris (36 Ikon)")
        )

        val currentSpacing = "${prefs.iconSpacing} dp"
        val spacingOptions = listOf(
            OptionItem("4", "4 dp (Kompak / Rapat)"),
            OptionItem("8", "8 dp (Standar)"),
            OptionItem("12", "12 dp (Sedang)"),
            OptionItem("16", "16 dp (Lebar)"),
            OptionItem("24", "24 dp (Ekstra Lebar)")
        )

        return ParentNode(
            id = "parent_grid",
            title = "Grid & Tata Letak",
            subtitle = "Kustomisasi dimensi kisi kolom, baris, dan jarak antar ikon",
            iconRes = R.drawable.ic_grid,
            isExpanded = true,
            children = listOf(
                createOptionNode(
                    id = "child_grid_layout",
                    parentId = "parent_grid",
                    title = "Tata Letak Kisi",
                    subtitle = "Jumlah kolom dan baris aplikasi pada layar utama",
                    currentValue = currentGrid,
                    displayValue = currentGrid,
                    options = gridOptions
                ) { key ->
                    val parts = key.split("x")
                    if (parts.size == 2) {
                        prefs.gridColumns = parts[0].toIntOrNull() ?: 5
                        prefs.gridRows = parts[1].toIntOrNull() ?: 6
                    }
                },
                createOptionNode(
                    id = "child_icon_spacing",
                    parentId = "parent_grid",
                    title = "Jarak Antar Ikon",
                    subtitle = "Margin pemisah vertikal dan horizontal antar item",
                    currentValue = prefs.iconSpacing.toString(),
                    displayValue = currentSpacing,
                    options = spacingOptions
                ) { key ->
                    key.toIntOrNull()?.let { prefs.iconSpacing = it }
                }
            )
        )
    }

    private fun buildIconLabelParent(): ParentNode {
        val iconPackMgr = IconPackManager.getInstance()
        val currentIconPack = prefs.iconPack
        val iconPackDisplay = iconPackMgr.getIconPackLabel(context, currentIconPack)

        val installedPacks = try {
            iconPackMgr.getInstalledIconPacks(context)
        } catch (e: Throwable) {
            emptyList()
        }

        val iconPackOptions = mutableListOf<OptionItem>()
        iconPackOptions.add(OptionItem(IconPackManager.SYSTEM_AOSP, "Default Sistem / AOSP"))
        for (pack in installedPacks) {
            iconPackOptions.add(OptionItem(pack.packageName, pack.label))
        }

        val currentSize = "${prefs.iconSize} dp"
        val iconSizeOptions = listOf(
            OptionItem("32", "32 dp (Sangat Kecil)"),
            OptionItem("48", "48 dp (Standar AOSP)"),
            OptionItem("56", "56 dp (Sedang)"),
            OptionItem("64", "64 dp (Besar)"),
            OptionItem("72", "72 dp (Sangat Besar)")
        )

        val currentLabelSize = "${prefs.labelSize.toInt()} sp"
        val labelSizeOptions = listOf(
            OptionItem("10", "10 sp (Kecil)"),
            OptionItem("12", "12 sp (Standar)"),
            OptionItem("14", "14 sp (Sedang)"),
            OptionItem("16", "16 sp (Besar)")
        )

        return ParentNode(
            id = "parent_icons",
            title = "Ikon & Label Aplikasi",
            subtitle = "Paket ikon, ukuran bitmap, sakelar nama, dan tipografi label",
            iconRes = R.drawable.ic_palette,
            isExpanded = true,
            children = listOf(
                // [Jalur Class]: com.silauncer.cepat.settings.SettingsNodeFactory
                // [Penjelasan]: Node pemilihan paket ikon (Default System / AOSP atau Third-Party Icon Pack terinstal).
                createOptionNode(
                    id = "child_icon_pack",
                    parentId = "parent_icons",
                    title = "Paket Ikon (Icon Pack)",
                    subtitle = "Pilih tampilan ikon dari sistem AOSP atau Icon Pack terinstal",
                    currentValue = currentIconPack,
                    displayValue = iconPackDisplay,
                    options = iconPackOptions
                ) { key ->
                    prefs.iconPack = key
                    IconCache.clear()
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        DiskIconCache.clear(context)
                    }
                    IconPackManager.getInstance().clearCache()
                },
                createOptionNode(
                    id = "child_icon_size",
                    parentId = "parent_icons",
                    title = "Ukuran Ikon",
                    subtitle = "Dimensi rendering bitmap ikon pada layar",
                    currentValue = prefs.iconSize.toString(),
                    displayValue = currentSize,
                    options = iconSizeOptions
                ) { key ->
                    key.toIntOrNull()?.let { prefs.iconSize = it }
                },
                createSwitchNode(
                    id = "child_show_labels",
                    parentId = "parent_icons",
                    title = "Tampilkan Label Aplikasi",
                    subtitle = "Tampilkan teks nama di bawah setiap ikon aplikasi",
                    isChecked = prefs.showAppLabel
                ) { isChecked ->
                    prefs.showAppLabel = isChecked
                },
                createOptionNode(
                    id = "child_label_size",
                    parentId = "parent_icons",
                    title = "Ukuran Teks Label",
                    subtitle = "Ukuran font teks nama aplikasi",
                    currentValue = prefs.labelSize.toInt().toString(),
                    displayValue = currentLabelSize,
                    options = labelSizeOptions
                ) { key ->
                    key.toFloatOrNull()?.let { prefs.labelSize = it }
                }
            )
        )
    }

    // [Jalur Class]: com.silauncer.cepat.settings.SettingsNodeFactory
    // [Penjelasan]: Membangun kategori Bahasa Aplikasi (Language Settings) dengan pilihan bahasa antarmuka.
    private fun buildLanguageParent(): ParentNode {
        val currentLang = prefs.appLanguage
        val langDisplay = LanguageHelper.getLanguageDisplayName(currentLang)

        val langOptions = LanguageHelper.getSupportedLanguages().map { (code, name) ->
            OptionItem(code, name)
        }

        return ParentNode(
            id = "parent_language",
            title = "Bahasa Aplikasi",
            subtitle = "Pengaturan bahasa tampilan antarmuka launcher",
            iconRes = R.drawable.ic_language,
            isExpanded = true,
            children = listOf(
                createOptionNode(
                    id = "child_app_language",
                    parentId = "parent_language",
                    title = "Bahasa Tampilan",
                    subtitle = "Bahasa yang digunakan untuk menu dan pengaturan",
                    currentValue = currentLang,
                    displayValue = langDisplay,
                    options = langOptions
                ) { key ->
                    prefs.appLanguage = key
                    LanguageHelper.applyLanguage(context, key)
                }
            )
        )
    }

    private fun buildAppDrawerParent(): ParentNode {
        val currentSortMode = prefs.sortMode
        val sortDisplay = when (currentSortMode) {
            "a_z" -> "A - Z"
            "z_a" -> "Z - A"
            "custom" -> "Kustom"
            else -> currentSortMode
        }

        val sortOptions = listOf(
            OptionItem("a_z", "Alfabetik (A - Z)"),
            OptionItem("z_a", "Alfabetik Terbalik (Z - A)"),
            OptionItem("custom", "Urutan Kustom (Drag & Drop)")
        )

        return ParentNode(
            id = "parent_drawer",
            title = "Laci Aplikasi & Urutan",
            subtitle = "Mode pengurutan aplikasi dan visibilitas aplikasi tersembunyi",
            iconRes = R.drawable.ic_sort,
            isExpanded = true,
            children = listOf(
                createOptionNode(
                    id = "child_sort_mode",
                    parentId = "parent_drawer",
                    title = "Mode Pengurutan",
                    subtitle = "Aturan urutan posisi aplikasi pada kisi",
                    currentValue = currentSortMode,
                    displayValue = sortDisplay,
                    options = sortOptions
                ) { key ->
                    prefs.sortMode = key
                },
                ActionChildNode(
                    id = "child_hidden_apps",
                    parentId = "parent_drawer",
                    title = "Kelola Aplikasi Tersembunyi",
                    subtitle = "Pilih aplikasi yang tidak ingin ditampilkan pada layar utama",
                    iconRes = R.drawable.ic_apps,
                    onAction = onManageHiddenApps
                )
            )
        )
    }

    private fun buildMaintenanceParent(): ParentNode {
        return ParentNode(
            id = "parent_maintenance",
            title = "Pemeliharaan & Reset",
            subtitle = "Kembalikan seluruh tata letak dan preferensi ke default",
            iconRes = R.drawable.ic_restore,
            isExpanded = false,
            children = listOf(
                ActionChildNode(
                    id = "child_reset_layout",
                    parentId = "parent_maintenance",
                    title = "Reset Tata Letak",
                    subtitle = "Mengembalikan pengaturan kisi, ukuran, dan spasi ke default",
                    iconRes = R.drawable.ic_restore,
                    onAction = onResetLayout
                )
            )
        )
    }
}
