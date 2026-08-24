package com.silauncer.cepat.folder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.deviceprofile.DeviceProfile
import com.silauncer.cepat.launcher.LauncherItem

/**
 * Folder
 *
 * // [Jalur Class]: com.silauncer.cepat.folder.Folder
 * // [Penjelasan]: Orchestrator utama untuk UI Modal Folder. Mengoordinasikan animasi, pagination,
 * // drag & drop, editor nama, dan dynamic wallpaper blur background overlay.
 */
class Folder @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), FolderInfo.FolderListener {

    private val scrimView: FrameLayout
    private val wallpaperImageView: ImageView
    private val dimOverlayView: View
    private val boundaryIndicatorView: View?
    private val cardContainer: View
    private val titleEditText: EditText
    private val itemsRecyclerView: RecyclerView

    private val animationController: FolderAnimationController
    private val wallpaperBlurController: FolderWallpaperBlurController
    private val nameEditor: FolderNameEditor
    private val pager: FolderPager
    private val dragDropController: FolderDragDropController

    private var folderInfo: FolderInfo? = null
    private var iconLoader: IconLoader? = null
    
    private var onAppClickListener: ((LauncherItem) -> Unit)? = null
    private var onDragOutListener: ((LauncherItem, FolderInfo, Float, Float) -> Unit)? = null
    var onDragOutBoundaryPassed: ((LauncherItem, FolderInfo, View?, Float, Float) -> Unit)? = null
    private var onCloseListener: (() -> Unit)? = null
    var onDragExitCallback: (() -> Unit)? = null

    var isOpen = false
        private set

    var onShowAppInfo: ((LauncherItem, View) -> Unit)? = null
        set(value) {
            field = value
            dragDropController.onShowAppInfoListener = value
        }

    var onDismissAppInfo: (() -> Unit)? = null
        set(value) {
            field = value
            dragDropController.onDismissAppInfoListener = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_folder_modal, this, true)

        scrimView = findViewById(R.id.folder_overlay_scrim)
        wallpaperImageView = findViewById(R.id.folder_wallpaper_background)
        dimOverlayView = findViewById(R.id.folder_dim_overlay)
        boundaryIndicatorView = findViewById(R.id.folder_drag_boundary_indicator)
        cardContainer = findViewById(R.id.folder_card_container)
        titleEditText = findViewById(R.id.folder_title_edit)
        itemsRecyclerView = findViewById(R.id.folder_items_grid)

        val itemsCardContainer: View = findViewById(R.id.folder_items_card_container)

        // Inisialisasi Controllers
        animationController = FolderAnimationController(
            scrimView = scrimView,
            cardContainer = cardContainer,
            resources = resources,
            wallpaperView = wallpaperImageView,
            dimOverlayView = dimOverlayView
        )
        wallpaperBlurController = FolderWallpaperBlurController(context)
        nameEditor = FolderNameEditor(context, titleEditText)
        pager = FolderPager(context, itemsRecyclerView, resources)
        dragDropController = FolderDragDropController(
            cardContainer = cardContainer,
            scrimView = scrimView,
            dragOutThreshold = context.resources.getDimension(R.dimen.folder_drag_out_threshold),
            contentView = itemsCardContainer,
            boundaryIndicatorView = boundaryIndicatorView
        )

        // Setup Callbacks
        scrimView.setOnClickListener { close() }
        cardContainer.setOnClickListener { /* consume touch event */ }

        pager.onAppClickListener = { item ->
            close(animate = false)
            onAppClickListener?.invoke(item)
        }
        
        pager.onItemLongClickListener = { item, itemView ->
            dragDropController.startDragOutTracking(item, itemView)
        }

        dragDropController.onCompleteCloseRequested = { completeClose() }
        dragDropController.onDisallowInterceptTouchEvent = { disallow ->
            pager.requestDisallowInterceptTouchEvent(disallow)
        }
        dragDropController.onFolderFadeRequested = {
            // [Jalur Class]: com.silauncer.cepat.folder.Folder
            // [Penjelasan]: Memanfaatkan FolderAnimationController untuk memudarkan overlay modal folder secara terpadu dan mulus saat drag keluar sambil mempertahankan visibilitas ikon aktif
            animationController.animateFadeOutForDragExit(dragDropController.activeDragItemView)
            onDragExitCallback?.invoke()
        }
        dragDropController.onDragOutListener = { item, x, y ->
            folderInfo?.let { onDragOutListener?.invoke(item, it, x, y) }
        }
        dragDropController.onDragOutBoundaryPassed = { item, view, rawX, rawY ->
            folderInfo?.let { onDragOutBoundaryPassed?.invoke(item, it, view, rawX, rawY) }
        }
    }

    /**
     * Membuka modal folder dan menampilkannya di atas parent ViewGroup (root launcher).
     */
    fun show(
        parent: ViewGroup,
        info: FolderInfo,
        loader: IconLoader,
        deviceProfile: DeviceProfile? = null,
        onAppClick: (LauncherItem) -> Unit,
        onDragOut: ((LauncherItem, FolderInfo, Float, Float) -> Unit)? = null,
        onDragExit: (() -> Unit)? = null,
        onClose: () -> Unit = {}
    ) {
        if (isOpen || animationController.isAnimating) return

        folderInfo?.removeListener(this)

        this.folderInfo = info
        this.iconLoader = loader
        this.onAppClickListener = onAppClick
        this.onDragOutListener = onDragOut
        this.onDragExitCallback = onDragExit
        this.onCloseListener = onClose

        info.addListener(this)

        nameEditor.bind(info)
        pager.bind(info, loader)

        // [Jalur Class]: com.silauncer.cepat.folder.Folder
        // [Penjelasan]: Terapkan dynamic wallpaper blur background dengan dimmed overlay 35%
        wallpaperBlurController.applyWallpaperBackground(wallpaperImageView, dimOverlayView)

        if (this.parent == null) {
            parent.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        visibility = View.VISIBLE
        cardContainer.visibility = View.VISIBLE
        scrimView.visibility = View.VISIBLE
        wallpaperImageView.visibility = View.VISIBLE
        dimOverlayView.visibility = View.VISIBLE
        dragDropController.reset()
        isOpen = true
        
        animationController.animateOpen { /* done */ }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (dragDropController.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    fun onDragExit(rawX: Float, rawY: Float) {
        dragDropController.forceDragExit(rawX, rawY)
    }

    fun close(animate: Boolean = true) {
        if (!isOpen || animationController.isAnimating) return

        nameEditor.clearFocus()

        if (animate) {
            animationController.animateClose { completeClose() }
        } else {
            completeClose()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animationController.cancel()
        wallpaperBlurController.clear()
    }

    private fun completeClose() {
        isOpen = false
        visibility = View.GONE
        (parent as? ViewGroup)?.removeView(this)

        folderInfo?.removeListener(this)
        folderInfo = null
        iconLoader = null
        onAppClickListener = null
        onDragOutListener = null

        wallpaperBlurController.clear()
        nameEditor.unbind()
        pager.unbind()
        dragDropController.reset()

        val cb = onCloseListener
        onCloseListener = null
        onDragExitCallback = null
        cb?.invoke()
    }

    override fun onTitleChanged(newTitle: String) {
        nameEditor.updateTitle(newTitle)
    }

    override fun onItemsChanged() {
        val info = folderInfo ?: return
        val loader = iconLoader ?: return
        pager.bind(info, loader)
    }
}

