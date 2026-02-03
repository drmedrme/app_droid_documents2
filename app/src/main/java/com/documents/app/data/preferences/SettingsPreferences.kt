package com.documents.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.documents.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System");

    companion object {
        fun fromString(value: String): ThemeMode {
            return entries.find { it.name == value } ?: SYSTEM
        }
    }
}

private val Context.settingsDataStore by preferencesDataStore(name = "documents_settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val apiBaseUrlKey = stringPreferencesKey("api_base_url")
    private val tenantIdKey = stringPreferencesKey("tenant_id")
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val speechPauseDurationKey = longPreferencesKey("speech_pause_duration_ms")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        val mode = preferences[themeModeKey] ?: ThemeMode.SYSTEM.name
        ThemeMode.fromString(mode)
    }

    val apiBaseUrl: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[apiBaseUrlKey] ?: BuildConfig.BASE_URL
    }

    val tenantId: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[tenantIdKey] ?: "11111111-1111-4111-8111-111111111111"
    }

    val apiKey: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[apiKeyKey] ?: BuildConfig.DEFAULT_API_KEY
    }

    val speechPauseDuration: Flow<Long> = context.settingsDataStore.data.map { preferences ->
        preferences[speechPauseDurationKey] ?: 3000L
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[apiBaseUrlKey] = url
        }
    }

    suspend fun setTenantId(id: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[tenantIdKey] = id
        }
    }

    suspend fun setApiKey(key: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[apiKeyKey] = key
        }
    }

    suspend fun setSpeechPauseDuration(ms: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[speechPauseDurationKey] = ms.coerceIn(1500L, 10000L)
        }
    }

    fun getApiBaseUrlBlocking(): String {
        return runBlocking { apiBaseUrl.first() }
    }

    fun getTenantIdBlocking(): String {
        return runBlocking { tenantId.first() }
    }

    fun getApiKeyBlocking(): String {
        return runBlocking { apiKey.first() }
    }
}
