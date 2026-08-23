package com.silauncer.cepat.secondarydisplay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.apps.AppDataSource
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.apps.AppStateHolder
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.launcher.LauncherAppController
import com.silauncer.cepat.launcher.PopupShortcutHandler
import com.silauncer.cepat.popup.SystemShortcut
import com.silauncer.cepat.storage.LauncherPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * SecondaryDisplayLauncher
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
 * // [Penjelasan]: Activity Launcher untuk layar sekunder (Secondary Display / Desktop Mode).
 * Menampilkan grid aplikasi tersemat di workspace, laci aplikasi melayang dengan animasi circular reveal, dan dukungan popup context menu.
 */
class SecondaryDisplayLauncher : Activity() {

    private lateinit var dragLayer: SecondaryDragLayer
    private lateinit var workspaceGrid: GridView
    private lateinit var allAppsButton: View
    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
    // [Penjelasan]: Diubah tipe data kontainer laci dari FrameLayout menjadi LinearLayout vertikal untuk memuat bilah Saran Pintar secara bertumpuk.
    private lateinit var appsDrawerContainer: LinearLayout
    private lateinit var allAppsGrid: GridView
    private lateinit var smartPredictionsTitle: View
    private lateinit var smartPredictionsRow: LinearLayout
    private lateinit var predictionsDivider: View
    private lateinit var allAppsTitle: View

    private lateinit var pinnedAppsAdapter: PinnedAppsAdapter
    private lateinit var allAppsAdapter: AllAppsGridAdapter

    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
    // [Penjelasan]: Mendaftarkan receiver perubahan paket sistem dan listener user profile untuk menyinkronkan daftar aplikasi secara reaktif.
    private lateinit var appChangeReceiver: com.silauncer.cepat.apps.AppChangeReceiver
    private var userChangeListener: java.io.Closeable? = null

    private lateinit var appController: LauncherAppController
    private lateinit var actionHandler: AppActionHandler
    private lateinit var popupShortcutHandler: PopupShortcutHandler
    private lateinit var secondaryPredictions: SecondaryDisplayPredictions

    private var isDrawerShown = false
    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
    // [Penjelasan]: CoroutineScope yang terikat pada siklus hidup SecondaryDisplayLauncher dengan SupervisorJob untuk pemuatan data dan icon
    private val scope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private lateinit var iconLoader: IconLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.secondary_launcher)

        iconLoader = IconLoader(scope)

        dragLayer = findViewById(R.id.drag_layer)
        workspaceGrid = findViewById(R.id.workspace_grid)
        allAppsButton = findViewById(R.id.all_apps_button)
        appsDrawerContainer = findViewById(R.id.apps_drawer_container)
        allAppsGrid = findViewById(R.id.all_apps_grid)
        
        // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
        // [Penjelasan]: Menginisialisasi komponen UI khusus prediksi aplikasi (Saran Pintar) pada laci aplikasi.
        smartPredictionsTitle = findViewById(R.id.smart_predictions_title)
        smartPredictionsRow = findViewById(R.id.smart_predictions_row)
        predictionsDivider = findViewById(R.id.predictions_divider)
        allAppsTitle = findViewById(R.id.all_apps_title)

        actionHandler = AppActionHandler(this)
        
        // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
        // [Penjelasan]: Menggunakan singleton LauncherPreferences.getInstance() untuk efisiensi alokasi memori.
        val prefs = LauncherPreferences.getInstance()
        val appStateHolder = AppStateHolder()
        val appDataSource = AppDataSource(this)
        appController = LauncherAppController(appDataSource, appStateHolder, prefs, com.silauncer.cepat.database.WorkspaceRepository(this))

        popupShortcutHandler = PopupShortcutHandler(this, actionHandler)
        secondaryPredictions = SecondaryDisplayPredictions.newInstance(this)

        initUi()
    }

    private fun initUi() {
        pinnedAppsAdapter = PinnedAppsAdapter(
            context = this,
            onAppClickListener = { view ->
                val appInfo = view.tag as? AppInfo
                if (appInfo != null) {
                    actionHandler.launchApp(appInfo)
                }
            },
            onAppLongClickListener = { view ->
                val appInfo = view.tag as? AppInfo
                if (appInfo != null) {
                    showAppShortcutPopup(view, appInfo)
                    true
                } else false
            }
        )
        workspaceGrid.adapter = pinnedAppsAdapter
        pinnedAppsAdapter.init()

        allAppsAdapter = AllAppsGridAdapter(
            iconLoader = iconLoader,
            onAppClickListener = { appInfo ->
                actionHandler.launchApp(appInfo)
            },
            onAppLongClickListener = { view, appInfo ->
                showAppShortcutPopup(view, appInfo)
                true
            }
        )
        allAppsGrid.adapter = allAppsAdapter

        allAppsButton.setOnClickListener {
            showAppDrawer(!isDrawerShown)
        }

        dragLayer.setupDrawerCallbacks(
            isOpenSupplier = { isDrawerShown },
            closeAction = { showAppDrawer(false) }
        )

        // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
        // [Penjelasan]: Mendaftarkan AppChangeReceiver untuk mendengarkan broadcast perubahan paket aplikasi (tambah/hapus/update) secara reaktif.
        appChangeReceiver = com.silauncer.cepat.apps.AppChangeReceiver { action, packageName, replacing, user ->
            scope.launch {
                val changed = appController.handlePackageEvent(action, packageName, replacing, user)
                if (changed) {
                    refreshSecondaryAppsUI()
                }
            }
        }
        appChangeReceiver.register(this)

        // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
        // [Penjelasan]: Mendaftarkan UserChangeListener untuk memuat ulang daftar aplikasi saat ada profil pengguna baru.
        userChangeListener = com.silauncer.cepat.pm.UserCache.getInstance(this).addUserChangeListener {
            scope.launch {
                appController.loadAppsInitial()
                refreshSecondaryAppsUI()
            }
        }

        scope.launch {
            appController.loadAppsInitial()
            refreshSecondaryAppsUI()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
    // [Penjelasan]: Menyegarkan adapter laci aplikasi (all apps), aplikasi tersemat di workspace (pinned apps), dan daftar saran pintar (smart predictions) secara sinkron dan reaktif.
    private suspend fun refreshSecondaryAppsUI() {
        val apps = appController.getAllAppsRaw()
        allAppsAdapter.setApps(apps)
        pinnedAppsAdapter.setAllApps(apps)
        secondaryPredictions.setPredictedApps(apps)
        renderPredictedApps()
    }

    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
    // [Penjelasan]: Merender jajaran item aplikasi terprediksi (saran pintar) ke dalam smart_predictions_row secara asinkron lengkap dengan pemuat ikon terkelola dan click listeners.
    private fun renderPredictedApps() {
        smartPredictionsRow.removeAllViews()
        val predictedList = secondaryPredictions.getPredictedApps()
        if (predictedList.isEmpty()) {
            smartPredictionsTitle.visibility = View.GONE
            smartPredictionsRow.visibility = View.GONE
            predictionsDivider.visibility = View.GONE
            allAppsTitle.visibility = View.GONE
            return
        }

        smartPredictionsTitle.visibility = View.VISIBLE
        smartPredictionsRow.visibility = View.VISIBLE
        predictionsDivider.visibility = View.VISIBLE
        allAppsTitle.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)

        for (appInfo in predictedList) {
            val appView = inflater.inflate(R.layout.item_app, smartPredictionsRow, false)
            val iconView = appView.findViewById<ImageView>(R.id.app_icon)
            val nameView = appView.findViewById<TextView>(R.id.app_name)

            iconView.tag = appInfo.cacheKey
            iconLoader.loadIconAsync(this, appInfo, 56) { drawable, loadedKey ->
                if (iconView.tag == loadedKey) {
                    iconView.setImageDrawable(drawable)
                }
            }
            nameView.text = appInfo.name
            appView.tag = appInfo

            appView.setOnClickListener {
                actionHandler.launchApp(appInfo)
                // Memperbarui urutan saran pintar sesaat setelah aplikasi diluncurkan agar interaktif dan real-time
                refreshPredictions()
            }
            appView.setOnLongClickListener {
                showAppShortcutPopup(appView, appInfo)
                true
            }

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            appView.layoutParams = params
            smartPredictionsRow.addView(appView)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
    // [Penjelasan]: Menyegarkan urutan prediksi saran pintar berdasarkan pembaruan data frekuensi peluncuran terakhir pengguna.
    private fun refreshPredictions() {
        val apps = allAppsAdapter.getApps()
        if (apps.isNotEmpty()) {
            secondaryPredictions.setPredictedApps(apps)
            renderPredictedApps()
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
     * // [Penjelasan]: Membuka atau menutup laci aplikasi (all apps drawer) menggunakan animasi circular reveal dari posisi tombol apps.
     */
    fun showAppDrawer(show: Boolean) {
        if (show == isDrawerShown) return

        val width = appsDrawerContainer.width.coerceAtLeast(1)
        val height = appsDrawerContainer.height.coerceAtLeast(1)
        val openR = hypot(width.toDouble(), height.toDouble()).toFloat()
        val closeR = 24f

        val startX = allAppsButton.x.toInt() + allAppsButton.width / 2
        val startY = allAppsButton.y.toInt() + allAppsButton.height / 2

        if (show) {
            isDrawerShown = true
            appsDrawerContainer.visibility = View.VISIBLE
            val anim = ViewAnimationUtils.createCircularReveal(
                appsDrawerContainer, startX, startY, closeR, openR
            )
            anim.start()
        } else {
            isDrawerShown = false
            val anim = ViewAnimationUtils.createCircularReveal(
                appsDrawerContainer, startX, startY, openR, closeR
            )
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    appsDrawerContainer.visibility = View.INVISIBLE
                }
            })
            anim.start()
        }
    }

    private fun showAppShortcutPopup(anchorView: View, appInfo: AppInfo) {
        val systemShortcuts = mutableListOf<SystemShortcut>()
        systemShortcuts.add(SystemShortcut.AppInfoShortcut(this, appInfo, anchorView))
        systemShortcuts.add(SystemShortcut.UninstallShortcut(this, appInfo, actionHandler, anchorView))
        systemShortcuts.add(pinnedAppsAdapter.getSystemShortcut(appInfo, anchorView))

        popupShortcutHandler.showAppMenu(appInfo, anchorView, systemShortcuts)
    }

    override fun onDestroy() {
        super.onDestroy()
        // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher
        // [Penjelasan]: Membersihkan receiver perubahan paket sistem, listener user profile, adapter tersemat, dan membatalkan coroutine scope saat activity dihancurkan untuk mencegah memory leak.
        if (::appChangeReceiver.isInitialized) {
            try {
                appChangeReceiver.unregister(this)
            } catch (e: Throwable) {
                // Ignore if not registered or already unregistered
            }
        }
        userChangeListener?.close()
        userChangeListener = null
        pinnedAppsAdapter.destroy()
        scope.cancel()
    }

    /**
     * AllAppsGridAdapter
     *
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher.AllAppsGridAdapter
     * // [Penjelasan]: Adapter GridView internal untuk menyajikan seluruh aplikasi terpasang pada laci aplikasi layar sekunder menggunakan IconLoader terkelola.
     */
    private class AllAppsGridAdapter(
        private val iconLoader: IconLoader,
        private val onAppClickListener: (AppInfo) -> Unit,
        private val onAppLongClickListener: (View, AppInfo) -> Boolean
    ) : BaseAdapter() {

        private val appsList = ArrayList<AppInfo>()

        fun setApps(apps: List<AppInfo>) {
            appsList.clear()
            appsList.addAll(apps)
            appsList.sortBy { it.name.lowercase() }
            notifyDataSetChanged()
        }

        // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayLauncher.AllAppsGridAdapter
        // [Penjelasan]: Mengekspos daftar aplikasi saat ini di adapter untuk penyegaran saran pintar secara dinamis.
        fun getApps(): List<AppInfo> = ArrayList(appsList)

        override fun getCount(): Int = appsList.size

        override fun getItem(position: Int): AppInfo = appsList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            val appInfo = getItem(position)

            val iconView = view.findViewById<ImageView>(R.id.app_icon)
            val nameView = view.findViewById<TextView>(R.id.app_name)

            iconView.tag = appInfo.cacheKey
            iconLoader.loadIconAsync(parent.context, appInfo, 56) { drawable, loadedKey ->
                if (iconView.tag == loadedKey) {
                    iconView.setImageDrawable(drawable)
                }
            }
            nameView.text = appInfo.name

            view.tag = appInfo
            view.setOnClickListener { onAppClickListener(appInfo) }
            view.setOnLongClickListener { onAppLongClickListener(view, appInfo) }

            return view
        }
    }
}
