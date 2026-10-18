package com.jabir.brot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jabir.brot.ReelCountApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalsUiState(
    val dailyTarget: Int = 50,
    val instagramTarget: Int = 20,
    val facebookTarget: Int = 15,
    val youtubeTarget: Int = 15,
    val isOverlayEnabled: Boolean = false,
    val isTrackingPaused: Boolean = false
)

private data class PlatformTargets(
    val ig: Int,
    val fb: Int,
    val yt: Int
)

class GoalsViewModel : ViewModel() {
    private val app = ReelCountApp.instance
    private val prefs = app.userPreferencesRepository
    private val coordinator = app.trackingCoordinator

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val platformTargetsFlow = combine(
                prefs.instagramTarget,
                prefs.facebookTarget,
                prefs.youtubeTarget
            ) { ig, fb, yt ->
                PlatformTargets(ig, fb, yt)
            }

            combine(
                prefs.dailyTarget,
                platformTargetsFlow,
                prefs.overlayEnabled,
                prefs.trackingPaused
            ) { daily, targets, overlay, paused ->
                GoalsUiState(
                    dailyTarget = daily,
                    instagramTarget = targets.ig,
                    facebookTarget = targets.fb,
                    youtubeTarget = targets.yt,
                    isOverlayEnabled = overlay,
                    isTrackingPaused = paused
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDailyTarget(target: Int) {
        viewModelScope.launch {
            prefs.setDailyTarget(target)
        }
    }

    fun setPlatformTargets(ig: Int, fb: Int, yt: Int) {
        viewModelScope.launch {
            prefs.setPlatformTargets(ig, fb, yt)
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setOverlayEnabled(enabled)
        }
    }

    fun setTrackingPaused(paused: Boolean) {
        viewModelScope.launch {
            prefs.setTrackingPaused(paused)
            coordinator.setTrackingPaused(paused)
        }
    }
}
