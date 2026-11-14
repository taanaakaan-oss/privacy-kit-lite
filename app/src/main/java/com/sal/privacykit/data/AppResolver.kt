package com.sal.privacykit.data

import android.content.Context
import android.content.Intent
import com.sal.privacykit.data.model.AppInfo

/**
 * Resolves real installed apps via PackageManager, replacing the old hardcoded 7-app catalog.
 * appId across the data layer is the real package name.
 */
class AppResolver(private val context: Context) {
    private val cache = mutableMapOf<String, AppInfo>()

    /** Resolves a single app, falling back to the bare package name if it's no longer installed. */
    fun resolve(packageName: String): AppInfo = synchronized(cache) {
        cache.getOrPut(packageName) {
            val pm = context.packageManager
            runCatching {
                val info = pm.getApplicationInfo(packageName, 0)
                AppInfo(
                    id = packageName,
                    name = pm.getApplicationLabel(info).toString(),
                    packageName = packageName,
                    icon = runCatching { pm.getApplicationIcon(info) }.getOrNull(),
                )
            }.getOrDefault(AppInfo(id = packageName, name = packageName, packageName = packageName))
        }
    }

    /** Lists launchable apps and explicitly included system apps. */
    fun listLaunchableApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        val packageNames = pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .toMutableSet()

        // Add Health Connect even if it has no launcher activity.
        val healthConnectPackage = "com.google.android.apps.healthdata"

        val isHealthConnectInstalled = runCatching {
            pm.getApplicationInfo(healthConnectPackage, 0)
        }.isSuccess

        if (isHealthConnectInstalled) {
            packageNames.add(healthConnectPackage)
        }

        return packageNames
            .map { resolve(it) }
            .sortedBy { it.name.lowercase() }
    }
}
