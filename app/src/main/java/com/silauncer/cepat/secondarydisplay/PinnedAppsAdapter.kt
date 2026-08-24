package com.silauncer.cepat.secondarydisplay

import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.database.WorkspaceRepository
import com.silauncer.cepat.pm.UserCache
import com.silauncer.cepat.popup.SystemShortcut
import com.silauncer.cepat.util.PackageUserKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * PinnedAppsAdapter
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
 * // [Penjelasan]: Adapter khusus untuk mengelola dan menampilkan aplikasi tersemat (pinned apps) pada grid desktop layar sekunder (Secondary Display).
 * Menyimpan persistensi aplikasi tersemat pada database Room melalui WorkspaceRepository secara terstruktur dan terisolasi dari preferensi ringan MMKV.
 */
class PinnedAppsAdapter(
    private val context: Context,
    private val onAppClickListener: View.OnClickListener,
    private val onAppLongClickListener: View.OnLongClickListener
) : BaseAdapter() {

    private val workspaceRepo = WorkspaceRepository(context)
    private val userCache: UserCache = UserCache.getInstance(context)
    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
    // [Penjelasan]: Mengelola CoroutineScope dengan SupervisorJob agar pemuatan icon terisolasi dan dapat dibatalkan saat destroy
    private val scope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private val iconLoader: IconLoader = IconLoader(scope)

    private val pinnedKeys = HashSet<PackageUserKey>()
    private val allApps = ArrayList<AppInfo>()
    private val displayedItems = ArrayList<AppInfo>()

    init {
        initPinnedKeys()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Memuat daftar aplikasi tersemat awal dari database Room.
     */
    fun init() {
        loadPinnedKeys()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Membatalkan coroutine scope ketika adapter/view dilepas dari window.
     */
    fun destroy() {
        scope.cancel()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Memperbarui daftar seluruh aplikasi terpasang dari LauncherAppController dan menyaring aplikasi yang tersemat.
     */
    fun setAllApps(apps: List<AppInfo>) {
        allApps.clear()
        allApps.addAll(apps)
        createFilteredAppsList()
    }

    private fun initPinnedKeys() {
        loadPinnedKeys()
    }

    private fun loadPinnedKeys() {
        scope.launch {
            val rawSet = workspaceRepo.getSecondaryPinnedApps()
            pinnedKeys.clear()
            for (raw in rawSet) {
                val key = parsePackageUserKey(raw)
                if (key != null) {
                    pinnedKeys.add(key)
                }
            }
            createFilteredAppsList()
        }
    }

    private fun savePinnedKeys() {
        val rawSet = HashSet<String>()
        for (key in pinnedKeys) {
            rawSet.add(encodePackageUserKey(key))
        }
        scope.launch {
            workspaceRepo.saveSecondaryPinnedApps(rawSet)
        }
    }

    private fun createFilteredAppsList() {
        displayedItems.clear()
        for (app in allApps) {
            val key = PackageUserKey(app.packageName, app.user)
            if (pinnedKeys.contains(key)) {
                displayedItems.add(app)
            }
        }
        displayedItems.sortBy { it.name.lowercase() }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = displayedItems.size

    override fun getItem(position: Int): AppInfo = displayedItems[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val appInfo = getItem(position)

        val iconView = view.findViewById<ImageView>(R.id.app_icon)
        val nameView = view.findViewById<TextView>(R.id.app_name)

        iconView.tag = appInfo.cacheKey
        iconLoader.loadIconAsync(context, appInfo, 56) { drawable, loadedKey ->
            if (iconView.tag == loadedKey) {
                iconView.setImageDrawable(drawable)
            }
        }
        nameView.text = appInfo.name

        view.tag = appInfo
        view.setOnClickListener(onAppClickListener)
        view.setOnLongClickListener(onAppLongClickListener)

        return view
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Menyematkan (pin) atau melepas sematan (unpin) aplikasi dari desktop layar sekunder.
     */
    fun togglePinned(appInfo: AppInfo) {
        val key = PackageUserKey(appInfo.packageName, appInfo.user)
        if (pinnedKeys.contains(key)) {
            pinnedKeys.remove(key)
        } else {
            pinnedKeys.add(key)
        }
        savePinnedKeys()
        createFilteredAppsList()
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Menyematkan aplikasi ke desktop layar sekunder.
     */
    fun addPinnedApp(appInfo: AppInfo) {
        val key = PackageUserKey(appInfo.packageName, appInfo.user)
        if (pinnedKeys.add(key)) {
            savePinnedKeys()
            createFilteredAppsList()
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Mencopot aplikasi dari daftar tersemat layar sekunder.
     */
    fun removePinnedApp(appInfo: AppInfo) {
        val key = PackageUserKey(appInfo.packageName, appInfo.user)
        if (pinnedKeys.remove(key)) {
            savePinnedKeys()
            createFilteredAppsList()
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Memeriksa apakah suatu aplikasi sedang tersemat di desktop layar sekunder.
     */
    fun isPinned(appInfo: AppInfo): Boolean {
        return pinnedKeys.contains(PackageUserKey(appInfo.packageName, appInfo.user))
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter
     * // [Penjelasan]: Membuat pintasan sistem (PinUnPinShortcut) untuk disajikan pada popup menu item aplikasi.
     */
    fun getSystemShortcut(appInfo: AppInfo, originalView: View?): SystemShortcut {
        return PinUnPinShortcut(context, appInfo, isPinned(appInfo), originalView) {
            togglePinned(appInfo)
        }
    }

    private fun parsePackageUserKey(raw: String): PackageUserKey? {
        val parts = raw.split("#")
        if (parts.isEmpty()) return null

        val packageName = parts[0]
        val userHandle: UserHandle = if (parts.size > 1) {
            try {
                userCache.getUserForSerialNumber(parts[1].toLong()) ?: Process.myUserHandle()
            } catch (_: Exception) {
                Process.myUserHandle()
            }
        } else {
            Process.myUserHandle()
        }

        return PackageUserKey(packageName, userHandle)
    }

    private fun encodePackageUserKey(key: PackageUserKey): String {
        val userHandle = key.user ?: Process.myUserHandle()
        val serial = userCache.getSerialNumberForUser(userHandle)
        return "${key.packageName}#$serial"
    }

    /**
     * PinUnPinShortcut
     *
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.PinnedAppsAdapter.PinUnPinShortcut
     * // [Penjelasan]: SystemShortcut khusus untuk menyematkan atau melepas sematan aplikasi pada desktop layar sekunder.
     */
    class PinUnPinShortcut(
        context: Context,
        appInfo: AppInfo,
        val isPinned: Boolean,
        originalView: View?,
        private val onToggleAction: () -> Unit
    ) : SystemShortcut(
        iconResId = if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin,
        labelResId = if (isPinned) R.string.remove_from_home else R.string.pin_to_home,
        targetContext = context,
        appInfo = appInfo,
        originalView = originalView
    ) {
        override fun onClick(view: View?) {
            onToggleAction()
        }
    }
}
