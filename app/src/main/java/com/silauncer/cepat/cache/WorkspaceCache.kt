package com.silauncer.cepat.cache

import com.silauncer.cepat.launcher.LauncherItem

// [Jalur Class]: com.silauncer.cepat.cache.WorkspaceCache
// [Penjelasan]: In-memory cache untuk menampung layout item workspace yang aktif guna mengurangi operasi baca I/O berulang ke database Room.
object WorkspaceCache {
    @Volatile
    private var cachedWorkspace: List<LauncherItem>? = null

    fun get(): List<LauncherItem>? = cachedWorkspace

    fun set(workspace: List<LauncherItem>) {
        cachedWorkspace = ArrayList(workspace)
    }

    /**
     * Mengosongkan cache layout workspace saat terjadi perubahan susunan ikon, folder, atau paket aplikasi.
     */
    fun invalidate() {
        cachedWorkspace = null
    }

    fun clear() {
        invalidate()
    }
}
