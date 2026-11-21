package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "border_ports_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_preference")
        val ACTIVE_PORT = stringPreferencesKey("active_port_name")
        val BIOMETRIC_USERNAME = stringPreferencesKey("biometric_username")
        val ALERT_SOUND_ENABLED = booleanPreferencesKey("alert_sound_enabled")
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME] ?: ThemePreference.DARK.name
        runCatching { ThemePreference.valueOf(raw) }.getOrDefault(ThemePreference.DARK)
    }

    val activePortName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PORT] ?: "المنفذ الرئيسي"
    }

    val biometricUsername: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_USERNAME]?.takeIf { it.isNotBlank() }
    }

    val alertSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ALERT_SOUND_ENABLED] ?: true
    }

    suspend fun setThemePreference(pref: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = pref.name }
    }

    suspend fun setActivePortName(portName: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PORT] = portName.trim() }
    }

    suspend fun setBiometricUsername(username: String?) {
        context.dataStore.edit { prefs ->
            if (username.isNullOrBlank()) {
                prefs.remove(Keys.BIOMETRIC_USERNAME)
            } else {
                prefs[Keys.BIOMETRIC_USERNAME] = username.trim()
            }
        }
    }

    suspend fun setAlertSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALERT_SOUND_ENABLED] = enabled }
    }
}
