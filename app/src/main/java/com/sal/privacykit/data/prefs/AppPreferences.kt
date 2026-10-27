package com.sal.privacykit.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class RegisteredApp(val appName: String, val enabled: Boolean)

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
        val STEP_COUNT_MIN = stringPreferencesKey("step_count_min")
        val STEP_COUNT_MAX = stringPreferencesKey("step_count_max")
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

    val registeredApps: Flow<Map<String, RegisteredApp>> = context.dataStore.data.map { prefs ->
        decodeRegisteredApps(prefs[Keys.REGISTERED_APPS] ?: "[]")
    }

    val registeredAppIds: Flow<Set<String>> = registeredApps.map { apps ->
        apps.filterValues { it.enabled }.keys
    }

    suspend fun registerApp(appId: String, appName: String = appId) {
        if (appId.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = decodeRegisteredApps(prefs[Keys.REGISTERED_APPS] ?: "[]").toMutableMap()
            val old = current[appId]
            val updated = RegisteredApp(appName.ifBlank { old?.appName ?: appId }, old?.enabled ?: true)
            if (old != updated) {
                current[appId] = updated
                prefs[Keys.REGISTERED_APPS] = encodeRegisteredApps(current)
            }
        }
    }

    suspend fun setAppEnabled(appId: String, appName: String, enabled: Boolean) {
        if (appId.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = decodeRegisteredApps(prefs[Keys.REGISTERED_APPS] ?: "[]").toMutableMap()
            current[appId] = RegisteredApp(appName.ifBlank { current[appId]?.appName ?: appId }, enabled)
            prefs[Keys.REGISTERED_APPS] = encodeRegisteredApps(current)
        }
    }

    private fun decodeRegisteredApps(raw: String): Map<String, RegisteredApp> {
        val json = runCatching { JSONObject(raw) }.getOrNull()
        if (json != null) {
            return json.keys().asSequence().mapNotNull { packageName ->
                val value = json.opt(packageName)
                val entry = if (value is JSONObject) {
                    RegisteredApp(value.optString("appName", packageName), value.optBoolean("enabled", true))
                } else RegisteredApp(packageName, true)
                packageName.takeIf(String::isNotBlank)?.let { it to entry }
            }.toMap()
        }
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyMap()
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf(String::isNotBlank)?.let { it to RegisteredApp(it, true) }
        }.toMap()
    }

    private fun encodeRegisteredApps(apps: Map<String, RegisteredApp>): String =
        JSONObject().apply {
            apps.toSortedMap().forEach { (packageName, entry) ->
                put(packageName, JSONObject().put("appName", entry.appName).put("enabled", entry.enabled))
            }
        }.toString()
    

    val stepCountMin: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.STEP_COUNT_MIN]?.toLongOrNull() ?: 12000L
    }

    val stepCountMax: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.STEP_COUNT_MAX]?.toLongOrNull() ?: 15000L
    }

    suspend fun setStepCountRange(min: Long, max: Long) {
        require(min >= 0L) { "Minimum steps must be non-negative" }
        require(max >= min) { "Maximum steps must be greater than or equal to minimum steps" }
        require(max <= Int.MAX_VALUE.toLong()) { "Maximum steps is too large" }
        context.dataStore.edit { prefs ->
            prefs[Keys.STEP_COUNT_MIN] = min.toString()
            prefs[Keys.STEP_COUNT_MAX] = max.toString()
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
