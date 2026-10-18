package com.jabir.brot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jabir.brot.ReelCountApp
import com.jabir.brot.service.DailyNotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val darkTheme: Boolean? = null,
    val pureDarkTheme: Boolean = true,
    val developerMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dailyRecapNotification: Boolean = true,
    val enabledPlatforms: Set<String> = setOf("INSTAGRAM", "FACEBOOK", "YOUTUBE"),
    val isOverlayEnabled: Boolean = false,
    val dataCleared: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val app = ReelCountApp.instance
    private val repository = app.countingRepository
    private val prefs = app.userPreferencesRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.darkTheme,
                prefs.pureDarkTheme,
                prefs.developerMode,
                prefs.notificationsEnabled,
                prefs.dailyRecapNotification
            ) { dark, pureDark, devMode, notif, recap ->
                Tuple5Settings(dark, pureDark, devMode, notif, recap)
            }.combine(
                combine(
                    prefs.enabledPlatforms,
                    prefs.overlayEnabled
                ) { platforms, overlay ->
                    platforms to overlay
                }
            ) { t, (platforms, overlay) ->
                SettingsUiState(
                    darkTheme = t.dark,
                    pureDarkTheme = t.pureDark,
                    developerMode = t.devMode,
                    notificationsEnabled = t.notif,
                    dailyRecapNotification = t.recap,
                    enabledPlatforms = platforms,
                    isOverlayEnabled = overlay
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDarkTheme(enabled: Boolean?) {
        viewModelScope.launch { prefs.setDarkTheme(enabled) }
    }

    fun setPureDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefs.setPureDarkTheme(enabled) }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDeveloperMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }

    fun setDailyRecapNotification(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setDailyRecapNotification(enabled)
            if (enabled) {
                DailyNotificationScheduler.scheduleDailyRecap(app)
            } else {
                DailyNotificationScheduler.cancelDailyRecap(app)
            }
        }
    }

    fun togglePlatform(platform: String) {
        viewModelScope.launch {
            val current = _uiState.value.enabledPlatforms.contains(platform)
            prefs.setPlatformEnabled(platform, !current)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiState.value = _uiState.value.copy(dataCleared = true)
        }
    }

    private data class Tuple5Settings(
        val dark: Boolean?,
        val pureDark: Boolean,
        val devMode: Boolean,
        val notif: Boolean,
        val recap: Boolean
    )
}
