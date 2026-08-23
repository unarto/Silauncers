package com.silauncer.cepat.popup

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppActionHandler
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.apps.PackageManagerHelper

/**
 * SystemShortcut
 *
 * // [Jalur Class]: com.silauncer.cepat.popup.SystemShortcut
 * // [Penjelasan]: Representasi item pintasan sistem (App Info, Uninstall) pada popup menu aplikasi, dengan icon, label, dan onClick listener sesuai arsitektur AOSP Launcher3.
 */
abstract class SystemShortcut(
    val iconResId: Int,
    val labelResId: Int,
    val targetContext: Context,
    val appInfo: AppInfo,
    val originalView: View? = null
) : View.OnClickListener {

    var isEnabled: Boolean = true

    // [Jalur Class]: com.silauncer.cepat.popup.SystemShortcut
    // [Penjelasan]: Mengisi visual icon dan teks label pada baris tampilan pintasan sistem.
    fun setIconAndLabelFor(iconView: ImageView, labelView: TextView) {
        iconView.setImageResource(iconResId)
        iconView.isEnabled = isEnabled
        labelView.setText(labelResId)
        labelView.isEnabled = isEnabled
    }

    interface Factory {
        fun getShortcut(context: Context, appInfo: AppInfo, originalView: View?): SystemShortcut?
    }

    // [Jalur Class]: com.silauncer.cepat.popup.SystemShortcut.AppInfoShortcut
    // [Penjelasan]: Pintasan sistem untuk membuka layar Info Aplikasi pada Pengaturan sistem Android.
    class AppInfoShortcut(
        context: Context,
        appInfo: AppInfo,
        originalView: View? = null
    ) : SystemShortcut(
        R.drawable.ic_info,
        R.string.app_info,
        context,
        appInfo,
        originalView
    ) {
        override fun onClick(view: View?) {
            val bounds = Rect()
            originalView?.getGlobalVisibleRect(bounds)
            val opts = ActivityOptions.makeBasic().toBundle()
            PackageManagerHelper(targetContext).startDetailsActivityForInfo(appInfo, bounds, opts)
        }

        companion object {
            val FACTORY = object : Factory {
                override fun getShortcut(context: Context, appInfo: AppInfo, originalView: View?): SystemShortcut {
                    return AppInfoShortcut(context, appInfo, originalView)
                }
            }
        }
    }

    // [Jalur Class]: com.silauncer.cepat.popup.SystemShortcut.UninstallShortcut
    // [Penjelasan]: Pintasan sistem untuk mencopot pemasangan aplikasi (Uninstall).
    class UninstallShortcut(
        context: Context,
        appInfo: AppInfo,
        private val actionHandler: AppActionHandler? = null,
        originalView: View? = null
    ) : SystemShortcut(
        android.R.drawable.ic_menu_delete,
        R.string.uninstall,
        context,
        appInfo,
        originalView
    ) {
        override fun onClick(view: View?) {
            if (actionHandler != null) {
                actionHandler.requestUninstall(appInfo)
            } else {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:${appInfo.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    targetContext.startActivity(intent)
                } catch (_: Exception) {}
            }
        }

        companion object {
            fun createFactory(actionHandler: AppActionHandler): Factory {
                return object : Factory {
                    override fun getShortcut(context: Context, appInfo: AppInfo, originalView: View?): SystemShortcut {
                        return UninstallShortcut(context, appInfo, actionHandler, originalView)
                    }
                }
            }
        }
    }
}
