package com.inferra.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val glassIntensity: Float = 0.8f,
    val isDarkMode: Boolean = true,
    val isTelemetryEnabled: Boolean = false,
    val autoRefreshData: Boolean = true,
    val modelStoragePath: String = "",
    val hfToken: String = ""
)

class SettingsRepository(private val context: Context) {

    private companion object {
        val KEY_GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_IS_TELEMETRY_ENABLED = booleanPreferencesKey("is_telemetry_enabled")
        val KEY_AUTO_REFRESH_DATA = booleanPreferencesKey("auto_refresh_data")
        val KEY_MODEL_STORAGE_PATH = stringPreferencesKey("model_storage_path")
        val KEY_HF_TOKEN = stringPreferencesKey("hf_token")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            glassIntensity = prefs[KEY_GLASS_INTENSITY] ?: 0.8f,
            isDarkMode = prefs[KEY_IS_DARK_MODE] ?: true,
            isTelemetryEnabled = prefs[KEY_IS_TELEMETRY_ENABLED] ?: false,
            autoRefreshData = prefs[KEY_AUTO_REFRESH_DATA] ?: true,
            modelStoragePath = prefs[KEY_MODEL_STORAGE_PATH] ?: "",
            hfToken = prefs[KEY_HF_TOKEN] ?: ""
        )
    }

    suspend fun updateGlassIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_INTENSITY] = intensity
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_DARK_MODE] = enabled
        }
    }

    suspend fun setTelemetryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_TELEMETRY_ENABLED] = enabled
        }
    }

    suspend fun setModelStoragePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MODEL_STORAGE_PATH] = path
        }
    }

    suspend fun setHfToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HF_TOKEN] = token
        }
    }
}
