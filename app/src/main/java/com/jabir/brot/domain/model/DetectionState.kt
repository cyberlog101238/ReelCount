package com.jabir.brot.domain.model

/**
 * Represents the current state in the video detection state machine.
 *
 * State transitions:
 * ```
 * OUTSIDE_SUPPORTED_FEED → FEED_DETECTED → VIDEO_CANDIDATE → VIDEO_CONFIRMED
 *       ↑                                                          ↓
 *       ←──────── TRACKING_UNCERTAIN ←──── TRANSITION_PENDING ←────┘
 * ```
 */
enum class DetectionState {
    /** The user is not in a supported short-form video feed. */
    OUTSIDE_SUPPORTED_FEED,

    /** A supported video feed has been detected (e.g., Instagram Reels tab). */
    FEED_DETECTED,

    /** A potential new video has been identified but not yet confirmed. */
    VIDEO_CANDIDATE,

    /** The video has been confirmed as a new, distinct video eligible for counting. */
    VIDEO_CONFIRMED,

    /** A video was just counted; waiting for the next transition/swipe. */
    TRANSITION_PENDING,

    /** Detection signals are ambiguous or conflicting; tracking accuracy uncertain. */
    TRACKING_UNCERTAIN
}
