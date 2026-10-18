package com.jabir.brot.domain.model

import java.util.UUID

/**
 * An immutable event emitted by a [com.jabir.brot.domain.detector.PlatformDetector]
 * each time screen analysis produces a meaningful detection result.
 *
 * Only events with [eligibleForCounting] == `true` AND
 * [newVideoTransition] == `true` should increment the user's video count.
 */
data class DetectionEvent(
    /** Unique identifier for this event instance. */
    val id: String = UUID.randomUUID().toString(),

    /** The platform this event was detected on. */
    val platform: Platform,

    /** Whether a supported short-form video feed is currently detected. */
    val feedDetected: Boolean,

    /** Whether this event represents a transition to a new, distinct video. */
    val newVideoTransition: Boolean,

    /** Human-readable description of the evidence that produced this event. */
    val evidence: String,

    /**
     * Detection confidence score in the range [0.0, 1.0].
     * Higher values indicate stronger signal agreement.
     */
    val confidence: Float,

    /** Epoch-millis timestamp when this event was generated. */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * `true` when all conditions are met for this event to be counted:
     * feed detected, new video confirmed, confidence above threshold.
     */
    val eligibleForCounting: Boolean,

    /**
     * SHA-256 fingerprint hash of the video (from [VideoFingerprint.computeHash]),
     * or `null` when the fingerprint could not be computed.
     */
    val videoFingerprint: String?
) {
    /** Categorical confidence level for display purposes. */
    val confidenceLevel: ConfidenceLevel
        get() = ConfidenceLevel.fromScore(confidence)
}
