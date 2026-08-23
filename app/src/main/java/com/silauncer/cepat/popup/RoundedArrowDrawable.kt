package com.silauncer.cepat.popup

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/**
 * RoundedArrowDrawable
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.RoundedArrowDrawable
 * // [Penjelasan]: Custom Drawable untuk menggambar panah segitiga berujung melengkung (rounded caret arrow) pada popup bubble, dipotong presisi (clipped) terhadap body popup sehingga tidak ada tumpang tindih. Diadaptasi dari AOSP Launcher3.
 */
class RoundedArrowDrawable(
    width: Float,
    height: Float,
    radius: Float,
    popupRadius: Float,
    popupWidth: Float,
    popupHeight: Float,
    arrowOffsetX: Float,
    arrowOffsetY: Float,
    isPointingUp: Boolean,
    leftAligned: Boolean,
    color: Int
) : Drawable() {

    private val mPath = Path()
    private val mPaint = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        // [Jalur Class]: com.silauncer.cepat.popup.RoundedArrowDrawable
        // [Penjelasan]: Membuat path panah mengarah ke bawah, lalu klip dengan batas rounded popup dan transformasi orientasi (atas/bawah, kiri/kanan).
        addDownPointingRoundedTriangleToPath(width, height, radius, mPath)
        clipPopupBodyFromPath(popupRadius, popupWidth, popupHeight, arrowOffsetX, arrowOffsetY, mPath)

        val pathTransform = Matrix()
        pathTransform.setScale(
            if (leftAligned) 1f else -1f,
            if (isPointingUp) -1f else 1f,
            width * 0.5f,
            height * 0.5f
        )
        mPath.transform(pathTransform)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(mPath, mPaint)
    }

    override fun getOutline(outline: Outline) {
        outline.setPath(mPath)
    }

    // [Jalur Class]: com.silauncer.cepat.popup.RoundedArrowDrawable
    // [Penjelasan]: Menandai @Deprecated dan menambahkan penekanan peringatan deprecation untuk getOpacity() sesuai persyaratan compiler Kotlin saat meng-override method Drawable Android yang usang.
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.colorFilter = colorFilter
    }

    companion object {
        // [Jalur Class]: com.silauncer.cepat.popup.RoundedArrowDrawable
        // [Penjelasan]: Menghitung trigonometri sudut panah untuk membentuk ujung segitiga melengkung (rounded tip).
        private fun addDownPointingRoundedTriangleToPath(
            width: Float,
            height: Float,
            radius: Float,
            path: Path
        ) {
            val tanTheta = width / (2.0f * height)
            val theta = atan(tanTheta.toDouble()).toFloat()

            val roundedPointCenterY = height - (radius / sin(theta.toDouble())).toFloat()
            val p = radius / tanTheta
            val lineRoundPointIntersectFromCenter = (p * sin(theta.toDouble())).toFloat()
            val lineRoundPointIntersectFromTop = height - (p * cos(theta.toDouble())).toFloat()

            val centerX = width / 2.0f
            val thetaDeg = Math.toDegrees(theta.toDouble()).toFloat()

            path.reset()
            path.moveTo(0f, 0f)
            path.lineTo(centerX - lineRoundPointIntersectFromCenter, lineRoundPointIntersectFromTop)
            path.arcTo(
                centerX - radius,
                roundedPointCenterY - radius,
                centerX + radius,
                roundedPointCenterY + radius,
                thetaDeg,
                180 - (2 * thetaDeg),
                false
            )
            path.lineTo(width, 0f)
            path.close()
        }

        // [Jalur Class]: com.silauncer.cepat.popup.RoundedArrowDrawable
        // [Penjelasan]: Memotong bagian belakang panah segitiga sesuai kelengkungan body popup.
        private fun clipPopupBodyFromPath(
            popupRadius: Float,
            popupWidth: Float,
            popupHeight: Float,
            arrowOffsetX: Float,
            arrowOffsetY: Float,
            path: Path
        ) {
            val clipPath = Path()
            clipPath.addRoundRect(
                -arrowOffsetX,
                -arrowOffsetY - popupHeight,
                -arrowOffsetX + popupWidth,
                -arrowOffsetY,
                popupRadius,
                popupRadius,
                Path.Direction.CW
            )
            path.op(clipPath, Path.Op.DIFFERENCE)
        }
    }
}
