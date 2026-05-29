package com.vitalai.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val onboardingCompleted: Flow<Boolean> = context.appSettingsStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val themeMode: Flow<String> = context.appSettingsStore.data.map { preferences ->
        preferences[THEME_MODE] ?: THEME_SYSTEM
    }

    val language: Flow<String> = context.appSettingsStore.data.map { preferences ->
        preferences[LANGUAGE] ?: LANGUAGE_SYSTEM
    }

    val pendingFcmToken: Flow<String?> = context.appSettingsStore.data.map { preferences ->
        preferences[PENDING_FCM_TOKEN]
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.appSettingsStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: String) {
        context.appSettingsStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setLanguage(language: String) {
        context.appSettingsStore.edit { it[LANGUAGE] = language }
    }

    suspend fun savePendingFcmToken(token: String) {
        context.appSettingsStore.edit { it[PENDING_FCM_TOKEN] = token }
    }

    suspend fun clearPendingFcmToken() {
        context.appSettingsStore.edit { it.remove(PENDING_FCM_TOKEN) }
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val LANGUAGE_SYSTEM = "system"

        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        private val PENDING_FCM_TOKEN = stringPreferencesKey("pending_fcm_token")
    }
}
