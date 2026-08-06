package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kernel_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val APPLY_ON_BOOT = booleanPreferencesKey("apply_on_boot")
        val SELECTED_PROFILE_ID = longPreferencesKey("selected_profile_id")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "dark", "light"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SHOW_DANGER_WARNINGS = booleanPreferencesKey("show_danger_warnings")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_API_KEY] ?: ""
    }

    val applyOnBoot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[APPLY_ON_BOOT] ?: false
    }

    val selectedProfileId: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_PROFILE_ID] ?: 2L // Default to Balanced Mode (ID 2)
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "dark"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR] ?: true
    }

    val showDangerWarnings: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_DANGER_WARNINGS] ?: true
    }

    suspend fun setApplyOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APPLY_ON_BOOT] = enabled
        }
    }

    suspend fun setSelectedProfileId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PROFILE_ID] = id
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setShowDangerWarnings(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_DANGER_WARNINGS] = enabled
        }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_API_KEY] = key
        }
    }
}
