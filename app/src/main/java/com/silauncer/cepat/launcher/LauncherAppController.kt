package com.silauncer.cepat.launcher

import android.content.Intent
import android.os.Process
import com.silauncer.cepat.apps.AppDataSource
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.apps.AppStateHolder
import com.silauncer.cepat.apps.AppSorter
import com.silauncer.cepat.cache.DiskIconCache
import com.silauncer.cepat.cache.IconCache
import com.silauncer.cepat.database.WorkspaceRepository
import com.silauncer.cepat.storage.LauncherPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// [Jalur Class]: com.silauncer.cepat.launcher.LauncherAppController
// [Penjelasan]: Mengelola event paket dan menyediakan list final item workspace. Menggunakan WorkspaceRepository untuk memuat folder dan posisi jika mode sorting custom. Menyimpan hasil rekonsiliasi ke database jika terjadi perubahan package.

class LauncherAppController(
    private val appDataSource: AppDataSource,
    private val appStateHolder: AppStateHolder,
    private val prefs: LauncherPreferences,
    private val workspaceRepo: WorkspaceRepository
) {
    // [Jalur Class]: com.silauncer.cepat.launcher.LauncherAppController
    // [Penjelasan]: Scope terpusat dengan SupervisorJob untuk menjalankan background task fire-and-forget (seperti invalidasi cache) tanpa membocorkan scope ad-hoc.
    private val controllerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    suspend fun getAllAppsRaw(): List<AppInfo> = appStateHolder.getApps()

    suspend fun loadAppsInitial(): List<LauncherItem> {
        val context = com.silauncer.cepat.launcher.LauncherApplication.appContext
        val profiles = com.silauncer.cepat.pm.UserCache.getInstance(context).getUserProfiles()

        val installedApps = mutableListOf<AppInfo>()
        for (user in profiles) {
            installedApps.addAll(appDataSource.getInstalledApps(null, user))
        }
        appStateHolder.setApps(installedApps)
        
        return getSortedVisibleWorkspace()
    }

    suspend fun refreshApps(): List<LauncherItem> {
        return getSortedVisibleWorkspace()
    }

    suspend fun saveCustomWorkspaceOrder(visibleItems: List<LauncherItem>) {
        if (prefs.sortMode != "custom") {
            prefs.sortMode = "custom"
        }
        workspaceRepo.saveWorkspace(visibleItems)
    }

    private suspend fun getSortedVisibleWorkspace(): List<LauncherItem> = withContext(Dispatchers.Default) {
        val apps = appStateHolder.getApps()
        val hidden = prefs.hiddenApps
        val visibleApps = apps.filter { !hidden.contains(it.componentName.packageName) }

        if (prefs.sortMode == "custom") {
            val workspaceItems = workspaceRepo.loadWorkspace(visibleApps)
            // Lakukan migrasi data lama (appOrder dari MMKV) jika database kosong
            if (workspaceItems.isEmpty() && prefs.appOrder.isNotEmpty()) {
                val legacyOrder = prefs.appOrder
                val sortedLegacy = AppSorter.sort(visibleApps, "custom", legacyOrder)
                val newLegacyItems = sortedLegacy.map { LauncherItem.App(it) }
                workspaceRepo.saveWorkspace(newLegacyItems)
                // Bersihkan legacy appOrder dari MMKV
                prefs.appOrder = emptyList()
                return@withContext newLegacyItems
            }
            return@withContext workspaceItems
        } else {
            // Mode sorting lain (a_z, z_a, dll) meratakan seluruh aplikasi (tanpa folder)
            val sortedApps = AppSorter.sort(visibleApps, prefs.sortMode, emptyList())
            return@withContext sortedApps.map { LauncherItem.App(it) }
        }
    }

    var onShortcutsChangedCallback: (() -> Unit)? = null

    private val launcherAppsCallback = object : android.content.pm.LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: android.os.UserHandle) {}
        override fun onPackageChanged(packageName: String, user: android.os.UserHandle) {}
        override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) {}
        override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {}
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {}
        
        override fun onShortcutsChanged(
            packageName: String,
            shortcuts: MutableList<android.content.pm.ShortcutInfo>,
            user: android.os.UserHandle
        ) {
            // [Jalur Class]: com.silauncer.cepat.launcher.LauncherAppController
            // [Penjelasan]: Invalidate IconCache (L1 Memory), DiskIconCache (L2 Disk), ShortcutCache, dan WorkspaceCache saat ada perubahan deep shortcut dari sistem. Memindahkan Disk I/O ke background thread menggunakan controllerScope terpusat.
            val context = try { com.silauncer.cepat.launcher.LauncherApplication.appContext } catch (e: Throwable) { null }
            IconCache.removePackage(packageName)
            if (context != null) {
                controllerScope.launch {
                    DiskIconCache.removePackage(context, packageName)
                }
            }
            com.silauncer.cepat.cache.ShortcutCache.removePackage(packageName)
            com.silauncer.cepat.cache.WorkspaceCache.invalidate()
            // Call the callback to trigger UI refresh
            onShortcutsChangedCallback?.invoke()
        }
    }

    fun registerShortcutCallback(context: android.content.Context) {
        val launcherApps = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
        launcherApps.registerCallback(launcherAppsCallback, android.os.Handler(android.os.Looper.getMainLooper()))
    }
    
    fun unregisterShortcutCallback(context: android.content.Context) {
        val launcherApps = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    suspend fun handlePackageEvent(
        action: String?,
        packageName: String?,
        replacing: Boolean,
        user: android.os.UserHandle
    ): Boolean {
        if (action == null || packageName == null) return false
        var changed = false

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                // [Jalur Class]: com.silauncer.cepat.launcher.LauncherAppController
                // [Penjelasan]: Invalidate AppCache, ShortcutCache, dan WorkspaceCache saat paket aplikasi baru diinstal.
                com.silauncer.cepat.cache.AppCache.removePackage(packageName)
                com.silauncer.cepat.cache.ShortcutCache.removePackage(packageName)
                com.silauncer.cepat.cache.WorkspaceCache.invalidate()
                val newApps = appDataSource.getInstalledApps(packageName, user)
                val added = appStateHolder.addApps(newApps)
                if (added.isNotEmpty()) changed = true
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (!replacing) {
                    // [Jalur Class]: com.silauncer.cepat.launcher.LauncherAppController
                    // [Penjelasan]: Invalidate IconCache (L1 Memory), DiskIconCache (L2 Disk), AppCache, ShortcutCache, NotificationCache, dan WorkspaceCache saat aplikasi di-uninstall agar data disk dan memori bersih dari package target. Disk I/O dijalankan di IO Thread.
                    val context = try { com.silauncer.cepat.launcher.LauncherApplication.appContext } catch (e: Throwable) { null }
                    appStateHolder.removePackage(packageName, user)
                    IconCache.removePackage(packageName)
                    if (context != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            DiskIconCache.removePackage(context, packageName)
                        }
                    }
                    com.silauncer.cepat.cache.AppCache.removePackage(packageName)
                    com.silauncer.cepat.cache.ShortcutCache.removePackage(packageName)
                    com.silauncer.cepat.cache.NotificationCache.removePackage(packageName)
                    com.silauncer.cepat.cache.WorkspaceCache.invalidate()

                    val currentHidden = prefs.hiddenApps
                    if (currentHidden.contains(packageName)) {
                        prefs.hiddenApps = currentHidden - packageName
                    }

                    changed = true
                }
            }
            Intent.ACTION_PACKAGE_CHANGED, Intent.ACTION_PACKAGE_REPLACED -> {
                // [Jalur Class]: com.silauncer.cepat.launcher.LauncherAppController
                // [Penjelasan]: Invalidate seluruh cache terkait paket aplikasi yang di-update atau di-ganti (L1 Memory IconCache dan L2 DiskIconCache) untuk mencegah ikon lama masuk kembali dari L2 ke L1. Disk I/O dijalankan di IO Thread.
                val context = try { com.silauncer.cepat.launcher.LauncherApplication.appContext } catch (e: Throwable) { null }
                appStateHolder.removePackage(packageName, user)
                IconCache.removePackage(packageName)
                if (context != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        DiskIconCache.removePackage(context, packageName)
                    }
                }
                com.silauncer.cepat.cache.AppCache.removePackage(packageName)
                com.silauncer.cepat.cache.ShortcutCache.removePackage(packageName)
                com.silauncer.cepat.cache.WorkspaceCache.invalidate()
                val newApps = appDataSource.getInstalledApps(packageName, user)
                val updated = appStateHolder.addApps(newApps)
                if (updated.isNotEmpty()) changed = true
            }
        }
        
        if (changed && prefs.sortMode == "custom") {
            // Auto-save reconciled workspace to persist any changes (like auto-dissolve or orphan app placement)
            val reconciled = getSortedVisibleWorkspace()
            saveCustomWorkspaceOrder(reconciled)
        }

        return changed
    }
}
