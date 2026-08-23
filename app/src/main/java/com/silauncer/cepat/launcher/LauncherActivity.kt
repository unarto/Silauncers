package com.silauncer.cepat.launcher

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.silauncer.cepat.dragndrop.GridDragAndDropHandler
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.apps.AppChangeReceiver
import com.silauncer.cepat.apps.AppDataSource
import com.silauncer.cepat.apps.AppStateHolder
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.folder.Folder
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.home.AppAdapter
import com.silauncer.cepat.home.OverScroll
import com.silauncer.cepat.storage.LauncherPreferences
import com.silauncer.cepat.pm.UserCache
import kotlinx.coroutines.launch
import java.io.Closeable

class LauncherActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private lateinit var prefs: LauncherPreferences
    private lateinit var appChangeReceiver: AppChangeReceiver
    
    private lateinit var appController: LauncherAppController
    private lateinit var actionHandler: AppActionHandler
    private lateinit var dragHandler: GridDragAndDropHandler
    
    private var activeFolder: Folder? = null
    private var isLoaded = false

    private var userChangeListener: Closeable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        
        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Menggunakan singleton LauncherPreferences.getInstance() untuk optimasi alokasi memori.
        prefs = LauncherPreferences.getInstance()
        val appDataSource = AppDataSource(applicationContext)
        val appStateHolder = AppStateHolder()
        
        appController = LauncherAppController(appDataSource, appStateHolder, prefs, com.silauncer.cepat.database.WorkspaceRepository(this))
        actionHandler = AppActionHandler(this)

        recyclerView = findViewById(R.id.app_grid)
        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Mengatur tata letak layar utama menggunakan GridLayoutManager bertipe vertikal (RecyclerView.VERTICAL) dengan jumlah kolom dinamis dari LauncherPreferences agar dapat di-scroll lancar dari atas ke bawah.
        recyclerView.layoutManager = GridLayoutManager(this, prefs.gridColumns, RecyclerView.VERTICAL, false)
        OverScroll.setup(recyclerView)

        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Menambahkan FocusedItemDecorator untuk menggambar highlight animasi fokus saat navigasi keyboard / D-pad
        val focusedItemDecorator = com.silauncer.cepat.keyboard.FocusedItemDecorator(recyclerView)
        recyclerView.addItemDecoration(focusedItemDecorator)

        val iconSizePx = (prefs.iconSize * resources.displayMetrics.density).toInt()
        val spacingPx = (prefs.iconSpacing * resources.displayMetrics.density).toInt()
        
        adapter = AppAdapter(
            coroutineScope = lifecycleScope,
            iconSizePx = iconSizePx,
            showAppLabel = prefs.showAppLabel,
            labelSizeSp = prefs.labelSize,
            iconSpacingPx = spacingPx,
            gridRows = prefs.gridRows,
            onClick = { app ->
                if (app.packageName == applicationContext.packageName) {
                    try {
                        startActivity(android.content.Intent(this, com.silauncer.cepat.settings.SettingsActivity::class.java))
                    } catch (e: Exception) {
                        android.util.Log.e("SILAUNCER", "CRASH: " + e.message, e)
                    }
                } else {
                    actionHandler.launchApp(app)
                }
            },
            onShortcutClick = { shortcut ->
                actionHandler.launchShortcut(shortcut)
            },
            onFolderClick = { folderInfo ->
                openFolderModal(folderInfo)
            }
        ).apply {
            focusChangeListener = focusedItemDecorator.getFocusListener()
        }
        recyclerView.adapter = adapter
        
        dragHandler = GridDragAndDropHandler(
            context = this,
            recyclerView = recyclerView,
            adapter = adapter,
            appController = appController,
            actionHandler = actionHandler,
            coroutineScope = lifecycleScope
        )
        
        appChangeReceiver = AppChangeReceiver { action, packageName, replacing, user ->
            lifecycleScope.launch {
                val changed = appController.handlePackageEvent(action, packageName, replacing, user)
                if (changed) {
                    refreshAppsUI()
                }
            }
        }
        appChangeReceiver.register(this)

        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Mendaftarkan callback perubahan pintasan dinamis LauncherApps untuk sinkronisasi otomatis
        appController.onShortcutsChangedCallback = {
            lifecycleScope.launch {
                refreshAppsUI()
            }
        }
        appController.registerShortcutCallback(this)

        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Mendaftarkan listener untuk memuat ulang daftar aplikasi saat ada profil user baru dan menyimpan objek Closeable untuk dilepas saat onDestroy
        userChangeListener = com.silauncer.cepat.pm.UserCache.getInstance(this).addUserChangeListener {
            lifecycleScope.launch {
                val sortedApps = appController.loadAppsInitial()
                syncNotificationState(sortedApps)
                adapter.submitLauncherItems(sortedApps)
            }
        }

        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Mendaftarkan observasi ke NotificationRepository untuk memperbarui notification dot pada launcher items secara live
        lifecycleScope.launch {
            com.silauncer.cepat.notification.NotificationRepository.getInstance().notificationState.collect { stateMap ->
                if (adapter.getLauncherItems().isEmpty()) return@collect
                val currentItems = adapter.getLauncherItems()
                var listUpdated = false
                val newItems = currentItems.map { item ->
                    if (item is LauncherItem.App) {
                        val state = stateMap[item.appInfo.packageName]
                        val hasDot = state?.hasNotification == true
                        if (item.appInfo.hasNotification != hasDot) {
                            listUpdated = true
                            LauncherItem.App(item.appInfo.copy(hasNotification = hasDot, dotInfo = null))
                        } else {
                            item
                        }
                    } else if (item is LauncherItem.Folder) {
                        for (folderApp in item.folderInfo.getItems()) {
                            val state = stateMap[folderApp.packageName]
                            val hasDot = state?.hasNotification == true
                            if (folderApp.hasNotification != hasDot) {
                                item.folderInfo.replaceItem(folderApp, folderApp.copy(hasNotification = hasDot, dotInfo = null))
                            }
                        }
                        item
                    } else {
                        item
                    }
                }
                
                if (listUpdated) {
                    adapter.submitLauncherItems(newItems)
                }
            }
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeFolder?.isOpen == true) {
                    activeFolder?.close()
                }
                // Do nothing else on back button as this is a launcher
            }
        })

        handlePinRequestIntent(intent)
        loadAppsInitialUI()
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
    // [Penjelasan]: Mengarahkan seluruh aliran sentuhan layar (TouchEvent) langsung ke GridDragAndDropHandler saat sesi drag sedang berlangsung, sehingga transisi drag dari folder ke workspace atau pergerakan pointer di luar batas RecyclerView tetap terlacak secara konsisten hingga gesture UP/CANCEL
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (::dragHandler.isInitialized && dragHandler.isDragging) {
            if (dragHandler.processTouchEvent(ev)) {
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
    // [Penjelasan]: Menangani intent baru untuk menyematkan pintasan dari aplikasi eksternal via PinItemRequest
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePinRequestIntent(intent)
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
    // [Penjelasan]: Mengekstrak dan menyetujui PinItemRequest dari Android O+ untuk menyematkan shortcut ke workspace
    private fun handlePinRequestIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val pinItemRequest = com.silauncer.cepat.pm.PinRequestHelper.getPinItemRequest(intent)
        if (pinItemRequest != null && pinItemRequest.isValid) {
            if (pinItemRequest.requestType == android.content.pm.LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                val shortcutInfo = pinItemRequest.shortcutInfo
                if (shortcutInfo != null) {
                    pinItemRequest.accept()
                    val workspaceShortcut = com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo.fromShortcutInfo(shortcutInfo)
                    lifecycleScope.launch {
                        val currentItems = adapter.getLauncherItems().toMutableList()
                        currentItems.add(LauncherItem.Shortcut(workspaceShortcut))
                        adapter.submitLauncherItems(currentItems)
                        appController.saveCustomWorkspaceOrder(currentItems)
                        android.widget.Toast.makeText(
                            this@LauncherActivity,
                            getString(R.string.shortcut_pinned_toast),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun openFolderModal(folderInfo: FolderInfo) {
        val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
        val folder = Folder(this)
        activeFolder = folder
        val iconLoader = IconLoader(lifecycleScope)

        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Sembunyikan ikon-ikon grid workspace saat folder terbuka agar tidak tumpang tindih dan wallpaper asli terekspos jernih
        recyclerView.animate().alpha(0f).setDuration(200).start()
        findViewById<View>(R.id.workspace_page_indicator)?.animate()?.alpha(0f)?.setDuration(200)?.start()

        folder.onShowAppInfo = { item, view ->
            if (item is LauncherItem.App) {
                PopupShortcutHandler(this, actionHandler).showAppMenu(item.appInfo, view)
            } else if (item is LauncherItem.Shortcut) {
                // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
                // [Penjelasan]: Menampilkan popup menu hapus pintasan saat shortcut di dalam folder di-long-press
                PopupShortcutHandler(this, actionHandler).showShortcutMenu(item.shortcutInfo.title.toString(), view) {
                    lifecycleScope.launch {
                        folderInfo.removeShortcut(item.shortcutInfo)
                        val currentItems = adapter.getLauncherItems()
                        appController.saveCustomWorkspaceOrder(currentItems)
                        if (folderInfo.itemCount() == 0) {
                            activeFolder?.close()
                        }
                    }
                }
            }
        }
        folder.onDragOutBoundaryPassed = { item, info, view, rawX, rawY ->
            // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
            // [Penjelasan]: Saat pointer melewati garis batas keluar folder, langsung tampilkan workspace dan alihkan drag aktif ke Workspace
            recyclerView.alpha = 1f
            findViewById<View>(R.id.workspace_page_indicator)?.alpha = 1f
            dragHandler.startDragFromFolder(item, info, rawX, rawY, view)
            folder.close(animate = false)
        }

        folder.show(
            parent = rootView,
            info = folderInfo,
            loader = iconLoader,
            onAppClick = { item ->
                when (item) {
                    is LauncherItem.App -> actionHandler.launchApp(item.appInfo)
                    is LauncherItem.Shortcut -> actionHandler.launchShortcut(item.shortcutInfo)
                    else -> {}
                }
            },
            onDragExit = {
                // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
                // [Penjelasan]: Menampilkan kembali grid workspace seketika saat sentuhan drag keluar dari batas folder agar pengguna dapat melihat tujuan drop
                recyclerView.animate().alpha(1f).setDuration(150).start()
                findViewById<View>(R.id.workspace_page_indicator)?.animate()?.alpha(1f)?.setDuration(150)?.start()
            },
            onDragOut = { item, info, rawX, rawY ->
                // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
                // [Penjelasan]: Tampilkan kembali grid workspace secara cepat saat item ditarik keluar dari folder agar area drop terlihat
                recyclerView.animate().alpha(1f).setDuration(150).start()
                findViewById<View>(R.id.workspace_page_indicator)?.animate()?.alpha(1f)?.setDuration(150)?.start()
                dragHandler.handleDragOutFromFolder(item, info, rawX, rawY)
            },
            onClose = {
                activeFolder = null
                // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
                // [Penjelasan]: Kembalikan opacity grid workspace secara halus saat folder ditutup
                recyclerView.animate().alpha(1f).setDuration(200).start()
                findViewById<View>(R.id.workspace_page_indicator)?.animate()?.alpha(1f)?.setDuration(200)?.start()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (recyclerView.layoutManager is GridLayoutManager) {
            val currentColumns = (recyclerView.layoutManager as GridLayoutManager).spanCount
            if (currentColumns != prefs.gridColumns) {
                // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
                // [Penjelasan]: Memperbarui jumlah kolom tata letak vertikal secara dinamis saat preferensi grid diubah oleh pengguna di Settings
                recyclerView.layoutManager = GridLayoutManager(this, prefs.gridColumns, RecyclerView.VERTICAL, false)
            }
        }
        val currentIconSizePx = (prefs.iconSize * resources.displayMetrics.density).toInt()
        val currentSpacingPx = (prefs.iconSpacing * resources.displayMetrics.density).toInt()
        adapter.updateConfig(currentIconSizePx, prefs.showAppLabel, prefs.labelSize, currentSpacingPx, prefs.gridRows)
        
        if (isLoaded) {
            refreshAppsUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appChangeReceiver.unregister(this)
        // [Jalur Class]: com.silauncer.cepat.launcher.LauncherActivity
        // [Penjelasan]: Membersihkan listener notifikasi, listener shortcut, dan listener UserCache serta menutup popup aktif via dragHandler saat LauncherActivity dihancurkan untuk mencegah memory leak dan WindowLeak.
        if (::dragHandler.isInitialized) {
            dragHandler.dismissPopups()
        }
        appController.unregisterShortcutCallback(this)
        userChangeListener?.close()
        userChangeListener = null
    }

    private fun syncNotificationState(items: List<com.silauncer.cepat.launcher.LauncherItem>): List<com.silauncer.cepat.launcher.LauncherItem> {
        val stateMap = com.silauncer.cepat.notification.NotificationRepository.getInstance().notificationState.value
        return items.map { item ->
            if (item is com.silauncer.cepat.launcher.LauncherItem.App) {
                val state = stateMap[item.appInfo.packageName]
                val hasDot = state?.hasNotification == true
                if (item.appInfo.hasNotification != hasDot) {
                    com.silauncer.cepat.launcher.LauncherItem.App(item.appInfo.copy(hasNotification = hasDot, dotInfo = null))
                } else {
                    item
                }
            } else if (item is com.silauncer.cepat.launcher.LauncherItem.Folder) {
                for (folderApp in item.folderInfo.getItems()) {
                    val state = stateMap[folderApp.packageName]
                    val hasDot = state?.hasNotification == true
                    if (folderApp.hasNotification != hasDot) {
                        item.folderInfo.replaceItem(folderApp, folderApp.copy(hasNotification = hasDot, dotInfo = null))
                    }
                }
                item
            } else {
                item
            }
        }
    }

    private fun loadAppsInitialUI() {
        lifecycleScope.launch {
            val sortedApps = appController.loadAppsInitial()
            val syncedApps = syncNotificationState(sortedApps)
            adapter.submitLauncherItems(syncedApps)
            isLoaded = true
        }
    }
    
    private fun refreshAppsUI() {
        lifecycleScope.launch {
            val sortedApps = appController.refreshApps()
            syncNotificationState(sortedApps)
            adapter.submitLauncherItems(sortedApps)
        }
    }
}
