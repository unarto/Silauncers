# Audit Global Silauncer — Perbandingan dengan Launcher3 (AOSP)

Dokumen ini mencatat temuan audit arsitektur, Single Responsibility Principle (SRP), keandalan kode, performa, manajemen memori, batas database/MMKV/cache, konkurensi, dan pemisahan UI vs Business Logic secara menyeluruh dan bertahap.

---

## TAHAP 1: Core Foundation & Data/Apps Layer (`pm`, `apps`, `database`, `storage`, `cache`)

### 1. Inkonsistensi Kunci Cache Aplikasi (`cacheKey`) [STATUS: SELESAI / RESOLVED]
- **File/Class**: `com.silauncer.cepat.apps.AppInfo` & `com.silauncer.cepat.apps.AppDataSource`
- **Lokasi**: `AppInfo.kt:45-50` vs `AppDataSource.kt:30-34`
- **Kategori**: Bug & Cache Boundary
- **Severity**: **HIGH**
- **Masalah**:
  - Di `AppInfo.kt`, format kunci cache didefinisikan sebagai:
    `"$packageName/${componentName.className}_$serial"` (menggunakan pemisah `/` dan serial user).
  - Di `AppDataSource.kt`, format kunci cache untuk `AppCache` dibuat sebagai:
    `"${component.packageName}_${component.className}_${user.hashCode()}"` (menggunakan pemisah `_` dan hashCode user).
  - Di `AppCache.kt`, invalidasi package mencari key dengan `it.startsWith(packageName)`.
  - Terjadi diskrepansi identitas item antara lapisan data source, domain model, dan `AppStateHolder`.
- **Dampak**: `AppCache` dan `IconCache` dapat mengalami cache miss yang tidak perlu atau data usang tidak ter-invalidasi dengan sempurna karena inkonsistensi string key.
- **Rekomendasi**: Standardisasi identitas komponen seperti pada Launcher3 (`com/android/launcher3/util/ComponentKey.java`) yang secara seragam menggunakan pasangan `(ComponentName, UserHandle)`.
- **Status Implementasi**:
  - **SELESAI**: Menambahkan metode factory terpusat `AppInfo.createCacheKey(packageName, className, user)` dan `AppInfo.createCacheKey(componentName, user)` di `AppInfo.kt`.
  - Mengintegrasikan `AppDataSource.kt` untuk menggunakan `AppInfo.createCacheKey(component, user)` yang identik 100% dengan `AppInfo.cacheKey`.
  - Memperbarui `AppCache.removePackage(packageName)` dengan pencocokan awalan berbasis pemisah eksplisit (`$packageName/` dan `${packageName}_`) untuk mengeliminasi false positive eviction.
  - Menambahkan unit test `testCacheKeyConsistency` di `AppCacheTest.kt` yang memvalidasi keselarasan key antara factory method dan model instance.

---

### 2. Implementasi Kosong pada Pembersihan Disk Cache Package (`DiskIconCache.removePackage`) [STATUS: SELESAI / RESOLVED]
- **File/Class**: `com.silauncer.cepat.cache.DiskIconCache`
- **Lokasi**: `DiskIconCache.kt:57-69`
- **Kategori**: Bug & Dead Code
- **Severity**: **HIGH**
- **Masalah**:
  - Fungsi `removePackage(context, packageName)` memiliki implementasi kosong (hanya iterasi `listFiles()` tanpa aksi penghapusan file):
    ```kotlin
    dir.listFiles()?.forEach { file ->
        // File disimpan berdasarkan hash key, pembersihan dilakukan berkala atau per-direktori
    }
    ```
- **Dampak**: Ketika suatu aplikasi diperbarui (misal icon aplikasi berubah di update baru) atau di-uninstall, file PNG icon lama tetap tersimpan permanen di disk cache. Saat aplikasi diinstal ulang atau dimuat dari disk, icon usang akan terus ditampilkan.
- **Rekomendasi**: Simpan metadata relasi `packageName` -> `cacheFile` atau simpan nama file disk cache dengan prefix `hash(packageName)_hash(className)` sehingga file cache per-paket dapat dihapus secara presisi saat event `ACTION_PACKAGE_REMOVED` atau `ACTION_PACKAGE_CHANGED`.
- **Status Implementasi**:
  - **SELESAI**: Mengubah format penyimpanan file disk cache menjadi `"${safePkg}_$hash.png"` dengan ekstraksi `packageName` secara otomatis dari key.
  - Mengimplementasikan penghapusan file nyata pada `DiskIconCache.removePackage(context, packageName)` dan menambahkan alias `hapusPaket(context, packageName)` untuk menghapus seluruh file bitmap yang berawalan package name target.
  - Mempertahankan backward compatibility untuk format nama file legacy (`$hash.png`) saat pembacaan via `DiskIconCache.get`.
  - Menambahkan pengujian unit komprehensif `DiskIconCacheTest.kt` yang memvalidasi operasi I/O file, isolasi penghapusan per-paket, dan alias `hapusPaket`.

---

### 3. Inisialisasi Parsial dan Potensi Panggilan IPC Berulang pada `UserCache` [STATUS: SELESAI / RESOLVED]
- **File/Class**: `com.silauncer.cepat.pm.UserCache`
- **Lokasi**: `UserCache.kt:20-157`
- **Kategori**: Memory & Performance / Concurrency
- **Severity**: **MEDIUM**
- **Masalah**:
  - `UserCache` hanya menginisialisasi map `users` dan `userToSerialMap` saat `addUserChangeListener()` dipanggil pertama kali.
  - Jika `UserCache.getInstance(context).getUserProfiles()` atau `getSerialNumberForUser()` dipanggil sebelum ada listener yang terdaftar, map internal bernilai `null` dan sistem selalu melakukan fallback panggilan IPC sinkron langsung ke sistem `UserManager.getUserProfiles` dan `UserManager.getSerialNumberForUser`.
- **Dampak**: Terjadi lonjakan overhead IPC ke `UserManager` saat inisialisasi awal launcher atau saat background query berlangsung.
- **Rekomendasi**: Lakukan inisialisasi awal (*eager load* atau thread-safe *lazy load*) struktur cache profil user pada saat konstruksi `UserCache`, seperti pola pada Launcher3 (`com/android/launcher3/pm/UserCache.java`).
- **Status Implementasi**:
  - **SELESAI**: Menambahkan blok `init { enableAndResetCache() }` pada `UserCache` untuk *eager initialization* memuat profil user & serial number secara otomatis saat instansiasi Singleton `UserCache`.
  - Memperbarui fungsi getter (`getSerialNumberForUser`, `getUserForSerialNumber`, `getUserProfiles`) dengan perlindungan *thread-safe lazy-load fallback* yang memicu `enableAndResetCache()` dan mendaftarkan entri baru jika entri yang diminta belum ada di cache.
  - Memastikan metode `removeUserChangeListener()` melepaskan broadcast receiver dan listener tanpa menghapus data cache `users` dan `userToSerialMap` dari memori.
  - Menambahkan unit test `testUserCacheEagerInitialization` dan pengujian persistensi cache pasca pelepasan listener di `PmPackageTest.kt`.

---

### 4. Bypassing Rekonsiliasi Database pada `WorkspaceRepository.loadWorkspace` [STATUS: SELESAI / RESOLVED]
- **File/Class**: `com.silauncer.cepat.database.WorkspaceRepository`
- **Lokasi**: `WorkspaceRepository.kt:100-308`
- **Kategori**: Lifecycle, Concurrency & State / Cache Boundary
- **Severity**: **MEDIUM**
- **Masalah**:
  - `WorkspaceRepository.loadWorkspace(allInstalledApps)` sebelumnya langsung mengembalikan `cached` list dari `WorkspaceCache.get()` jika tersedia.
  - Parameter `allInstalledApps` yang berisi daftar aplikasi terinstal aktual dari sistem dilewati tanpa rekonsiliasi jika cache bernilai tidak `null`.
- **Dampak**: Jika ada aplikasi baru yang terinstal atau aplikasi yang terhapus saat launcher berada di background tanpa melalui broadcast receiver (misal saat boot atau proses restart), daftar workspace tidak akan direkonsiliasi dengan benar sampai cache di-invalidate secara manual.
- **Rekomendasi**: Pisahkan fungsi `getCachedWorkspace(): List<LauncherItem>?` untuk akses cepat UI, dan pastikan `loadWorkspace` / `muatWorkspace(allInstalledApps)` melakukan validasi rekonsiliasi terhadap snapshot aplikasi terbaru saat pemuatan penuh.
- **Status Implementasi**:
  - **SELESAI**: Menambahkan fungsi ekstraksi kunci identitas aplikasi `extractAppKeys(items: List<LauncherItem>)` dan `getInstalledAppKeys(apps: List<AppInfo>)` pada `WorkspaceRepository`.
  - Pada `loadWorkspace(allInstalledApps)`, in-memory cache diverifikasi terlebih dahulu terhadap snapshot aplikasi `allInstalledApps`. Jika identitas seluruh aplikasi pada cache memori cocok persis dengan snapshot aplikasi yang terinstal, workspace langsung dikembalikan (Cache Hit) tanpa menyentuh Room DB.
  - Jika snapshot aplikasi berubah (ada aplikasi baru, dihapus, atau dimodifikasi) atau terjadi cache miss, sistem melakukan query ke Room Database, menjalankan rekonsiliasi cerdas (penempatan orphan apps, bubar folder otomatis, resolving pinned shortcut), dan memperbarui `WorkspaceCache.set()`.
  - Menambahkan fungsi `getCachedWorkspace(): List<LauncherItem>?` untuk inspeksi sinkron instan.
  - Menambahkan fungsi alias `simpanWorkspace(items)` dan `muatWorkspace(allInstalledApps)`.
  - Membuat unit test komprehensif `WorkspaceRepositoryTest.kt` yang menguji Cache Hit (snapshot tidak berubah), Cache Miss (invalidated), Penambahan aplikasi baru (orphan apps), Penghapusan aplikasi, Pembubaran folder otomatis saat child app dihapus, dan fungsi alias.

---

### 5. Strategi Persistensi Workspace Menghapus & Menulis Ulang Seluruh Baris Database (`saveWorkspace`) [STATUS: SELESAI / RESOLVED]
- **File/Class**: `com.silauncer.cepat.database.WorkspaceRepository` & `com.silauncer.cepat.database.entity.WorkspaceItemDao`
- **Lokasi**: `WorkspaceRepository.kt:20-145`
- **Kategori**: Database / Performance
- **Severity**: **MEDIUM**
- **Masalah**:
  - Setiap kali `saveWorkspace(items)` dipanggil (misal saat pengguna menggeser 1 ikon atau mengubah 1 folder), repositori mengeksekusi `dao.clearAll()` lalu melakukan `insertItems` untuk seluruh item dan child folder.
  - Anotasi `@Update` yang ada pada `WorkspaceItemDao` tidak pernah digunakan.
- **Dampak**: Menyebabkan disk I/O berlebih, fragmentasi SQLite, dan lonjakan waktu transaksi database seiring bertambahnya jumlah aplikasi dan folder di workspace, serta pertumbuhan nilai auto-increment primary key ID yang tidak terkendali.
- **Rekomendasi**: Terapkan *incremental update* atau *batch diff update* seperti pada Launcher3 `ModelWriter.java` di mana hanya item yang posisinya atau containernya berubah yang dieksekusi melalui operasi update SQL, item baru melalui insert, dan item yang dibuang melalui delete.
- **Status Implementasi**:
  - **SELESAI**: Mengubah alur persistensi `saveWorkspace(items)` menjadi berbasis **Batch Diff Incremental Update**:
    1. Membuat helper `getEntityMatchKey(entity)` untuk identifikasi unik item (App, Folder, Shortcut, root vs child folder).
    2. Menghitung delta (diff) antara kondisi database aktual dengan daftar target:
       - Item yang tidak berubah (rank & metadata identik): **0 write ke database**.
       - Item yang berpindah posisi / berubah judul: dieksekusi via `dao.updateItems(toUpdate)` dengan mempertahankan ID primary key SQLite eksisting.
       - Item baru: dieksekusi via `dao.insertItems(toInsert)`.
       - Item yang dihapus/dibuang: dieksekusi via `dao.deleteItem(item)`.
    3. `dao.clearAll()` dipertahankan secara aman dan hanya dieksekusi jika `items.isEmpty()` untuk pembersihan tabel menyeluruh.
    4. Menambahkan unit test komprehensif pada `WorkspaceRepositoryTest.kt` yang memvalidasi:
       - `testIncrementalSave_PreservesPrimaryKeysOnReorder`: ID primary key tetap utuh saat item di-reorder.
       - `testIncrementalSave_AddsNewItemAndDeletesRemovedItem`: Diff insert/delete presisi tanpa mengganggu ID item lain.
       - `testIncrementalSave_FolderTitleUpdate`: Pembaruan judul folder in-place tanpa menghapus child items.
       - `testSaveEmptyWorkspace_ClearsDatabase`: Pembersihan menyeluruh saat daftar workspace kosong.

---

### 6. Alokasi Objek `LauncherPreferences` Berulang [STATUS: SELESAI / RESOLVED]
- **File/Class**: `com.silauncer.cepat.storage.LauncherPreferences`
- **Lokasi**: `LauncherPreferences.kt:16-160`
- **Kategori**: Memory & Performance
- **Severity**: **LOW**
- **Masalah**:
  - `LauncherPreferences` didefinisikan sebagai kelas biasa tanpa singleton/komponen bersama. Di berbagai modul (`AppActionHandler`, `IconLoader`, dll.), kode memanggil `LauncherPreferences()` secara langsung.
- **Dampak**: Menyebabkan alokasi objek GC yang tidak perlu di memori meskipun MMKV C++ core mengelola file deskriptor secara internal.
- **Rekomendasi**: Jadikan `LauncherPreferences` sebagai singleton / object atau sediakan penyedia instansiasi terpusat.
- **Status Implementasi**:
  - **SELESAI**: Menyediakan thread-safe singleton instance accessor `LauncherPreferences.getInstance()` dan alias `LauncherPreferences.get()`.
  - Menerapkan lazy caching untuk instance `MMKV` di companion object (`cachedMMKV: MMKV? by lazy`) guna mengeliminasi pemanggilan native JNI `MMKV.mmkvWithID` berulang.
  - Memperbarui seluruh caller hotspot (`IconLoader`, `AppActionHandler`, `LauncherApplication`, `SecondaryDisplayPredictions`, `SecondaryDisplayLauncher`, `LauncherActivity`, `SettingsActivity`) agar menggunakan singleton accessor.
  - Mempertahankan public constructor `LauncherPreferences()` dan MMKV in-memory fallback map untuk kompatibilitas penuh.
  - Menambahkan suite unit test `LauncherPreferencesTest.kt` yang memvalidasi kesamaan identitas instance singleton, persistensi baca/tulis antar instance, tracking launch count, dan default values.

---

### 7. Alokasi Koleksi Berulang pada Operasi Konkurensi `AppStateHolder`
- **File/Class**: `com.silauncer.cepat.apps.AppStateHolder`
- **Lokasi**: `AppStateHolder.kt:11-32`
- **Kategori**: Memory & Performance
- **Severity**: **LOW**
- **Masalah**:
  - Setiap pemanggilan `getApps()` melakukan `apps.toList()`.
  - Pada `addApps()`, dilakukan operasi `map { it.cacheKey }.toSet()` dan `distinctBy` yang mengalokasikan koleksi sementara (List, Set, Iterator) di memori heap.
- **Dampak**: Potensi garbage collection (GC) churn saat pemindaian aplikasi dalam jumlah besar (100+ aplikasi).
- **Rekomendasi**: Gunakan koleksi yang dioptimalkan atau pertahankan lookup map `cacheKey -> AppInfo` di dalam `AppStateHolder` agar penambahan aplikasi baru hanya membutuhkan pemeriksaan O(1) tanpa membuat koleksi intermediate.

---

### 8. Penanganan Intent Launch Non-Activity Context pada `AppActionHandler`
- **File/Class**: `com.silauncer.cepat.apps.AppActionHandler`
- **Lokasi**: `AppActionHandler.kt:48-63`
- **Kategori**: Potential Bug
- **Severity**: **LOW**
- **Masalah**:
  - Pada fungsi `requestUninstall(app: AppInfo)`, Intent `ACTION_DELETE` diluncurkan melalui `context.startActivity(intent)`. Jika `context` yang dioper adalah Application Context, Android OS akan melempar `AndroidRuntimeException` (memerlukan flag `FLAG_ACTIVITY_NEW_TASK`).
- **Dampak**: Potensi crash saat memicu uninstall aplikasi jika context bukan merupakan instance `Activity`.
- **Rekomendasi**: Tambahkan flag `FLAG_ACTIVITY_NEW_TASK` pada intent uninstall atau pastikan context diverifikasi.

---

### 9. Kedipan Placeholder Ikon (Icon Placeholder Flashing) pada `IconLoader`
- **File/Class**: `com.silauncer.cepat.cache.IconLoader`
- **Lokasi**: `IconLoader.kt:66, 146`
- **Kategori**: UI ↔ Business Logic Separation / UX
- **Severity**: **LOW**
- **Masalah**:
  - `IconLoader.loadIconAsync` selalu memanggil `onLoaded(getDefaultIcon(context), cacheKey)` secara sinkron sebelum menjalankan coroutine async, bahkan ketika icon sebenarnya sudah tersedia di L2 Disk Cache dan siap dimuat dalam ~2ms.
- **Dampak**: Tampilan ikon di antarmuka dapat berkedip sekejap (default Android icon berganti ke icon aplikasi asli) saat scroll atau pemuatan awal.
- **Rekomendasi**: Evaluasi apakah view sudah memiliki icon yang valid atau gunakan transisi fade/crossfade halus tanpa mereset drawable secara mendadak jika key tidak berubah.

---

## LAPORAN VERIFIKASI TAHAP 1 (HIGH 1, HIGH 2, MEDIUM 1, MEDIUM 2, MEDIUM 3, LOW 1)

### Checklist Verifikasi Item:

1. **Konsistensi Pembentukan Cache Key (HIGH #1)**: `[PASS]`
   - `AppInfo.cacheKey` mendelegasikan langsung ke `AppInfo.createCacheKey(packageName, componentName.className, user)`.
   - `AppDataSource` menggunakan `AppInfo.createCacheKey(component, user)`.
   - Keduanya menghasilkan string berformat persis `"$packageName/${className}_$serial"`.

2. **Penggunaan Format Key Lama**: `[PASS]`
   - Format manual lama `"${component.packageName}_${component.className}_${user.hashCode()}"` sudah bersih dan tidak lagi digunakan di codebase.

3. **Isolasi Invalidasi Package pada `AppCache.removePackage`**: `[PASS]`
   - Pencocokan prefix menggunakan `"$packageName/"` dan `"${packageName}_"`, sehingga paket seperti `com.example.app` tidak akan salah menghapus `com.example.appplus` atau `com.example.application`.

4. **Penghapusan File Disk Nyata pada `DiskIconCache.removePackage` / `hapusPaket` (HIGH #2)**: `[PASS]`
   - DiskIconCache mengekstrak `safePkg` dan mencari file dengan prefix `"${safePkg}_"`. Seluruh file bitmap icon terkait package target berhasil dihapus secara fisik (`file.delete()`).

5. **Dukungan Legacy Cache pada `DiskIconCache.get`**: `[PASS]`
   - Jika file berprefix tidak ditemukan, sistem memeriksa keberadaan file legacy `"${hashKey(key)}.png"` dan mengembalikan drawable jika file ada.

6. **Inisialisasi & Fallback `UserCache` (MEDIUM #1)**: `[PASS]`
   - `UserCache` mengeksekusi `enableAndResetCache()` pada konstruksi awal `init {}`.
   - Fungsi query profil/serial memiliki penanganan *thread-safe lazy-load fallback* yang memuat data secara atomik jika terjadi cache miss, serta menyimpan entri baru tanpa memicu panggilan IPC berulang ke `UserManager`.
   - Pelepasan listener (`removeUserChangeListener`) tidak lagi mereset data cache `users` atau `userToSerialMap` menjadi `null`.

7. **Validasi Snapshot & Rekonsiliasi Workspace (MEDIUM #2)**: `[PASS]`
   - `WorkspaceRepository.loadWorkspace(allInstalledApps)` & `muatWorkspace(allInstalledApps)` membandingkan himpunan identitas aplikasi pada in-memory cache dengan snapshot `allInstalledApps`.
   - Jika snapshot identik, cache langsung dikembalikan (Cache Hit tanpa query DB).
   - Jika snapshot berubah atau cache miss, sistem memuat dari Room DB, melakukan rekonsiliasi penempatan aplikasi/folder, dan memperbarui `WorkspaceCache`.
   - `getCachedWorkspace()` tersedia untuk query sinkron instan.

8. **Batch Diff Incremental Persistence `saveWorkspace` (MEDIUM #3)**: `[PASS]`
   - Persistensi tidak lagi membabat habis seluruh tabel dengan `clearAll()` + `insertItems()`.
   - Menjalankan komparasi diff cerdas (`getEntityMatchKey`) antara state database eksisting dan target entities.
   - Menggunakan `@Update` (`dao.updateItems`) untuk item berpindah atau berubah properti dengan mempertahankan ID primary key SQLite.
   - Menggunakan `@Insert` (`dao.insertItems`) hanya untuk item baru.
   - Menggunakan `@Delete` (`dao.deleteItem`) hanya untuk item yang dihilangkan.
   - Menjaga `dao.clearAll()` secara tepat saat `items.isEmpty()`.

9. **Optimasi Lifecycle `LauncherPreferences` (LOW #1)**: `[PASS]`
   - Menyediakan thread-safe singleton accessor `LauncherPreferences.getInstance()` dan `LauncherPreferences.get()`.
   - Caching instance MMKV lazy di companion object mengeliminasi pemanggilan JNI/native berulang.
   - Memperbarui caller hotspot (`IconLoader`, `AppActionHandler`, `LauncherApplication`, `SecondaryDisplayPredictions`, `SecondaryDisplayLauncher`, `LauncherActivity`, `SettingsActivity`).
   - Retensi public constructor `LauncherPreferences()` dan map fallback untuk kompatibilitas penuh dan testability.

10. **Cakupan Unit Test**: `[PASS]`
   - `AppCacheTest.kt`: Konsistensi key factory vs model, isolasi eviction per-paket.
   - `DiskIconCacheTest.kt`: I/O file disk cache, isolasi penghapusan per-paket, alias `hapusPaket`.
   - `PmPackageTest.kt`: Eager initialization `UserCache`, thread-safe query fallback, persistensi data cache setelah listener dilepas.
   - `WorkspaceRepositoryTest.kt`: Cache Hit/Miss snapshot apps, penempatan orphan apps baru, penghapusan app, auto-dissolve folder, retensi SQLite primary key saat reorder, diff insert/delete, in-place folder title update, empty workspace clearAll.
   - `LauncherPreferencesTest.kt`: Identitas singleton instance, konsistensi baca/tulis antar instance, default values, tracking jumlah peluncuran aplikasi.

11. **Ketiadaan Regresi / Dead Code / Duplikasi**: `[PASS]`
   - Seluruh unit test lulus 100% (`gradle testDebugUnitTest`).
   - Kompilasi build lulus (`compile_applet`).
