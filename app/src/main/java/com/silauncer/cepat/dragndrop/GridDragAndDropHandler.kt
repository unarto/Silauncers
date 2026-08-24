package com.silauncer.cepat.dragndrop

import android.app.Activity
import android.content.Context
import android.graphics.PointF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.home.AppAdapter
import com.silauncer.cepat.launcher.*
import com.silauncer.cepat.workspace.CellLayout
import kotlinx.coroutines.CoroutineScope

/**
 * [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
 * [Penjelasan]: Pengatur orkestrasi Drag and Drop komprehensif modular berbasis arsitektur Launcher3 AOSP.
 * Memisahkan tanggung jawab ke controller terisolasi (DropTargetBarController, DragSpringAnimationHelper,
 * FolderCollisionHelper, FolderManager, CellLayout, dan WorkspaceGestureDetector).
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
    private val springAnimator = DragSpringAnimationHelper()
    private val dropTargetBarController = DropTargetBarController {
        recyclerView.parent as? ViewGroup
    }
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

    init {
        // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
        // [Penjelasan]: Menginisialisasi gesture detector dengan callback startDrag presisi dan penyimpanan urutan
        gestureDetector = WorkspaceGestureDetector(
            context = context,
            adapter = adapter,
            actionHandler = actionHandler,
            startDragCallback = { viewHolder, initialRawX, initialRawY, currentRawX, currentRawY ->
                startDrag(viewHolder, initialRawX, initialRawY, currentRawX, currentRawY)
            },
            onOrderModified = {
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
        return if (lm != null && lm.spanCount > 0) lm.spanCount else com.silauncer.cepat.deviceprofile.InvariantDeviceProfile.DEFAULT_COLUMNS
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

        viewHolder.itemView.alpha = 0f

        val isApp = adapter.getItem(pos) is LauncherItem.App
        // dropTargetBarController.show(isApp)
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Memulai sesi live drag di Workspace untuk item yang baru ditarik keluar dari Folder
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

        val isApp = item is LauncherItem.App
        // dropTargetBarController.show(isApp)
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
    // [Penjelasan]: Memproses aliran MotionEvent global dari level Activity saat drag aktif
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
    // [Penjelasan]: Memproses event sentuhan saat mode drag aktif (ACTION_MOVE, ACTION_UP, ACTION_CANCEL)
    private fun handleDragTouchEvent(e: MotionEvent) {
        val currentDragView = dragView ?: return

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                handleDragMove(e, currentDragView)
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
    // [Penjelasan]: Menangani pembaruan koordinat kursor, deteksi hover drop target bar, kolisi folder, dan reflow real-time
    private fun handleDragMove(e: MotionEvent, currentDragView: DragView) {
        currentDragView.move(e.rawX, e.rawY)

        val vWidth = draggedViewHolder?.itemView?.width ?: 100
        val vHeight = draggedViewHolder?.itemView?.height ?: 100
        val dragCenterX = e.rawX - currentDragView.registrationX + vWidth / 2f
        val dragCenterY = e.rawY - currentDragView.registrationY + vHeight / 2f

        val isHoveringBar = dropTargetBarController.updateHoverState(e.rawX, e.rawY) { hapticType ->
            recyclerView.performHapticFeedback(hapticType)
        }

        if (isHoveringBar) {
            activeFolderTarget = null
            collisionHelper.clearHover()
            cellLayout.resetReorder(recyclerView, animate = true)
            return
        }

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

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menyelesaikan proses pelepasan drop ke zona yang sesuai secara terisolasi
    private fun finishDrop(rawX: Float, rawY: Float) {
        val currentDragView = dragView ?: return
        val root = getRootViewGroup()
        val columns = getGridColumns()
        val sourcePos = dragStartPos

        dropTargetBarController.hide()

        val droppedOnRemove = dropTargetBarController.isHoveringRemove
        val droppedOnInfo = dropTargetBarController.isHoveringInfo
        val folderTarget = activeFolderTarget

        dropTargetBarController.resetHoverState()
        collisionHelper.clearHover()
        activeFolderTarget = null

        if (sourcePos == RecyclerView.NO_POSITION && draggedItemFromFolder == null) {
            cancelDrag()
            return
        }

        val fromFolderItem = draggedItemFromFolder
        val fromFolderInfo = sourceFolderInfo
        if (fromFolderItem != null && fromFolderInfo != null) {
            handleFolderItemDrop(
                currentDragView, fromFolderItem, fromFolderInfo,
                rawX, rawY, columns, root, droppedOnRemove, droppedOnInfo
            )
            return
        }

        if (droppedOnRemove) {
            handleWorkspaceRemoveDrop(currentDragView, sourcePos)
            return
        }

        if (droppedOnInfo) {
            handleWorkspaceInfoDrop(currentDragView, sourcePos, columns, root)
            return
        }

        if (folderTarget != null && folderTarget.position != sourcePos) {
            handleCreateFolderDrop(currentDragView, sourcePos, folderTarget, root)
            return
        }

        handleWorkspaceReorderDrop(currentDragView, sourcePos, rawX, rawY, columns, root)
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menangani peletakan item yang ditarik keluar dari folder ke Workspace atau Drop Target Bar
    private fun handleFolderItemDrop(
        currentDragView: DragView,
        fromFolderItem: LauncherItem,
        fromFolderInfo: FolderInfo,
        rawX: Float,
        rawY: Float,
        columns: Int,
        root: ViewGroup,
        droppedOnRemove: Boolean,
        droppedOnInfo: Boolean
    ) {
        if (droppedOnRemove) {
            currentDragView.animateFadeOut {
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
            dropTargetBarController.openAppInfo(context, fromFolderItem)
        }

        val fallbackItemDim = (48f * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
        val vWidth = fallbackItemDim
        val vHeight = fallbackItemDim
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
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menangani penghapusan item Workspace saat dijatuhkan pada zona Hapus
    private fun handleWorkspaceRemoveDrop(currentDragView: DragView, sourcePos: Int) {
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
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menangani pembukaan informasi aplikasi saat dijatuhkan pada zona Info aplikasi
    private fun handleWorkspaceInfoDrop(
        currentDragView: DragView,
        sourcePos: Int,
        columns: Int,
        root: ViewGroup
    ) {
        val currentItems = adapter.getLauncherItems()
        if (sourcePos in currentItems.indices) {
            val item = currentItems[sourcePos]
            dropTargetBarController.openAppInfo(context, item)
        }
        val returnPoint = cellLayout.cellToPoint(sourcePos, recyclerView, columns, root)
        currentDragView.animateTo(returnPoint.x, returnPoint.y) {
            cellLayout.resetReorder(recyclerView, animate = false)
            draggedViewHolder?.itemView?.alpha = 1f
            resetDragState()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menangani penggabungan item ke dalam folder yang sudah ada atau pembuatan folder baru
    private fun handleCreateFolderDrop(
        currentDragView: DragView,
        sourcePos: Int,
        folderTarget: FolderCollisionHelper.DropTargetResult,
        root: ViewGroup
    ) {
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
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menangani peletakan normal ke sel Workspace baru/semula dengan animasi SNAP presisi
    private fun handleWorkspaceReorderDrop(
        currentDragView: DragView,
        sourcePos: Int,
        rawX: Float,
        rawY: Float,
        columns: Int,
        root: ViewGroup
    ) {
        val fallbackDim = (48f * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
        val vWidth = draggedViewHolder?.itemView?.width?.takeIf { it > 0 } ?: fallbackDim
        val vHeight = draggedViewHolder?.itemView?.height?.takeIf { it > 0 } ?: fallbackDim
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

            val finalVh = recyclerView.findViewHolderForAdapterPosition(targetPos)
            finalVh?.itemView?.let { landingView ->
                springAnimator.animateLandingBounce(landingView)
            }

            resetDragState()
        }
    }

    private fun cancelDrag() {
        dropTargetBarController.hide()
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

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Menangani peletakan item yang ditarik keluar dari folder ke posisi workspace terdekat
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
            springAnimator.triggerDragOutAnimations(recyclerView, newItems, item, folderInfo, targetPos)
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.GridDragAndDropHandler
    // [Penjelasan]: Meneruskan pemanggilan penutupan popup menu ke gesture detector untuk mencegah window leak saat Activity dihancurkan
    fun dismissPopups() {
        gestureDetector.dismissPopups()
        dropTargetBarController.cleanup()
    }
}
