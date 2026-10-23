package com.sal.privacykit.data

import android.content.Context
import com.sal.privacykit.data.db.AppDatabase
import com.sal.privacykit.data.prefs.AppPreferences
import com.sal.privacykit.data.repository.AppRepository
import com.sal.privacykit.data.repository.ProfileApplier
import com.sal.privacykit.data.repository.ProfileRepository
import com.sal.privacykit.data.repository.RootAppDataManager
import com.sal.privacykit.xposed.XposedServiceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(
    context: Context,
) {
    private val database = AppDatabase.get(context)
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val appPreferences = AppPreferences(context)
    val appResolver = AppResolver(context)
    val xposedServiceConnection = XposedServiceConnection()
    val rootAppDataManager = RootAppDataManager()    
    private val profileWallpaperManager = ProfileWallpaperManager(appContext)
    val profileApplier = ProfileApplier(rootAppDataManager, profileWallpaperManager,appPreferences)
    val profileRepository = ProfileRepository(database, xposedServiceConnection, appPreferences, rootAppDataManager,profileApplier)
    val appRepository = AppRepository(profileRepository, appResolver, appPreferences)
    
    init {
        xposedServiceConnection.onBind = {
            applicationScope.launch { profileRepository.resync() }
        }
        applicationScope.launch {
            profileRepository.backfillMissingStaticValues()
            profileRepository.fixTemplateMismatchedValues()
            profileRepository.observeAll().first()
            val activeProfileId = appPreferences.activeProfileGlobalId.first()
            if (activeProfileId != null) {
                rootAppDataManager.switchAppsToProfile(appPreferences.registeredApps.first().keys, activeProfileId)
            }
        }
    }
}
