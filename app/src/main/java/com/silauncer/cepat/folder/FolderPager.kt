package com.silauncer.cepat.folder

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.cache.IconLoader
import com.silauncer.cepat.keyboard.FocusedItemDecorator
import com.silauncer.cepat.launcher.LauncherItem

/**
 * FolderPager
 *
 * // [Penjelasan]: Mengelola tampilan grid folder, adapter item aplikasi (pagination virtual/grid setup), serta event klik pada item-item di dalam folder.
 */
class FolderPager(
    private val context: Context,
    private val itemsRecyclerView: RecyclerView,
    private val resources: Resources
) {
    var onAppClickListener: ((LauncherItem) -> Unit)? = null
    var onItemLongClickListener: ((LauncherItem, View) -> Unit)? = null

    init {
        // [Penjelasan]: Menggunakan layout grid 3 kolom secara eksplisit sesuai spesifikasi desain.
        itemsRecyclerView.layoutManager = GridLayoutManager(context, 3)

        // [Penjelasan]: Menambahkan FocusedItemDecorator untuk indikator navigasi fokus keyboard di dalam grid folder
        val itemDecorator = FocusedItemDecorator(itemsRecyclerView)
        if (itemsRecyclerView.itemDecorationCount == 0) {
            itemsRecyclerView.addItemDecoration(itemDecorator)
        }
    }

    fun bind(info: FolderInfo, loader: IconLoader) {
        val itemIconSizePx = resources.getDimensionPixelSize(R.dimen.folder_modal_item_icon_size)
        // [Jalur Class]: com.silauncer.cepat.folder.FolderPager
        // [Penjelasan]: Menggunakan dimensi Pixel murni untuk label size agar menghindari dependensi manual pada API scaledDensity yang telah deprecated, dan menyetelnya via setTextSize dengan TypedValue.COMPLEX_UNIT_PX.
        val itemLabelSizePx = resources.getDimension(R.dimen.folder_modal_item_label_size)

        val itemDecorator = FocusedItemDecorator(itemsRecyclerView)
        val focusListener = itemDecorator.getFocusListener()

        val adapter = FolderItemAdapter(
            items = info.getAllItems(),
            iconLoader = loader,
            iconSizePx = itemIconSizePx,
            labelSizePx = itemLabelSizePx,
            focusListener = focusListener,
            onClick = { item ->
                onAppClickListener?.invoke(item)
            },
            onItemLongClick = { item, itemView ->
                onItemLongClickListener?.invoke(item, itemView)
            }
        )
        itemsRecyclerView.adapter = adapter
    }

    fun unbind() {
        itemsRecyclerView.adapter = null
        onAppClickListener = null
        onItemLongClickListener = null
    }

    fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        itemsRecyclerView.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    /**
     * Internal Adapter untuk grid item di dalam folder modal.
     */
    private class FolderItemAdapter(
        private val items: List<LauncherItem>,
        private val iconLoader: IconLoader,
        private val iconSizePx: Int,
        private val labelSizePx: Float,
        private val focusListener: View.OnFocusChangeListener?,
        private val onClick: (LauncherItem) -> Unit,
        private val onItemLongClick: (LauncherItem, View) -> Unit
    ) : RecyclerView.Adapter<FolderItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: ImageView = itemView.findViewById(R.id.folder_item_icon)
            private val nameView: TextView = itemView.findViewById(R.id.folder_item_name)
            private val notificationDot: View = itemView.findViewById(R.id.folder_notification_dot)

            fun bind(item: LauncherItem) {
                itemView.isFocusable = true
                itemView.onFocusChangeListener = focusListener

                if (iconView.layoutParams.width != iconSizePx || iconView.layoutParams.height != iconSizePx) {
                    iconView.layoutParams = iconView.layoutParams.apply {
                        width = iconSizePx
                        height = iconSizePx
                    }
                }
                
                when (item) {
                    is LauncherItem.App -> {
                        val app = item.appInfo
                        val currentCacheKey = app.cacheKey
                        iconView.tag = currentCacheKey
                        iconLoader.loadIconAsync(itemView.context, app, iconSizePx) { drawable, loadedKey ->
                            if (iconView.tag == loadedKey) {
                                iconView.setImageDrawable(drawable)
                            }
                        }
                        nameView.text = app.name
                        nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, labelSizePx)
                        
                        notificationDot.visibility = if (app.hasNotification) View.VISIBLE else View.GONE
                    }
                    is LauncherItem.Shortcut -> {
                        val shortcut = item.shortcutInfo
                        val currentCacheKey = shortcut.cacheKey
                        iconView.tag = currentCacheKey
                        iconLoader.loadShortcutIconAsync(itemView.context, shortcut, iconSizePx) { drawable, loadedKey ->
                            if (iconView.tag == loadedKey) {
                                iconView.setImageDrawable(drawable)
                            }
                        }
                        nameView.text = shortcut.title
                        nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, labelSizePx)
                        notificationDot.visibility = View.GONE
                    }
                    else -> {}
                }

                itemView.setOnClickListener { onClick(item) }
                itemView.setOnLongClickListener {
                    onItemLongClick(item, itemView)
                    true
                }
            }
        }
    }
}
