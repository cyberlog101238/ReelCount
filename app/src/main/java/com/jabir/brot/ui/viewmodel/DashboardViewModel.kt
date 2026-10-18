package com.jabir.brot.ui.viewmodel

import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jabir.brot.ReelCountApp
import com.jabir.brot.domain.BrainState
import com.jabir.brot.domain.model.Platform
import com.jabir.brot.domain.model.TrackingStatus
import com.jabir.brot.service.ReelCountAccessibilityService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todayTotal: Int = 0,
    val instagramCount: Int = 0,
    val facebookCount: Int = 0,
    val youtubeCount: Int = 0,
    val durationMs: Long = 0,
    val dailyTarget: Int = 50,
    val brainState: BrainState = BrainState.FRESH,
    val trackingStatus: TrackingStatus = TrackingStatus.ServiceNotRunning,
    val lastDetectionTime: Long? = null,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = true,
    val isOverlayEnabled: Boolean = false,
    val isTrackingPaused: Boolean = false
)

class DashboardViewModel : ViewModel() {
    private val app = ReelCountApp.instance
    private val repository = app.countingRepository
    private val prefs = app.userPreferencesRepository
    private val calculator = app.statisticsCalculator
    private val coordinator = app.trackingCoordinator

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        initializeCounts()
        observeData()
    }

    fun refreshPermissions() {
        val hasOverlay = Settings.canDrawOverlays(app)
        val hasNotif = NotificationManagerCompat.from(app).areNotificationsEnabled()
        val hasAccess = ReelCountAccessibilityService.isRunning

        _uiState.update {
            it.copy(
                isAccessibilityEnabled = hasAccess,
                isOverlayPermissionGranted = hasOverlay,
                isNotificationPermissionGranted = hasNotif
            )
        }
    }

    private fun initializeCounts() {
        viewModelScope.launch {
            try {
                val total = repository.getTodayCount().first()
                val ig = repository.getTodayCountByPlatform(Platform.INSTAGRAM).first()
                val fb = repository.getTodayCountByPlatform(Platform.FACEBOOK).first()
                val yt = repository.getTodayCountByPlatform(Platform.YOUTUBE).first()
                val dbDuration = repository.getTodayDuration().first()
                val liveDuration = coordinator.liveTodayDurationMs.value
                val duration = maxOf(dbDuration, liveDuration)
                val lastTime = repository.getLatestDetectionTime()
                coordinator.syncInitialCounts(total, ig, fb, yt, duration, lastTime)

                _uiState.update { current ->
                    current.copy(
                        todayTotal = maxOf(current.todayTotal, total),
                        instagramCount = maxOf(current.instagramCount, ig),
                        facebookCount = maxOf(current.facebookCount, fb),
                        youtubeCount = maxOf(current.youtubeCount, yt),
                        durationMs = maxOf(current.durationMs, duration),
                        lastDetectionTime = lastTime ?: current.lastDetectionTime,
                        brainState = calculator.calculateBrainState(maxOf(current.todayTotal, total))
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            val countsFlow = combine(
                coordinator.liveTodayTotal,
                repository.getTodayCount().catch { emit(0) },
                coordinator.livePlatformCounts,
                coordinator.liveLastDetectionTime
            ) { liveTotal, roomTotal, platforms, lastTime ->
                val total = maxOf(liveTotal, roomTotal)
                val ig = platforms[Platform.INSTAGRAM] ?: 0
                val fb = platforms[Platform.FACEBOOK] ?: 0
                val yt = platforms[Platform.YOUTUBE] ?: 0
                StatsData(total, ig, fb, yt, lastTime)
            }

            val targetAndDurationFlow = combine(
                repository.getTodayDuration().catch { emit(0L) },
                coordinator.liveTodayDurationMs,
                prefs.dailyTarget.catch { emit(50) }
            ) { dbDuration, liveDuration, target ->
                maxOf(dbDuration, liveDuration) to target
            }

            val statusFlow = combine(
                coordinator.trackingStatus,
                prefs.overlayEnabled.catch { emit(false) },
                coordinator.isTrackingPausedFlow
            ) { status, overlay, paused ->
                Triple(status, overlay, paused)
            }

            combine(countsFlow, targetAndDurationFlow, statusFlow) { stats, (duration, target), (status, overlay, paused) ->
                val hasOverlay = Settings.canDrawOverlays(app)
                val hasNotif = NotificationManagerCompat.from(app).areNotificationsEnabled()

                DashboardUiState(
                    todayTotal = stats.total,
                    instagramCount = stats.ig,
                    facebookCount = stats.fb,
                    youtubeCount = stats.yt,
                    durationMs = duration,
                    dailyTarget = target,
                    brainState = calculator.calculateBrainState(stats.total),
                    trackingStatus = if (paused) TrackingStatus.TrackingPaused
                    else if (!ReelCountAccessibilityService.isRunning) TrackingStatus.ServiceNotRunning
                    else status,
                    lastDetectionTime = stats.lastTime,
                    isAccessibilityEnabled = ReelCountAccessibilityService.isRunning,
                    isOverlayPermissionGranted = hasOverlay,
                    isNotificationPermissionGranted = hasNotif,
                    isOverlayEnabled = overlay,
                    isTrackingPaused = paused
                )
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleTrackingPause() {
        setTrackingPaused(!_uiState.value.isTrackingPaused)
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

    private data class StatsData(
        val total: Int,
        val ig: Int,
        val fb: Int,
        val yt: Int,
        val lastTime: Long?
    )
}
