package com.jabir.brot.domain.model

/**
 * Represents the overall tracking engine status as a sealed hierarchy.
 *
 * UI layers observe this to display appropriate status indicators and
 * guide the user toward resolution when tracking is unavailable.
 */
sealed class TrackingStatus {

    /** Tracking is active and monitoring a supported platform feed. */
    data class Active(val platform: Platform) : TrackingStatus()

    /** A required permission has not been granted. */
    data class PermissionMissing(val permission: String) : TrackingStatus()

    /** The current screen is not a supported short-form video feed. */
    data object UnsupportedScreen : TrackingStatus()

    /** Detection signals are ambiguous; counting accuracy may be degraded. */
    data object DetectionUncertain : TrackingStatus()

    /** The user has manually paused tracking. */
    data object TrackingPaused : TrackingStatus()

    /** The accessibility service is not running. */
    data object ServiceNotRunning : TrackingStatus()

    /** A human-readable summary for diagnostics and UI display. */
    val displayMessage: String
        get() = when (this) {
            is Active -> "Tracking ${platform.displayName}"
            is PermissionMissing -> "Missing permission: $permission"
            is UnsupportedScreen -> "Not in a supported video feed"
            is DetectionUncertain -> "Detection uncertain"
            is TrackingPaused -> "Tracking paused"
            is ServiceNotRunning -> "Service not running"
        }
}
