package com.sal.privacykit.data.repository

import com.sal.privacykit.data.prefs.AppPreferences
import com.sal.privacykit.data.ProfileWallpaperManager
import kotlinx.coroutines.flow.first

class ProfileApplier (
    private val rootAppDataManager: RootAppDataManager,
    private val profileWallpaperManager: ProfileWallpaperManager,
    private val appPreferences: AppPreferences,
) {
    suspend fun applyProfile(profileId: Long) {
        val packages = appPreferences.registeredApps.first().keys

        rootAppDataManager.switchAppsToProfile(packages, profileId)
        profileWallpaperManager.apply(profileId)
    }
}