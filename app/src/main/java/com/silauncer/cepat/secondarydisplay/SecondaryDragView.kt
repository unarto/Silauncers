package com.silauncer.cepat.secondarydisplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.silauncer.cepat.apps.AppInfo

/**
 * SecondaryDragView
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragView
 * // [Penjelasan]: Tampilan preview melayang (floating drag view) yang mengikuti pergerakan sentuhan jari/kursor mouse saat drag and drop ikon aplikasi di layar sekunder.
 */
class SecondaryDragView(
    context: Context,
    private val bitmap: Bitmap,
    private val registrationX: Int,
    private val registrationY: Int
) : View(context) {

    var appInfo: AppInfo? = null
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        gravity = Gravity.TOP or Gravity.START
        width = bitmap.width
        height = bitmap.height
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var isAttached = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragView
     * // [Penjelasan]: Menampilkan preview drag pada koordinat layar awal (touchX, touchY).
     */
    fun show(touchX: Int, touchY: Int) {
        layoutParams.x = touchX - registrationX
        layoutParams.y = touchY - registrationY
        try {
            windowManager.addView(this, layoutParams)
            isAttached = true
        } catch (_: Exception) {}
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragView
     * // [Penjelasan]: Memindahkan posisi preview drag mengikuti koordinat touch terbaru.
     */
    fun move(touchX: Int, touchY: Int) {
        if (!isAttached) return
        layoutParams.x = touchX - registrationX
        layoutParams.y = touchY - registrationY
        try {
            windowManager.updateViewLayout(this, layoutParams)
        } catch (_: Exception) {}
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragView
     * // [Penjelasan]: Menghapus preview drag dari layar setelah proses drop selesai atau dibatalkan.
     */
    fun remove() {
        if (isAttached) {
            try {
                windowManager.removeView(this)
            } catch (_: Exception) {}
            isAttached = false
        }
    }
}
