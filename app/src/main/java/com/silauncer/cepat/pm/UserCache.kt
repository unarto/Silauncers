package com.silauncer.cepat.pm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserHandle
import android.os.UserManager
import android.util.ArrayMap
import android.util.LongSparseArray
import java.io.Closeable

/**
 * UserCache
 *
 * // [Jalur Class]: com.silauncer.cepat.pm.UserCache
 * // [Penjelasan]: Cache lokal untuk UserHandle guna menghindari panggilan RPC berulang ke UserManager.
 * Mendengarkan perubahan pada profile user (seperti Work Profile) dan menyimpan daftar user secara sinkron.
 */
class UserCache private constructor(private val context: Context) {

    private val userManager: UserManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val userChangeListeners = mutableListOf<Runnable>()

    private var users: LongSparseArray<UserHandle>? = null
    private var userToSerialMap: ArrayMap<UserHandle, Long>? = null

    // [Jalur Class]: com.silauncer.cepat.pm.UserCache
    // [Penjelasan]: Inisialisasi awal eager load untuk mengisi cache profil dan serial number pengguna sejak pertama kali UserCache dibuat.
    init {
        enableAndResetCache()
    }

    private val userChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            enableAndResetCache()
            userChangeListeners.forEach { it.run() }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserCache? = null

        fun getInstance(context: Context): UserCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.pm.UserCache
    // [Penjelasan]: Mendaftarkan listener perubahan profil user (misal managed profile ditambah/dihapus) dan mendaftarkan broadcast receiver jika belum aktif.
    fun addUserChangeListener(command: Runnable): Closeable {
        synchronized(this) {
            if (userChangeListeners.isEmpty()) {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
                    addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                    addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                    addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                }
                context.registerReceiver(userChangeReceiver, filter)
                enableAndResetCache()
            }
            userChangeListeners.add(command)
            return Closeable { removeUserChangeListener(command) }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.pm.UserCache
    // [Penjelasan]: Memuat ulang seluruh daftar profil user dan pemetaan serial number secara atomik dan thread-safe.
    private fun enableAndResetCache() {
        synchronized(this) {
            val newUsers = LongSparseArray<UserHandle>()
            val newMap = ArrayMap<UserHandle, Long>()
            val userProfiles = userManager.userProfiles ?: emptyList()
            for (user in userProfiles) {
                val serial = userManager.getSerialNumberForUser(user)
                newUsers.put(serial, user)
                newMap.put(user, serial)
            }
            users = newUsers
            userToSerialMap = newMap
        }
    }

    // [Jalur Class]: com.silauncer.cepat.pm.UserCache
    // [Penjelasan]: Melepaskan listener dan unregister broadcast receiver jika sudah tidak ada listener aktif, tanpa membuang cache profil di memori.
    private fun removeUserChangeListener(command: Runnable) {
        synchronized(this) {
            userChangeListeners.remove(command)
            if (userChangeListeners.isEmpty()) {
                try {
                    context.unregisterReceiver(userChangeReceiver)
                } catch (e: IllegalArgumentException) {
                    // Receiver not registered
                }
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.pm.UserCache
    // [Penjelasan]: Mengembalikan serial number dari cache memori tanpa melakukan IPC ke UserManager, dengan fallback lazy-load jika cache belum terisi.
    fun getSerialNumberForUser(user: UserHandle): Long {
        synchronized(this) {
            userToSerialMap?.get(user)?.let { return it }
            if (userToSerialMap == null) {
                enableAndResetCache()
                userToSerialMap?.get(user)?.let { return it }
            }
        }
        val serial = userManager.getSerialNumberForUser(user)
        if (serial >= 0) {
            synchronized(this) {
                users?.put(serial, user)
                userToSerialMap?.put(user, serial)
            }
        }
        return serial
    }

    // [Jalur Class]: com.silauncer.cepat.pm.UserCache
    // [Penjelasan]: Mengembalikan UserHandle dari cache memori berdasarkan serial number, dengan lazy-load jika belum terdaftar di cache.
    fun getUserForSerialNumber(serialNumber: Long): UserHandle? {
        synchronized(this) {
            users?.get(serialNumber)?.let { return it }
            if (users == null) {
                enableAndResetCache()
                users?.get(serialNumber)?.let { return it }
            }
        }
        val user = userManager.getUserForSerialNumber(serialNumber)
        if (user != null) {
            synchronized(this) {
                users?.put(serialNumber, user)
                userToSerialMap?.put(user, serialNumber)
            }
        }
        return user
    }

    // [Jalur Class]: com.silauncer.cepat.pm.UserCache
    // [Penjelasan]: Mengembalikan daftar profil user dari cache memori tanpa memicu panggilan IPC berulang ke UserManager.
    fun getUserProfiles(): List<UserHandle> {
        synchronized(this) {
            userToSerialMap?.let {
                return ArrayList(it.keys)
            }
            enableAndResetCache()
            userToSerialMap?.let {
                return ArrayList(it.keys)
            }
        }
        return userManager.userProfiles ?: emptyList()
    }
}
