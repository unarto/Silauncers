package com.silauncer.cepat.secondarydisplay

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridView
import com.silauncer.cepat.R

/**
 * SecondaryDragLayer
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragLayer
 * // [Penjelasan]: Root FrameLayout khusus untuk antarmuka layar sekunder (Secondary Display).
 * Mengatur tata letak elemen utama (workspace grid, tombol all apps, laci aplikasi) serta intersepsi sentuhan untuk menutup laci aplikasi ketika pengguna mengetuk area luar.
 */
class SecondaryDragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var workspaceGrid: GridView? = null
    private var allAppsButton: View? = null
    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragLayer
    // [Penjelasan]: Mengubah tipe appsDrawerContainer menjadi View agar kompatibel dengan kontainer LinearLayout pada layout secondary_launcher.xml dan mencegah ClassCastException saat inflasi view.
    private var appsDrawerContainer: View? = null
    private var isDrawerOpenSupplier: (() -> Boolean)? = null
    private var closeDrawerAction: (() -> Unit)? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        workspaceGrid = findViewById(R.id.workspace_grid)
        allAppsButton = findViewById(R.id.all_apps_button)
        appsDrawerContainer = findViewById(R.id.apps_drawer_container)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDragLayer
     * // [Penjelasan]: Menghubungkan fungsi penutup laci aplikasi dari SecondaryDisplayLauncher.
     */
    fun setupDrawerCallbacks(isOpenSupplier: () -> Boolean, closeAction: () -> Unit) {
        this.isDrawerOpenSupplier = isOpenSupplier
        this.closeDrawerAction = closeAction
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val isOpen = isDrawerOpenSupplier?.invoke() ?: false
            if (isOpen && appsDrawerContainer != null) {
                val rect = Rect()
                appsDrawerContainer?.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    closeDrawerAction?.invoke()
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
