package com.silauncer.cepat.popup

import android.view.View
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.dot.DotInfo
import com.silauncer.cepat.util.PackageUserKey

/**
 * PopupLiveUpdateHandler
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.PopupLiveUpdateHandler
 * // [Penjelasan]: Menangani pembaruan data secara live (seperti saat notifikasi masuk/dihapus) pada PopupContainerWithArrow saat popup terpasang di window. Diadaptasi dari AOSP Launcher3.
 */
class PopupLiveUpdateHandler(
    private val popupContainer: PopupContainerWithArrow,
    private val popupDataProvider: PopupDataProvider,
    private val appInfo: AppInfo
) : PopupDataProvider.PopupDataChangeListener, View.OnAttachStateChangeListener {

    private val packageUserKey = PackageUserKey(appInfo.packageName, appInfo.user)

    init {
        popupContainer.addOnAttachStateChangeListener(this)
        if (popupContainer.isAttachedToWindow) {
            onViewAttachedToWindow(popupContainer)
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupLiveUpdateHandler
     * // [Penjelasan]: Callback saat container terpasang di window, mendaftarkan diri ke PopupDataProvider sebagai change listener.
     */
    override fun onViewAttachedToWindow(v: View) {
        popupDataProvider.setChangeListener(this)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupLiveUpdateHandler
     * // [Penjelasan]: Callback saat container dilepas dari window, melepas change listener dari PopupDataProvider.
     */
    override fun onViewDetachedFromWindow(v: View) {
        popupDataProvider.setChangeListener(null)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupLiveUpdateHandler
     * // [Penjelasan]: Memperbarui header notifikasi pada popup jika ada perubahan jumlah notifikasi untuk aplikasi target.
     */
    override fun onNotificationDotsUpdated(packageUserKey: PackageUserKey) {
        if (this.packageUserKey == packageUserKey) {
            popupContainer.updateNotificationHeader()
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupLiveUpdateHandler
     * // [Penjelasan]: Memotong atau menyembunyikan kontainer notifikasi jika notifikasi di-dismiss oleh pengguna saat popup terbuka.
     */
    override fun trimNotifications(updatedDots: Map<PackageUserKey, DotInfo>) {
        val dotInfo = updatedDots[packageUserKey]
        val notificationKeys = dotInfo?.getNotificationKeys() ?: emptyList()
        popupContainer.trimNotifications(notificationKeys)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.popup.PopupLiveUpdateHandler
     * // [Penjelasan]: Memperbarui tampilan pintasan sistem jika terjadi perubahan izin atau status aplikasi.
     */
    override fun onSystemShortcutsUpdated() {
        // [Jalur Class]: com.silauncer.cepat.popup.PopupLiveUpdateHandler
        // [Penjelasan]: Memperbarui tampilan pintasan sistem jika aplikasi diubah.
    }
}
