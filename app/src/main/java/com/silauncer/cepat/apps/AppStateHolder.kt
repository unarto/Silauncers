package com.silauncer.cepat.apps

import android.os.UserHandle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// [Jalur Class]: com.silauncer.cepat.apps.AppStateHolder
// [Penjelasan]: Mengelola cache memori thread-safe untuk daftar aplikasi yang terinstal menggunakan Mutex dan struktur data HashSet terindeks untuk mengeliminasi alokasi koleksi perantara (intermediate collections) saat getApps, setApps, addApps, dan removePackage.
class AppStateHolder {
    private val apps = ArrayList<AppInfo>()
    private val appKeys = HashSet<String>()
    private val mutex = Mutex()

    // [Jalur Class]: com.silauncer.cepat.apps.AppStateHolder
    // [Penjelasan]: Mengembalikan snapshot daftar aplikasi terinstal yang aman dari modifikasi konkuren di bawah proteksi Mutex.
    suspend fun getApps(): List<AppInfo> = mutex.withLock {
        ArrayList(apps)
    }

    // [Jalur Class]: com.silauncer.cepat.apps.AppStateHolder
    // [Penjelasan]: Memperbarui seluruh daftar aplikasi dengan deduplikasi O(1) berbasis cacheKey tanpa alokasi koleksi perantara (distinctBy).
    suspend fun setApps(newApps: List<AppInfo>) {
        mutex.withLock {
            apps.clear()
            appKeys.clear()
            for (i in 0 until newApps.size) {
                val app = newApps[i]
                if (appKeys.add(app.cacheKey)) {
                    apps.add(app)
                }
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.apps.AppStateHolder
    // [Penjelasan]: Menambahkan aplikasi baru secara efisien dalam satu pass O(N) dengan pengecekan O(1) via appKeys, mengeliminasi alokasi intermediate Set/List (map, toSet, distinctBy, filter).
    suspend fun addApps(newApps: List<AppInfo>): List<AppInfo> {
        val added = ArrayList<AppInfo>()
        mutex.withLock {
            for (i in 0 until newApps.size) {
                val app = newApps[i]
                if (appKeys.add(app.cacheKey)) {
                    apps.add(app)
                    added.add(app)
                }
            }
        }
        return added
    }

    // [Jalur Class]: com.silauncer.cepat.apps.AppStateHolder
    // [Penjelasan]: Menghapus seluruh entri komponen yang cocok dengan packageName dan user secara in-place via iterator sambil menjaga sinkronisasi appKeys dan apps tanpa alokasi memori tambahan.
    suspend fun removePackage(packageName: String, user: UserHandle) {
        mutex.withLock {
            val iterator = apps.iterator()
            while (iterator.hasNext()) {
                val app = iterator.next()
                if (app.user == user && app.packageName == packageName) {
                    appKeys.remove(app.cacheKey)
                    iterator.remove()
                }
            }
        }
    }
}
