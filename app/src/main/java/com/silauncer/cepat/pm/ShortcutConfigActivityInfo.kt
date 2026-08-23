package com.silauncer.cepat.pm

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.silauncer.cepat.R
import com.silauncer.cepat.util.PackageUserKey

/**
 * // [Jalur Class]: com.silauncer.cepat.pm.ShortcutConfigActivityInfo
 * // [Penjelasan]: Pembungkus kelas untuk mewakili aktivitas konfigurasi pintasan (shortcut configuration activity).
 * Memungkinkan query dan peluncuran aktivitas konfigurasi pintasan di seluruh profil pengguna (multi-user/Work Profile).
 */
abstract class ShortcutConfigActivityInfo protected constructor(
    private val componentName: ComponentName,
    private val user: UserHandle
) {

    fun getComponent(): ComponentName = componentName

    fun getUser(): UserHandle = user

    abstract fun getLabel(pm: PackageManager): CharSequence

    abstract fun getIcon(pm: PackageManager): Drawable?

    open fun startConfigActivity(activity: Activity, requestCode: Int): Boolean {
        // [Jalur Class]: com.silauncer.cepat.pm.ShortcutConfigActivityInfo
        // [Penjelasan]: Memulai aktivitas pembuatan shortcut bawaan dengan meluncurkan Intent ACTION_CREATE_SHORTCUT.
        val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
            component = getComponent()
        }
        return try {
            activity.startActivityForResult(intent, requestCode)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show()
            false
        } catch (e: SecurityException) {
            Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Launcher does not have permission to launch $intent", e)
            false
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.pm.ShortcutConfigActivityInfo.ShortcutConfigActivityInfoVO
     * // [Penjelasan]: Implementasi khusus Android O+ (API 26+) yang membungkus LauncherActivityInfo
     * untuk mendukung peluncuran lintas profil pengguna (Work Profile) via LauncherApps.getShortcutConfigActivityIntent.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    class ShortcutConfigActivityInfoVO(
        private val info: LauncherActivityInfo
    ) : ShortcutConfigActivityInfo(info.componentName, info.user) {

        override fun getLabel(pm: PackageManager): CharSequence {
            return info.label
        }

        override fun getIcon(pm: PackageManager): Drawable? {
            return info.getIcon(0)
        }

        override fun startConfigActivity(activity: Activity, requestCode: Int): Boolean {
            // [Jalur Class]: com.silauncer.cepat.pm.ShortcutConfigActivityInfoVO
            // [Penjelasan]: Jika user adalah user utama, gunakan metode dasar. Jika beda profil user,
            // dapatkan IntentSender dari system service LauncherApps untuk meluncurkan aktivitas konfigurasi.
            if (getUser() == Process.myUserHandle()) {
                return super.startConfigActivity(activity, requestCode)
            }
            val launcherApps = activity.getSystemService(LauncherApps::class.java)
            val intentSender: IntentSender? = launcherApps?.getShortcutConfigActivityIntent(info)
            return try {
                if (intentSender != null) {
                    activity.startIntentSenderForResult(intentSender, requestCode, null, 0, 0, 0)
                    true
                } else {
                    false
                }
            } catch (e: IntentSender.SendIntentException) {
                Toast.makeText(activity, R.string.activity_not_found, Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    companion object {
        private const val TAG = "SCActivityInfo"

        /**
         * // [Jalur Class]: com.silauncer.cepat.pm.ShortcutConfigActivityInfo
         * // [Penjelasan]: Mengueri daftar aktivitas konfigurasi shortcut dari sistem untuk seluruh user profile yang aktif.
         */
        @JvmStatic
        fun queryList(context: Context, packageUser: PackageUserKey?): List<ShortcutConfigActivityInfo> {
            val result = mutableListOf<ShortcutConfigActivityInfo>()
            val users: List<UserHandle>
            val packageName: String?

            if (packageUser == null) {
                users = UserCache.getInstance(context).getUserProfiles()
                packageName = null
            } else {
                users = listOfNotNull(packageUser.user)
                packageName = packageUser.packageName
            }

            val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return result

            for (user in users) {
                try {
                    val activityList = launcherApps.getShortcutConfigActivityList(packageName, user)
                    for (activityInfo in activityList) {
                        if (activityInfo.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.O) {
                            result.add(ShortcutConfigActivityInfoVO(activityInfo))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query shortcut config activity list for user $user", e)
                }
            }
            return result
        }
    }
}
