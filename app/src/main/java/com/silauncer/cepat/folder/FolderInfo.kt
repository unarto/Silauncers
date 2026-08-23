package com.silauncer.cepat.folder

import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * FolderInfo
 *
 * Single Responsibility:
 * Data model murni untuk menyimpan state folder (id, title, dan koleksi AppInfo serta Shortcut).
 * Menyediakan fungsi manipulasi item internal (add, remove, reorder, rename)
 * dan event listener untuk mengamati perubahan state tanpa ketergantungan logika UI.
 */
class FolderInfo(
    val id: String = UUID.randomUUID().toString(),
    initialTitle: String = "",
    initialContents: List<AppInfo> = emptyList(),
    initialShortcuts: List<WorkspaceShortcutInfo> = emptyList()
) {
    /**
     * Interface listener untuk mengamati perubahan internal pada folder.
     */
    interface FolderListener {
        fun onTitleChanged(newTitle: String) {}
        fun onItemsChanged() {}
        fun onItemAdded(item: AppInfo, rank: Int) {}
        fun onItemRemoved(item: AppInfo) {}
        fun onShortcutAdded(shortcut: WorkspaceShortcutInfo, rank: Int) {}
        fun onShortcutRemoved(shortcut: WorkspaceShortcutInfo) {}
    }

    var title: String = initialTitle
        private set
        
    private val contents: MutableList<AppInfo> = ArrayList(initialContents)
    private val shortcutContents: MutableList<WorkspaceShortcutInfo> = ArrayList(initialShortcuts)
    private val listeners: MutableList<FolderListener> = CopyOnWriteArrayList()

    /**
     * Mengubah nama judul folder dan memberi tahu seluruh listener yang terdaftar.
     */
    fun setTitle(newTitle: String) {
        if (title != newTitle) {
            title = newTitle
            listeners.forEach { it.onTitleChanged(newTitle) }
        }
    }

    /**
     * Menambahkan aplikasi baru ke dalam folder.
     * Jika [rank] negatif atau melebihi batas, item ditambahkan di akhir.
     */
    fun add(item: AppInfo, rank: Int = -1) {
        val targetIndex = if (rank < 0 || rank > contents.size) {
            contents.size
        } else {
            rank
        }
        contents.add(targetIndex, item)
        listeners.forEach {
            it.onItemAdded(item, targetIndex)
            it.onItemsChanged()
        }
    }

    fun addShortcut(shortcut: WorkspaceShortcutInfo, rank: Int = -1) {
        val targetIndex = if (rank < 0 || rank > shortcutContents.size) {
            shortcutContents.size
        } else {
            rank
        }
        shortcutContents.add(targetIndex, shortcut)
        listeners.forEach {
            it.onShortcutAdded(shortcut, targetIndex)
            it.onItemsChanged()
        }
    }

    /**
     * Menambahkan sekumpulan aplikasi ke dalam folder sekaligus.
     */
    fun addAll(items: Collection<AppInfo>) {
        if (items.isEmpty()) return
        contents.addAll(items)
        listeners.forEach { it.onItemsChanged() }
    }

    /**
     * Menghapus aplikasi tertentu dari dalam folder.
     */
    fun remove(item: AppInfo): Boolean {
        val removed = contents.remove(item)
        if (removed) {
            listeners.forEach {
                it.onItemRemoved(item)
                it.onItemsChanged()
            }
        }
        return removed
    }

    fun removeShortcut(shortcut: WorkspaceShortcutInfo): Boolean {
        val removed = shortcutContents.remove(shortcut)
        if (removed) {
            listeners.forEach {
                it.onShortcutRemoved(shortcut)
                it.onItemsChanged()
            }
        }
        return removed
    }

    /**
     * Menghapus aplikasi pada indeks tertentu.
     */
    fun removeAt(index: Int): AppInfo {
        val removed = contents.removeAt(index)
        listeners.forEach {
            it.onItemRemoved(removed)
            it.onItemsChanged()
        }
        return removed
    }

    /**
     * Mengubah urutan item di dalam folder dari [fromPosition] ke [toPosition].
     */
    fun reorder(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 ||
            fromPosition >= contents.size || toPosition >= contents.size ||
            fromPosition == toPosition
        ) {
            return
        }
        val item = contents.removeAt(fromPosition)
        contents.add(toPosition, item)
        listeners.forEach { it.onItemsChanged() }
    }

    /**
     * Menghapus semua aplikasi yang berasal dari package tertentu (misal saat uninstalled).
     */
    fun removePackage(packageName: String): Boolean {
        val initialSize = contents.size
        contents.removeAll { it.packageName == packageName }
        
        val initialShortcutSize = shortcutContents.size
        shortcutContents.removeAll { it.packageName == packageName }
        
        val changed = contents.size != initialSize || shortcutContents.size != initialShortcutSize
        if (changed) {
            listeners.forEach { it.onItemsChanged() }
        }
        return changed
    }

    /**
     * Mengambil snapshot immutable dari daftar aplikasi di dalam folder.
     */
    fun getItems(): List<AppInfo> = ArrayList(contents)

    fun getShortcuts(): List<WorkspaceShortcutInfo> = ArrayList(shortcutContents)

    fun getAllItems(): List<com.silauncer.cepat.launcher.LauncherItem> {
        val list = mutableListOf<com.silauncer.cepat.launcher.LauncherItem>()
        list.addAll(contents.map { com.silauncer.cepat.launcher.LauncherItem.App(it) })
        list.addAll(shortcutContents.map { com.silauncer.cepat.launcher.LauncherItem.Shortcut(it) })
        return list
    }

    /**
     * Mengambil item pada posisi tertentu.
     */
    fun getItem(index: Int): AppInfo? {
        return if (index in 0 until contents.size) contents[index] else null
    }

    /**
     * Memeriksa apakah folder tidak memiliki isi item aplikasi.
     */
    fun isEmpty(): Boolean = contents.isEmpty()

    fun isShortcutsEmpty(): Boolean = shortcutContents.isEmpty()

    /**
     * Mengembalikan jumlah item aplikasi di dalam folder.
     */
    fun itemCount(): Int = contents.size + shortcutContents.size

    /**
     * Memeriksa apakah aplikasi tertentu ada di dalam folder.
     */
    fun contains(item: AppInfo): Boolean = contents.contains(item)

    /**
     * Memeriksa apakah ada aplikasi dari package tertentu di dalam folder.
     */
    fun hasPackage(packageName: String): Boolean = contents.any { it.packageName == packageName } || shortcutContents.any { it.packageName == packageName }

    /**
     * Mengganti instance AppInfo lama dengan instance baru yang telah dikloning.
     */
    fun replaceItem(oldItem: AppInfo, newItem: AppInfo): Boolean {
        val index = contents.indexOf(oldItem)
        if (index != -1) {
            contents[index] = newItem
            listeners.forEach { it.onItemsChanged() }
            return true
        }
        return false
    }

    /**
     * Memeriksa apakah folder memenuhi syarat untuk dibubarkan otomatis (Auto-Dissolve)
     * saat sisa item di dalam folder <= 1.
     */
    fun shouldAutoDissolve(): Boolean = (contents.size + shortcutContents.size) <= 1

    /**
     * Mengambil satu aplikasi yang tersisa saat folder menyisakan tepat 1 item.
     */
    fun getSingleRemainingApp(): AppInfo? = if (contents.size == 1 && shortcutContents.isEmpty()) contents[0] else null

    fun getSingleRemainingShortcut(): WorkspaceShortcutInfo? = if (shortcutContents.size == 1 && contents.isEmpty()) shortcutContents[0] else null

    /**
     * Mendaftarkan listener untuk memantau perubahan state folder.
     */
    fun addListener(listener: FolderListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * Melepas listener dari daftar observasi.
     */
    fun removeListener(listener: FolderListener) {
        listeners.remove(listener)
    }

    /**
     * Membersihkan semua listener terdaftar.
     */
    fun clearListeners() {
        listeners.clear()
    }
}
