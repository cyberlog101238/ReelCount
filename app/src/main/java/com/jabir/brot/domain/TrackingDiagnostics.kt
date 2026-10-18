package com.jabir.brot.domain

import com.jabir.brot.domain.model.DetectionEvent
import com.jabir.brot.domain.model.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Diagnostic log entry for individual screen evaluations.
 */
data class LoggedEvent(
    val event: DetectionEvent,
    val statusText: String, // "COMMITTED", "DUPLICATE SUPPRESSED", "EVALUATING", "UNCERTAIN"
    val isCommitted: Boolean
)

/**
 * Diagnostic summary for display in the developer/QA diagnostics dashboard.
 */
data class DiagnosticsSummary(
    val totalEventsReceived: Long = 0,
    val confirmedCount: Long = 0,
    val uncertainCount: Long = 0,
    val duplicateSuppressedCount: Long = 0,
    val recentEvents: List<LoggedEvent> = emptyList(),
    val lastActivePlatform: Platform? = null,
    val lastDetectionTimestamp: Long? = null
)

/**
 * Diagnostics logger and statistics tracker for the tracking engine.
 */
class TrackingDiagnostics(private val maxRecentEvents: Int = 50) {
    private var totalEvents: Long = 0
    private var confirmed: Long = 0
    private var uncertain: Long = 0
    private var duplicatesSuppressed: Long = 0
    private var lastPlatform: Platform? = null
    private var lastTimestamp: Long? = null

    private val recentEventsQueue = ConcurrentLinkedQueue<LoggedEvent>()

    private val _summary = MutableStateFlow(DiagnosticsSummary())
    val summary: StateFlow<DiagnosticsSummary> = _summary.asStateFlow()

    @Synchronized
    fun recordEvent(event: DetectionEvent, isCommitted: Boolean = false, statusLabel: String? = null) {
        totalEvents++
        lastPlatform = event.platform
        lastTimestamp = event.timestamp

        val status = statusLabel ?: when {
            isCommitted -> {
                confirmed++
                "COMMITTED"
            }
            event.eligibleForCounting -> {
                // If marked eligible but not committed, it was suppressed by duplicate guards
                duplicatesSuppressed++
                "DUPLICATE SUPPRESSED"
            }
            !event.feedDetected && event.confidence < 0.5f -> {
                uncertain++
                "UNCERTAIN"
            }
            else -> "EVALUATING"
        }

        recentEventsQueue.add(LoggedEvent(event, status, isCommitted))
        while (recentEventsQueue.size > maxRecentEvents) {
            recentEventsQueue.poll()
        }

        updateSummary()
    }

    @Synchronized
    fun recordDuplicateSuppressed(event: DetectionEvent? = null) {
        duplicatesSuppressed++
        if (event != null) {
            totalEvents++
            lastPlatform = event.platform
            lastTimestamp = event.timestamp
            recentEventsQueue.add(LoggedEvent(event, "DUPLICATE SUPPRESSED", false))
            while (recentEventsQueue.size > maxRecentEvents) {
                recentEventsQueue.poll()
            }
        }
        updateSummary()
    }

    @Synchronized
    fun clear() {
        totalEvents = 0
        confirmed = 0
        uncertain = 0
        duplicatesSuppressed = 0
        recentEventsQueue.clear()
        updateSummary()
    }

    private fun updateSummary() {
        _summary.value = DiagnosticsSummary(
            totalEventsReceived = totalEvents,
            confirmedCount = confirmed,
            uncertainCount = uncertain,
            duplicateSuppressedCount = duplicatesSuppressed,
            recentEvents = recentEventsQueue.toList().reversed(),
            lastActivePlatform = lastPlatform,
            lastDetectionTimestamp = lastTimestamp
        )
    }
}
