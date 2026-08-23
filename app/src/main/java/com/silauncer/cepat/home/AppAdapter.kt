package com.silauncer.cepat.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.folder.FolderIcon
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.launcher.LauncherItem
import kotlinx.coroutines.CoroutineScope

class AppAdapter(
    private val coroutineScope: CoroutineScope,
    private var iconSizePx: Int,
    private var showAppLabel: Boolean,
    private var labelSizeSp: Float,
    private var iconSpacingPx: Int,
    private var gridRows: Int,
    private val onClick: (AppInfo) -> Unit,
    private val onShortcutClick: (com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo) -> Unit = {},
    private val onFolderClick: (FolderInfo) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<LauncherItem>()
    private val iconLoader = IconLoader(coroutineScope)
    private var recyclerView: RecyclerView? = null
    private var lastHeight = 0
    // [Jalur Class]: com.silauncer.cepat.home.AppAdapter
    // [Penjelasan]: Listener perubahan fokus untuk animasi visual navigasi keyboard / D-pad
    var focusChangeListener: View.OnFocusChangeListener? = null

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rv = recyclerView ?: return@OnGlobalLayoutListener
        val newHeight = rv.measuredHeight
        if (newHeight > 0 && newHeight != lastHeight) {
            lastHeight = newHeight
            rv.post {
                if (recyclerView != null) {
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        this.recyclerView = null
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is LauncherItem.App -> VIEW_TYPE_APP
            is LauncherItem.Folder -> VIEW_TYPE_FOLDER
            is LauncherItem.Shortcut -> VIEW_TYPE_SHORTCUT
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is AppViewHolder -> holder.unbind()
            is FolderViewHolder -> holder.unbind()
            is ShortcutViewHolder -> holder.unbind()
        }
    }

    fun submitList(newApps: List<AppInfo>) {
        val launcherItems = newApps.map { LauncherItem.App(it) }
        submitLauncherItems(launcherItems)
    }

    fun submitLauncherItems(newItems: List<LauncherItem>) {
        val oldSnapshot = ArrayList(items)
        val newSnapshot = ArrayList(newItems)
        val diffResult = DiffUtil.calculateDiff(LauncherItemDiffCallback(oldSnapshot, newSnapshot))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }


    /**
     * Menambahkan item aplikasi ke posisi tertentu pada workspace.
     */
    fun insertAppAt(position: Int, app: AppInfo) {
        val insertPos = position.coerceIn(0, items.size)
        items.add(insertPos, LauncherItem.App(app))
        notifyItemInserted(insertPos)
    }

    fun getItems(): List<AppInfo> {
        val list = mutableListOf<AppInfo>()
        for (item in items) {
            when (item) {
                is LauncherItem.App -> list.add(item.appInfo)
                is LauncherItem.Folder -> list.addAll(item.folderInfo.getItems())
                is LauncherItem.Shortcut -> {} // Do not add to appinfo list
            }
        }
        return list
    }

    fun getLauncherItems(): List<LauncherItem> = items

    fun getItem(position: Int): LauncherItem? {
        return if (position in 0 until items.size) items[position] else null
    }

    fun updateConfig(
        newIconSizePx: Int,
        newShowLabel: Boolean,
        newLabelSizeSp: Float,
        newIconSpacingPx: Int,
        newGridRows: Int
    ) {
        var changed = false
        if (iconSizePx != newIconSizePx) {
            iconSizePx = newIconSizePx
            changed = true
        }
        if (showAppLabel != newShowLabel) {
            showAppLabel = newShowLabel
            changed = true
        }
        if (labelSizeSp != newLabelSizeSp) {
            labelSizeSp = newLabelSizeSp
            changed = true
        }
        if (iconSpacingPx != newIconSpacingPx) {
            iconSpacingPx = newIconSpacingPx
            changed = true
        }
        if (gridRows != newGridRows) {
            gridRows = newGridRows
            changed = true
        }
        if (changed) {
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FOLDER -> {
                val folderIcon = FolderIcon(parent.context)
                folderIcon.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                FolderViewHolder(folderIcon)
            }
            VIEW_TYPE_SHORTCUT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
                ShortcutViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
                AppViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LauncherItem.App -> (holder as AppViewHolder).bind(item.appInfo)
            is LauncherItem.Folder -> (holder as FolderViewHolder).bind(item.folderInfo)
            is LauncherItem.Shortcut -> (holder as ShortcutViewHolder).bind(item.shortcutInfo)
        }
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.app_icon)
        private val nameView: TextView = itemView.findViewById(R.id.app_name)
        private val notificationDot: View = itemView.findViewById(R.id.notification_dot)

        fun bind(app: AppInfo) {
            // [Jalur Class]: com.silauncer.cepat.home.AppAdapter
            // [Penjelasan]: Mereset transformasi visual (alpha, scale, translation) agar view yang didaur ulang kembali ke kondisi normal
            itemView.alpha = 1.0f
            itemView.scaleX = 1.0f
            itemView.scaleY = 1.0f
            itemView.translationX = 0f
            itemView.translationY = 0f
            itemView.elevation = 0f

            itemView.isFocusable = true
            itemView.onFocusChangeListener = focusChangeListener
            itemView.setPadding(iconSpacingPx, iconSpacingPx, iconSpacingPx, iconSpacingPx)

            if (iconView.layoutParams.width != iconSizePx || iconView.layoutParams.height != iconSizePx) {
                iconView.layoutParams = iconView.layoutParams.apply {
                    width = iconSizePx
                    height = iconSizePx
                }
            }

            val currentCacheKey = app.cacheKey
            iconView.tag = currentCacheKey
            iconLoader.loadIconAsync(itemView.context, app, iconSizePx) { drawable, loadedKey ->
                if (iconView.tag == loadedKey) {
                    iconView.setImageDrawable(drawable)
                }
            }

            if (showAppLabel) {
                nameView.visibility = View.VISIBLE
                nameView.text = app.name
                nameView.textSize = labelSizeSp
            } else {
                nameView.visibility = View.GONE
            }
            
            // [Jalur Class]: com.silauncer.cepat.home.AppAdapter
            // [Penjelasan]: Tampilkan notification dot jika app memiliki notifikasi
            notificationDot.visibility = if (app.hasNotification) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick(app) }
        }

        fun unbind() {
            iconView.tag = null
            iconView.setImageDrawable(null)
            itemView.setOnClickListener(null)
            itemView.onFocusChangeListener = null
        }
    }

    inner class FolderViewHolder(val folderIcon: FolderIcon) : RecyclerView.ViewHolder(folderIcon) {
        fun bind(folderInfo: FolderInfo) {
            folderIcon.alpha = 1.0f
            folderIcon.scaleX = 1.0f
            folderIcon.scaleY = 1.0f
            folderIcon.translationX = 0f
            folderIcon.translationY = 0f
            folderIcon.elevation = 0f

            folderIcon.isFocusable = true
            folderIcon.onFocusChangeListener = focusChangeListener
            // [Jalur Class]: com.silauncer.cepat.home.AppAdapter
            // [Penjelasan]: Meneruskan iconSizePx ke FolderIcon agar ukuran preview container folder konsisten dengan preferensi ukuran icon App dan Shortcut
            folderIcon.bind(
                info = folderInfo,
                loader = iconLoader,
                deviceProfile = null,
                iconSizePx = iconSizePx,
                showAppLabel = showAppLabel,
                labelSizeSp = labelSizeSp,
                iconSpacingPx = iconSpacingPx,
                onClick = { info -> onFolderClick(info) }
            )
        }

        fun unbind() {
            folderIcon.onFocusChangeListener = null
            folderIcon.unbind()
        }
    }

    inner class ShortcutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.app_icon)
        private val nameView: TextView = itemView.findViewById(R.id.app_name)
        private val notificationDot: View = itemView.findViewById(R.id.notification_dot)
        
        // [Jalur Class]: com.silauncer.cepat.home.AppAdapter.ShortcutViewHolder
        // [Penjelasan]: Mengikat data pintasan (WorkspaceShortcutInfo) ke view, termasuk memuat ikon asinkron, menampilkan label, dan menyesuaikan alpha jika pintasan dinonaktifkan
        fun bind(shortcut: com.silauncer.cepat.shortcuts.WorkspaceShortcutInfo) {
            itemView.scaleX = 1.0f
            itemView.scaleY = 1.0f
            itemView.translationX = 0f
            itemView.translationY = 0f
            itemView.elevation = 0f

            itemView.isFocusable = true
            itemView.onFocusChangeListener = focusChangeListener
            itemView.setPadding(iconSpacingPx, iconSpacingPx, iconSpacingPx, iconSpacingPx)
            itemView.alpha = if (shortcut.isEnabled) 1.0f else 0.5f
            
            if (iconView.layoutParams.width != iconSizePx || iconView.layoutParams.height != iconSizePx) {
                iconView.layoutParams = iconView.layoutParams.apply {
                    width = iconSizePx
                    height = iconSizePx
                }
            }
            
            val currentCacheKey = shortcut.cacheKey
            iconView.tag = currentCacheKey
            iconLoader.loadShortcutIconAsync(itemView.context, shortcut, iconSizePx) { drawable, loadedKey ->
                if (iconView.tag == loadedKey) {
                    iconView.setImageDrawable(drawable)
                }
            }
            
            if (showAppLabel) {
                nameView.visibility = View.VISIBLE
                nameView.text = shortcut.title
                nameView.textSize = labelSizeSp
            } else {
                nameView.visibility = View.GONE
            }
            
            notificationDot.visibility = View.GONE
            
            itemView.setOnClickListener {
                onShortcutClick(shortcut)
            }
        }
        
        fun unbind() {
            iconView.tag = null
            iconView.setImageDrawable(null)
            itemView.setOnClickListener(null)
            itemView.onFocusChangeListener = null
        }
    }

    companion object {
        const val VIEW_TYPE_APP = 1
        const val VIEW_TYPE_FOLDER = 2
        const val VIEW_TYPE_SHORTCUT = 3
    }
}

// [Jalur Class]: com.silauncer.cepat.home.LauncherItemDiffCallback
// [Penjelasan]: Menghitung perbedaan antara list LauncherItem lama dan baru (App, Folder, Shortcut) secara presisi untuk optimasi DiffUtil
class LauncherItemDiffCallback(
    private val oldList: List<LauncherItem>,
    private val newList: List<LauncherItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return when {
            old is LauncherItem.App && new is LauncherItem.App ->
                old.appInfo.componentName == new.appInfo.componentName && old.appInfo.user == new.appInfo.user
            old is LauncherItem.Folder && new is LauncherItem.Folder ->
                old.folderInfo.id == new.folderInfo.id
            old is LauncherItem.Shortcut && new is LauncherItem.Shortcut ->
                old.shortcutInfo.shortcutId == new.shortcutInfo.shortcutId &&
                    old.shortcutInfo.packageName == new.shortcutInfo.packageName &&
                    old.shortcutInfo.user == new.shortcutInfo.user
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return when {
            old is LauncherItem.App && new is LauncherItem.App -> old.appInfo == new.appInfo
            old is LauncherItem.Folder && new is LauncherItem.Folder ->
                old.folderInfo.title == new.folderInfo.title && old.folderInfo.itemCount() == new.folderInfo.itemCount()
            old is LauncherItem.Shortcut && new is LauncherItem.Shortcut ->
                old.shortcutInfo.title == new.shortcutInfo.title && old.shortcutInfo.isEnabled == new.shortcutInfo.isEnabled
            else -> false
        }
    }
}
