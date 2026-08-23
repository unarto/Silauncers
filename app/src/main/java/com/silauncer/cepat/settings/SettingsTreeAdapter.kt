// [Jalur Class]: com.silauncer.cepat.settings.SettingsTreeAdapter
// [Tanggung Jawab SRP]: Khusus menangani rendering RecyclerView untuk TreeView bertingkat (Parent Node vs Child Node) serta interaksi Expand/Collapse dan Toggle Switch.
package com.silauncer.cepat.settings

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.settings.SettingsNode.ChildNode.ActionChildNode
import com.silauncer.cepat.settings.SettingsNode.ChildNode.OptionChildNode
import com.silauncer.cepat.settings.SettingsNode.ChildNode.SwitchChildNode
import com.silauncer.cepat.settings.SettingsNode.ParentNode

/**
 * SettingsTreeAdapter
 *
 * Mengelola rendering hierarki TreeView pada RecyclerView:
 * - Parent Node (Kategori dengan indikator panah putar expand/collapse)
 * - Child Option Node (Item konfigurasi dengan lencana nilai & pemilih opsi)
 * - Child Switch Node (Item sakelar toggle SwitchMaterial / SwitchCompat)
 * - Child Action Node (Item tombol tindakan / dialog)
 */
class SettingsTreeAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var parentNodes: List<ParentNode> = emptyList()
    private val displayItems = mutableListOf<SettingsNode>()

    fun setNodes(nodes: List<ParentNode>) {
        this.parentNodes = nodes
        rebuildDisplayItems()
        notifyDataSetChanged()
    }

    private fun rebuildDisplayItems() {
        displayItems.clear()
        for (parent in parentNodes) {
            displayItems.add(parent)
            if (parent.isExpanded) {
                displayItems.addAll(parent.children)
            }
        }
    }

    override fun getItemCount(): Int = displayItems.size

    override fun getItemViewType(position: Int): Int {
        return when (val item = displayItems[position]) {
            is ParentNode -> VIEW_TYPE_PARENT
            is OptionChildNode -> VIEW_TYPE_CHILD_OPTION
            is SwitchChildNode -> VIEW_TYPE_CHILD_SWITCH
            is ActionChildNode -> VIEW_TYPE_CHILD_ACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PARENT -> {
                val view = inflater.inflate(R.layout.item_settings_parent, parent, false)
                ParentViewHolder(view)
            }
            VIEW_TYPE_CHILD_OPTION -> {
                val view = inflater.inflate(R.layout.item_settings_child_select, parent, false)
                OptionChildViewHolder(view)
            }
            VIEW_TYPE_CHILD_SWITCH -> {
                val view = inflater.inflate(R.layout.item_settings_child_switch, parent, false)
                SwitchChildViewHolder(view)
            }
            VIEW_TYPE_CHILD_ACTION -> {
                val view = inflater.inflate(R.layout.item_settings_child_action, parent, false)
                ActionChildViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipe view tidak dikenali: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is ParentNode -> (holder as ParentViewHolder).bind(item)
            is OptionChildNode -> (holder as OptionChildViewHolder).bind(item)
            is SwitchChildNode -> (holder as SwitchChildViewHolder).bind(item)
            is ActionChildNode -> (holder as ActionChildViewHolder).bind(item)
        }
    }

    private fun toggleParent(parent: ParentNode, holder: ParentViewHolder) {
        val position = displayItems.indexOf(parent)
        if (position == -1) return

        parent.isExpanded = !parent.isExpanded

        // Animasi putar chevron 180 derajat
        val targetRotation = if (parent.isExpanded) 180f else 0f
        ObjectAnimator.ofFloat(holder.ivChevron, "rotation", holder.ivChevron.rotation, targetRotation)
            .setDuration(220)
            .start()

        if (parent.isExpanded) {
            val children = parent.children
            displayItems.addAll(position + 1, children)
            notifyItemRangeInserted(position + 1, children.size)
        } else {
            val childrenCount = parent.children.size
            if (position + 1 + childrenCount <= displayItems.size) {
                // Hapus sub-item dari tampilan
                for (i in 0 until childrenCount) {
                    displayItems.removeAt(position + 1)
                }
                notifyItemRangeRemoved(position + 1, childrenCount)
            }
        }
    }

    private fun showOptionDialog(node: OptionChildNode) {
        val optionLabels = node.options.map { it.label }.toTypedArray()
        val currentIndex = node.options.indexOfFirst { it.key == node.currentValue }.let {
            if (it >= 0) it else 0
        }

        AlertDialog.Builder(context)
            .setTitle(node.title)
            .setSingleChoiceItems(optionLabels, currentIndex) { dialog, which ->
                node.onSelected(node.options[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ==========================================
    // VIEW HOLDERS
    // ==========================================

    inner class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivParentIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvParentTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvParentSubtitle)
        val ivChevron: ImageView = itemView.findViewById(R.id.ivExpandChevron)

        fun bind(node: ParentNode) {
            ivIcon.setImageResource(node.iconRes)
            tvTitle.text = node.title
            tvSubtitle.text = node.subtitle
            ivChevron.rotation = if (node.isExpanded) 180f else 0f

            itemView.setOnClickListener {
                toggleParent(node, this)
            }
        }
    }

    inner class OptionChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSelectTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSelectSubtitle)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvSelectValueBadge)

        fun bind(node: OptionChildNode) {
            tvTitle.text = node.title
            if (!node.subtitle.isNullOrEmpty()) {
                tvSubtitle.text = node.subtitle
                tvSubtitle.visibility = View.VISIBLE
            } else {
                tvSubtitle.visibility = View.GONE
            }
            tvBadge.text = node.displayValue

            itemView.setOnClickListener {
                showOptionDialog(node)
            }
        }
    }

    inner class SwitchChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSwitchTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSwitchSubtitle)
        private val switchToggle: SwitchCompat = itemView.findViewById(R.id.switchToggle)

        fun bind(node: SwitchChildNode) {
            tvTitle.text = node.title
            if (!node.subtitle.isNullOrEmpty()) {
                tvSubtitle.text = node.subtitle
                tvSubtitle.visibility = View.VISIBLE
            } else {
                tvSubtitle.visibility = View.GONE
            }

            // Lepas listener sementara untuk mencegah loop event saat rebinding
            switchToggle.setOnCheckedChangeListener(null)
            switchToggle.isChecked = node.isChecked

            switchToggle.setOnCheckedChangeListener { _, isChecked ->
                node.onCheckedChange(isChecked)
            }

            itemView.setOnClickListener {
                switchToggle.isChecked = !switchToggle.isChecked
            }
        }
    }

    inner class ActionChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvActionTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvActionSubtitle)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivActionIcon)

        fun bind(node: ActionChildNode) {
            tvTitle.text = node.title
            if (!node.subtitle.isNullOrEmpty()) {
                tvSubtitle.text = node.subtitle
                tvSubtitle.visibility = View.VISIBLE
            } else {
                tvSubtitle.visibility = View.GONE
            }
            if (node.iconRes != null) {
                ivIcon.setImageResource(node.iconRes)
                ivIcon.visibility = View.VISIBLE
            } else {
                ivIcon.visibility = View.GONE
            }

            itemView.setOnClickListener {
                node.onAction()
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_PARENT = 1
        private const val VIEW_TYPE_CHILD_OPTION = 2
        private const val VIEW_TYPE_CHILD_SWITCH = 3
        private const val VIEW_TYPE_CHILD_ACTION = 4
    }
}
