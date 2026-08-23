package com.silauncer.cepat.dragndrop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.home.AppAdapter
import com.silauncer.cepat.launcher.*
import com.silauncer.cepat.workspace.CellLayout
import kotlinx.coroutines.CoroutineScope

/**
 * GridDragAndDropHandler
 *
 * [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
 * [Penjelasan]: Pengatur orkestrasi Drag and Drop komprehensif berbasis arsitektur Launcher3 AOSP.
 * Menggantikan ItemTouchHelper dengan sistem DragView floating independen, pelacakan koordinat MotionEvent 1:1,
 * reflow animasi pergeseran sel tetangga (realtimeReorder), deteksi kolisi folder dan drop target bar,
 * serta animasi snapping presisi bebas tumpang tindih (*overlap*).
 */
class GridDragAndDropHandler(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val adapter: AppAdapter,
    appController: LauncherAppController,
    actionHandler: AppActionHandler,
    coroutineScope: CoroutineScope
) : RecyclerView.OnItemTouchListener {

    private val collisionHelper = FolderCollisionHelper(context)
    private val orderPersistence = AppOrderPersistence(appController, coroutineScope)
    private val folderManager = FolderManager()
    private val cellLayout = CellLayout()
    private val gestureDetector: WorkspaceGestureDetector

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Status apakah sedang ada sesi seret (drag) aktif baik dari Workspace maupun dari Folder
    var isDragging = false
        private set
    private var dragStartPos: Int = RecyclerView.NO_POSITION
    private var draggedViewHolder: RecyclerView.ViewHolder? = null
    private var draggedItemFromFolder: LauncherItem? = null
    private var sourceFolderInfo: FolderInfo? = null
    private var dragView: DragView? = null
    private var activeFolderTarget: FolderCollisionHelper.DropTargetResult? = null

    // Properti DropTargetBar ("Hapus" dan "Info aplikasi")
    private var dropTargetBarView: View? = null
    private var targetRemoveView: View? = null
    private var targetInfoView: View? = null
    private var isHoveringRemove = false
    private var isHoveringInfo = false

    init {
        // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
        // [Penjelasan]: Mendaftarkan gesture detector dengan callback startDrag presisi yang menerima koordinat sentuhan awal dan saat ini
        gestureDetector = WorkspaceGestureDetector(
            context = context,
            adapter = adapter,
            actionHandler = actionHandler,
            startDragCallback = { viewHolder, initialRawX, initialRawY, currentRawX, currentRawY ->
                startDrag(viewHolder, initialRawX, initialRawY, currentRawX, currentRawY)
            },
            onOrderModified = { newItems ->
                orderPersistence.saveOrder(adapter.getLauncherItems().toList())
            }
        )
        recyclerView.addOnItemTouchListener(this)
    }

    private fun getRootViewGroup(): ViewGroup {
        return (context as? Activity)?.window?.decorView as? ViewGroup
            ?: recyclerView.rootView as ViewGroup
    }

    private fun getGridColumns(): Int {
        val lm = recyclerView.layoutManager as? GridLayoutManager
        return if (lm != null && lm.spanCount > 0) lm.spanCount else 4
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Memulai sesi drag: membuat DragView floating, menyembunyikan view asli, memunculkan DropTargetBar, dan getaran haptik
    private fun startDrag(
        viewHolder: RecyclerView.ViewHolder,
        initialRawX: Float,
        initialRawY: Float,
        currentRawX: Float,
        currentRawY: Float
    ) {
        val pos = viewHolder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return

        isDragging = true
        dragStartPos = pos
        draggedViewHolder = viewHolder
        draggedItemFromFolder = null
        sourceFolderInfo = null

        val root = getRootViewGroup()
        val dv = DragView.createFromView(viewHolder.itemView, initialRawX, initialRawY)
        dragView = dv
        dv.show(root, currentRawX, currentRawY)

        // Sembunyikan item asli di grid selama diseret
        viewHolder.itemView.alpha = 0f

        initDropTargetBar()
        val isApp = adapter.getItem(pos) is LauncherItem.App
        targetInfoView?.visibility = if (isApp) View.VISIBLE else View.GONE

        dropTargetBarView?.let { bar ->
            bar.visibility = View.VISIBLE
            bar.alpha = 0f
            bar.translationY = -bar.height.toFloat()
            bar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Memulai sesi live drag di Workspace untuk item yang baru ditarik keluar dari Folder, melanjutkan pelacakan sentuhan secara seamless
    fun startDragFromFolder(
        item: LauncherItem,
        folderInfo: FolderInfo,
        rawX: Float,
        rawY: Float,
        sourceView: View?
    ) {
        if (isDragging) return
        isDragging = true
        dragStartPos = RecyclerView.NO_POSITION
        draggedViewHolder = null
        draggedItemFromFolder = item
        sourceFolderInfo = folderInfo

        val root = getRootViewGroup()
        val dv = if (sourceView != null && sourceView.width > 0 && sourceView.height > 0) {
            DragView.createFromView(sourceView, rawX, rawY)
        } else {
            DragView.createFromView(recyclerView, rawX, rawY)
        }
        dragView = dv
        dv.show(root, rawX, rawY)

        initDropTargetBar()
        val isApp = item is LauncherItem.App
        targetInfoView?.visibility = if (isApp) View.VISIBLE else View.GONE

        dropTargetBarView?.let { bar ->
            bar.visibility = View.VISIBLE
            bar.alpha = 0f
            bar.translationY = -bar.height.toFloat()
            bar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun initDropTargetBar() {
        if (dropTargetBarView == null) {
            val parentView = recyclerView.parent as? ViewGroup
            dropTargetBarView = parentView?.findViewById(R.id.drop_target_bar_layout)
            targetRemoveView = dropTargetBarView?.findViewById(R.id.target_remove)
            targetInfoView = dropTargetBarView?.findViewById(R.id.target_info)
        }
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (isDragging) {
            handleDragTouchEvent(e)
            return true
        }
        val handled = gestureDetector.processTouchEvent(rv, e)
        return handled || isDragging
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (isDragging) {
            handleDragTouchEvent(e)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Memproses aliran MotionEvent global dari level Activity saat drag aktif agar pergerakan pointer di luar batas RecyclerView tetap terlacak secara konsisten
    fun processTouchEvent(e: MotionEvent): Boolean {
        if (isDragging) {
            handleDragTouchEvent(e)
            return true
        }
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) {
            gestureDetector.cancelLongPress()
            if (isDragging) {
                cancelDrag()
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Memproses event sentuhan saat mode drag aktif (ACTION_MOVE, ACTION_UP, ACTION_CANCEL) secara real-time
    private fun handleDragTouchEvent(e: MotionEvent) {
        val currentDragView = dragView ?: return

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                // 1. Update posisi DragView bebas 360 derajat secara 1:1
                currentDragView.move(e.rawX, e.rawY)

                val vWidth = draggedViewHolder?.itemView?.width ?: 100
                val vHeight = draggedViewHolder?.itemView?.height ?: 100
                val dragCenterX = e.rawX - currentDragView.registrationX + vWidth / 2f
                val dragCenterY = e.rawY - currentDragView.registrationY + vHeight / 2f

                // 2. Evaluasi kolisi dengan DropTargetBar ("Hapus" dan "Info aplikasi")
                initDropTargetBar()
                var hoverRemove = false
                var hoverInfo = false

                dropTargetBarView?.let { bar ->
                    if (bar.visibility == View.VISIBLE) {
                        targetRemoveView?.let { removeView ->
                            val loc = IntArray(2)
                            removeView.getLocationOnScreen(loc)
                            val rect = Rect(loc[0], loc[1], loc[0] + removeView.width, loc[1] + removeView.height)
                            if (rect.contains(e.rawX.toInt(), e.rawY.toInt())) {
                                hoverRemove = true
                            }
                        }

                        if (targetInfoView?.visibility == View.VISIBLE) {
                            targetInfoView?.let { infoView ->
                                val loc = IntArray(2)
                                infoView.getLocationOnScreen(loc)
                                val rect = Rect(loc[0], loc[1], loc[0] + infoView.width, loc[1] + infoView.height)
                                if (rect.contains(e.rawX.toInt(), e.rawY.toInt())) {
                                    hoverInfo = true
                                }
                            }
                        }
                    }
                }

                if (hoverRemove != isHoveringRemove) {
                    isHoveringRemove = hoverRemove
                    targetRemoveView?.setBackgroundColor(if (hoverRemove) 0x33FF3B30.toInt() else Color.TRANSPARENT)
                    if (hoverRemove) recyclerView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }

                if (hoverInfo != isHoveringInfo) {
                    isHoveringInfo = hoverInfo
                    targetInfoView?.setBackgroundColor(if (hoverInfo) 0x33007AFF.toInt() else Color.TRANSPARENT)
                    if (hoverInfo) recyclerView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }

                if (isHoveringRemove || isHoveringInfo) {
                    activeFolderTarget = null
                    collisionHelper.clearHover()
                    cellLayout.resetReorder(recyclerView, animate = true)
                    return
                }

                // 3. Evaluasi kolisi folder pembuatan
                val rvLoc = IntArray(2)
                recyclerView.getLocationOnScreen(rvLoc)
                val relDragX = dragCenterX - rvLoc[0]
                val relDragY = dragCenterY - rvLoc[1]

                val target = collisionHelper.findDropTarget(recyclerView, relDragX, relDragY, dragStartPos)
                if (target != null) {
                    activeFolderTarget = target
                    collisionHelper.updateHoverState(target)
                    cellLayout.resetReorder(recyclerView, animate = true)
                } else {
                    activeFolderTarget = null
                    collisionHelper.clearHover()

                    // 4. Real-time reflow & neighbor shifting animasi ala Launcher3
                    val columns = getGridColumns()
                    val targetPos = cellLayout.findMatchingCellToTarget(
                        dragCenterX = dragCenterX,
                        dragCenterY = dragCenterY,
                        recyclerView = recyclerView,
                        columns = columns,
                        itemCount = adapter.itemCount
                    )
                    cellLayout.realtimeReorder(dragStartPos, targetPos, recyclerView, columns)
                }
            }

            MotionEvent.ACTION_UP -> {
                finishDrop(e.rawX, e.rawY)
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelDrag()
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menyelesaikan proses pelepasan drop: menghapus, membuka info, memasukkan ke folder, atau snap ke sel target
    private fun finishDrop(rawX: Float, rawY: Float) {
        val currentDragView = dragView ?: return
        val root = getRootViewGroup()
        val columns = getGridColumns()
        val sourcePos = dragStartPos

        // Sembunyikan DropTargetBar
        hideDropTargetBar()

        val droppedOnRemove = isHoveringRemove
        val droppedOnInfo = isHoveringInfo
        val folderTarget = activeFolderTarget

        // Reset status hover
        isHoveringRemove = false
        isHoveringInfo = false
        targetRemoveView?.setBackgroundColor(Color.TRANSPARENT)
        targetInfoView?.setBackgroundColor(Color.TRANSPARENT)
        collisionHelper.clearHover()
        activeFolderTarget = null

        if (sourcePos == RecyclerView.NO_POSITION && draggedItemFromFolder == null) {
            cancelDrag()
            return
        }

        // Penanganan jika item berasal dari Folder yang diseret keluar
        val fromFolderItem = draggedItemFromFolder
        val fromFolderInfo = sourceFolderInfo
        if (fromFolderItem != null && fromFolderInfo != null) {
            if (droppedOnRemove) {
                currentDragView.animateFadeOut {
                    // Hapus item dari folder dan perbarui workspace jika folder auto-disband
                    val tempItems = adapter.getLauncherItems().toList()
                    val newItems = folderManager.removeAppFromFolder(tempItems, fromFolderInfo, fromFolderItem, -1)
                    val finalItems = newItems ?: tempItems
                    adapter.submitLauncherItems(finalItems)
                    orderPersistence.saveOrder(finalItems)
                    recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    resetDragState()
                }
                return
            }

            if (droppedOnInfo) {
                val app = (fromFolderItem as? LauncherItem.App)?.appInfo
                if (app != null) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SILAUNCER", "Gagal membuka Info Aplikasi: ${e.message}")
                    }
                }
            }

            // Hitung target posisi sel Workspace untuk item dari folder
            val vWidth = 100
            val vHeight = 100
            val dragCenterX = rawX - currentDragView.registrationX + vWidth / 2f
            val dragCenterY = rawY - currentDragView.registrationY + vHeight / 2f

            val targetPos = cellLayout.findMatchingCellToTarget(
                dragCenterX = dragCenterX,
                dragCenterY = dragCenterY,
                recyclerView = recyclerView,
                columns = columns,
                itemCount = adapter.itemCount
            )
            val snapTargetPoint = cellLayout.cellToPoint(targetPos, recyclerView, columns, root)

            currentDragView.animateTo(snapTargetPoint.x, snapTargetPoint.y, targetScale = 1.0f, duration = 180L) {
                cellLayout.resetReorder(recyclerView, animate = false)
                handleDragOutFromFolder(fromFolderItem, fromFolderInfo, rawX, rawY, forcedTargetPos = targetPos)
                resetDragState()
            }
            return
        }

        if (droppedOnRemove) {
            // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
            // [Penjelasan]: Menghapus item saat dijatuhkan pada zona Hapus dengan animasi fade out & scale down
            currentDragView.animateFadeOut {
                val currentItems = adapter.getLauncherItems().toMutableList()
                if (sourcePos in currentItems.indices) {
                    currentItems.removeAt(sourcePos)
                    adapter.submitLauncherItems(currentItems)
                    recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    orderPersistence.saveOrder(adapter.getLauncherItems().toList())
                }
                draggedViewHolder?.itemView?.alpha = 1f
                resetDragState()
            }
            return
        }

        if (droppedOnInfo) {
            // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
            // [Penjelasan]: Membuka system app info saat dijatuhkan pada zona Info aplikasi dan snap kembali ke posisi semula
            val currentItems = adapter.getLauncherItems()
            if (sourcePos in currentItems.indices) {
                val item = currentItems[sourcePos]
                val app = (item as? LauncherItem.App)?.appInfo
                if (app != null) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SILAUNCER", "Gagal membuka Info Aplikasi: ${e.message}")
                    }
                }
            }
            val returnPoint = cellLayout.cellToPoint(sourcePos, recyclerView, columns, root)
            currentDragView.animateTo(returnPoint.x, returnPoint.y) {
                cellLayout.resetReorder(recyclerView, animate = false)
                draggedViewHolder?.itemView?.alpha = 1f
                resetDragState()
            }
            return
        }

        if (folderTarget != null && folderTarget.position != sourcePos) {
            // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
            // [Penjelasan]: Animasi menciut ke dalam target folder saat penggabungan / pembuatan folder baru
            val targetView = folderTarget.itemView
            val loc = IntArray(2)
            targetView.getLocationOnScreen(loc)
            val rootLoc = IntArray(2)
            root.getLocationOnScreen(rootLoc)
            val snapX = (loc[0] - rootLoc[0]).toFloat()
            val snapY = (loc[1] - rootLoc[1]).toFloat()

            currentDragView.animateTo(snapX, snapY, targetScale = 0.5f, duration = 150L) {
                val newItems = folderManager.createFolder(adapter.getLauncherItems(), folderTarget.position, sourcePos)
                if (newItems != null) {
                    adapter.submitLauncherItems(newItems)
                    recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    orderPersistence.saveOrder(adapter.getLauncherItems().toList())
                }
                cellLayout.resetReorder(recyclerView, animate = false)
                draggedViewHolder?.itemView?.alpha = 1f
                resetDragState()
            }
            return
        }

        // Drop normal ke sel Workspace baru / semula
        val vWidth = draggedViewHolder?.itemView?.width ?: 100
        val vHeight = draggedViewHolder?.itemView?.height ?: 100
        val dragCenterX = rawX - currentDragView.registrationX + vWidth / 2f
        val dragCenterY = rawY - currentDragView.registrationY + vHeight / 2f

        val targetPos = cellLayout.findMatchingCellToTarget(
            dragCenterX = dragCenterX,
            dragCenterY = dragCenterY,
            recyclerView = recyclerView,
            columns = columns,
            itemCount = adapter.itemCount
        )

        val snapTargetPoint = cellLayout.cellToPoint(targetPos, recyclerView, columns, root)

        // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
        // [Penjelasan]: Animasi SNAP mulus menuju titik target dengan DecelerateInterpolator dan pembaruan urutan dataset
        currentDragView.animateTo(snapTargetPoint.x, snapTargetPoint.y, targetScale = 1.0f, duration = 180L) {
            cellLayout.resetReorder(recyclerView, animate = false)

            if (targetPos != sourcePos) {
                val items = adapter.getLauncherItems().toMutableList()
                if (sourcePos in items.indices && targetPos in items.indices) {
                    val movedItem = items.removeAt(sourcePos)
                    items.add(targetPos, movedItem)
                    adapter.submitLauncherItems(items)
                    orderPersistence.saveOrder(items)
                }
            }

            draggedViewHolder?.itemView?.alpha = 1f
            recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            // Animasi efek pegas mikro saat mendarat
            val finalVh = recyclerView.findViewHolderForAdapterPosition(targetPos)
            finalVh?.itemView?.let { landingView ->
                landingView.scaleX = 1.08f
                landingView.scaleY = 1.08f
                val springX = SpringAnimation(landingView, DynamicAnimation.SCALE_X, 1.0f).apply {
                    spring.stiffness = SpringForce.STIFFNESS_LOW
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                }
                val springY = SpringAnimation(landingView, DynamicAnimation.SCALE_Y, 1.0f).apply {
                    spring.stiffness = SpringForce.STIFFNESS_LOW
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                }
                springX.start()
                springY.start()
            }

            resetDragState()
        }
    }

    private fun cancelDrag() {
        hideDropTargetBar()
        val currentDragView = dragView
        val root = getRootViewGroup()
        val columns = getGridColumns()
        val sourcePos = dragStartPos

        if (currentDragView != null && sourcePos != RecyclerView.NO_POSITION) {
            val returnPoint = cellLayout.cellToPoint(sourcePos, recyclerView, columns, root)
            currentDragView.animateTo(returnPoint.x, returnPoint.y) {
                cellLayout.resetReorder(recyclerView, animate = false)
                draggedViewHolder?.itemView?.alpha = 1f
                resetDragState()
            }
        } else {
            cellLayout.resetReorder(recyclerView, animate = false)
            draggedViewHolder?.itemView?.alpha = 1f
            resetDragState()
        }
    }

    private fun resetDragState() {
        dragView?.remove()
        dragView = null
        draggedViewHolder = null
        draggedItemFromFolder = null
        sourceFolderInfo = null
        dragStartPos = RecyclerView.NO_POSITION
        isDragging = false
        activeFolderTarget = null
        gestureDetector.cancelLongPress()
    }

    private fun hideDropTargetBar() {
        dropTargetBarView?.let { bar ->
            bar.animate()
                .alpha(0f)
                .translationY(-bar.height.toFloat())
                .setDuration(200)
                .withEndAction {
                    bar.visibility = View.GONE
                }
                .start()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menangani peletakan item yang ditarik keluar dari folder ke posisi workspace terdekat atau posisi target sel yang ditentukan dengan efek fisika pegas (SpringAnimation) ala Launcher3 AOSP dan transisi auto-disband jika sisa item < 2
    fun handleDragOutFromFolder(
        item: LauncherItem,
        folderInfo: FolderInfo,
        rawX: Float,
        rawY: Float,
        forcedTargetPos: Int = -1
    ) {
        val targetPos = if (forcedTargetPos >= 0) forcedTargetPos else collisionHelper.findNearestWorkspacePosition(recyclerView, rawX, rawY)
        val newItems = folderManager.removeAppFromFolder(adapter.getLauncherItems(), folderInfo, item, targetPos)
        if (newItems != null) {
            adapter.submitLauncherItems(newItems)
            recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            orderPersistence.saveOrder(adapter.getLauncherItems().toList())

            // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
            // [Penjelasan]: Menerapkan efek pegas elastis SpringAnimation Physics-based ala Launcher3 AOSP pada item yang baru ditempatkan di grid workspace
            recyclerView.post {
                val droppedIndex = newItems.indexOfFirst { it.id == item.id }
                val animIndex = if (droppedIndex != -1) droppedIndex else targetPos
                val vh = recyclerView.findViewHolderForAdapterPosition(animIndex)
                vh?.itemView?.let { targetView ->
                    targetView.scaleX = 1.18f
                    targetView.scaleY = 1.18f
                    val springX = SpringAnimation(targetView, DynamicAnimation.SCALE_X, 1.0f).apply {
                        spring.stiffness = SpringForce.STIFFNESS_LOW
                        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                    }
                    val springY = SpringAnimation(targetView, DynamicAnimation.SCALE_Y, 1.0f).apply {
                        spring.stiffness = SpringForce.STIFFNESS_LOW
                        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                    }
                    springX.start()
                    springY.start()
                }

                // [Penjelasan]: Jika folder mengalami auto-disband (tersisa 1 item yang berubah menjadi ikon mandiri), berikan efek pegas halus pada ikon tersebut
                val remainingApp = folderInfo.getSingleRemainingApp()
                val remainingShortcut = folderInfo.getSingleRemainingShortcut()
                if (remainingApp != null || remainingShortcut != null) {
                    val dissolvedIndex = newItems.indexOfFirst {
                        if (remainingApp != null) {
                            (it as? LauncherItem.App)?.appInfo?.cacheKey == remainingApp.cacheKey
                        } else if (remainingShortcut != null) {
                            (it as? LauncherItem.Shortcut)?.shortcutInfo?.cacheKey == remainingShortcut.cacheKey
                        } else false
                    }
                    if (dissolvedIndex != -1 && dissolvedIndex != animIndex) {
                        val vhDissolved = recyclerView.findViewHolderForAdapterPosition(dissolvedIndex)
                        vhDissolved?.itemView?.let { dView ->
                            dView.scaleX = 0.85f
                            dView.scaleY = 0.85f
                            val sX = SpringAnimation(dView, DynamicAnimation.SCALE_X, 1.0f).apply {
                                spring.stiffness = SpringForce.STIFFNESS_LOW
                                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                            }
                            val sY = SpringAnimation(dView, DynamicAnimation.SCALE_Y, 1.0f).apply {
                                spring.stiffness = SpringForce.STIFFNESS_LOW
                                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                            }
                            sX.start()
                            sY.start()
                        }
                    }
                }
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Meneruskan pemanggilan penutupan popup menu ke gesture detector untuk mencegah window leak saat Activity dihancurkan.
    fun dismissPopups() {
        gestureDetector.dismissPopups()
    }
}
