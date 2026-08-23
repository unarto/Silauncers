package com.silauncer.cepat.notification

import android.app.Notification
import android.app.NotificationChannel
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import com.silauncer.cepat.util.PackageUserKey
import java.util.concurrent.CopyOnWriteArraySet

/**
 * NotificationListener
 *
 * // [Jalur Class]: com.silauncer.cepat.notification.NotificationListener
 * // [Penjelasan]: Layanan NotificationListenerService yang menangani event notifikasi sistem Android (posted, removed, refresh, rank update), penyaringan notifikasi UI, dan koordinasi group notification (adaptasi AOSP Launcher3 NotificationListener).
 */
open class NotificationListener : NotificationListenerService() {

    companion object {
        const val TAG = "NotificationListener"

        // [Jalur Class]: com.silauncer.cepat.notification.NotificationListener
        // [Penjelasan]: Mengecek apakah akses notifikasi (NotificationListenerService) telah diizinkan oleh pengguna
        fun isNotificationAccessGranted(context: android.content.Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(context.packageName) == true
        }

        // [Jalur Class]: com.silauncer.cepat.notification.NotificationListener
        // [Penjelasan]: Membuka pengaturan akses notifikasi Android
        fun requestNotificationAccess(context: android.content.Context) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private const val MSG_NOTIFICATION_POSTED = 1
        private const val MSG_NOTIFICATION_REMOVED = 2
        private const val MSG_NOTIFICATION_FULL_REFRESH = 3
        private const val MSG_CANCEL_NOTIFICATION = 4
        private const val MSG_RANKING_UPDATE = 5

        @Volatile
        private var sNotificationListenerInstance: NotificationListener? = null

        private val sNotificationsChangedListeners = CopyOnWriteArraySet<NotificationsChangedListener>()

        @Volatile
        private var sIsConnected = false

        @JvmStatic
        fun getInstanceIfConnected(): NotificationListener? {
            return if (sIsConnected) sNotificationListenerInstance else null
        }

        @JvmStatic
        fun addNotificationsChangedListener(listener: NotificationsChangedListener?) {
            if (listener == null) return
            sNotificationsChangedListeners.add(listener)

            val instance = getInstanceIfConnected()
            if (instance != null) {
                instance.onNotificationFullRefresh()
            } else {
                Handler(Looper.getMainLooper()).post {
                    listener.onNotificationFullRefresh(emptyList())
                }
            }
        }

        @JvmStatic
        fun removeNotificationsChangedListener(listener: NotificationsChangedListener?) {
            if (listener != null) {
                sNotificationsChangedListeners.remove(listener)
            }
        }
    }

    private val workerThread = HandlerThread("NotificationListenerWorker").apply { start() }
    private val mWorkerHandler = Handler(workerThread.looper, ::handleWorkerMessage)
    private val mUiHandler = Handler(Looper.getMainLooper(), ::handleUiMessage)
    private val mTempRanking = Ranking()

    private val mNotificationGroupMap = HashMap<String, NotificationGroup>()
    private val mNotificationGroupKeyMap = HashMap<String, String>()
    private var mLastKeyDismissedByLauncher: String? = null

    init {
        sNotificationListenerInstance = this
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sIsConnected = true
        sNotificationListenerInstance = this
        onNotificationFullRefresh()
    }

    // [Jalur Class]: com.silauncer.cepat.notification.NotificationListener
    // [Penjelasan]: Membersihkan resource saat service dihancurkan (onDestroy), menghentikan HandlerThread background dengan quitSafely(), membatalkan antrean pesan pending pada UI dan Worker handler, melepaskan referensi static instance, serta membersihkan group mapping guna mencegah memory dan thread leak.
    override fun onDestroy() {
        super.onDestroy()
        sIsConnected = false
        if (sNotificationListenerInstance === this) {
            sNotificationListenerInstance = null
        }

        mUiHandler.removeCallbacksAndMessages(null)
        mWorkerHandler.removeCallbacksAndMessages(null)

        mNotificationGroupMap.clear()
        mNotificationGroupKeyMap.clear()

        try {
            if (workerThread.isAlive) {
                workerThread.quitSafely()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error quitting workerThread safely in onDestroy", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        sIsConnected = false
        onNotificationFullRefresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            mWorkerHandler.obtainMessage(MSG_NOTIFICATION_POSTED, sbn).sendToTarget()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) {
            mWorkerHandler.obtainMessage(MSG_NOTIFICATION_REMOVED, sbn).sendToTarget()
        }
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        if (rankingMap != null) {
            mWorkerHandler.obtainMessage(MSG_RANKING_UPDATE, rankingMap).sendToTarget()
        }
    }

    fun onNotificationFullRefresh() {
        mWorkerHandler.obtainMessage(MSG_NOTIFICATION_FULL_REFRESH).sendToTarget()
    }

    /**
     * Membatalkan notifikasi dari launcher.
     */
    fun cancelNotificationFromLauncher(key: String) {
        mWorkerHandler.obtainMessage(MSG_CANCEL_NOTIFICATION, key).sendToTarget()
    }

    private fun handleWorkerMessage(message: Message): Boolean {
        when (message.what) {
            MSG_NOTIFICATION_POSTED -> {
                val sbn = message.obj as StatusBarNotification
                val validForUi = notificationIsValidForUI(sbn)
                if (validForUi) {
                    com.silauncer.cepat.notification.NotificationRepository.getInstance().onNotificationPosted(sbn)
                } else {
                    com.silauncer.cepat.notification.NotificationRepository.getInstance().onNotificationRemoved(sbn)
                }
                
                val what = if (validForUi) MSG_NOTIFICATION_POSTED else MSG_NOTIFICATION_REMOVED
                mUiHandler.obtainMessage(what, toKeyPair(sbn)).sendToTarget()
                return true
            }
            MSG_NOTIFICATION_REMOVED -> {
                val sbn = message.obj as StatusBarNotification
                com.silauncer.cepat.notification.NotificationRepository.getInstance().onNotificationRemoved(sbn)
                
                mUiHandler.obtainMessage(MSG_NOTIFICATION_REMOVED, toKeyPair(sbn)).sendToTarget()

                val groupKey = sbn.groupKey
                val notificationGroup = mNotificationGroupMap[groupKey]
                val key = sbn.key
                if (notificationGroup != null) {
                    notificationGroup.removeChildKey(key)
                    if (notificationGroup.isEmpty()) {
                        if (key == mLastKeyDismissedByLauncher) {
                            notificationGroup.groupSummaryKey?.let { cancelNotification(it) }
                        }
                        mNotificationGroupMap.remove(groupKey)
                    }
                }
                if (key == mLastKeyDismissedByLauncher) {
                    mLastKeyDismissedByLauncher = null
                }
                return true
            }
            MSG_NOTIFICATION_FULL_REFRESH -> {
                val activeNotifications: List<StatusBarNotification> = if (sIsConnected) {
                    getActiveNotificationsSafely(null).filter { notificationIsValidForUI(it) }
                } else {
                    emptyList()
                }
                com.silauncer.cepat.notification.NotificationRepository.getInstance().onNotificationFullRefresh(activeNotifications)
                
                mUiHandler.obtainMessage(MSG_NOTIFICATION_FULL_REFRESH, activeNotifications).sendToTarget()
                return true
            }
            MSG_CANCEL_NOTIFICATION -> {
                val key = message.obj as String
                mLastKeyDismissedByLauncher = key
                cancelNotification(key)
                return true
            }
            MSG_RANKING_UPDATE -> {
                val rankingMap = message.obj as RankingMap
                val keys = rankingMap.orderedKeys
                for (sbn in getActiveNotificationsSafely(keys)) {
                    updateGroupKeyIfNecessary(sbn)
                }
                return true
            }
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleUiMessage(message: Message): Boolean {
        when (message.what) {
            MSG_NOTIFICATION_POSTED -> {
                val msg = message.obj as Pair<PackageUserKey, NotificationKeyData>
                for (listener in sNotificationsChangedListeners) {
                    listener.onNotificationPosted(msg.first, msg.second)
                }
            }
            MSG_NOTIFICATION_REMOVED -> {
                val msg = message.obj as Pair<PackageUserKey, NotificationKeyData>
                for (listener in sNotificationsChangedListeners) {
                    listener.onNotificationRemoved(msg.first, msg.second)
                }
            }
            MSG_NOTIFICATION_FULL_REFRESH -> {
                val list = message.obj as List<StatusBarNotification>
                for (listener in sNotificationsChangedListeners) {
                    listener.onNotificationFullRefresh(list)
                }
            }
        }
        return true
    }

    fun getActiveNotificationsSafely(keys: Array<String>?): Array<StatusBarNotification> {
        return try {
            activeNotifications ?: emptyArray()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException fetching notifications: ${e.message}")
            emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }
    }

    fun getNotificationsForKeys(keys: List<NotificationKeyData>): List<StatusBarNotification> {
        val keySet = keys.map { it.notificationKey }.toSet()
        return getActiveNotificationsSafely(null).filter { keySet.contains(it.key) }
    }

    private fun updateGroupKeyIfNecessary(sbn: StatusBarNotification) {
        val childKey = sbn.key
        val oldGroupKey = mNotificationGroupKeyMap[childKey]
        val newGroupKey = sbn.groupKey
        if (oldGroupKey == null || oldGroupKey != newGroupKey) {
            mNotificationGroupKeyMap[childKey] = newGroupKey
            if (oldGroupKey != null && mNotificationGroupMap.containsKey(oldGroupKey)) {
                val oldGroup = mNotificationGroupMap[oldGroupKey]
                oldGroup?.removeChildKey(childKey)
                if (oldGroup?.isEmpty() == true) {
                    mNotificationGroupMap.remove(oldGroupKey)
                }
            }
        }
        if (sbn.isGroup && newGroupKey != null) {
            var notificationGroup = mNotificationGroupMap[newGroupKey]
            if (notificationGroup == null) {
                notificationGroup = NotificationGroup()
                mNotificationGroupMap[newGroupKey] = notificationGroup
            }
            val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            if (isGroupSummary) {
                notificationGroup.setGroupSummaryKey(childKey)
            } else {
                notificationGroup.addChildKey(childKey)
            }
        }
    }

    fun notificationIsValidForUI(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        updateGroupKeyIfNecessary(sbn)

        try {
            currentRanking?.getRanking(sbn.key, mTempRanking)
            if (mTempRanking.canShowBadge() == false) {
                return false
            }
            val channel = mTempRanking.channel
            if (channel != null && channel.id == NotificationChannel.DEFAULT_CHANNEL_ID) {
                if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
                    return false
                }
            }
        } catch (_: Exception) {
            // Abaikan jika pemeringkatan belum tersedia
        }

        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)
        val text = notification.extras?.getCharSequence(Notification.EXTRA_TEXT)
        val missingTitleAndText = TextUtils.isEmpty(title) && TextUtils.isEmpty(text)
        val isGroupHeader = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        return !isGroupHeader && !missingTitleAndText
    }

    private fun toKeyPair(sbn: StatusBarNotification): Pair<PackageUserKey, NotificationKeyData> {
        return Pair.create(
            PackageUserKey.fromNotification(sbn),
            NotificationKeyData.fromNotification(sbn)
        )
    }

    interface NotificationsChangedListener {
        fun onNotificationPosted(postedPackageUserKey: PackageUserKey, notificationKey: NotificationKeyData)
        fun onNotificationRemoved(removedPackageUserKey: PackageUserKey, notificationKey: NotificationKeyData)
        fun onNotificationFullRefresh(activeNotifications: List<StatusBarNotification>)
    }
}
