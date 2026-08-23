package com.silauncer.cepat.folder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.silauncer.cepat.R
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.deviceprofile.DeviceProfile
import com.silauncer.cepat.dot.FolderDotInfo
import com.silauncer.cepat.dot.IconLabelDotView

/**
 * FolderIcon
 *
 * Single Responsibility:
 * Component View/Widget visual di Workspace yang menampilkan preview thumbnail
 * (grid mini 2x2 dari ikon-ikon aplikasi di dalam folder) dan title folder.
 * Menggunakan IconLoader dan dimensi dari DeviceProfile/Resources untuk rendering presisi,
 * serta mendengarkan event perubahan state pada FolderInfo.
 */
class FolderIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), FolderInfo.FolderListener, IconLabelDotView {

    private val previewContainer: FrameLayout
    private val previewGrid: GridLayout
    private val previewIcons: Array<ImageView>
    private val titleView: TextView
    private val notificationDot: View

    // [Jalur Class]: com.silauncer.cepat.folder.FolderIcon
    // [Penjelasan]: Melacak agregasi notifikasi anak folder menggunakan FolderDotInfo
    private val folderDotInfo = FolderDotInfo()
    private var forceHideDot: Boolean = false

    private var folderInfo: FolderInfo? = null
    private var iconLoader: IconLoader? = null
    private var onFolderClickListener: ((FolderInfo) -> Unit)? = null

    fun getFolderInfo(): FolderInfo? = folderInfo
    fun getDotInfo(): FolderDotInfo = folderDotInfo

    // [Jalur Class]: com.silauncer.cepat.folder.FolderIcon
    // [Penjelasan]: Mengontrol visibilitas container preview icon (IconLabelDotView contract)
    override fun setIconVisible(visible: Boolean) {
        previewContainer.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    // [Jalur Class]: com.silauncer.cepat.folder.FolderIcon
    // [Penjelasan]: Mengontrol apakah notification dot disembunyikan paksa (misal saat drag)
    override fun setForceHideDot(hide: Boolean) {
        forceHideDot = hide
        updateDotVisibility()
    }

    private fun updateDotVisibility() {
        notificationDot.visibility = if (!forceHideDot && folderDotInfo.hasDot()) View.VISIBLE else View.GONE
    }

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
        LayoutInflater.from(context).inflate(R.layout.view_folder_icon, this, true)
        previewContainer = findViewById(R.id.folder_preview_container)
        previewGrid = findViewById(R.id.folder_preview_grid)
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIcon
        // [Penjelasan]: Inisialisasi 9 preview ImageView untuk grid mini 3x3 di dalam workspace folder icon
        previewIcons = arrayOf(
            findViewById(R.id.preview_icon_0),
            findViewById(R.id.preview_icon_1),
            findViewById(R.id.preview_icon_2),
            findViewById(R.id.preview_icon_3),
            findViewById(R.id.preview_icon_4),
            findViewById(R.id.preview_icon_5),
            findViewById(R.id.preview_icon_6),
            findViewById(R.id.preview_icon_7),
            findViewById(R.id.preview_icon_8)
        )
        titleView = findViewById(R.id.folder_name)
        notificationDot = findViewById(R.id.folder_notification_dot)

        setOnClickListener {
            folderInfo?.let { info ->
                onFolderClickListener?.invoke(info)
            }
        }
    }

    /**
     * Menghubungkan FolderInfo dan konfigurasi visual ke FolderIcon.
     */
    fun bind(
        info: FolderInfo,
        loader: IconLoader,
        deviceProfile: DeviceProfile? = null,
        iconSizePx: Int = 0,
        showAppLabel: Boolean = true,
        labelSizeSp: Float = 0f,
        iconSpacingPx: Int = 0,
        onClick: (FolderInfo) -> Unit
    ) {
        // Melepas listener lama jika ada
        folderInfo?.removeListener(this)

        this.folderInfo = info
        this.iconLoader = loader
        this.onFolderClickListener = onClick

        info.addListener(this)

        // Terapkan dimensi padding container
        if (iconSpacingPx > 0) {
            setPadding(iconSpacingPx, iconSpacingPx, iconSpacingPx, iconSpacingPx)
        }

        // [Jalur Class]: com.silauncer.cepat.folder.FolderIcon
        // [Penjelasan]: Terapkan ukuran preview container dari iconSizePx jika > 0, dengan fallback ke deviceProfile?.actualIconSizePx, atau fallback ke R.dimen.folder_preview_size
        val previewSizePx = if (iconSizePx > 0) {
            iconSizePx
        } else {
            deviceProfile?.actualIconSizePx ?: resources.getDimensionPixelSize(R.dimen.folder_preview_size)
        }
        if (previewContainer.layoutParams.width != previewSizePx || previewContainer.layoutParams.height != previewSizePx) {
            previewContainer.layoutParams = previewContainer.layoutParams.apply {
                width = previewSizePx
                height = previewSizePx
            }
        }

        // Konfigurasi Label Judul
        if (showAppLabel) {
            titleView.visibility = View.VISIBLE
            titleView.text = if (info.title.isNotBlank()) info.title else context.getString(R.string.folder_unnamed)
            if (labelSizeSp > 0f) {
                titleView.textSize = labelSizeSp
            }
        } else {
            titleView.visibility = View.GONE
        }

        // Render mini grid preview ikon
        renderPreviewThumbnails()
    }

    /**
     * Me-render hingga 4 thumbnail ikon aplikasi teratas ke dalam grid mini 2x2.
     */
    private fun renderPreviewThumbnails() {
        val info = folderInfo ?: return
        val loader = iconLoader ?: return
        val items = info.getItems()

        val miniIconSizePx = resources.getDimensionPixelSize(R.dimen.folder_preview_item_size)
        folderDotInfo.clear()

        // [Jalur Class]: com.silauncer.cepat.folder.FolderIcon
        // [Penjelasan]: Agregasi dot info dari seluruh item di dalam folder
        for (item in items) {
            val dInfo = item.dotInfo
            if (dInfo != null) {
                folderDotInfo.addDotInfo(dInfo)
            } else if (item.hasNotification) {
                folderDotInfo.setNotificationCount(folderDotInfo.getNotificationCount() + 1)
            }
        }

        for (i in previewIcons.indices) {
            val iconView = previewIcons[i]
            if (i < items.size) {
                val app = items[i]
                iconView.visibility = View.VISIBLE
                val currentCacheKey = app.cacheKey
                iconView.tag = currentCacheKey
                loader.loadIconAsync(context, app, miniIconSizePx) { drawable, loadedKey ->
                    if (iconView.tag == loadedKey) {
                        iconView.setImageDrawable(drawable)
                    }
                }
            } else {
                iconView.tag = null
                iconView.setImageDrawable(null)
                iconView.visibility = View.INVISIBLE
            }
        }
        
        // [Jalur Class]: com.silauncer.cepat.folder.FolderIcon
        // [Penjelasan]: Menampilkan notifikasi indikator jika folder memiliki notifikasi aktif dan tidak di-force hide
        updateDotVisibility()
    }

    override fun onTitleChanged(newTitle: String) {
        post {
            titleView.text = if (newTitle.isNotBlank()) newTitle else context.getString(R.string.folder_unnamed)
        }
    }

    override fun onItemsChanged() {
        post {
            renderPreviewThumbnails()
        }
    }

    /**
     * Membersihkan binding saat view di-recycle.
     */
    fun unbind() {
        folderInfo?.removeListener(this)
        folderInfo = null
        iconLoader = null
        onFolderClickListener = null
        for (iconView in previewIcons) {
            iconView.tag = null
            iconView.setImageDrawable(null)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        folderInfo?.removeListener(this)
    }
}
