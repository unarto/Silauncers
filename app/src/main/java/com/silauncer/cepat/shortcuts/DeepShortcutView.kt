package com.silauncer.cepat.shortcuts

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.silauncer.cepat.R

/**
 * DeepShortcutView
 *
 * // [Jalur Class]: com.silauncer.cepat.shortcuts.DeepShortcutView
 * // [Penjelasan]: Custom ViewGroup untuk menampilkan satu baris shortcut (ikon + teks label + indikator drag) pada popup menu aplikasi (adaptasi dari AOSP Launcher3 DeepShortcutView).
 */
class DeepShortcutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var bubbleText: DeepShortcutTextView
    private lateinit var iconView: ImageView

    private var shortcutInfo: ShortcutInfo? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        bubbleText = findViewById(R.id.bubble_text)
        iconView = findViewById(R.id.icon)
    }

    /**
     * Menerapkan data [ShortcutInfo] ke dalam komponen UI DeepShortcutView.
     */
    fun applyShortcutInfo(
        info: ShortcutInfo,
        onClick: (ShortcutInfo) -> Unit,
        onLongClick: ((ShortcutInfo, View) -> Boolean)? = null
    ) {
        shortcutInfo = info

        // Gunakan longLabel jika ada, jika tidak fallback ke shortLabel atau ID
        val label = info.longLabel?.takeIf { it.isNotBlank() }
            ?: info.shortLabel?.takeIf { it.isNotBlank() }
            ?: info.id

        bubbleText.text = label

        // Memuat ikon shortcut dari LauncherApps jika tersedia
        loadShortcutIcon(info)

        bubbleText.setOnClickListener {
            onClick(info)
        }

        onLongClick?.let { longClickListener ->
            bubbleText.setOnLongClickListener {
                longClickListener(info, this)
            }
        }
    }

    private fun loadShortcutIcon(info: ShortcutInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                val density = resources.displayMetrics.densityDpi
                val drawable: Drawable? = launcherApps?.getShortcutIconDrawable(info, density)
                if (drawable != null) {
                    iconView.setImageDrawable(drawable)
                    iconView.visibility = View.VISIBLE
                    // Sesuaikan padding start pada teks agar tidak bertumpukan dengan ikon
                    val startPadding = resources.getDimensionPixelSize(R.dimen.popup_padding_start) +
                        resources.getDimensionPixelSize(R.dimen.deep_shortcut_icon_size) +
                        resources.getDimensionPixelSize(R.dimen.deep_shortcut_drawable_padding)
                    bubbleText.setPaddingRelative(
                        startPadding,
                        bubbleText.paddingTop,
                        bubbleText.paddingEnd,
                        bubbleText.paddingBottom
                    )
                    return
                }
            } catch (e: Exception) {
                // Abaikan kesalahan pembacaan ikon
            }
        }
        iconView.visibility = View.GONE
    }

    fun getIconView(): View = iconView

    fun getBubbleText(): DeepShortcutTextView = bubbleText

    fun getShortcutInfo(): ShortcutInfo? = shortcutInfo
}
