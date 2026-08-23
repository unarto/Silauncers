package com.silauncer.cepat.dot

/**
 * FolderDotInfo
 *
 * // [Jalur Class]: com.silauncer.cepat.dot.FolderDotInfo
 * // [Penjelasan]: Subclass DotInfo khusus untuk Folder yang mengagregasi total notifikasi dari item-item yang ada di dalam folder (adaptasi dari AOSP Launcher3 FolderDotInfo).
 */
class FolderDotInfo : DotInfo() {

    companion object {
        private const val MIN_COUNT = 0
    }

    private var numNotifications: Int = 0

    /**
     * Menambahkan informasi dot dari item folder.
     */
    fun addDotInfo(dotToAdd: DotInfo?) {
        if (dotToAdd == null) return
        numNotifications += dotToAdd.getNotificationKeys().size
        numNotifications = numNotifications.coerceIn(MIN_COUNT, MAX_COUNT)
    }

    /**
     * Mengurangi informasi dot dari item folder yang dihapus/berubah.
     */
    fun subtractDotInfo(dotToSubtract: DotInfo?) {
        if (dotToSubtract == null) return
        numNotifications -= dotToSubtract.getNotificationKeys().size
        numNotifications = numNotifications.coerceIn(MIN_COUNT, MAX_COUNT)
    }

    /**
     * Menetapkan jumlah notifikasi secara langsung.
     */
    fun setNotificationCount(count: Int) {
        numNotifications = count.coerceIn(MIN_COUNT, MAX_COUNT)
    }

    override fun getNotificationCount(): Int {
        return numNotifications
    }

    override fun hasDot(): Boolean {
        return numNotifications > 0
    }

    override fun clear() {
        super.clear()
        numNotifications = 0
    }
}
