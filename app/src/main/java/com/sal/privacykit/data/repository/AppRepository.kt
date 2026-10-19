package com.sal.privacykit.data.repository

import com.sal.privacykit.data.AppResolver
import com.sal.privacykit.data.model.AppInfo
import com.sal.privacykit.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppWithProfileCount(
    val app: AppInfo,
    val profileCount: Int,
)

class AppRepository(
    private val profileRepository: ProfileRepository,
    private val appResolver: AppResolver,
    private val appPreferences: AppPreferences,
) {
    fun observeApps(): Flow<List<AppWithProfileCount>> =
        combine(profileRepository.observeAll(), appPreferences.registeredAppIds) { profiles, registeredIds ->
            val allAppIds = registeredIds + profiles.map { it.appId }
            allAppIds.map { appId ->
                AppWithProfileCount(
                    app = appResolver.resolve(appId),
                    profileCount = profiles.count { it.appId == appId && !it.archived },
                )
            }.sortedBy { it.app.name.lowercase() }
        }
}
