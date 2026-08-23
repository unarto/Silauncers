package com.silauncer.cepat.popup

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.notification.NotificationContainer
import com.silauncer.cepat.notification.NotificationInfo
import com.silauncer.cepat.notification.NotificationKeyData
import com.silauncer.cepat.shortcuts.DeepShortcutView
import com.silauncer.cepat.shortcuts.ShortcutLauncher

/**
 * PopupContainerWithArrow
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
 * // [Penjelasan]: Kontainer tampilan kartu popup mengambang (floating popup card) berujung panah panah presisi (RoundedArrowDrawable), yang menyatukan notifikasi aktif, deep shortcuts, dan pintasan sistem (App info, Uninstall) dengan animasi transisi mulus dan pembaruan live. Diadaptasi dari AOSP Launcher3.
 */
class PopupContainerWithArrow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ArrowPopup(context, attrs, defStyleAttr) {

    private val shortcutLauncher = ShortcutLauncher(context)
    private var popupWindow: PopupWindow? = null
    private var liveUpdateHandler: PopupLiveUpdateHandler? = null

    private var notificationContainer: NotificationContainer? = null
    private var deepShortcutsContainer: LinearLayout? = null
    private var systemShortcutsContainer: LinearLayout? = null

    private val cornerRadius = resources.getDimension(R.dimen.dialog_corner_radius)
    private val popupBackgroundColor = ContextCompat.getColor(context, R.color.popup_color_primary)

    init {
        // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
        // [Penjelasan]: Set background kontainer utama menjadi transparan agar kartu gelembung (bubble cards) di dalamnya dapat terpisah secara visual dengan latar belakang gelembung masing-masing.
        background = null
    }

    // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
    // [Penjelasan]: Mengisi semua elemen popup (notifikasi, shortcuts, system shortcuts) dan menampilkan PopupWindow beranimasi di dekat view target, mendukung sematan pintasan ke workspace via onPinShortcutCallback.
    fun showForApp(
        anchorView: View,
        appInfo: AppInfo,
        shortcuts: List<ShortcutInfo>,
        notifications: List<NotificationInfo>,
        systemShortcuts: List<SystemShortcut>,
        popupDataProvider: PopupDataProvider? = null,
        onPinShortcutCallback: ((ShortcutInfo) -> Unit)? = null,
        onDismissCallback: (() -> Unit)? = null
    ) {
        val inflater = LayoutInflater.from(context)

        // Bersihkan arrow lama jika terpasang
        if (isArrowInitialized() && getArrowView().parent != null) {
            removeView(getArrowView())
        }

        notificationContainer = findViewById(R.id.notification_container)
        deepShortcutsContainer = findViewById(R.id.deep_shortcuts_container)
        systemShortcutsContainer = findViewById(R.id.system_shortcuts_container)

        // 1. Notifikasi
        if (notifications.isNotEmpty() && notificationContainer != null) {
            notificationContainer?.visibility = View.VISIBLE
            notificationContainer?.applyNotificationInfos(notifications)
        } else {
            notificationContainer?.visibility = View.GONE
        }

        // 2. Pilah dan Isi Dua Bagian (System Actions vs App Shortcuts) sesuai spesifikasi
        systemShortcutsContainer?.removeAllViews()
        deepShortcutsContainer?.removeAllViews()

        if (shortcuts.isEmpty()) {
            // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
            // [Penjelasan]: Jika aplikasi tidak memiliki shortcut, tampilkan 3 aksi horizontal "Info aplikasi", "Hapus", "Bagikan" (Screenshot 1)
            val horizontalRow = inflater.inflate(R.layout.system_shortcuts_horizontal, systemShortcutsContainer, false)
            
            horizontalRow.findViewById<View>(R.id.btn_horizontal_info).setOnClickListener {
                dismissPopup()
                SystemShortcut.AppInfoShortcut(context, appInfo, anchorView).onClick(it)
            }
            horizontalRow.findViewById<View>(R.id.btn_horizontal_uninstall).setOnClickListener {
                dismissPopup()
                SystemShortcut.UninstallShortcut(context, appInfo, null, anchorView).onClick(it)
            }
            horizontalRow.findViewById<View>(R.id.btn_horizontal_share).setOnClickListener {
                dismissPopup()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${appInfo.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan ${appInfo.name}").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {}
            }
            systemShortcutsContainer?.addView(horizontalRow)
            deepShortcutsContainer?.visibility = View.GONE
        } else {
            // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
            // [Penjelasan]: Jika aplikasi memiliki shortcut, tampilkan "Info aplikasi" (Aksi Sistem - Upper Bubble) dan daftar deep shortcuts (Lower Bubble)
            val row = inflater.inflate(R.layout.system_shortcut_row, systemShortcutsContainer, false)
            val iconView = row.findViewById<ImageView>(R.id.icon)
            val labelView = row.findViewById<TextView>(R.id.bubble_text)

            iconView.setImageResource(R.drawable.ic_info)
            labelView.setText(R.string.info_aplikasi)
            row.setOnClickListener {
                dismissPopup()
                SystemShortcut.AppInfoShortcut(context, appInfo, anchorView).onClick(row)
            }
            systemShortcutsContainer?.addView(row)

            deepShortcutsContainer?.visibility = View.VISIBLE
            val filteredShortcuts = PopupPopulator.sortAndFilterShortcuts(
                shortcuts,
                if (notifications.isNotEmpty()) PopupPopulator.MAX_SHORTCUTS_IF_NOTIFICATIONS else PopupPopulator.MAX_SHORTCUTS
            )

            for (shortcut in filteredShortcuts) {
                val shortcutView = inflater.inflate(R.layout.deep_shortcut, deepShortcutsContainer, false) as DeepShortcutView
                shortcutView.applyShortcutInfo(
                    info = shortcut,
                    onClick = { info ->
                        dismissPopup()
                        shortcutLauncher.startShortcut(info, anchorView)
                    },
                    onLongClick = { info, _ ->
                        dismissPopup()
                        onPinShortcutCallback?.invoke(info)
                        true
                    }
                )
                deepShortcutsContainer?.addView(shortcutView)
            }
        }

        // 3. Inisialisasi Live Update Handler jika data provider dipasok
        if (popupDataProvider != null) {
            liveUpdateHandler = PopupLiveUpdateHandler(this, popupDataProvider, appInfo)
        }

        // Hitung perkiraan ukuran popup tanpa arrow
        measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        var popupWidth = measuredWidth
        var popupHeight = measuredHeight

        // Hitung lokasi anchor view
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorCenterX = anchorX + (anchorView.width / 2)

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Tentukan posisi di atas/bawah ikon secara dinamis
        mIsAboveIcon = (anchorY - popupHeight - 24) >= 0
        val estimatedPopupX = (anchorCenterX - (popupWidth / 2)).coerceIn(16, screenWidth - popupWidth - 16)
        val arrowX = (anchorCenterX - estimatedPopupX - (mArrowWidth / 2)).coerceIn(16, popupWidth - mArrowWidth - 16)

        // Konfigurasi drawable panah presisi (Inisialisasi mArrow secara internal via setupArrow)
        setupArrow(
            isPointingUp = !mIsAboveIcon,
            leftAligned = true,
            arrowColor = android.graphics.Color.parseColor("#2C2C2E"), // Selaras dengan bg_popup_bubble
            popupRadius = cornerRadius,
            popupWidth = popupWidth.toFloat(),
            popupHeight = popupHeight.toFloat(),
            arrowOffsetX = arrowX.toFloat(),
            arrowOffsetY = 0f
        )

        // 4. Konfigurasi dan Tambahkan Caret Arrow secara Dinamis
        val arrowView = getArrowView()
        val arrowLp = LayoutParams(mArrowWidth, mArrowHeight).apply {
            leftMargin = arrowX
            gravity = Gravity.START
        }
        arrowView.layoutParams = arrowLp

        if (arrowView.parent != null) {
            (arrowView.parent as? ViewGroup)?.removeView(arrowView)
        }

        if (mIsAboveIcon) {
            addView(arrowView) // Di bawah (Arrow pointing down)
        } else {
            addView(arrowView, 0) // Di atas (Arrow pointing up)
        }

        // Hitung ulang ukuran presisi akhir dengan arrow terpasang
        measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val finalPopupWidth = measuredWidth
        val finalPopupHeight = measuredHeight

        val popupX = (anchorCenterX - (finalPopupWidth / 2)).coerceIn(16, screenWidth - finalPopupWidth - 16)
        val popupY = if (mIsAboveIcon) {
            anchorY - finalPopupHeight - 8
        } else {
            anchorY + anchorView.height + 8
        }

        mArrowOffsetHorizontal = arrowX
        setPivotForIcon(anchorCenterX.toFloat(), anchorY.toFloat())

        // Siapkan PopupWindow
        val window = PopupWindow(
            this,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(null)
            setOnDismissListener {
                closeComplete()
                onDismissCallback?.invoke()
            }
        }
        popupWindow = window

        try {
            window.showAtLocation(anchorView, Gravity.NO_GRAVITY, popupX, popupY)
            animateOpen()
        } catch (_: Exception) {}
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
     * // [Penjelasan]: Memperbarui angka/teks header pada kontainer notifikasi jika notifikasi aktif berubah.
     */
    fun updateNotificationHeader() {
        notificationContainer?.let { container ->
            if (container.visibility == View.VISIBLE) {
                // Header terbarui secara otomatis saat anak notifikasi diubah
            }
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
     * // [Penjelasan]: Memotong atau menyembunyikan kontainer notifikasi saat tidak ada notifikasi yang tersisa untuk aplikasi target.
     */
    fun trimNotifications(notificationKeys: List<NotificationKeyData>) {
        if (notificationKeys.isEmpty()) {
            notificationContainer?.visibility = View.GONE
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupContainerWithArrow
     * // [Penjelasan]: Menutup popup window secara langsung dengan animasi menghilang.
     */
    fun dismissPopup() {
        if (popupWindow?.isShowing == true) {
            animateClose()
        }
    }

    override fun closeComplete() {
        super.closeComplete()
        try {
            if (popupWindow?.isShowing == true) {
                popupWindow?.dismiss()
            }
        } catch (_: Exception) {}
    }
}
