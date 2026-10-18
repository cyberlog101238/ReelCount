package com.jabir.brot.domain

import com.jabir.brot.domain.model.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Information regarding the currently active short-video viewing session.
 */
data class ActiveSession(
    val id: String = UUID.randomUUID().toString(),
    val platform: Platform,
    val startTime: Long = System.currentTimeMillis(),
    val videoCount: Int = 0
)

/**
 * Manages short-video viewing sessions.
 *
 * A session begins when the user enters a supported feed (Instagram Reels, FB Reels, YT Shorts)
 * and concludes when they navigate away, switch apps, or lock their phone.
 */
class SessionManager {
    private val _activeSession = MutableStateFlow<ActiveSession?>(null)
    val activeSession: StateFlow<ActiveSession?> = _activeSession.asStateFlow()

    /**
     * Starts a new viewing session or returns the active session if already in the same platform.
     */
    @Synchronized
    fun startOrGetSession(platform: Platform): ActiveSession {
        val current = _activeSession.value
        if (current != null && current.platform == platform) {
            return current
        }

        // Switching platforms or starting fresh
        if (current != null) {
            endSession()
        }

        val newSession = ActiveSession(platform = platform)
        _activeSession.value = newSession
        return newSession
    }

    /** Increments the count for the active session. */
    @Synchronized
    fun incrementVideoCount(): ActiveSession? {
        val current = _activeSession.value ?: return null
        val updated = current.copy(videoCount = current.videoCount + 1)
        _activeSession.value = updated
        return updated
    }

    /**
     * Ends the active session and returns it with the final statistics.
     */
    @Synchronized
    fun endSession(): ActiveSession? {
        val current = _activeSession.value ?: return null
        _activeSession.value = null
        return current
    }

    /** Returns the current active session ID or generates a transient fallback ID. */
    val currentSessionId: String
        get() = _activeSession.value?.id ?: UUID.randomUUID().toString()
}
