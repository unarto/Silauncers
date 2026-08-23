# Laporan Kinerja & Implementasi Cache Prioritas

## 1. Ringkasan Implementasi Cache Prioritas

Sesuai aturan arsitektur sistem dan prinsip *Single Responsibility Principle* (SRP), implementasi cache prioritas difokuskan murni pada 7 komponen inti tanpa membuat God Class, tanpa memodifikasi MMKV, dan tanpa mengubah Room Database yang tetap menjadi *Single Source of Truth* untuk persistensi data.

| Komponen | Kelas Cache | Mekanisme & Kapasitas | Strategi Invalidasi |
| :--- | :--- | :--- | :--- |
| **App Icon** | `IconCache` | Bounded `LruCache` (Heap RAM 12.5%) | Dihapus saat aplikasi di-update/di-uninstall atau paket icon pack berubah (`removePackage`). |
| **App List / AppInfo** | `AppCache` | Bounded `LruCache<String, AppInfo>` (500 entri) | Dihapus saat ada event paket aplikasi (`ACTION_PACKAGE_ADDED`, `REMOVED`, `CHANGED`, `REPLACED`). |
| **Folder Contents** | `FolderCache` | Bounded `LruCache<String, FolderInfo>` (50 entri) | Diperbarui saat penambahan/pengurangan item; dihapus saat folder dibubarkan (*auto-dissolve*). |
| **Workspace Layout** | `WorkspaceCache` | In-Memory `List<LauncherItem>` | Di-invalidate saat urutan workspace disimpan, rekonsiliasi paket, atau perubahan shortcut. |
| **Device Profile / Grid** | `DeviceProfileCache` | Memoized Cache terkomputasi | Di-invalidate saat perubahan resolusi layar, orientasi landscape/portrait, atau ukuran grid di preferensi. |
| **Notification Dots** | `NotificationCache` | Thread-safe `ConcurrentHashMap` | Diperbarui secara instan melalui callback `StatusBarNotification` (posted/removed/refresh). |
| **Shortcut / Deep Shortcut** | `ShortcutCache` | Bounded `LruCache<String, WorkspaceShortcutInfo>` (100 entri) | Dihapus saat ada pembaruan shortcut via callback `onShortcutsChanged` atau uninstall aplikasi. |

---

## 2. Rincian Invalidation Handler

1. **Package Events (`LauncherAppController` & `AppChangeReceiver`)**:
   - `Intent.ACTION_PACKAGE_ADDED`: Invalidate `AppCache`, `ShortcutCache`, dan `WorkspaceCache`.
   - `Intent.ACTION_PACKAGE_REMOVED`: Invalidate `IconCache`, `AppCache`, `ShortcutCache`, `NotificationCache`, dan `WorkspaceCache`.
   - `Intent.ACTION_PACKAGE_CHANGED` / `REPLACED`: Invalidate `IconCache`, `AppCache`, `ShortcutCache`, dan `WorkspaceCache`.

2. **Folder Events (`FolderManager` & `WorkspaceRepository`)**:
   - Pembuatan folder baru: Disimpan ke `FolderCache`.
   - Modifikasi folder: State diperbarui di `FolderCache`.
   - Pembubaran (*auto-dissolve*): Dihapus dari `FolderCache` dan `WorkspaceCache` di-invalidate.

3. **Shortcut Events (`LauncherAppController`)**:
   - Callback `onShortcutsChanged`: `IconCache.removePackage`, `ShortcutCache.removePackage`, `WorkspaceCache.invalidate`.

4. **Device Profile & Grid**:
   - Perubahan konfigurasi layar atau pengaturan grid langsung memanggil `DeviceProfileCache.invalidate()`.

---

## 3. Hasil Validasi
- **Kompilasi**: `compile_applet` PASS / SUCCEEDED.
- **Unit Tests**:
  - `IconCacheTest.kt` (PASS)
  - `AppCacheTest.kt` (PASS)
  - `FolderCacheTest.kt` (PASS)
  - `WorkspaceCacheTest.kt` (PASS)
  - `DeviceProfileCacheTest.kt` (PASS)
  - `NotificationCacheTest.kt` (PASS)
  - `ShortcutCacheTest.kt` (PASS)
  - `CacheInvalidationTest.kt` (PASS)
