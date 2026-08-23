package com.silauncer.cepat.launcher

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.home.AppAdapter
import com.silauncer.cepat.touch.CheckLongPressHelper
import kotlin.math.abs

/**
 * WorkspaceGestureDetector
 *
 * [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
 * [Penjelasan]: Menangani deteksi gestur long press dan shortcut, dipisahkan dari GridDragAndDropHandler sesuai prinsip SRP.
 */
// [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
// [Penjelasan]: Menambahkan parameter koordinat sentuhan awal dan terkini (initialRawX, initialRawY, currentRawX, currentRawY) pada callback mulai drag untuk inisialisasi presisi DragView
class WorkspaceGestureDetector(
    private val context: Context,
    private val adapter: AppAdapter,
    actionHandler: AppActionHandler,
    private val startDragCallback: (RecyclerView.ViewHolder, Float, Float, Float, Float) -> Unit,
    private val onOrderModified: (List<com.silauncer.cepat.launcher.LauncherItem>) -> Unit
) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val popupHandler = PopupShortcutHandler(context, actionHandler)

    private var initialX = 0f
    private var initialY = 0f
    private var initialRawX = 0f
    private var initialRawY = 0f
    private var activePointerId = -1

    var currentTarget: RecyclerView.ViewHolder? = null
        private set
    private var currentApp: AppInfo? = null
    private var currentFolder: com.silauncer.cepat.folder.FolderInfo? = null
    private var currentShortcut: com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo? = null

    var hasPerformedLongPress = false
        private set
    private var pendingPopup = false
    private var longPressHelper: CheckLongPressHelper? = null

    // [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
    // [Penjelasan]: Memicu haptic feedback dan menandai pending popup saat gestur long press terdeteksi untuk app, folder, atau shortcut
    private fun triggerLongPress() {
        if (currentTarget != null && (currentApp != null || currentFolder != null || currentShortcut != null) && !hasPerformedLongPress) {
            hasPerformedLongPress = true
            currentTarget!!.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            pendingPopup = true
        }
    }

    fun cancelLongPress() {
        longPressHelper?.cancelLongPress()
        hasPerformedLongPress = false
        pendingPopup = false
    }

    fun processTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
                // [Penjelasan]: Menutup popup lama secara instan pada ACTION_DOWN baru agar tidak mengganggu interaksi sentuhan baru
                popupHandler.dismissAppMenu()

                activePointerId = e.getPointerId(0)
                initialX = e.x
                initialY = e.y
                initialRawX = e.rawX
                initialRawY = e.rawY
                cancelLongPress()

                val child = rv.findChildViewUnder(e.x, e.y)
                if (child != null) {
                    currentTarget = rv.getChildViewHolder(child)
                    val pos = currentTarget?.bindingAdapterPosition ?: RecyclerView.NO_POSITION
                    if (pos != RecyclerView.NO_POSITION) {
                        val item = adapter.getItem(pos)
                        currentApp = (item as? LauncherItem.App)?.appInfo
                        currentFolder = (item as? LauncherItem.Folder)?.folderInfo
                        currentShortcut = (item as? LauncherItem.Shortcut)?.shortcutInfo
                        if (currentApp != null || currentFolder != null || currentShortcut != null) {
                            longPressHelper = CheckLongPressHelper(child, View.OnLongClickListener {
                                triggerLongPress()
                                true
                            })
                            longPressHelper?.onTouchEvent(e, child.left.toFloat(), child.top.toFloat())
                        }
                    }
                } else {
                    currentTarget = null
                    currentApp = null
                    currentFolder = null
                    currentShortcut = null
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == -1) return false
                val pointerIndex = e.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return false

                val child = currentTarget?.itemView
                val offsetX = child?.left?.toFloat() ?: 0f
                val offsetY = child?.top?.toFloat() ?: 0f
                longPressHelper?.onTouchEvent(e, offsetX, offsetY)

                val x = e.getX(pointerIndex)
                val y = e.getY(pointerIndex)
                val dx = abs(x - initialX)
                val dy = abs(y - initialY)

                if (dx > touchSlop || dy > touchSlop) {
                    if (hasPerformedLongPress && currentTarget != null) {
                        pendingPopup = false
                        popupHandler.dismissAppMenu()
                        // [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
                        // [Penjelasan]: Memicu callback drag dengan mengirim view target serta titik awal raw sentuhan dan koordinat saat ini
                        startDragCallback(currentTarget!!, initialRawX, initialRawY, e.rawX, e.rawY)
                        return true // drag started
                    } else if (!hasPerformedLongPress) {
                        cancelLongPress()
                        currentTarget = null
                        currentApp = null
                        currentFolder = null
                        currentShortcut = null
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                val wasLongPress = hasPerformedLongPress
                val child = currentTarget?.itemView
                val offsetX = child?.left?.toFloat() ?: 0f
                val offsetY = child?.top?.toFloat() ?: 0f
                longPressHelper?.onTouchEvent(e, offsetX, offsetY)
                if (pendingPopup && currentTarget != null) {
                    if (currentApp != null) {
                        popupHandler.showAppMenu(
                            app = currentApp!!,
                            view = currentTarget!!.itemView,
                            onPinShortcut = { shortcutInfo ->
                                val workspaceShortcut = com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo.fromShortcutInfo(shortcutInfo)
                                val currentItems = adapter.getLauncherItems().toMutableList()
                                currentItems.add(LauncherItem.Shortcut(workspaceShortcut))
                                adapter.submitLauncherItems(currentItems)
                                onOrderModified(currentItems)
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(com.silauncer.cepat.R.string.shortcut_pinned_toast),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else if (currentFolder != null) {
                        val targetFolder = currentFolder!!
                        val folderName = if (targetFolder.title.isNotBlank()) targetFolder.title else context.getString(com.silauncer.cepat.R.string.folder_unnamed)
                        val anchor = currentTarget?.itemView
                        // [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
                        // [Penjelasan]: Menampilkan popup menu floating 'Hapus folder' yang ter-anchor ke folder icon
                        popupHandler.showFolderMenu(folderName, anchor) {
                            // [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
                            // [Penjelasan]: Langsung mengembalikan seluruh item/aplikasi dari dalam folder ke workspace, menghapus folder dari database & tampilan secara realtime tanpa dialog konfirmasi bertumpuk
                            val allItems = targetFolder.getAllItems()
                            val currentItems = adapter.getLauncherItems().toMutableList()
                            val folderIndex = currentItems.indexOfFirst {
                                (it as? LauncherItem.Folder)?.folderInfo?.id == targetFolder.id
                            }
                            if (folderIndex != -1) {
                                currentItems.removeAt(folderIndex)
                                currentItems.addAll(folderIndex, allItems)
                                adapter.submitLauncherItems(currentItems)
                                onOrderModified(currentItems)
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(com.silauncer.cepat.R.string.folder_dissolved_toast),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else if (currentShortcut != null) {
                        val targetShortcut = currentShortcut!!
                        val shortcutName = targetShortcut.title.toString()
                        val anchor = currentTarget?.itemView
                        // [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
                        // [Penjelasan]: Menampilkan popup menu floating 'Hapus pintasan' yang ter-anchor ke shortcut icon
                        popupHandler.showShortcutMenu(shortcutName, anchor) {
                            val currentItems = adapter.getLauncherItems().toMutableList()
                            val shortcutIndex = currentItems.indexOfFirst {
                                (it as? LauncherItem.Shortcut)?.shortcutInfo?.cacheKey == targetShortcut.cacheKey
                            }
                            if (shortcutIndex != -1) {
                                currentItems.removeAt(shortcutIndex)
                                adapter.submitLauncherItems(currentItems)
                                onOrderModified(currentItems)
                            }
                        }
                    }
                }
                pendingPopup = false
                cancelLongPress()
                activePointerId = -1
                currentTarget = null
                currentApp = null
                currentFolder = null
                currentShortcut = null
                if (wasLongPress) return true
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressHelper?.onTouchEvent(e)
                pendingPopup = false
                cancelLongPress()
                activePointerId = -1
                currentTarget = null
                currentApp = null
                currentFolder = null
                currentShortcut = null
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (e.getPointerId(e.actionIndex) == activePointerId) {
                    longPressHelper?.onTouchEvent(e)
                    cancelLongPress()
                    activePointerId = -1
                }
            }
        }
        return false
    }

    // [Jalur Class]: com.silauncer.cepat.launcher.WorkspaceGestureDetector
    // [Penjelasan]: Menutup popup context menu yang aktif untuk mencegah window leak saat terjadi interupsi atau lifecycle destroy.
    fun dismissPopups() {
        popupHandler.dismissAppMenu()
    }
}
