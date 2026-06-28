package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "DARK", "LIGHT", "SYSTEM"
        val FOCUS_MODE = booleanPreferencesKey("focus_mode")
        val SYNC_SERVER_URL = stringPreferencesKey("sync_server_url")
        val SYNC_API_TOKEN = stringPreferencesKey("sync_api_token")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val SIMULATED_DEVICE_B_ENABLED = booleanPreferencesKey("simulated_device_b_enabled")

        // Customizable shortcuts
        val SHORTCUT_BOLD = stringPreferencesKey("shortcut_bold")
        val SHORTCUT_ITALIC = stringPreferencesKey("shortcut_italic")
        val SHORTCUT_PREVIEW = stringPreferencesKey("shortcut_preview")
        val SHORTCUT_FOCUS = stringPreferencesKey("shortcut_focus")
        val SHORTCUT_SYNC = stringPreferencesKey("shortcut_sync")

        val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
        val CUSTOM_DICTIONARY = stringPreferencesKey("custom_dictionary")
    }

    // Default shortcuts
    val defaultShortcuts = mapOf(
        "Bold" to "Ctrl+B",
        "Italic" to "Ctrl+I",
        "Preview Toggle" to "Ctrl+P",
        "Focus Toggle" to "Ctrl+F",
        "Sync Now" to "Ctrl+S"
    )

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "DARK" // Default to DARK mode as requested for distraction-free writing
    }

    val focusModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FOCUS_MODE] ?: false
    }

    val syncServerUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SYNC_SERVER_URL] ?: ""
    }

    val syncApiTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SYNC_API_TOKEN] ?: ""
    }

    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC_TIME] ?: 0L
    }

    val simulatedDeviceBEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SIMULATED_DEVICE_B_ENABLED] ?: false
    }

    val autoSaveEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SAVE_ENABLED] ?: true // Defaults to true
    }

    val customDictionaryFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_DICTIONARY] ?: ""
    }

    // Shortcuts
    val shortcutBoldFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SHORTCUT_BOLD] ?: "Ctrl+B"
    }

    val shortcutItalicFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SHORTCUT_ITALIC] ?: "Ctrl+I"
    }

    val shortcutPreviewFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SHORTCUT_PREVIEW] ?: "Ctrl+P"
    }

    val shortcutFocusFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SHORTCUT_FOCUS] ?: "Ctrl+F"
    }

    val shortcutSyncFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SHORTCUT_SYNC] ?: "Ctrl+S"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setFocusMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_MODE] = enabled
        }
    }

    suspend fun setSyncConfig(serverUrl: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_SERVER_URL] = serverUrl
            preferences[SYNC_API_TOKEN] = token
        }
    }

    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME] = time
        }
    }

    suspend fun setSimulatedDeviceBEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SIMULATED_DEVICE_B_ENABLED] = enabled
        }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SAVE_ENABLED] = enabled
        }
    }

    suspend fun setCustomDictionary(words: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_DICTIONARY] = words.joinToString(",")
        }
    }

    suspend fun updateShortcut(action: String, keyCombination: String) {
        context.dataStore.edit { preferences ->
            when (action) {
                "Bold" -> preferences[SHORTCUT_BOLD] = keyCombination
                "Italic" -> preferences[SHORTCUT_ITALIC] = keyCombination
                "Preview Toggle" -> preferences[SHORTCUT_PREVIEW] = keyCombination
                "Focus Toggle" -> preferences[SHORTCUT_FOCUS] = keyCombination
                "Sync Now" -> preferences[SHORTCUT_SYNC] = keyCombination
            }
        }
    }
}
