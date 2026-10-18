package com.jabir.brot.domain.detector

import com.jabir.brot.domain.model.DetectionEvent
import com.jabir.brot.domain.model.DetectionState
import com.jabir.brot.domain.model.Platform
import com.jabir.brot.domain.model.ScreenSnapshot

/**
 * Interface contract for platform-specific short-form video feed detectors.
 *
 * Implementations evaluate abstracted [ScreenSnapshot] representations of the
 * active window to detect feed presence, video changes, and emit structured
 * [DetectionEvent]s without binding directly to Android framework APIs.
 */
interface PlatformDetector {
    /** The target platform this detector is specialized for. */
    val platform: Platform

    /**
     * Evaluates a screen snapshot and updates internal state machine.
     *
     * @param snapshot The normalized snapshot of the active screen.
     * @return A [DetectionEvent] if a state transition or confirmed video occurs,
     *         or `null` if no actionable event occurred.
     */
    fun evaluate(snapshot: ScreenSnapshot): DetectionEvent?

    /** Returns the current state of the platform state machine. */
    fun getCurrentState(): DetectionState

    /** Resets the detector state back to [DetectionState.OUTSIDE_SUPPORTED_FEED]. */
    fun reset()
}
