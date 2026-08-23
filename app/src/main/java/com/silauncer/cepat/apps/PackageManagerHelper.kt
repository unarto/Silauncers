package com.silauncer.cepat.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class PackageManagerHelper(private val context: Context) {
    private val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    fun startDetailsActivityForInfo(info: AppInfo, sourceBounds: Rect?, opts: Bundle?) {
        try {
            launcherApps.startAppDetailsActivity(info.componentName, info.user, sourceBounds, opts)
        } catch (e: SecurityException) {
            Toast.makeText(context, "Activity not found", Toast.LENGTH_SHORT).show()
            Log.e("PackageManagerHelper", "Unable to launch settings", e)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Activity not found", Toast.LENGTH_SHORT).show()
            Log.e("PackageManagerHelper", "Unable to launch settings", e)
        }
    }
}
