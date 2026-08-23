package com.silauncer.cepat.database

import android.content.Context
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.database.entity.WorkspaceItemEntity
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.launcher.LauncherItem
import com.silauncer.cepat.pm.UserCache
import com.silauncer.cepat.shortcuts.ShortcutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
// [Penjelasan]: Repositori untuk mengelola layout workspace dan folder dari Database Room, memetakan entity Room menjadi LauncherItem domain model, serta mengimplementasikan REKONSILIASI cerdas (menangani duplikat, aplikasi dihapus, folder bubar otomatis, dan pinned shortcuts).

class WorkspaceRepository(private val context: Context) {
    private val dao = LauncherDatabase.getDatabase(context).workspaceItemDao()
    private val userCache = UserCache.getInstance(context)
    private val shortcutRepo = ShortcutRepository(context)

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Menghasilkan kunci pembeda unik untuk entitas workspace item guna membandingkan data database eksisting dengan data target secara efisien.
    private fun getEntityMatchKey(entity: WorkspaceItemEntity): String {
        return when (entity.itemType) {
            WorkspaceItemEntity.ITEM_TYPE_FOLDER -> "FOLDER:${entity.itemUid}"
            WorkspaceItemEntity.ITEM_TYPE_APP -> "APP:${entity.containerUid.orEmpty()}:${entity.componentName}:${entity.userSerial}"
            WorkspaceItemEntity.ITEM_TYPE_SHORTCUT -> "SHORTCUT:${entity.containerUid.orEmpty()}:${entity.componentName}:${entity.userSerial}:${entity.shortcutId.orEmpty()}"
            else -> "UNKNOWN:${entity.id}"
        }
    }

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Menyimpan item-item workspace ke database Room menggunakan strategi batch diff incremental update untuk meminimalkan operasi I/O dan fragmentasi SQLite. In-memory cache WorkspaceCache di-invalidate agar data selalu sinkron.
    suspend fun saveWorkspace(items: List<LauncherItem>) = withContext(Dispatchers.IO) {
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
        // [Penjelasan]: Invalidasi WorkspaceCache saat ada perubahan storage untuk menjaga sinkronisasi data cache.
        com.silauncer.cepat.cache.WorkspaceCache.invalidate()
        
        if (items.isEmpty()) {
            // [Penjelasan]: Jika seluruh item dikosongkan, panggil clearAll untuk pembersihan menyeluruh secara efisien.
            dao.clearAll()
            return@withContext
        }

        val targetEntities = mutableListOf<WorkspaceItemEntity>()
        var workspaceRank = 0
        
        // Petakan semua item (App, Folder, Shortcut) di workspace
        for (item in items) {
            when (item) {
                is LauncherItem.App -> {
                    val serial = userCache.getSerialNumberForUser(item.appInfo.user)
                    targetEntities.add(
                        WorkspaceItemEntity(
                            itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                            containerUid = null, // null = workspace
                            rank = workspaceRank++,
                            componentName = item.appInfo.componentName.flattenToString(),
                            userSerial = serial
                        )
                    )
                }
                is LauncherItem.Shortcut -> {
                    val serial = userCache.getSerialNumberForUser(item.shortcutInfo.user)
                    targetEntities.add(
                        WorkspaceItemEntity(
                            itemType = WorkspaceItemEntity.ITEM_TYPE_SHORTCUT,
                            containerUid = null,
                            rank = workspaceRank++,
                            componentName = item.shortcutInfo.packageName,
                            userSerial = serial,
                            shortcutId = item.shortcutInfo.shortcutId
                        )
                    )
                }
                is LauncherItem.Folder -> {
                    val folderUid = item.folderInfo.id
                    targetEntities.add(
                        WorkspaceItemEntity(
                            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
                            itemUid = folderUid,
                            containerUid = null,
                            rank = workspaceRank++,
                            title = item.folderInfo.title
                        )
                    )
                    
                    // Simpan isi dalam Folder (App atau Shortcut)
                    var folderRank = 0
                    for (child in item.folderInfo.getItems()) {
                        val serial = userCache.getSerialNumberForUser(child.user)
                        targetEntities.add(
                            WorkspaceItemEntity(
                                itemType = WorkspaceItemEntity.ITEM_TYPE_APP,
                                containerUid = folderUid,
                                rank = folderRank++,
                                componentName = child.componentName.flattenToString(),
                                userSerial = serial
                            )
                        )
                    }
                    for (child in item.folderInfo.getShortcuts()) {
                        val serial = userCache.getSerialNumberForUser(child.user)
                        targetEntities.add(
                            WorkspaceItemEntity(
                                itemType = WorkspaceItemEntity.ITEM_TYPE_SHORTCUT,
                                containerUid = folderUid,
                                rank = folderRank++,
                                componentName = child.packageName,
                                userSerial = serial,
                                shortcutId = child.shortcutId
                            )
                        )
                    }
                }
            }
        }
        
        val existingEntities = dao.getAllItemsSync()
        if (existingEntities.isEmpty()) {
            dao.insertItems(targetEntities)
            return@withContext
        }

        // [Penjelasan]: Hitung diff antara database saat ini dengan targetEntities untuk menentukan operasi insert, update, dan delete secara minimal.
        val existingMap = LinkedHashMap<String, WorkspaceItemEntity>()
        val duplicatesToDelete = mutableListOf<WorkspaceItemEntity>()
        for (entity in existingEntities) {
            val key = getEntityMatchKey(entity)
            if (existingMap.containsKey(key)) {
                duplicatesToDelete.add(entity)
            } else {
                existingMap[key] = entity
            }
        }

        val toInsert = mutableListOf<WorkspaceItemEntity>()
        val toUpdate = mutableListOf<WorkspaceItemEntity>()

        for (target in targetEntities) {
            val key = getEntityMatchKey(target)
            val existing = existingMap.remove(key)
            if (existing != null) {
                if (existing.rank != target.rank || existing.title != target.title) {
                    toUpdate.add(target.copy(id = existing.id))
                }
            } else {
                toInsert.add(target)
            }
        }

        val toDelete = mutableListOf<WorkspaceItemEntity>()
        toDelete.addAll(duplicatesToDelete)
        toDelete.addAll(existingMap.values)

        for (item in toDelete) {
            dao.deleteItem(item)
        }
        if (toUpdate.isNotEmpty()) {
            dao.updateItems(toUpdate)
        }
        if (toInsert.isNotEmpty()) {
            dao.insertItems(toInsert)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Alias fungsi simpanWorkspace untuk kompatibilitas penamaan berbahasa Indonesia.
    suspend fun simpanWorkspace(items: List<LauncherItem>) = saveWorkspace(items)

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Mengembalikan representasi cache memori workspace saat ini secara cepat tanpa pemuatan ulang dari Room Database.
    fun getCachedWorkspace(): List<LauncherItem>? = com.silauncer.cepat.cache.WorkspaceCache.get()

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Mengekstrak himpunan kunci unik identitas aplikasi (componentName_userSerial) dari daftar LauncherItem termasuk yang berada di dalam folder.
    private fun extractAppKeys(items: List<LauncherItem>): Set<String> {
        val keys = HashSet<String>()
        for (item in items) {
            when (item) {
                is LauncherItem.App -> {
                    val serial = userCache.getSerialNumberForUser(item.appInfo.user)
                    keys.add("${item.appInfo.componentName.flattenToString()}_$serial")
                }
                is LauncherItem.Folder -> {
                    for (child in item.folderInfo.getItems()) {
                        val serial = userCache.getSerialNumberForUser(child.user)
                        keys.add("${child.componentName.flattenToString()}_$serial")
                    }
                }
                is LauncherItem.Shortcut -> {
                    // Shortcut bukan merupakan AppInfo terinstal sehingga tidak diekstrak ke himpunan app keys
                }
            }
        }
        return keys
    }

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Mengekstrak himpunan kunci unik identitas aplikasi dari snapshot daftar aplikasi terinstal (allInstalledApps).
    private fun getInstalledAppKeys(apps: List<AppInfo>): Set<String> {
        val keys = HashSet<String>(apps.size)
        for (app in apps) {
            val serial = userCache.getSerialNumberForUser(app.user)
            keys.add("${app.componentName.flattenToString()}_$serial")
        }
        return keys
    }

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Memuat tata letak workspace dengan validasi cache memori terhadap snapshot aplikasi terinstal. Jika cache memori cocok persis dengan snapshot, cache langsung dikembalikan tanpa query Room DB. Jika snapshot berubah atau cache miss, rekonstruksi ulang dan rekonsiliasi dilakukan terhadap database Room.
    suspend fun loadWorkspace(allInstalledApps: List<AppInfo>): List<LauncherItem> = withContext(Dispatchers.IO) {
        val cached = com.silauncer.cepat.cache.WorkspaceCache.get()
        if (cached != null) {
            val cachedAppKeys = extractAppKeys(cached)
            val installedAppKeys = getInstalledAppKeys(allInstalledApps)
            // [Penjelasan]: Validasi apakah snapshot aplikasi terinstal sama persis dengan yang ada di dalam cache memori
            if (cachedAppKeys == installedAppKeys) {
                return@withContext cached
            }
        }
        val entities = dao.getAllItemsSync()
        if (entities.isEmpty()) {
            return@withContext emptyList()
        }

        val appLookup = allInstalledApps.associateBy {
            val serial = userCache.getSerialNumberForUser(it.user)
            "${it.componentName.flattenToString()}_$serial"
        }

        val workspaceItems = mutableListOf<Pair<Int, LauncherItem>>()
        val folderMap = mutableMapOf<String, FolderInfo>()
        val usedAppKeys = mutableSetOf<String>()
        val dissolvedFolderApps = mutableListOf<AppInfo>()
        
        // Pertama, rekonstruksi semua folder
        for (entity in entities) {
            if (entity.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER && entity.itemUid != null) {
                // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
                // [Penjelasan]: Membuat instance FolderInfo baru yang bersih untuk menampung item yang tervalidasi dari database Room dan menyimpannya ke FolderCache.
                val folderInfo = FolderInfo(
                    id = entity.itemUid,
                    initialTitle = entity.title ?: "",
                    initialContents = emptyList()
                )
                folderMap[entity.itemUid] = folderInfo
                com.silauncer.cepat.cache.FolderCache.put(entity.itemUid, folderInfo)
            }
        }

        // Kumpulkan semua request shortcut yang perlu diresolve
        val shortcutRequests = mutableMapOf<Long, MutableMap<String, MutableList<String>>>() // userSerial -> (packageName -> list of shortcutIds)
        for (entity in entities) {
            if (entity.itemType == WorkspaceItemEntity.ITEM_TYPE_SHORTCUT && entity.componentName != null && entity.shortcutId != null) {
                val userRequests = shortcutRequests.getOrPut(entity.userSerial) { mutableMapOf() }
                val packageRequests = userRequests.getOrPut(entity.componentName) { mutableListOf() }
                packageRequests.add(entity.shortcutId)
            }
        }

        // Resolve shortcuts
        val resolvedShortcuts = mutableMapOf<String, com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo>()
        for ((userSerial, packageMap) in shortcutRequests) {
            val user = userCache.getUserForSerialNumber(userSerial)
            if (user != null) {
                for ((packageName, shortcutIds) in packageMap) {
                    val shortcuts = shortcutRepo.getPinnedShortcuts(packageName, shortcutIds, user)
                    for (shortcut in shortcuts) {
                        val key = "${packageName}_${shortcut.shortcutId}_$userSerial"
                        resolvedShortcuts[key] = shortcut
                    }
                }
            }
        }

        // Kedua, tempatkan aplikasi dan shortcut ke container masing-masing dan cegah duplikat
        for (entity in entities) {
            if (entity.itemType == WorkspaceItemEntity.ITEM_TYPE_APP) {
                val key = "${entity.componentName}_${entity.userSerial}"
                val app = appLookup[key]
                if (app != null && !usedAppKeys.contains(key)) {
                    usedAppKeys.add(key)
                    if (entity.containerUid == null) {
                        workspaceItems.add(Pair(entity.rank, LauncherItem.App(app)))
                    } else {
                        val folder = folderMap[entity.containerUid]
                        if (folder != null) {
                            folder.add(app)
                        } else {
                            // Folder tidak ditemukan, jadikan orphan dan taruh di akhir workspace
                            workspaceItems.add(Pair(Int.MAX_VALUE, LauncherItem.App(app)))
                        }
                    }
                }
            } else if (entity.itemType == WorkspaceItemEntity.ITEM_TYPE_SHORTCUT) {
                val key = "${entity.componentName}_${entity.shortcutId}_${entity.userSerial}"
                val shortcut = resolvedShortcuts[key]
                if (shortcut != null) {
                    if (entity.containerUid == null) {
                        workspaceItems.add(Pair(entity.rank, LauncherItem.Shortcut(shortcut)))
                    } else {
                        val folder = folderMap[entity.containerUid]
                        if (folder != null) {
                            folder.addShortcut(shortcut)
                        } else {
                            workspaceItems.add(Pair(Int.MAX_VALUE, LauncherItem.Shortcut(shortcut)))
                        }
                    }
                }
            }
        }

        // Ketiga, tambahkan folder ke workspace items dan lakukan evaluasi pembubaran (auto-dissolve)
        for (entity in entities) {
            if (entity.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER && entity.itemUid != null) {
                val folder = folderMap[entity.itemUid]
                if (folder != null) {
                    if (folder.shouldAutoDissolve()) {
                        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
                        // [Penjelasan]: Menghapus folder dari FolderCache saat folder dibubarkan secara otomatis.
                        com.silauncer.cepat.cache.FolderCache.remove(entity.itemUid)
                        val singleApp = folder.getSingleRemainingApp()
                        if (singleApp != null) {
                            dissolvedFolderApps.add(singleApp)
                        } else {
                            // Cek jika yang tersisa adalah single shortcut
                            val singleShortcut = folder.getSingleRemainingShortcut()
                            if (singleShortcut != null) {
                                workspaceItems.add(Pair(entity.rank, LauncherItem.Shortcut(singleShortcut)))
                            }
                        }
                    } else if (!folder.isEmpty() || !folder.isShortcutsEmpty()) { 
                        workspaceItems.add(Pair(entity.rank, LauncherItem.Folder(folder)))
                    }
                }
            }
        }

        // Urutkan berdasarkan rank
        val sortedWorkspace = workspaceItems.sortedBy { it.first }.map { it.second }.toMutableList()

        // Tambahkan aplikasi dari folder yang dibubarkan ke akhir workspace
        for (app in dissolvedFolderApps) {
            sortedWorkspace.add(LauncherItem.App(app))
        }

        // Periksa aplikasi yang baru diinstal tapi tidak ada di DB (Orphan apps baru)
        for (app in allInstalledApps) {
            val serial = userCache.getSerialNumberForUser(app.user)
            val key = "${app.componentName.flattenToString()}_$serial"
            if (!usedAppKeys.contains(key)) {
                // Tambahkan di akhir
                sortedWorkspace.add(LauncherItem.App(app))
            }
        }
        
        // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
        // [Penjelasan]: Menyimpan hasil rekonstruksi workspace ke dalam WorkspaceCache
        com.silauncer.cepat.cache.WorkspaceCache.set(sortedWorkspace)
        
        return@withContext sortedWorkspace
    }

    // [Jalur Class]: com.silauncer.cepat.database.WorkspaceRepository
    // [Penjelasan]: Alias fungsi muatWorkspace untuk kompatibilitas penamaan berbahasa Indonesia.
    suspend fun muatWorkspace(allInstalledApps: List<AppInfo>): List<LauncherItem> = loadWorkspace(allInstalledApps)
}
