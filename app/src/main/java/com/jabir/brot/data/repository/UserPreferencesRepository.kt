package com.jabir.brot.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reelcount_settings")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_DAILY_TARGET = intPreferencesKey("daily_target")
        val KEY_INSTAGRAM_TARGET = intPreferencesKey("instagram_target")
        val KEY_FACEBOOK_TARGET = intPreferencesKey("facebook_target")
        val KEY_YOUTUBE_TARGET = intPreferencesKey("youtube_target")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val KEY_TRACKING_PAUSED = booleanPreferencesKey("tracking_paused")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_PURE_DARK_THEME = booleanPreferencesKey("pure_dark_theme")
        val KEY_DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_DAILY_RECAP_NOTIFICATION = booleanPreferencesKey("daily_recap_notification")
        val KEY_ENABLED_PLATFORMS = stringSetPreferencesKey("enabled_platforms")
    }

    val dailyTarget: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DAILY_TARGET] ?: 50 }

    val instagramTarget: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_INSTAGRAM_TARGET] ?: 20 }

    val facebookTarget: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FACEBOOK_TARGET] ?: 15 }

    val youtubeTarget: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_YOUTUBE_TARGET] ?: 15 }

    val overlayEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OVERLAY_ENABLED] ?: false }

    val trackingPaused: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_TRACKING_PAUSED] ?: false }

    val darkTheme: Flow<Boolean?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DARK_THEME] }

    val pureDarkTheme: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PURE_DARK_THEME] ?: true }

    val developerMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DEVELOPER_MODE] ?: false }

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }

    val dailyRecapNotification: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DAILY_RECAP_NOTIFICATION] ?: true }

    val enabledPlatforms: Flow<Set<String>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            it[KEY_ENABLED_PLATFORMS] ?: setOf("INSTAGRAM", "FACEBOOK", "YOUTUBE")
        }

    suspend fun setDailyTarget(target: Int) {
        dataStore.edit { it[KEY_DAILY_TARGET] = target }
    }

    suspend fun setPlatformTargets(instagram: Int, facebook: Int, youtube: Int) {
        dataStore.edit {
            it[KEY_INSTAGRAM_TARGET] = instagram
            it[KEY_FACEBOOK_TARGET] = facebook
            it[KEY_YOUTUBE_TARGET] = youtube
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_OVERLAY_ENABLED] = enabled }
    }

    suspend fun setTrackingPaused(paused: Boolean) {
        dataStore.edit { it[KEY_TRACKING_PAUSED] = paused }
    }

    suspend fun setDarkTheme(enabled: Boolean?) {
        dataStore.edit {
            if (enabled != null) it[KEY_DARK_THEME] = enabled
            else it.remove(KEY_DARK_THEME)
        }
    }

    suspend fun setPureDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_PURE_DARK_THEME] = enabled }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DEVELOPER_MODE] = enabled }
    }

    suspend fun setDailyRecapNotification(enabled: Boolean) {
        dataStore.edit { it[KEY_DAILY_RECAP_NOTIFICATION] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setPlatformEnabled(platform: String, enabled: Boolean) {
        dataStore.edit {
            val current = (it[KEY_ENABLED_PLATFORMS] ?: setOf("INSTAGRAM", "FACEBOOK", "YOUTUBE")).toMutableSet()
            if (enabled) current.add(platform) else current.remove(platform)
            it[KEY_ENABLED_PLATFORMS] = current
        }
    }
}
