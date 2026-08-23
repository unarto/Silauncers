package com.silauncer.cepat.dragndrop

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * DragView
 *
 * [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
 * [Penjelasan]: Tampilan mengambang (floating view) independen yang menampilkan bitmap/visual ikon yang sedang diseret.
 * Mengikuti koordinat sentuhan secara real-time 360 derajat tanpa lag atau locking sumbu, serta mendukung animasi snap
 * berbasis DecelerateInterpolator atau SpringAnimation menuju titik tengah cell target saat dilepaskan.
 */
class DragView(
    context: Context,
    private val bitmap: Bitmap,
    val registrationX: Float,
    val registrationY: Float,
    private val initialScale: Float = 1.0f,
    private val dragScale: Float = 1.15f
) : FrameLayout(context) {

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
    // [Penjelasan]: ImageView penampung visual bitmap dari item yang sedang diseret
    private val imageView: ImageView = ImageView(context).apply {
        setImageBitmap(bitmap)
        scaleType = ImageView.ScaleType.FIT_XY
        layoutParams = LayoutParams(bitmap.width, bitmap.height)
    }

    private var parentViewGroup: ViewGroup? = null
    private val defaultInterpolator: TimeInterpolator = DecelerateInterpolator(1.5f)

    init {
        // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
        // [Penjelasan]: Menyiapkan elevasi bayangan (shadow) dan batas outline untuk visualisasi kedalaman ala Launcher3
        addView(imageView)
        elevation = 16f * context.resources.displayMetrics.density
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRect(0, 0, bitmap.width, bitmap.height)
            }
        }
        clipToOutline = false
    }

    companion object {
        // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
        // [Penjelasan]: Fungsi utilitas untuk merender representasi visual View menjadi Bitmap ARGB_8888 yang tajam dan menghitung registration offset presisi tanpa memicu error RippleDrawable STYLE_PATTERNED pada software canvas
        fun createFromView(view: View, touchRawX: Float, touchRawY: Float): DragView {
            val fallbackSize = (48f * view.context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
            val width = if (view.width > 0) view.width else fallbackSize
            val height = if (view.height > 0) view.height else fallbackSize
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val background = view.background
            val wasPressed = view.isPressed
            val wasSelected = view.isSelected
            val wasActivated = view.isActivated
            try {
                if (wasPressed) view.isPressed = false
                if (wasSelected) view.isSelected = false
                if (wasActivated) view.isActivated = false
                background?.jumpToCurrentState()

                if (background is android.graphics.drawable.RippleDrawable) {
                    view.background = null
                    view.draw(canvas)
                    view.background = background
                } else {
                    view.draw(canvas)
                }
            } finally {
                if (wasPressed) view.isPressed = wasPressed
                if (wasSelected) view.isSelected = wasSelected
                if (wasActivated) view.isActivated = wasActivated
            }

            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            val viewScreenX = if (loc[0] == 0 && loc[1] == 0 && (view.left > 0 || view.top > 0)) view.left else loc[0]
            val viewScreenY = if (loc[0] == 0 && loc[1] == 0 && (view.left > 0 || view.top > 0)) view.top else loc[1]
            val regX = (touchRawX - viewScreenX).coerceIn(0f, width.toFloat())
            val regY = (touchRawY - viewScreenY).coerceIn(0f, height.toFloat())

            return DragView(
                context = view.context,
                bitmap = bitmap,
                registrationX = regX,
                registrationY = regY
            )
        }

        // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
        // [Penjelasan]: Fungsi utilitas untuk membuat DragView langsung dari Bitmap yang sudah ada dengan registration point di tengah atau kustom
        fun createFromBitmap(context: Context, bitmap: Bitmap, regX: Float, regY: Float): DragView {
            return DragView(
                context = context,
                bitmap = bitmap,
                registrationX = regX.coerceIn(0f, bitmap.width.toFloat()),
                registrationY = regY.coerceIn(0f, bitmap.height.toFloat())
            )
        }
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
    // [Penjelasan]: Menempelkan DragView ke root container, memposisikan di awal sentuhan dengan offset root container, dan menjalankan animasi pick-up zoom
    fun show(root: ViewGroup, startRawX: Float, startRawY: Float) {
        parentViewGroup = root
        val lp = LayoutParams(bitmap.width, bitmap.height)
        root.addView(this, lp)

        val rootLoc = IntArray(2)
        root.getLocationOnScreen(rootLoc)

        // Set koordinat awal persis di bawah jari dengan memperhitungkan posisi parent root
        translationX = startRawX - registrationX - rootLoc[0]
        translationY = startRawY - registrationY - rootLoc[1]
        scaleX = initialScale
        scaleY = initialScale

        // Animasi pembesaran (lift-up) saat pertama kali diangkat
        animate()
            .scaleX(dragScale)
            .scaleY(dragScale)
            .setDuration(150)
            .setInterpolator(defaultInterpolator)
            .start()
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
    // [Penjelasan]: Memperbarui posisi floating view secara 1:1 langsung dari MotionEvent dengan sinkronisasi offset root container
    fun move(rawX: Float, rawY: Float) {
        val root = parentViewGroup ?: return
        val rootLoc = IntArray(2)
        root.getLocationOnScreen(rootLoc)
        translationX = rawX - registrationX - rootLoc[0]
        translationY = rawY - registrationY - rootLoc[1]
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
    // [Penjelasan]: Menjalankan animasi SNAP halus menuju koordinat target cell (toX, toY) dengan DecelerateInterpolator
    fun animateTo(
        toX: Float,
        toY: Float,
        targetScale: Float = 1.0f,
        duration: Long = 180L,
        onComplete: Runnable? = null
    ) {
        animate().cancel()
        animate()
            .translationX(toX)
            .translationY(toY)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .alpha(1.0f)
            .setDuration(duration)
            .setInterpolator(defaultInterpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.run()
                    remove()
                }
            })
            .start()
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
    // [Penjelasan]: Animasi menghilang (fade-out & scale-down) saat item dijatuhkan pada zona Hapus
    fun animateFadeOut(duration: Long = 150L, onComplete: Runnable? = null) {
        animate().cancel()
        animate()
            .alpha(0f)
            .scaleX(0.2f)
            .scaleY(0.2f)
            .setDuration(duration)
            .setInterpolator(defaultInterpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.run()
                    remove()
                }
            })
            .start()
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragView
    // [Penjelasan]: Menghapus floating DragView dari hirarki parent view secara aman
    fun remove() {
        animate().cancel()
        parentViewGroup?.removeView(this)
        parentViewGroup = null
    }
}
