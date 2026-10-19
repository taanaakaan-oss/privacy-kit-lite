package com.sal.privacykit.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        fun activeProfileForApp(appId: String) = stringPreferencesKey("active_profile_$appId")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val PROFILE_ORGANIZATION_ENABLED = booleanPreferencesKey("profile_organization_enabled")
        val CUSTOM_HOOKS_JSON = stringPreferencesKey("custom_hooks_json")
        val ACTIVE_PROFILE_GLOBAL = stringPreferencesKey("active_profile_global")
        val REGISTERED_APPS = stringPreferencesKey("registered_apps")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    fun activeProfileId(appId: String): Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.activeProfileForApp(appId)]?.toLongOrNull()
    }

    suspend fun setActiveProfile(appId: String, profileId: Long) {
        context.dataStore.edit { it[Keys.activeProfileForApp(appId)] = profileId.toString() }
    }

    val activeProfileGlobalId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROFILE_GLOBAL]?.toLongOrNull()
    }

    suspend fun setActiveProfileGlobal(profileId: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_PROFILE_GLOBAL] = profileId.toString() }
    }

    val registeredAppIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        runCatching {
            val array = JSONArray(prefs[Keys.REGISTERED_APPS] ?: "[]")
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.toSet()
        }.getOrDefault(emptySet())
    }

    suspend fun registerApp(appId: String) {
        if (appId.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = runCatching {
                val array = JSONArray(prefs[Keys.REGISTERED_APPS] ?: "[]")
                (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.toSet()
            }.getOrDefault(emptySet())
            prefs[Keys.REGISTERED_APPS] = JSONArray((current + appId).sorted()).toString()
        }
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_ONBOARDING] ?: false
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = seen }
    }

    val profileOrganizationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PROFILE_ORGANIZATION_ENABLED] ?: true
    }

    suspend fun setProfileOrganizationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PROFILE_ORGANIZATION_ENABLED] = enabled }
    }

    val customHooksJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_HOOKS_JSON] ?: "[]"
    }

    suspend fun setCustomHooksJson(json: String) {
        context.dataStore.edit { it[Keys.CUSTOM_HOOKS_JSON] = json }
    }
}
