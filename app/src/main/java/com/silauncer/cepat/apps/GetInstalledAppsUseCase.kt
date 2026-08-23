package com.silauncer.cepat.apps

import android.content.Context
import android.os.Process
import android.os.UserHandle
import com.silauncer.cepat.pm.UserCache

class GetInstalledAppsUseCase(private val appDataSource: AppDataSource, private val context: Context) {
    constructor(context: Context) : this(AppDataSource(context.applicationContext), context.applicationContext)

    suspend operator fun invoke(): List<AppInfo> {
        val userProfiles = UserCache.getInstance(context).getUserProfiles()
        val allApps = mutableListOf<AppInfo>()
        for (user in userProfiles) {
            allApps.addAll(appDataSource.getInstalledApps(null, user))
        }
        return allApps
    }
}
