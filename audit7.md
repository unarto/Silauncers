# Laporan Audit Komprehensif Arsitektur & Kualitas Kode: Silauncer (Audit 7)

Dokumen ini merupakan laporan audit statis mendalam (*deep static analysis*) terhadap seluruh basis kode Android pada project **Silauncer**.
Audit dilakukan secara menyeluruh mencakup seluruh package, modul presenter, repositori, layout XML, dan resource grafis.

---

## Matriks Ringkasan Temuan

| Prioritas | Kategori | Jumlah Temuan | Area Terdampak |
| :--- | :--- | :---: | :--- |
| **[CRITICAL/BUG]** | Memory Leak & Resource Retention | 3 | `LauncherAppController`, `PinnedAppsAdapter`, `FolderWallpaperBlurController` |
| **[HIGH/CODE SMELL]** | Hardcoded Hex Colors & Raw Preferences | 4 | `NotificationMainView`, `FolderWallpaperBlurController`, `PinnedAppsAdapter` |
| **[HIGH/CODE SMELL]** | God Class / File Size Boundary | 2 | `GridDragAndDropHandler`, `PageIndicatorDots` |
| **[MEDIUM/DUPLICATION]** | Duplikasi Rendering Bitmap & DP-PX Math | 3 | `DragView`, `SecondaryDragController`, `GridDragAndDropHandler` |
| **[LOW/CLEANUP]** | Dead Code / Redundant Imports & Annotations | 5 | Berbagai modul utilitas & testing |

---

## 1. Dead Code & Unused Assets ([LOW/CLEANUP])

### 1.1. Parameter `context` Terbengkalai pada `DragSpringAnimationHelper`
- **Lokasi**: `com.silauncer.cepat.dragndrop.DragSpringAnimationHelper` (Fungsi `animateFolderDisband`)
- **Analisis Masalah**:
  Parameter `context: Context` diteruskan ke dalam helper namun tidak pernah dibaca di dalam fungsi karena perhitungan animasi pegas murni memanfaatkan `View` dan `SpringAnimation` internal.
- **Rekomendasi Perbaikan**: Hapus parameter yang tidak digunakan atau manfaatkan untuk penyesuaian interpolasi berbasis refresh rate perangkat.

### 1.2. Method `getGridColumns()` Fallback Redundan di `GridDragAndDropHandler`
- **Lokasi**: `com.silauncer.cepat.dragndrop.GridDragAndDropHandler.kt` (Baris 77-80)
- **Kode**:
  ```kotlin
  private fun getGridColumns(): Int {
      val lm = recyclerView.layoutManager as? GridLayoutManager
      return if (lm != null && lm.spanCount > 0) lm.spanCount else com.silauncer.cepat.deviceprofile.InvariantDeviceProfile.DEFAULT_COLUMNS
  }
  ```
- **Analisis Masalah**:
  `recyclerView.layoutManager` selalu diinisialisasi sebagai `GridLayoutManager` pada `LauncherActivity.onCreate()`. Pengecekan fallback aman, namun pemanggilan berulang pada setiap frame drag event dapat di-cache ke variabel lokal sesi drag.

### 1.3. Unused / Deprecated API References di Unit Test Suite
- **Lokasi**: 
  - `com.silauncer.cepat.apps.AppsPackageTest.kt` (Baris ~199: `getParcelableExtra`)
  - `com.silauncer.cepat.shortcuts.ShortcutPackageTest.kt` (Baris 86-88: Redundant instance-of checks)
- **Analisis Masalah**:
  Penggunaan `getParcelableExtra` deprecated sejak Android 13 (API 33). Pengecekan `is` yang selalu bernilai `true` menghasilkan compiler warnings.
- **Rekomendasi Perbaikan**: Migrasikan ke `IntentCompat.getParcelableExtra(intent, key, Class)` dan bersihkan redundant assertions.

---

## 2. Code Duplication ([MEDIUM/DUPLICATION])

### 2.1. Duplikasi Logika Pembuatan Software Bitmap View untuk Drag Preview
- **Lokasi**:
  - `com.silauncer.cepat.dragndrop.DragView.kt` (`createFromView()`, baris 66-95)
  - `com.silauncer.cepat.secondarydisplay.SecondaryDragController.kt` (`createViewBitmap()`, baris 98-120)
- **Analisis Masalah**:
  Kedua kelas mengimplementasikan logika yang identik:
  1. Menghitung fallback density dimension jika view belum terukur (`width <= 0`).
  2. Alokasi `Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)`.
  3. Mengisolasi `Canvas(bitmap)` dan melakukan pengecekan `RippleDrawable` / `STYLE_PATTERNED` untuk menghindari software rendering crash di Android 12+.
- **Klasifikasi**: **[MEDIUM/DUPLICATION]**
- **Rekomendasi Perbaikan**: Ekstrak fungsi utilitas murni ke `com.silauncer.cepat.graphics.BitmapUtils.createHardwareSafeBitmap(view: View): Bitmap`.

### 2.2. Duplikasi Rumus Konversi DP ke PX Manual
- **Lokasi**:
  - `com.silauncer.cepat.popup.PopupContainerWithArrow.kt` (Baris 178-180)
  - `com.silauncer.cepat.dragndrop.GridDragAndDropHandler.kt` (Baris 320, 418)
  - `com.silauncer.cepat.dragndrop.DragView.kt` (Baris 67)
- **Kode**:
  ```kotlin
  val margin16 = (16f * displayMetrics.density).toInt()
  val fallbackSize = (48f * view.context.resources.displayMetrics.density).toInt()
  ```
- **Klasifikasi**: **[MEDIUM/DUPLICATION]**
- **Rekomendasi Perbaikan**: Buat extension property inline: `inline val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()`.

---

## 3. Placeholders, Mock & Fake Data ([LOW - NO CRITICAL ISSUE])

### 3.1. Penelusuran String & State Simulasi
- **Hasil Audit**:
  - **TIDAK DITEMUKAN** data mock atau return value palsu pada jalur eksekusi production.
  - Semua data aplikasi bersumber langsung dari `LauncherApps` / `PackageManager` AOSP via `LauncherAppController` dan `AppDataSource`.
  - Komentar atau penanda `placeholder` pada `DeepShortcutTextView.kt` dan `IconLoader.kt` adalah mekanisme rendering visual *loading shimmer placeholder* resmi, bukan dummy logic.
  - Keyword `open` pada `AppDataSource.kt` sengaja didesain untuk mockability pada Robolectric unit testing.

---

## 4. Hardcoded Values & Code Smells ([HIGH/CODE SMELL & CRITICAL/BUG])

### 4.1. [CRITICAL/BUG] Unmanaged CoroutineScope Lifecycle & Static Callback Retain
- **Lokasi 1**: `com.silauncer.cepat.launcher.LauncherAppController.kt` (Baris 28)
  ```kotlin
  private val controllerScope = kotlinx.coroutines.CoroutineScope(
      kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
  )
  ```
  *Masalah*: `LauncherAppController` bertindak sebagai singleton/controller global, namun scope IO tidak memiliki hook pembersihan eksplisit saat sistem membunuh proses launcher, sehingga job aktif berpotensi terus berjalan.
- **Lokasi 2**: `com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter.kt`
  *Status*: **Telah Direfaktor**. Coroutine scope terisolasi kini dibatalkan pada `destroy()` saat view/adapter terlepas.
- **Lokasi 3**: `com.silauncer.cepat.folder.FolderWallpaperBlurController.kt`
  *Status*: **Telah Direfaktor**. Coroutine scope dan active job di-cancel secara aman pada pemanggilan `clear()`.

### 4.2. [HIGH/CODE SMELL] Arsitektur Storage: Room Database + MMKV (Zero SharedPreferences)
- **Arsitektur Penyimpanan**:
  1. **Room Database (`WorkspaceRepository`, `WorkspaceDao`, `WorkspaceItemEntity`)**: Mengelola struktur data hirarki item workspace, posisi grid, isi folder, dan pengurutan aplikasi utama.
  2. **Tencent MMKV (`LauncherPreferences`)**: Mengelola konfigurasi kecepatan tinggi berbasis memory-mapped I/O (jumlah baris/kolom, ukuran ikon, sort mode, hidden apps, bahasa aplikasi, dan `pinnedApps` untuk secondary display).
- **Temuan Sebelumnya**:
  `PinnedAppsAdapter.kt` sebelumnya sempat menggunakan SharedPreferences bawaan Android (`"pinned_apps_prefs"`).
- **Resolusi**:
  Telah dimigrasikan 100% ke `LauncherPreferences.pinnedApps` (MMKV) sehingga tidak ada lagi SharedPreferences legacy yang memblokir main thread di seluruh codebase.

### 4.3. [HIGH/CODE SMELL] Hardcoded Literal ARGB Hex Colors
- **Lokasi 1**: `com.silauncer.cepat.notification.NotificationMainView.kt` (`Color.parseColor("#2C2C2E")`)
  *Status*: **Telah Direfaktor**. Menggunakan `@color/notification_bubble_background` dari `res/values/colors.xml`.
- **Lokasi 2**: `com.silauncer.cepat.folder.FolderWallpaperBlurController.kt` (`0x1A000000`)
  *Status*: **Telah Direfaktor**. Menggunakan `@color/folder_dim_overlay` dari `res/values/colors.xml`.

### 4.4. [HIGH/CODE SMELL] Ukuran Kelas Mendekati Batas (God Class Risk)
- **Lokasi**: `com.silauncer.cepat.dragndrop.GridDragAndDropHandler.kt` (517 baris)
  *Analisis*: Meskipun telah direfaktor dengan memisahkan `DropTargetBarController`, `FolderCollisionHelper`, dan `DragSpringAnimationHelper`, kelas ini masih menangani:
  1. Scroll tepi layar otomatis (*Edge Auto-scroll*).
  2. Hit-testing target drop grid / swap reorder.
  3. Koordinasi dialog popup konfirmasi hapus aplikasi.
  *Rekomendasi*: Ekstrak auto-scroller ke `GridEdgeAutoScroller.kt`.

---

## 5. Rencana Tindakan Perbaikan Prioritas (Action Plan)

1. **Tahap 1 (Immediate Fix - Memory & Coroutines)**:
   - Sediakan fungsi `destroy()` / `clear()` pada `PinnedAppsAdapter` dan `FolderWallpaperBlurController` untuk membatalkan `SupervisorJob`.
2. **Tahap 2 (Resource Centralization)**:
   - Ekstrak `#2C2C2E` dan `0x1A000000` ke `res/values/colors.xml` (`notification_bubble_background`, `folder_dim_overlay`).
   - Pindahkan `PINNED_APPS_PREF` ke `LauncherPreferences.PREF_PINNED_APPS`.
3. **Tahap 3 (Deduplikasi Grafis)**:
   - Satukan pembuatan bitmap view yang aman dari software rendering ke `BitmapUtils.createHardwareSafeBitmap()`.
4. **Tahap 4 (Pembersihan SRP & Edge Scroll)**:
   - Modularisasi auto-scroll tepi layar dari `GridDragAndDropHandler` ke helper terpisah agar ukuran file tetap ramping (<400 baris).
