package com.silauncer.cepat.secondarydisplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.silauncer.cepat.apps.AppInfo

/**
 * SecondaryDragController
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragController
 * // [Penjelasan]: Pengendali utama proses drag and drop aplikasi pada layar sekunder, mengelola pembuatan drag view preview, pelacakan posisi sentuhan, serta aksi penyematan ikon ke workspace.
 */
class SecondaryDragController(private val context: Context) {

    private var activeDragView: SecondaryDragView? = null
    private var isDragging = false
    private var dragListener: DragListener? = null

    interface DragListener {
        fun onDragStart(appInfo: AppInfo)
        fun onDragEnd(appInfo: AppInfo?, success: Boolean)
    }

    fun setDragListener(listener: DragListener?) {
        this.dragListener = listener
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragController
     * // [Penjelasan]: Memulai operasi drag and drop untuk view item aplikasi pada layar sekunder.
     */
    fun startDrag(view: View, appInfo: AppInfo, touchX: Int, touchY: Int): Boolean {
        if (isDragging) return false

        val bitmap = createViewBitmap(view) ?: return false
        val registrationX = bitmap.width / 2
        val registrationY = bitmap.height / 2

        val dragView = SecondaryDragView(context, bitmap, registrationX, registrationY).apply {
            this.appInfo = appInfo
        }
        activeDragView = dragView
        isDragging = true

        dragView.show(touchX, touchY)
        dragListener?.onDragStart(appInfo)
        return true
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragController
     * // [Penjelasan]: Memproses event gerakan sentuhan (MotionEvent.ACTION_MOVE, ACTION_UP, ACTION_CANCEL) selama operasi drag berlangsung.
     */
    fun onTouchEvent(event: MotionEvent, dropTargetCheck: ((x: Int, y: Int) -> Boolean)? = null): Boolean {
        if (!isDragging || activeDragView == null) return false

        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                activeDragView?.move(x, y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val appInfo = activeDragView?.appInfo
                val success = dropTargetCheck?.invoke(x, y) ?: true
                cancelDrag(success)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelDrag(false)
                return true
            }
        }
        return false
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragController
     * // [Penjelasan]: Membatalkan atau menghentikan operasi drag and drop aktif dan membersihkan preview drag.
     */
    fun cancelDrag(success: Boolean = false) {
        val appInfo = activeDragView?.appInfo
        activeDragView?.remove()
        activeDragView = null
        isDragging = false
        dragListener?.onDragEnd(appInfo, success)
    }

    fun isDragging(): Boolean = isDragging

    private fun createViewBitmap(view: View): Bitmap? {
        val width = view.width.takeIf { it > 0 } ?: 100
        val height = view.height.takeIf { it > 0 } ?: 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}
