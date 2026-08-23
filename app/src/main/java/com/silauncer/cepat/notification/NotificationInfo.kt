package com.silauncer.cepat.notification

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.view.View
import com.silauncer.cepat.graphics.IconPalette
import com.silauncer.cepat.util.PackageUserKey

/**
 * NotificationInfo
 *
 * // [Jalur Class]: com.silauncer.cepat.notification.NotificationInfo
 * // [Penjelasan]: Objek ekstraksi informasi notifikasi nyata dari StatusBarNotification (judul, isi teks, icon drawable, pending intent, flag auto-cancel, dan dismissable) (adaptasi AOSP Launcher3 NotificationInfo).
 */
class NotificationInfo(
    context: Context,
    val statusBarNotification: StatusBarNotification
) : View.OnClickListener {

    val packageUserKey: PackageUserKey = PackageUserKey.fromNotification(statusBarNotification)
    val notificationKey: String = statusBarNotification.key
    val title: CharSequence?
    val text: CharSequence?
    val intent: PendingIntent?
    val autoCancel: Boolean
    val dismissable: Boolean

    private var iconDrawable: Drawable? = null
    private var iconColor: Int = 0
    private var isIconLarge: Boolean = false

    init {
        val notification = statusBarNotification.notification
        val extras = notification.extras
        title = extras?.getCharSequence(Notification.EXTRA_TITLE)
        text = extras?.getCharSequence(Notification.EXTRA_TEXT)

        val iconType = notification.badgeIconType
        val largeIcon: Icon? = if (iconType == Notification.BADGE_ICON_SMALL) null else notification.getLargeIcon()

        if (largeIcon == null) {
            val smallIcon: Icon? = notification.smallIcon
            iconDrawable = smallIcon?.loadDrawable(context)
            iconColor = notification.color
            isIconLarge = false
        } else {
            iconDrawable = largeIcon.loadDrawable(context)
            isIconLarge = true
        }

        if (iconDrawable == null) {
            try {
                iconDrawable = context.packageManager.getApplicationIcon(statusBarNotification.packageName)
            } catch (_: Exception) {
                // Gunakan default icon jika tidak ditemukan
            }
        }

        intent = notification.contentIntent
        autoCancel = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0
        dismissable = (notification.flags and Notification.FLAG_ONGOING_EVENT) == 0
    }

    override fun onClick(v: View) {
        if (intent == null) return

        val opts = ActivityOptions.makeClipRevealAnimation(
            v, 0, 0, v.width.coerceAtLeast(1), v.height.coerceAtLeast(1)
        ).toBundle()

        try {
            intent.send(v.context, 0, null, null, null, null, opts)
        } catch (e: Exception) {
            // Intent mungkin dibatalkan atau tidak valid
        }

        if (autoCancel) {
            NotificationListener.getInstanceIfConnected()?.cancelNotificationFromLauncher(notificationKey)
        }
    }

    /**
     * Menghasilkan Drawable ikon dengan kontras warna latar belakang yang sesuai.
     */
    fun getIconForBackground(context: Context, background: Int): Drawable? {
        val drawable = iconDrawable ?: return null
        if (isIconLarge) {
            return drawable
        }
        val resolvedColor = IconPalette.resolveContrastColor(iconColor, background)
        val mutated = drawable.mutate()
        mutated.setTint(resolvedColor)
        return mutated
    }

    fun getIconDrawable(): Drawable? = iconDrawable
}
