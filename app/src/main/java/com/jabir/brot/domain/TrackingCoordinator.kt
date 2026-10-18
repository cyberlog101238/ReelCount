package com.jabir.brot.domain

import com.jabir.brot.ReelCountApp
import com.jabir.brot.domain.detector.VideoTransitionDetector
import com.jabir.brot.domain.guard.DuplicateEventGuard
import com.jabir.brot.domain.model.*
import com.jabir.brot.service.ReelCountAccessibilityService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central orchestrator connecting the Accessibility Service, platform detectors,
 * duplicate guards, and session state.
 *
 * Exposes hot in-memory StateFlows for zero-latency UI updates in addition
 * to Room-backed persistence.
 */
class TrackingCoordinator(
    val transitionDetector: VideoTransitionDetector = VideoTransitionDetector(),
    val duplicateGuard: DuplicateEventGuard = DuplicateEventGuard(),
    val sessionManager: SessionManager = SessionManager(),
    val diagnostics: TrackingDiagnostics = TrackingDiagnostics()
) {
    private val coordinatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _trackingStatus = MutableStateFlow<TrackingStatus>(TrackingStatus.ServiceNotRunning)
    val trackingStatus: StateFlow<TrackingStatus> = _trackingStatus.asStateFlow()

    private val _isInsideReelFeed = MutableStateFlow(false)
    val isInsideReelFeed: StateFlow<Boolean> = _isInsideReelFeed.asStateFlow()

    private val _confirmedEvents = MutableSharedFlow<DetectionEvent>(extraBufferCapacity = 64)
    val confirmedEvents: SharedFlow<DetectionEvent> = _confirmedEvents.asSharedFlow()

    private val _liveTodayTotal = MutableStateFlow(0)
    val liveTodayTotal: StateFlow<Int> = _liveTodayTotal.asStateFlow()

    private val _livePlatformCounts = MutableStateFlow<Map<Platform, Int>>(
        mapOf(
            Platform.INSTAGRAM to 0,
            Platform.FACEBOOK to 0,
            Platform.YOUTUBE to 0
        )
    )
    val livePlatformCounts: StateFlow<Map<Platform, Int>> = _livePlatformCounts.asStateFlow()

    private val _liveLastDetectionTime = MutableStateFlow<Long?>(null)
    val liveLastDetectionTime: StateFlow<Long?> = _liveLastDetectionTime.asStateFlow()

    private val _liveTodayDurationMs = MutableStateFlow(0L)
    val liveTodayDurationMs: StateFlow<Long> = _liveTodayDurationMs.asStateFlow()

    private val _isTrackingPausedFlow = MutableStateFlow(false)
    val isTrackingPausedFlow: StateFlow<Boolean> = _isTrackingPausedFlow.asStateFlow()

    val isTrackingPaused: Boolean
        get() = _isTrackingPausedFlow.value

    fun setTrackingPaused(paused: Boolean) {
        _isTrackingPausedFlow.value = paused
        if (paused) {
            _trackingStatus.value = TrackingStatus.TrackingPaused
            _isInsideReelFeed.value = false
        }
    }

    private val _liveDiagnostics = MutableStateFlow(LiveDiagnostics())
    val liveDiagnostics: StateFlow<LiveDiagnostics> = _liveDiagnostics.asStateFlow()

    private var duplicateSuppressedCount = 0
    private var totalProcessedSnapshots = 0L

    init {
        // Real-time second-by-second duration counter while user is actively in reel feeds
        coordinatorScope.launch {
            while (isActive) {
                delay(1000)
                if (_isInsideReelFeed.value && !isTrackingPaused) {
                    _liveTodayDurationMs.value += 1000L
                }
            }
        }
    }

    fun onExitMonitoredApp() {
        if (_isInsideReelFeed.value) {
            handleSessionEnd()
            _isInsideReelFeed.value = false
            _trackingStatus.value = TrackingStatus.UnsupportedScreen
        }
    }

    fun onServiceConnected() {
        _trackingStatus.value = TrackingStatus.UnsupportedScreen
        _isInsideReelFeed.value = false
        _liveDiagnostics.update { it.copy(isServiceRunning = true) }
    }

    fun onServiceDisconnected() {
        handleSessionEnd()
        transitionDetector.resetAll()
        duplicateGuard.clear()
        _isInsideReelFeed.value = false
        _trackingStatus.value = TrackingStatus.ServiceNotRunning
        _liveDiagnostics.update { it.copy(isServiceRunning = false) }
    }

    fun isCandidatePending(platform: Platform): Boolean {
        return transitionDetector.getCurrentState(platform) == DetectionState.VIDEO_CANDIDATE
    }

    fun syncInitialCounts(total: Int, ig: Int, fb: Int, yt: Int, duration: Long = 0L, lastTime: Long? = null) {
        _liveTodayTotal.value = maxOf(_liveTodayTotal.value, total)
        _livePlatformCounts.value = mapOf(
            Platform.INSTAGRAM to maxOf(_livePlatformCounts.value[Platform.INSTAGRAM] ?: 0, ig),
            Platform.FACEBOOK to maxOf(_livePlatformCounts.value[Platform.FACEBOOK] ?: 0, fb),
            Platform.YOUTUBE to maxOf(_livePlatformCounts.value[Platform.YOUTUBE] ?: 0, yt)
        )
        if (duration > 0L) {
            _liveTodayDurationMs.value = maxOf(_liveTodayDurationMs.value, duration)
        }
        if (lastTime != null) {
            _liveLastDetectionTime.value = maxOf(_liveLastDetectionTime.value ?: 0L, lastTime)
        }

        // Update home screen widget with initial counts
        val app = ReelCountApp.instance
        coordinatorScope.launch {
            try {
                val target = app.userPreferencesRepository.dailyTarget.first()
                val brainState = app.statisticsCalculator.calculateBrainState(_liveTodayTotal.value)
                com.jabir.brot.ui.widget.ReelCountAppWidgetProvider.updateAllWidgets(
                    app, _liveTodayTotal.value, _liveTodayDurationMs.value, target, brainState
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSessionEnd() {
        val previousSession = sessionManager.endSession()
        if (previousSession != null) {
            transitionDetector.reset(previousSession.platform)
            duplicateGuard.onFeedExited(previousSession.platform)
            coordinatorScope.launch {
                try {
                    ReelCountApp.instance.countingRepository.endSession(previousSession.id, previousSession.videoCount)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun incrementLiveCount(platform: Platform, timestamp: Long) {
        val newTotal = _liveTodayTotal.value + 1
        _liveTodayTotal.value = newTotal
        val current = _livePlatformCounts.value.toMutableMap()
        current[platform] = (current[platform] ?: 0) + 1
        _livePlatformCounts.value = current
        _liveLastDetectionTime.value = timestamp

        // Check daily target, trigger alarm if limit reached, and update home screen widget
        coordinatorScope.launch {
            try {
                val app = ReelCountApp.instance
                val target = app.userPreferencesRepository.dailyTarget.first()
                val notificationsEnabled = app.userPreferencesRepository.notificationsEnabled.first()
                if (notificationsEnabled) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val today = sdf.format(Date(timestamp))
                    GoalAlertManager.checkAndTriggerAlert(app, newTotal, target, today)
                }
                val brainState = app.statisticsCalculator.calculateBrainState(newTotal)
                com.jabir.brot.ui.widget.ReelCountAppWidgetProvider.updateAllWidgets(
                    app, newTotal, _liveTodayDurationMs.value, target, brainState
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Processes an incoming screen snapshot from the accessibility service.
     *
     * @param snapshot Abstracted representation of the active window.
     * @return Confirmed [DetectionEvent] if a valid new video transition occurred, null otherwise.
     */
    fun processSnapshot(snapshot: ScreenSnapshot): DetectionEvent? {
        if (isTrackingPaused) {
            _isInsideReelFeed.value = false
            return null
        }

        val platform = Platform.fromPackageName(snapshot.packageName)
        if (platform == null) {
            // User is not inside any supported app
            handleSessionEnd()
            _isInsideReelFeed.value = false
            _trackingStatus.value = TrackingStatus.UnsupportedScreen
            return null
        }

        val activePlatform = sessionManager.activeSession.value?.platform
        if (activePlatform != null && activePlatform != platform) {
            handleSessionEnd()
        }

        val event = transitionDetector.evaluate(snapshot)
        if (event == null) {
            val state = transitionDetector.getCurrentState(platform)
            if (state == DetectionState.OUTSIDE_SUPPORTED_FEED) {
                _isInsideReelFeed.value = false
                _trackingStatus.value = TrackingStatus.UnsupportedScreen
            } else {
                _isInsideReelFeed.value = true
                _trackingStatus.value = TrackingStatus.Active(platform)
            }
            return null
        }

        if (event.feedDetected) {
            _isInsideReelFeed.value = true
            _trackingStatus.value = TrackingStatus.Active(platform)
            val session = sessionManager.startOrGetSession(platform)

            if (event.eligibleForCounting && event.newVideoTransition) {
                val shouldCommit = duplicateGuard.shouldCommit(event, session.id)
                if (shouldCommit) {
                    sessionManager.incrementVideoCount()
                    incrementLiveCount(platform, event.timestamp)
                    diagnostics.recordEvent(event, isCommitted = true)
                    _confirmedEvents.tryEmit(event)
                    _liveDiagnostics.update {
                        it.copy(
                            lastFingerprintHash = event.videoFingerprint ?: "None",
                            lastConfidence = event.confidence
                        )
                    }
                    return event
                } else {
                    duplicateSuppressedCount++
                    diagnostics.recordEvent(event, isCommitted = false, statusLabel = "DUPLICATE SUPPRESSED")
                    _liveDiagnostics.update { it.copy(duplicateSuppressedCount = duplicateSuppressedCount) }
                }
            } else {
                diagnostics.recordEvent(event, isCommitted = false)
            }
        } else {
            diagnostics.recordEvent(event, isCommitted = false)
            _isInsideReelFeed.value = false
            _trackingStatus.value = TrackingStatus.UnsupportedScreen
        }

        return null
    }

    fun simulateReelEvent(platform: Platform) {
        val now = System.currentTimeMillis()
        val event = DetectionEvent(
            platform = platform,
            feedDetected = true,
            newVideoTransition = true,
            evidence = "Simulated ${platform.displayName} Reel",
            confidence = 1.0f,
            timestamp = now,
            eligibleForCounting = true,
            videoFingerprint = "sim_${platform.name.take(2)}_$now"
        )
        incrementLiveCount(platform, now)
        _liveDiagnostics.update {
            it.copy(
                lastPlatform = platform.displayName,
                lastState = "SIMULATED_REEL",
                lastFingerprintHash = event.videoFingerprint ?: "None",
                lastConfidence = 1.0f,
                lastEventTimestamp = now
            )
        }
        coordinatorScope.launch {
            try {
                ReelCountApp.instance.countingRepository.recordViewingEvent(
                    event,
                    sessionManager.currentSessionId
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetDiagnostics() {
        duplicateSuppressedCount = 0
        totalProcessedSnapshots = 0L
        _liveDiagnostics.value = LiveDiagnostics(isServiceRunning = ReelCountAccessibilityService.isRunning)
    }
}

data class LiveDiagnostics(
    val lastPackage: String = "None",
    val lastActivity: String = "None",
    val lastPlatform: String = "None",
    val lastState: String = "IDLE",
    val lastFingerprintHash: String = "None",
    val lastConfidence: Float = 0f,
    val duplicateSuppressedCount: Int = 0,
    val totalProcessedSnapshots: Long = 0,
    val isServiceRunning: Boolean = false,
    val lastEventTimestamp: Long = 0L
)
