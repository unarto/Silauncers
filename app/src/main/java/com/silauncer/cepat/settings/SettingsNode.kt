// [Jalur Class]: com.silauncer.cepat.settings.SettingsNode
// [Tanggung Jawab SRP]: Definisi struktur model data hierarki TreeView (Parent Node, Child Option, Child Switch, Child Action).
package com.silauncer.cepat.settings

/**
 * SettingsNode
 *
 * Representasi node dalam hierarki TreeView Pengaturan.
 */
sealed class SettingsNode {
    abstract val id: String

    /**
     * ParentNode: Kategori utama pengaturan yang menampung sub-menu turunan (Child Nodes)
     * dan dapat di-expand/collapse secara vertikal.
     */
    data class ParentNode(
        override val id: String,
        val title: String,
        val subtitle: String,
        val iconRes: Int,
        var isExpanded: Boolean = true,
        val children: List<ChildNode>
    ) : SettingsNode()

    /**
     * ChildNode: Sub-item di dalam kategori parent.
     */
    sealed class ChildNode : SettingsNode() {
        abstract val parentId: String
        abstract val title: String
        abstract val subtitle: String?

        /**
         * OptionChildNode: Item konfigurasi dengan pilihan jamak (misal: ukuran grid, ukuran ikon, spasi, sorting).
         */
        data class OptionChildNode(
            override val id: String,
            override val parentId: String,
            override val title: String,
            override val subtitle: String? = null,
            val currentValue: String,
            val displayValue: String,
            val options: List<OptionItem>,
            val onSelected: (OptionItem) -> Unit
        ) : ChildNode()

        /**
         * SwitchChildNode: Item konfigurasi tunggal dengan sakelar toggle (SwitchMaterial / SwitchCompat) ON/OFF.
         */
        data class SwitchChildNode(
            override val id: String,
            override val parentId: String,
            override val title: String,
            override val subtitle: String? = null,
            val isChecked: Boolean,
            val onCheckedChange: (Boolean) -> Unit
        ) : ChildNode()

        /**
         * ActionChildNode: Item tindakan yang memicu dialog aksi atau eksekusi fungsi (misal: Kelola Aplikasi Tersembunyi, Reset Tata Letak).
         */
        data class ActionChildNode(
            override val id: String,
            override val parentId: String,
            override val title: String,
            override val subtitle: String? = null,
            val iconRes: Int? = null,
            val onAction: () -> Unit
        ) : ChildNode()
    }

    /**
     * OptionItem: Item pilihan individual untuk dialog pemilih opsi.
     */
    data class OptionItem(
        val key: String,
        val label: String
    )
}
