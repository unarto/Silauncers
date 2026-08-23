package com.silauncer.cepat.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.RectEvaluator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.FloatProperty
import android.view.View

/**
 * ItemFocusIndicatorHelper
 *
 * // [Jalur Class]: com.silauncer.cepat.keyboard.ItemFocusIndicatorHelper
 * // [Penjelasan]: Kelas abstrak pembantu untuk menggambar dan menganimasikan latar belakang/highlight fokus
 * (adaptasi dari AOSP Launcher3 ItemFocusIndicatorHelper). Mendukung interpolasi pergeseran posisi (shift) dan fade alpha.
 */
abstract class ItemFocusIndicatorHelper<T>(
    private val container: View,
    color: Int
) : ValueAnimator.AnimatorUpdateListener {

    protected val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maxAlpha: Int = Color.alpha(color)

    private val dirtyRect = Rect()
    private var isDirty = false

    private var lastFocusedItem: T? = null
    private var currentItem: T? = null
    private var targetItem: T? = null

    /**
     * Fraksi pergeseran (0..1) antara item saat ini dan item target
     */
    var shift: Float = 0f

    private var currentAnimation: ObjectAnimator? = null
    var alpha: Float = 0f
        private set

    var radius: Float = 0f

    init {
        paint.color = 0xFF000000.toInt() or color
        setAlphaInternal(0f)
        shift = 0f
    }

    fun setAlphaInternal(value: Float) {
        alpha = value
        paint.alpha = (alpha * maxAlpha).toInt()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        invalidateDirty()
    }

    protected fun invalidateDirty() {
        // [Jalur Class]: com.silauncer.cepat.keyboard.ItemFocusIndicatorHelper
        // [Penjelasan]: Memanggil container.invalidate() secara penuh menggantikan pemanggilan invalidate(Rect) yang telah deprecated sejak API 30+ (hardware acceleration).
        if (isDirty || getDrawRect() != null) {
            container.invalidate()
        }
        isDirty = false
    }

    /**
     * Menggambar indikator fokus rounded rectangle pada canvas.
     */
    open fun draw(c: Canvas) {
        if (alpha <= 0f) return

        val newRect = getDrawRect()
        if (newRect != null) {
            dirtyRect.set(newRect)
            c.drawRoundRect(
                dirtyRect.left.toFloat(),
                dirtyRect.top.toFloat(),
                dirtyRect.right.toFloat(),
                dirtyRect.bottom.toFloat(),
                radius,
                radius,
                paint
            )
            isDirty = true
        }
    }

    private fun getDrawRect(): Rect? {
        val curr = currentItem
        if (curr != null && shouldDraw(curr)) {
            viewToRect(curr, tempRect1)
            val target = targetItem
            return if (shift > 0f && target != null) {
                viewToRect(target, tempRect2)
                rectEvaluator.evaluate(shift, tempRect1, tempRect2)
            } else {
                tempRect1
            }
        }
        return null
    }

    protected open fun shouldDraw(item: T): Boolean = true

    /**
     * Memperbarui fokus item dengan animasi transisi yang mulus.
     */
    open fun changeFocus(item: T, hasFocus: Boolean) {
        if (hasFocus) {
            endCurrentAnimation()

            if (alpha > MIN_VISIBLE_ALPHA) {
                targetItem = item
                val alphaHolder = PropertyValuesHolder.ofFloat(ALPHA_PROPERTY, 1f)
                val shiftHolder = PropertyValuesHolder.ofFloat(SHIFT_PROPERTY, 1f)
                val anim = ObjectAnimator.ofPropertyValuesHolder(this, alphaHolder, shiftHolder)
                anim.addListener(ViewSetListener(item, true))
                currentAnimation = anim
            } else {
                setCurrentItem(item)
                val alphaHolder = PropertyValuesHolder.ofFloat(ALPHA_PROPERTY, 1f)
                val anim = ObjectAnimator.ofPropertyValuesHolder(this, alphaHolder)
                currentAnimation = anim
            }
            lastFocusedItem = item
        } else {
            if (lastFocusedItem == item) {
                lastFocusedItem = null
                endCurrentAnimation()
                val alphaHolder = PropertyValuesHolder.ofFloat(ALPHA_PROPERTY, 0f)
                val anim = ObjectAnimator.ofPropertyValuesHolder(this, alphaHolder)
                anim.addListener(ViewSetListener(null, false))
                currentAnimation = anim
            }
        }

        invalidateDirty()
        lastFocusedItem = if (hasFocus) item else null
        currentAnimation?.let {
            it.addUpdateListener(this)
            it.duration = ANIM_DURATION
            it.start()
        }
    }

    protected fun endCurrentAnimation() {
        currentAnimation?.cancel()
        currentAnimation = null
    }

    protected fun setCurrentItem(item: T?) {
        currentItem = item
        shift = 0f
        targetItem = null
    }

    /**
     * Menghitung bounding box item T relatif terhadap kontainer.
     */
    abstract fun viewToRect(item: T, outRect: Rect)

    fun getCurrentItem(): T? = currentItem

    private inner class ViewSetListener(
        private val itemToSet: T?,
        private val callOnCancel: Boolean
    ) : AnimatorListenerAdapter() {
        private var called = false

        override fun onAnimationCancel(animation: Animator) {
            if (!callOnCancel) {
                called = true
            }
        }

        override fun onAnimationEnd(animation: Animator) {
            if (!called) {
                setCurrentItem(itemToSet)
                called = true
            }
        }
    }

    companion object {
        private const val MIN_VISIBLE_ALPHA = 0.2f
        private const val ANIM_DURATION = 150L

        private val rectEvaluator = RectEvaluator(Rect())
        private val tempRect1 = Rect()
        private val tempRect2 = Rect()

        val ALPHA_PROPERTY = object : FloatProperty<ItemFocusIndicatorHelper<*>>("alpha") {
            override fun setValue(obj: ItemFocusIndicatorHelper<*>, value: Float) {
                obj.setAlphaInternal(value)
            }

            override fun get(obj: ItemFocusIndicatorHelper<*>): Float {
                return obj.alpha
            }
        }

        val SHIFT_PROPERTY = object : FloatProperty<ItemFocusIndicatorHelper<*>>("shift") {
            override fun setValue(obj: ItemFocusIndicatorHelper<*>, value: Float) {
                obj.shift = value
            }

            override fun get(obj: ItemFocusIndicatorHelper<*>): Float {
                return obj.shift
            }
        }
    }
}
