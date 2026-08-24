package com.silauncer.cepat.folder

import android.graphics.Rect
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.silauncer.cepat.launcher.LauncherItem
import com.silauncer.cepat.touch.GestureDragState
import kotlin.math.abs

/**
 * FolderDragDropController
 *
 * // [Jalur Class]: com.silauncer.cepat.folder.FolderDragDropController
 * // [Penjelasan]: Mengelola logika drag & drop item dari dalam folder dengan dukungan perpindahan bebas 360 derajat (1:1 koordinat tanpa axis lock), boundary indicator visual, dan transisi mulus keluar batas folder ke workspace.
 */
class FolderDragDropController(
    private val cardContainer: View,
    private val scrimView: View,
    private val dragOutThreshold: Float,
    private val contentView: View? = null,
    var boundaryIndicatorView: View? = null
) {
    var currentState: GestureDragState = GestureDragState.IDLE
        private set

    var isDraggingItem = false
        private set
    var activeDragApp: LauncherItem? = null
        private set
    private var hasExitedFolder = false
    var activeDragItemView: View? = null
        private set
    private var initialTouchX = -1f
    private var initialTouchY = -1f
    private var hasMovedBeyondSlop = false
    private val touchSlop = ViewConfiguration.get(cardContainer.context).scaledTouchSlop

    // Callbacks
    var onDragOutListener: ((LauncherItem, Float, Float) -> Unit)? = null
    var onDragOutBoundaryPassed: ((LauncherItem, View?, Float, Float) -> Unit)? = null
    var onShowAppInfoListener: ((LauncherItem, View) -> Unit)? = null
    var onDismissAppInfoListener: (() -> Unit)? = null
    var onCompleteCloseRequested: (() -> Unit)? = null
    var onDisallowInterceptTouchEvent: ((Boolean) -> Unit)? = null
    var onFolderFadeRequested: (() -> Unit)? = null

    // [Jalur Class]: com.silauncer.cepat.folder.FolderDragDropController
    // [Penjelasan]: Memulai pelacakan drag item di dalam folder dengan feedback haptic, inisialisasi koordinat awal, elevasi visual ikon yang diseret, dan memunculkan garis batas visual transisi ke workspace.
    fun startDragOutTracking(app: LauncherItem, itemView: View) {
        itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        
        // [Penjelasan]: Tampilkan popup saat long press langsung.
        onShowAppInfoListener?.invoke(app, itemView)

        currentState = GestureDragState.DRAGGING_FOLDER
        isDraggingItem = true
        hasExitedFolder = false
        hasMovedBeyondSlop = false
        activeDragApp = app
        activeDragItemView = itemView
        initialTouchX = -1f
        initialTouchY = -1f
        itemView.bringToFront()
        itemView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).start()
        onDisallowInterceptTouchEvent?.invoke(true)

        // Tampilkan indikator garis batas (boundary indicator) transisi folder -> workspace
        boundaryIndicatorView?.let { indicator ->
            indicator.visibility = View.VISIBLE
            indicator.alpha = 0f
            indicator.animate().alpha(1f).setDuration(180).start()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.folder.FolderDragDropController
    // [Penjelasan]: Memproses aliran MotionEvent untuk drag bebas 360 derajat (bebas sumbu X dan Y), evaluasi batas area kontainer folder, dan pencegahan popup jika pointer digeser.
    fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isDraggingItem && activeDragApp != null) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (initialTouchX < 0f) {
                        initialTouchX = ev.rawX
                        initialTouchY = ev.rawY
                    }

                    val dx = abs(ev.rawX - initialTouchX)
                    val dy = abs(ev.rawY - initialTouchY)
                    if (dx > touchSlop || dy > touchSlop) {
                        hasMovedBeyondSlop = true
                        onDismissAppInfoListener?.invoke()
                    }

                    // [Penjelasan]: Memperbarui posisi translasi X dan Y secara 1:1 tanpa batasan sumbu/axis lock (bebas 360 derajat)
                    val deltaX = ev.rawX - initialTouchX
                    val deltaY = ev.rawY - initialTouchY
                    activeDragItemView?.translationX = deltaX
                    activeDragItemView?.translationY = deltaY
                    activeDragItemView?.elevation = 16f

                    if (!hasExitedFolder) {
                        val targetView = contentView ?: cardContainer
                        val rect = Rect()
                        val hasGlobalRect = targetView.getGlobalVisibleRect(rect)

                        if (hasGlobalRect) {
                            // Perluas batas container sebesar dragOutThreshold di semua arah (kiri, kanan, atas, bawah)
                            rect.inset(-dragOutThreshold.toInt(), -dragOutThreshold.toInt())
                            if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                                hasExitedFolder = true
                                currentState = GestureDragState.DRAG_OUT_FOLDER
                                hideBoundaryIndicator()
                                forceDragExit(ev.rawX, ev.rawY)
                            }
                        } else {
                            val hitRect = Rect()
                            cardContainer.getHitRect(hitRect)
                            hitRect.inset(-dragOutThreshold.toInt(), -dragOutThreshold.toInt())
                            if (!hitRect.contains(ev.x.toInt(), ev.y.toInt())) {
                                hasExitedFolder = true
                                currentState = GestureDragState.DRAG_OUT_FOLDER
                                hideBoundaryIndicator()
                                forceDragExit(ev.rawX, ev.rawY)
                            }
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    hideBoundaryIndicator()
                    val draggedApp = activeDragApp
                    val draggedView = activeDragItemView
                    val exited = hasExitedFolder
                    val moved = hasMovedBeyondSlop
                    val dropX = ev.rawX
                    val dropY = ev.rawY

                    if (!exited) {
                        // [Jalur Class]: com.silauncer.cepat.folder.FolderDragDropController
                        // [Penjelasan]: Menerapkan animasi fisika pegas (SpringAnimation Physics-based) ala Launcher3 AOSP agar ikon memiliki efek pantul/membal yang kenyal saat dilepaskan kembali ke dalam posisi sel folder
                        if (draggedView != null) {
                            val springX = SpringAnimation(draggedView, DynamicAnimation.TRANSLATION_X, 0f).apply {
                                spring.stiffness = SpringForce.STIFFNESS_LOW
                                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                            }
                            val springY = SpringAnimation(draggedView, DynamicAnimation.TRANSLATION_Y, 0f).apply {
                                spring.stiffness = SpringForce.STIFFNESS_LOW
                                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                            }
                            val springScaleX = SpringAnimation(draggedView, DynamicAnimation.SCALE_X, 1.0f).apply {
                                spring.stiffness = SpringForce.STIFFNESS_LOW
                                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                            }
                            val springScaleY = SpringAnimation(draggedView, DynamicAnimation.SCALE_Y, 1.0f).apply {
                                spring.stiffness = SpringForce.STIFFNESS_LOW
                                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                            }
                            springScaleY.addEndListener { _, _, _, _ ->
                                draggedView.elevation = 0f
                            }
                            springX.start()
                            springY.start()
                            springScaleX.start()
                            springScaleY.start()
                        }
                    } else {
                        draggedView?.translationX = 0f
                        draggedView?.translationY = 0f
                        draggedView?.scaleX = 1.0f
                        draggedView?.scaleY = 1.0f
                        draggedView?.elevation = 0f
                    }

                    resetState()

                    if (exited && draggedApp != null) {
                        onDragOutListener?.invoke(draggedApp, dropX, dropY)
                        onCompleteCloseRequested?.invoke()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    hideBoundaryIndicator()
                    activeDragItemView?.animate()
                        ?.translationX(0f)
                        ?.translationY(0f)
                        ?.scaleX(1.0f)
                        ?.scaleY(1.0f)
                        ?.setDuration(100)
                        ?.withEndAction {
                            activeDragItemView?.elevation = 0f
                        }
                        ?.start()
                    resetState()
                    onCompleteCloseRequested?.invoke()
                    return true
                }
            }
        }
        return false
    }

    private fun hideBoundaryIndicator() {
        boundaryIndicatorView?.let { indicator ->
            indicator.animate().alpha(0f).setDuration(150).withEndAction {
                indicator.visibility = View.GONE
            }.start()
        }
    }

    // [Jalur Class]: com.silauncer.cepat.folder.FolderDragDropController
    // [Penjelasan]: Memicu efek penutupan dan pemudaran overlay folder secara mulus saat item ditarik keluar dari batas folder (360 derajat drag-out) melalui callback onFolderFadeRequested dan onDragOutBoundaryPassed.
    fun forceDragExit(rawX: Float, rawY: Float) {
        hasExitedFolder = true
        currentState = GestureDragState.DRAG_OUT_FOLDER
        hideBoundaryIndicator()
        onFolderFadeRequested?.invoke()

        val app = activeDragApp
        val view = activeDragItemView
        if (app != null) {
            onDragOutBoundaryPassed?.invoke(app, view, rawX, rawY)
        }

        // [Penjelasan]: Jika dipanggil secara terpisah tanpa aliran touch active (misal programmatic/test), selesaikan drag exit
        if (!isDraggingItem && activeDragApp != null) {
            activeDragApp = null
            onCompleteCloseRequested?.invoke()
            if (app != null) {
                onDragOutListener?.invoke(app, rawX, rawY)
            }
        }
    }

    private fun resetState() {
        currentState = GestureDragState.IDLE
        isDraggingItem = false
        activeDragApp = null
        activeDragItemView = null
        hasExitedFolder = false
        hasMovedBeyondSlop = false
        initialTouchX = -1f
        initialTouchY = -1f
    }

    fun reset() {
        hideBoundaryIndicator()
        resetState()
    }
}
