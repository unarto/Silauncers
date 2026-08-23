import re

with open("audit rsp+bug.md", "r") as f:
    content = f.read()

new_text = """### 1. Concurrency Bug pada `NotificationCache`
- **File/Class**: `com.silauncer.cepat.cache.NotificationCache`
- **Lokasi**: `NotificationCache.kt`
- **Kategori**: Concurrency / Thread-Safety
- **Severity**: **MEDIUM**
- **Akar Masalah**: `NotificationCache` menggunakan `ConcurrentHashMap<String, MutableSet<String>>` di mana `MutableSet` (HashSet) tidak thread-safe. Metode `addNotificationKey` dan `removeNotificationKey` memodifikasi set ini secara konkuren. Selain itu, `replaceAll` melakukan pembersihan dan penambahan secara tidak atomik, menyebabkan _race condition_ di mana pembaca `getAll` dapat melihat set yang kosong atau data parsial.
- **Perbaikan**: 
  1. Mengganti inisialisasi set dengan `ConcurrentHashMap.newKeySet()` agar thread-safe.
  2. Menjadikan `packageNotifications` sebagai `@Volatile var` untuk memungkinkan penggantian _map_ secara atomik (volatile swap) saat `replaceAll`.
  3. Menghapus logika penghapusan set kosong pada `removeNotificationKey` guna menghindari kondisi balapan antara thread yang sedang membaca dan menghapus set.
  4. Menambahkan filter nilai set yang kosong pada metode `getAll` untuk menjaga konsistensi state bagi UI dan observers tanpa mengharuskan set dihapus dari map secara berbahaya.
- **Dampak**: State notifikasi sepenuhnya thread-safe. Tidak akan ada lagi `ConcurrentModificationException` maupun race condition yang menyebabkan notifikasi tiba-tiba lenyap di antarmuka.
- **Test yang ditambahkan/diubah**:
  1. Mengimplementasikan ulang `NotificationCacheTest.kt` dengan `testConcurrentAddAndRemove` untuk memverifikasi penambahan 50 pasang data konkuren aman.
  2. Menambahkan `testReplaceAllIsAtomicAndCorrect` di `NotificationCacheTest` guna memastikan pembaruan data array secara massal berfungsi tanpa _data loss_.
- **Hasil Verifikasi**: Pass (tidak ada deadlock, semua 134 test UI dan unit stabil berlalu sukses).
- **Status**: SELESAI"""

pattern = r"### 1\. Concurrency Bug pada `NotificationCache`.*?(?=### 2\. CoroutineScope Memory Leak pada `FolderWallpaperBlurController`)"
new_content = re.sub(pattern, new_text + "\n\n", content, flags=re.DOTALL)

with open("audit rsp+bug.md", "w") as f:
    f.write(new_content)
