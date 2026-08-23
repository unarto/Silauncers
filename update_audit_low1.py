import re

with open("audit rsp+bug.md", "r") as f:
    content = f.read()

new_text = """### 2. CoroutineScope Memory Leak pada `FolderWallpaperBlurController`
- **File/Class**: `com.silauncer.cepat.folder.FolderWallpaperBlurController`
- **Lokasi**: Default parameter `scope: CoroutineScope = CoroutineScope(Dispatchers.Main)` pada fungsi `applyWallpaperBackground()`.
- **Temuan**: Objek `CoroutineScope` (beserta internal root `Job`) baru diciptakan secara on-the-fly setiap kali `applyWallpaperBackground` dipanggil jika parameter `scope` tidak di-supply secara eksplisit.
- **Bukti dari kode**: 
  - Pada `Folder.kt`, pemanggilan fungsi tidak me-supply `scope`, sehingga memicu instance baru `CoroutineScope(Dispatchers.Main)`.
  - Pada `FolderWallpaperBlurController.kt`, fungsi `clear()` hanya memanggil `activeJob?.cancel()` yang merupakan _child job_, tanpa menyimpan atau membatalkan _root job_ dari `scope` itu sendiri.
- **Root Cause**: Scope tidak terikat pada lifecycle dari View (seperti `findViewTreeLifecycleOwner()?.lifecycleScope`). Akibatnya, root `Job` dari scope yang dihasilkan akan terus berada dalam status _Active_.
- **Dampak**: 
  Setiap pembukaan `Folder`, akan teralokasi sebuah root `Job` baru yang statusnya tidak pernah _Cancelled_. Namun, karena tidak ada referensi strong yang menahan scope tersebut di class (hanya param lokal) dan _child job_ (`activeJob`) telah dimatikan di `clear()`, garbage collector Java/Kotlin tetap mampu membersihkan root job tersebut (karena tidak teregistrasi ke global registry).
- **Severity**: **LOW** (Bukan _catastrophic memory leak_, melainkan _False Positive_ dari sisi permanent memory leak, namun tetap merupakan Wasted Allocation dan Bad Practice Lifecycle Management).
- **Rekomendasi**: 
  - (Opsi 1 - Terbaik): Mengubah pemanggil (`Folder.kt`) agar menginjeksi `findViewTreeLifecycleOwner()?.lifecycleScope` ke dalam `applyWallpaperBackground()`.
  - (Opsi 2): Menyediakan satu class-level `val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)` di dalam `FolderWallpaperBlurController` dan menyediakan metode `destroy()` untuk dipanggil saat Launcher dihancurkan.
- **Status**: TERKONFIRMASI (sebagai Bad Practice / Wasted Allocation, BUKAN permanent leak)."""

pattern = r"### 2\. CoroutineScope Memory Leak pada `FolderWallpaperBlurController`.*?(?=### 3\. Folder Icon Size Tidak Konsisten dengan Preferensi Pengguna)"
new_content = re.sub(pattern, new_text + "\n\n", content, flags=re.DOTALL)

with open("audit rsp+bug.md", "w") as f:
    f.write(new_content)
