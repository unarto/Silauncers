# AUDIT GLOBAL — TAHAP 4

## STATUS
AUDIT: SELESAI
IMPLEMENTASI: BELUM DIMULAI

## ATURAN PENGERJAAN
Setiap temuan akan dikerjakan secara bertahap.
Satu instruksi hanya boleh menangani temuan yang disebutkan.
Temuan lain tidak boleh disentuh.

---

# TEMUAN

## [HIGH #1] Disk I/O Block pada Main Thread & Duplikasi Invalidation

**Status:** TERKONFIRMASI

**File:**
`app/src/main/java/com/silauncer/cepat/apps/AppChangeReceiver.kt`
`app/src/main/java/com/silauncer/cepat/launcher/LauncherAppController.kt`
`app/src/main/java/com/silauncer/cepat/cache/DiskIconCache.kt`

**Class/Method:**
`AppChangeReceiver.onReceive()`, `LauncherAppController.handlePackageEvent()`, `DiskIconCache.removePackage()`

**Root Cause:**
Fungsi `DiskIconCache.removePackage` melakukan penghapusan file di memori disk (`java.io.File.delete`) secara sinkron. Namun, fungsi ini dipanggil langsung di `AppChangeReceiver.onReceive` (Main Thread) serta dipanggil kembali secara redundan di `LauncherAppController.handlePackageEvent` (yang dieksekusi di `lifecycleScope.launch` tanpa konteks `Dispatchers.IO`).

**Alur Masalah:**
Saat sistem memancarkan Intent `ACTION_PACKAGE_REMOVED`, `AppChangeReceiver.onReceive` memanggil blok `DiskIconCache.removePackage` -> UI Thread terblokir untuk melakukan disk I/O -> Receiver mendelegasikan _callback_ ke `LauncherActivity` -> dipanggil di `lifecycleScope.launch` (Main Thread) -> `handlePackageEvent` memanggil _lagi_ `DiskIconCache.removePackage`.

**Dampak:**
Berpotensi memicu ANR (Application Not Responding) atau _frame drop_ ketika sistem menghapus paket yang memiliki banyak cache ikon di perangkat berspesifikasi disk lambat, serta menyalahi prinsip DRY karena pembersihan cache dipanggil di dua tempat berbeda.

**Bukti:**
- `AppChangeReceiver.kt` Baris 41: `com.silauncer.cepat.cache.DiskIconCache.removePackage(context, packageName)`
- `DiskIconCache.kt` Baris 56: `file.delete()`

**Rekomendasi:**
Pindahkan operasi disk I/O ke Coroutine `Dispatchers.IO`. Hapus salah satu pemanggilan redundan (lebih baik dipusatkan pada `LauncherAppController.handlePackageEvent` dengan `withContext(Dispatchers.IO)` atau biarkan di `AppChangeReceiver` namun delegasikan ke scope background yang aman).

**Scope Perubahan:**
`AppChangeReceiver.kt`, `LauncherAppController.kt`

**Risiko Perbaikan:**
MEDIUM

**Status Pengerjaan:**
[ ] BELUM DIKERJAKAN
[ ] IMPLEMENTASI SELESAI
[ ] VERIFIED
[ ] CLOSED

---

## [LOW #1] API getParcelableArrayList Deprecated

**Status:** TERKONFIRMASI

**File:**
`app/src/main/java/com/silauncer/cepat/notification/NotificationKeyData.kt`

**Class/Method:**
`NotificationKeyData.Companion.fromNotification()`

**Root Cause:**
Menggunakan pemanggilan fungsi `Bundle.getParcelableArrayList()` yang telah berstatus *deprecated* sejak API 33 tanpa ada kompatibilitas turunan atau percabangan SDK.

**Alur Masalah:**
Pengecekan `notif.extras?.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)` memberikan warning kompilasi dan risiko *type-safety* di masa mendatang.

**Dampak:**
Lint warning, risiko kompabilitas tipe pada versi OS baru.

**Bukti:**
- `NotificationKeyData.kt` Baris 33: `notif.extras?.getParcelableArrayList<Person>(...)`

**Rekomendasi:**
Ganti pemanggilan dengan metode `androidx.core.os.BundleCompat.getParcelableArrayList` atau buat ekstensi yang memanfaatkan tipe yang lebih aman.

**Scope Perubahan:**
`NotificationKeyData.kt`

**Risiko Perbaikan:**
LOW

**Status Pengerjaan:**
[ ] BELUM DIKERJAKAN
[ ] IMPLEMENTASI SELESAI
[ ] VERIFIED
[ ] CLOSED

---

## [LOW #2] Redundant & Deprecated Window Transparency API

**Status:** TERKONFIRMASI

**File:**
`app/src/main/java/com/silauncer/cepat/settings/SettingsWallpaperHelper.kt`

**Class/Method:**
`SettingsWallpaperHelper.applyWindowTransparency()`

**Root Cause:**
Pemanggilan manual `window.setDecorFitsSystemWindows(false)` dan manipulasi bit mask `systemUiVisibility` dilakukan di bagian bawah kode, padahal baris sebelumnya telah memanggil `WindowCompat.setDecorFitsSystemWindows(window, false)` yang sejatinya sudah mengatasi kompatibilitas API secara internal. Penggunaan `window.statusBarColor` juga dilabeli _deprecated_ (dalam konteks edge-to-edge).

**Alur Masalah:**
Duplikasi setting flag _fullscreen_. Logika IF-ELSE untuk `SDK_INT >= Build.VERSION_CODES.R` merupakan langkah yang redundan terhadap fungsi `WindowCompat` yang baru saja dieksekusi.

**Dampak:**
DRY violation dan penumpukan _warning_ deprecation saat build.

**Bukti:**
- `SettingsWallpaperHelper.kt` Baris 37, 39, 47, 50.

**Rekomendasi:**
Hapus blok redundan dan manipulasi manual `systemUiVisibility`. Biarkan `WindowCompat` menangani *insets* seutuhnya secara aman.

**Scope Perubahan:**
`SettingsWallpaperHelper.kt`

**Risiko Perbaikan:**
LOW

**Status Pengerjaan:**
[ ] BELUM DIKERJAKAN
[ ] IMPLEMENTASI SELESAI
[ ] VERIFIED
[ ] CLOSED

---

## [LOW #3] View.invalidate(Rect) Deprecated di Canvas API

**Status:** TERKONFIRMASI

**File:**
`app/src/main/java/com/silauncer/cepat/keyboard/ItemFocusIndicatorHelper.kt`

**Class/Method:**
`ItemFocusIndicatorHelper.invalidateDirty()`

**Root Cause:**
Pemanggilan fungsi `invalidate(Rect)` pada view/container telah di-_deprecate_ sejak API 30+ karena hardware acceleration modern tidak lagi memperoleh manfaat signifikan dari lokalisasi invalidasi _dirty rect_, dan justru bisa mendegradasi konsistensi rendering UI.

**Alur Masalah:**
`onAnimationUpdate` selalu memicu `invalidateDirty()` yang memanggil `container.invalidate(dirtyRect)`.

**Dampak:**
Peringatan _deprecated_ dan potensi deviasi rendering pada level OS yang lebih tinggi.

**Bukti:**
- `ItemFocusIndicatorHelper.kt` Baris 66 & 71.

**Rekomendasi:**
Ganti pemanggilan dengan `container.invalidate()` penuh (tanpa parameter `Rect`).

**Scope Perubahan:**
`ItemFocusIndicatorHelper.kt`

**Risiko Perbaikan:**
LOW

**Status Pengerjaan:**
[ ] BELUM DIKERJAKAN
[ ] IMPLEMENTASI SELESAI
[ ] VERIFIED
[ ] CLOSED

---

## [LOW #4] API scaledDensity Deprecated

**Status:** TERKONFIRMASI

**File:**
`app/src/main/java/com/silauncer/cepat/folder/FolderPager.kt`

**Class/Method:**
`FolderPager.bind()`

**Root Cause:**
Akses properti `scaledDensity` pada `DisplayMetrics` telah di-_deprecate_. Akses langsung ini tidak lagi direkomendasikan karena perilaku _font scaling_ di versi Android terbaru menjadi non-linear.

**Alur Masalah:**
Ukuran label didapatkan dari perhitungan manual menggunakan pembagian dengan `resources.displayMetrics.scaledDensity`.

**Dampak:**
Lint warning dan potensi bug penskalaan teks jika pengguna mengaktifkan font _non-linear_ di OS terbaru.

**Bukti:**
- `FolderPager.kt` Baris 43: `val itemLabelSizeSp = resources.getDimension(...) / resources.displayMetrics.scaledDensity`

**Rekomendasi:**
Sederhanakan dengan fungsi resolusi dimensi resource modern tanpa harus menghitung deviasi _scaledDensity_ secara manual.

**Scope Perubahan:**
`FolderPager.kt`

**Risiko Perbaikan:**
LOW

**Status Pengerjaan:**
[ ] BELUM DIKERJAKAN
[ ] IMPLEMENTASI SELESAI
[ ] VERIFIED
[ ] CLOSED
