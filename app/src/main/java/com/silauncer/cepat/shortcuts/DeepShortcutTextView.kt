package com.silauncer.cepat.shortcuts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.silauncer.cepat.R

/**
 * DeepShortcutTextView
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.DeepShortcutTextView
 * // [Penjelasan]: Custom TextView untuk item shortcut yang menangani rendering judul, placeholder saat memuat, dan deteksi sentuhan pada drag handle (adaptasi dari AOSP Launcher3 DeepShortcutTextView).
 */
class DeepShortcutTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val dragHandleBounds = Rect()
    private val dragHandleWidth: Int
    private var showInstructionToast = false
    private var instructionToast: Toast? = null

    private var showLoadingState = false
    private var loadingStatePlaceholder: Drawable? = null
    private val loadingStateBounds = Rect()

    init {
        val res = resources
        dragHandleWidth = res.getDimensionPixelSize(R.dimen.popup_padding_end) +
            res.getDimensionPixelSize(R.dimen.deep_shortcut_drag_handle_size) +
            res.getDimensionPixelSize(R.dimen.deep_shortcut_drawable_padding) / 2
        showLoadingState(true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        dragHandleBounds.set(0, 0, dragHandleWidth, measuredHeight)
        val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        if (!isRtl) {
            dragHandleBounds.offset(measuredWidth - dragHandleBounds.width(), 0)
        }
        setLoadingBounds()
    }

    private fun setLoadingBounds() {
        val placeholder = loadingStatePlaceholder ?: return
        val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val startPadding = if (isRtl) paddingRight else paddingLeft
        loadingStateBounds.set(
            0,
            0,
            (measuredWidth - dragHandleWidth - startPadding).coerceAtLeast(0),
            placeholder.intrinsicHeight.coerceAtLeast(16)
        )
        val offsetX = if (isRtl) dragHandleWidth else startPadding
        val offsetY = ((measuredHeight - placeholder.intrinsicHeight) / 2).coerceAtLeast(0)
        loadingStateBounds.offset(offsetX, offsetY)
        placeholder.bounds = loadingStateBounds
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        if (!TextUtils.isEmpty(text)) {
            showLoadingState(false)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            showInstructionToast = dragHandleBounds.contains(event.x.toInt(), event.y.toInt())
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        if (showInstructionToast) {
            showInstructionToast()
            return true
        }
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        if (showLoadingState) {
            loadingStatePlaceholder?.draw(canvas)
            return
        }
        super.onDraw(canvas)
    }

    fun showLoadingState(loading: Boolean) {
        if (loading == showLoadingState) return
        showLoadingState = loading
        if (loading) {
            loadingStatePlaceholder = ContextCompat.getDrawable(
                context,
                R.drawable.deep_shortcuts_text_placeholder
            )
            setLoadingBounds()
        } else {
            loadingStatePlaceholder = null
        }
        invalidate()
    }

    private fun showInstructionToast() {
        instructionToast?.cancel()
        val msg = context.getString(R.string.long_press_shortcut_to_add)
        instructionToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT)
        instructionToast?.show()
    }
}
