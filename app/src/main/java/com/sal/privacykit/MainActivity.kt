package com.sal.privacykit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.sal.privacykit.data.LocalAppContainer
import com.sal.privacykit.data.prefs.ThemeMode
import com.sal.privacykit.ui.navigation.PrivacyKitNavHost
import com.sal.privacykit.ui.theme.PrivacyKitTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Surface

class MainActivity : ComponentActivity() {
    private val requestHealthConnectPermission = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
        val writePermission = HealthPermission.getWritePermission(StepsRecord::class)
        if (writePermission in grantedPermissions) {
            lifecycleScope.launch {
                (application as PrivacyKitApplication).container.profileRepository
                    .refreshHealthConnectForActiveProfile()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PrivacyKitApplication).container

        lifecycleScope.launch {
            lifecycle.whenStarted {
                runCatching {
                    if (HealthConnectClient.getSdkStatus(this@MainActivity) != HealthConnectClient.SDK_AVAILABLE) {
                        return@whenStarted
                    }
                    val client = HealthConnectClient.getOrCreate(this@MainActivity)
                    val writePermission = HealthPermission.getWritePermission(StepsRecord::class)
                    if (writePermission !in client.permissionController.getGrantedPermissions()) {
                        requestHealthConnectPermission.launch(setOf(writePermission))
                    }
                }.onFailure { error ->
                    Log.w("MainActivity", "Unable to check Health Connect write permission", error)
                }
            }
        }

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val themeMode by container.appPreferences.themeMode.collectAsState(initial = ThemeMode.DARK)
                val systemDark = isSystemInDarkTheme()
                val darkTheme = when (themeMode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                PrivacyKitTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PrivacyKitNavHost()
                    }
                }
            }
        }
    }
}
