package com.silauncer.cepat.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.apps.PackageManagerHelper
import com.silauncer.cepat.shortcuts.ShortcutLauncher
import com.silauncer.cepat.shortcuts.ShortcutRequest

/**
 * PopupShortcutHandler
 *
 * // [Jalur Class]: com.silauncer.cepat.launcher.PopupShortcutHandler
 * // [Penjelasan]: Mengelola floating popup context menu kartu gelap untuk folder dan aplikasi, serta mendelegasikan shortcuts ke com.silauncer.cepat.shortcuts.
 */
// [Jalur Class]: com.silauncer.cepat.launcher.PopupShortcutHandler
// [Penjelasan]: Mengelola floating popup context menu kartu gelap untuk folder dan aplikasi, mengintegrasikan notifikasi aktif, deep shortcuts, dan pintasan sistem melalui PopupContainerWithArrow.
class PopupShortcutHandler(
    private val context: Context,
    private val actionHandler: AppActionHandler
) {
    private var activeDialog: AlertDialog? = null
    private var activePopupWindow: PopupWindow? = null
    private var activePopupContainer: com.silauncer.cepat.popup.PopupContainerWithArrow? = null
    private val popupDataProvider = com.silauncer.cepat.popup.PopupDataProvider()

    // [Jalur Class]: com.silauncer.cepat.launcher.PopupShortcutHandler
    // [Penjelasan]: Menampilkan popup menu floating modern (PopupContainerWithArrow) yang mencakup notifikasi aktif, deep shortcuts terurut, dan pintasan sistem (App Info, Uninstall, Pin/Unpin).
    fun showAppMenu(
        app: AppInfo,
        view: View,
        customSystemShortcuts: List<com.silauncer.cepat.popup.SystemShortcut>? = null,
        onPinShortcut: ((ShortcutInfo) -> Unit)? = null
    ) {
        dismissAppMenu()
        
        if (view.windowToken != null) {
            val popupContainer = LayoutInflater.from(context).inflate(
                R.layout.popup_container,
                null
            ) as com.silauncer.cepat.popup.PopupContainerWithArrow
            activePopupContainer = popupContainer

            val shortcuts = fetchShortcuts(app)
            val notifications = popupDataProvider.getNotificationsForItem(context, app)
            val systemShortcuts = customSystemShortcuts ?: listOf(
                com.silauncer.cepat.popup.SystemShortcut.AppInfoShortcut(context, app, view),
                com.silauncer.cepat.popup.SystemShortcut.UninstallShortcut(context, app, actionHandler, view)
            )

            popupContainer.showForApp(
                anchorView = view,
                appInfo = app,
                shortcuts = shortcuts,
                notifications = notifications,
                systemShortcuts = systemShortcuts,
                popupDataProvider = popupDataProvider,
                onPinShortcutCallback = onPinShortcut,
                onDismissCallback = {
                    activePopupContainer = null
                }
            )
            return
        }

        // Fallback ke AlertDialog jika view belum terpasang di window
        val shortcuts = fetchShortcuts(app)
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        val shortcutLauncher = com.silauncer.cepat.shortcuts.ShortcutLauncher(context)
        for (shortcut in shortcuts) {
            val label = shortcut.longLabel?.takeIf { it.isNotBlank() }
                ?: shortcut.shortLabel?.takeIf { it.isNotBlank() }
                ?: shortcut.id
            options.add(label.toString())
            actions.add { shortcutLauncher.startShortcut(shortcut, view) }
        }

        options.add(context.getString(R.string.app_info))
        actions.add {
            val bounds = Rect()
            view.getGlobalVisibleRect(bounds)
            val opts = ActivityOptions.makeBasic().toBundle()
            PackageManagerHelper(context).startDetailsActivityForInfo(app, bounds, opts)
        }

        options.add(context.getString(R.string.uninstall))
        actions.add { actionHandler.requestUninstall(app) }

        activeDialog = AlertDialog.Builder(context)
            .setTitle(app.name)
            .setItems(options.toTypedArray()) { _, which ->
                actions[which]()
            }
            .setOnDismissListener { activeDialog = null }
            .show()
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.PopupShortcutHandler
    // [Penjelasan]: Menampilkan popup menu floating dark card khusus untuk Shortcut di workspace (Hapus pintasan)
    fun showShortcutMenu(shortcutName: String, anchorView: View? = null, onDelete: () -> Unit) {
        dismissAppMenu()
        if (anchorView != null && anchorView.windowToken != null) {
            val popupView = LayoutInflater.from(context).inflate(R.layout.view_folder_context_popup, null)
            val btnDelete = popupView.findViewById<View>(R.id.btn_delete_folder)
            val tvLabel = popupView.findViewById<android.widget.TextView>(R.id.tv_delete_folder_label)
            tvLabel?.setText(R.string.hapus_pintasan)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                elevation = 16f
                setOnDismissListener { activePopupWindow = null }
            }

            btnDelete?.setOnClickListener {
                popupWindow.dismiss()
                onDelete()
            }

            // Hitung posisi di atas / samping anchorView
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight

            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            val anchorX = location[0]
            val anchorY = location[1]

            val xOff = anchorX + (anchorView.width - popupWidth) / 2
            val yOff = anchorY - popupHeight - 16

            activePopupWindow = popupWindow
            try {
                popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xOff.coerceAtLeast(16), yOff.coerceAtLeast(16))
                return
            } catch (_: Exception) {
                // Fallback ke alert dialog jika window attachment bermasalah
            }
        }

        val options = arrayOf(context.getString(R.string.hapus_pintasan))
        activeDialog = AlertDialog.Builder(context)
            .setTitle(shortcutName)
            .setItems(options) { _, _ ->
                onDelete()
            }
            .setOnDismissListener { activeDialog = null }
            .show()
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.PopupShortcutHandler
    // [Penjelasan]: Menampilkan popup menu floating dark card khusus untuk folder di workspace (Hapus Folder) sesuai Screenshot 2.
    fun showFolderMenu(folderName: String, anchorView: View? = null, onDelete: () -> Unit) {
        dismissAppMenu()
        if (anchorView != null && anchorView.windowToken != null) {
            val popupView = LayoutInflater.from(context).inflate(R.layout.view_folder_context_popup, null)
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                elevation = 16f
                setOnDismissListener { activePopupWindow = null }
            }

            popupView.findViewById<View>(R.id.btn_delete_folder).setOnClickListener {
                popupWindow.dismiss()
                onDelete()
            }

            // Hitung posisi di atas / samping anchorView
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight

            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            val anchorX = location[0]
            val anchorY = location[1]

            val xOff = anchorX + (anchorView.width - popupWidth) / 2
            val yOff = anchorY - popupHeight - 16

            activePopupWindow = popupWindow
            try {
                popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xOff.coerceAtLeast(16), yOff.coerceAtLeast(16))
                return
            } catch (_: Exception) {
                // Fallback ke alert dialog jika window attachment bermasalah
            }
        }

        val options = arrayOf(context.getString(R.string.delete_folder))
        activeDialog = AlertDialog.Builder(context)
            .setTitle(folderName)
            .setItems(options) { _, _ ->
                onDelete()
            }
            .setOnDismissListener { activeDialog = null }
            .show()
    }

    private fun fetchShortcuts(app: AppInfo): List<ShortcutInfo> {
        // [Jalur Class]: com.silauncer.cepat.launcher.PopupShortcutHandler
        // [Penjelasan]: Menggunakan ShortcutRequest untuk query shortcut Launcher3
        val request = ShortcutRequest(context, app.user)
            .forPackage(app.packageName)
        val result = request.query(ShortcutRequest.ALL)
        return if (result.wasSuccess) result else emptyList()
    }

    fun dismissAppMenu() {
        activePopupContainer?.closeComplete()
        activePopupContainer = null
        activePopupWindow?.dismiss()
        activePopupWindow = null
        activeDialog?.dismiss()
        activeDialog = null
    }
}

