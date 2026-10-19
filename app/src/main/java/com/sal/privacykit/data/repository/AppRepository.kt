package com.sal.privacykit.data.repository

import com.sal.privacykit.data.AppResolver
import com.sal.privacykit.data.model.AppInfo
import com.sal.privacykit.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppWithProfileCount(
    val app: AppInfo,
    val profileCount: Int,
    val enabled: Boolean,
)

class AppRepository(
    private val profileRepository: ProfileRepository,
    private val appResolver: AppResolver,
    private val appPreferences: AppPreferences,
) {
    suspend fun setAppEnabled(appId: String, appName: String, enabled: Boolean) {
        appPreferences.setAppEnabled(appId, appName, enabled)
        profileRepository.resync()
    }

    suspend fun registerApp(appId: String, appName: String) = appPreferences.registerApp(appId, appName)

    fun observeApps(): Flow<List<AppWithProfileCount>> =
        combine(profileRepository.observeAll(), appPreferences.registeredApps) { profiles, registeredApps ->
            val allAppIds = registeredApps.keys + profiles.map { it.appId }
            allAppIds.map { appId ->
                AppWithProfileCount(
                    app = appResolver.resolve(appId),
                    profileCount = profiles.count { it.appId == appId && !it.archived },
                    enabled = registeredApps[appId]?.enabled ?: true,
                )
            }.sortedBy { it.app.name.lowercase() }
        }
}
