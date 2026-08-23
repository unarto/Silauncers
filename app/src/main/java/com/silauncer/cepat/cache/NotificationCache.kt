package com.silauncer.cepat.cache

import java.util.concurrent.ConcurrentHashMap

// [Jalur Class]: com.silauncer.cepat.cache.NotificationCache
// [Penjelasan]: Thread-safe in-memory cache untuk memetakan nama package ke himpunan key notifikasi aktif (Notification Dots).
object NotificationCache {
    // [Jalur Class]: com.silauncer.cepat.cache.NotificationCache
    // [Penjelasan]: Menggunakan @Volatile var agar referensi map dapat diganti secara atomik pada operasi replaceAll, mencegah thread pembaca (getAll) melihat data parsial saat sedang direfresh.
    @Volatile
    private var packageNotifications = ConcurrentHashMap<String, MutableSet<String>>()

    fun getKeysForPackage(packageName: String): MutableSet<String> {
        // [Jalur Class]: com.silauncer.cepat.cache.NotificationCache
        // [Penjelasan]: Menggunakan ConcurrentHashMap.newKeySet() sebagai pengganti mutableSetOf() agar setiap elemen set thread-safe terhadap modifikasi konkuren.
        return packageNotifications.getOrPut(packageName) { ConcurrentHashMap.newKeySet() }
    }

    fun getAll(): Map<String, Set<String>> {
        // [Jalur Class]: com.silauncer.cepat.cache.NotificationCache
        // [Penjelasan]: Mengembalikan snapshot read-only. Memfilter entri dengan set yang kosong agar sesuai dengan behavior lama dan state UI tidak memuat package tanpa notifikasi.
        return packageNotifications.filterValues { it.isNotEmpty() }.mapValues { it.value.toSet() }
    }

    fun addNotificationKey(packageName: String, key: String): Boolean {
        val keys = getKeysForPackage(packageName)
        return keys.add(key)
    }

    fun removeNotificationKey(packageName: String, key: String): Boolean {
        val keys = packageNotifications[packageName] ?: return false
        // [Jalur Class]: com.silauncer.cepat.cache.NotificationCache
        // [Penjelasan]: Menghapus pemanggilan packageNotifications.remove(packageName) saat keys.isEmpty() 
        // untuk mencegah race condition di mana thread lain mungkin baru saja mendapatkan referensi set ini 
        // sebelum dihapus dari map. Set kosong dibiarkan dan akan dibersihkan utuh saat removePackage dipanggil (uninstall).
        return keys.remove(key)
    }

    fun removePackage(packageName: String) {
        packageNotifications.remove(packageName)
    }

    fun replaceAll(newMap: Map<String, Set<String>>) {
        // [Jalur Class]: com.silauncer.cepat.cache.NotificationCache
        // [Penjelasan]: Membuat instance ConcurrentHashMap baru dan memindah data untuk kemudian ditukar secara atomik (volatile swap). 
        // Ini memastikan pemanggil getAll() tidak akan membaca cache dalam kondisi kosong/parsial saat refresh sedang berjalan.
        val newCache = ConcurrentHashMap<String, MutableSet<String>>()
        for ((pkg, keys) in newMap) {
            val newSet = ConcurrentHashMap.newKeySet<String>()
            newSet.addAll(keys)
            newCache[pkg] = newSet
        }
        packageNotifications = newCache
    }

    fun clear() {
        packageNotifications.clear()
    }
}
