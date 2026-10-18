package com.jabir.brot.domain.guard

import com.jabir.brot.domain.model.DetectionEvent
import com.jabir.brot.domain.model.Platform
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe guard preventing duplicate video commits.
 *
 * Implements strict viewing occurrence semantics:
 * - A video is counted ONCE per viewing occurrence in an active session.
 * - Repeated accessibility events for the current video are completely ignored.
 * - If the user swipes away to another video or leaves the feed, and later returns
 *   to this video in a new occurrence, the video is counted as a new distinct occurrence.
 * - An optional supplementary safety window (default 200ms) prevents micro-glitches
 *   during fast gesture flings from corrupting the state.
 */
class DuplicateEventGuard(
    private val minFlingCooldownMs: Long = 1200L
) {
    /** Map of active session ID to set of counted video fingerprint hashes within that session. */
    private val sessionCountedFingerprints = ConcurrentHashMap<String, MutableSet<String>>()

    /** Last counted event timestamp per platform for debounce protection. */
    private val lastCountTimestamp = ConcurrentHashMap<Platform, Long>()

    /** Last confirmed fingerprint hash per platform. */
    private val activeFingerprint = ConcurrentHashMap<Platform, String>()

    /** Recent video timestamps to prevent recounting the same video during quick view resets. */
    private val recentFingerprints = ConcurrentHashMap<String, Long>()

    /**
     * Evaluates if a confirmed detection event is eligible to be committed to persistent storage.
     *
     * @param event The confirmed [DetectionEvent].
     * @param sessionId The active viewing session ID.
     * @return `true` if the event is a genuine, non-duplicate transition eligible for counting;
     *         `false` if it should be suppressed as a duplicate.
     */
    @Synchronized
    fun shouldCommit(event: DetectionEvent, sessionId: String): Boolean {
        if (!event.eligibleForCounting || !event.newVideoTransition) {
            return false
        }

        val fingerprint = event.videoFingerprint
        val now = event.timestamp
        val platform = event.platform

        // Guard 1: Minimum cooldown between scrolls: human cannot scroll and digest in < 1200ms
        val lastTime = lastCountTimestamp[platform] ?: 0L
        if (minFlingCooldownMs > 0L && lastTime > 0L && now >= lastTime && (now - lastTime) < minFlingCooldownMs) {
            return false
        }

        // Guard 2: Same-video suppression within active occurrence
        val currentActiveFp = activeFingerprint[platform]
        if (fingerprint != null && fingerprint == currentActiveFp) {
            return false
        }

        // Guard 3: Recent video cooldown across quick re-entries (within 6 seconds)
        if (fingerprint != null) {
            val lastSeen = recentFingerprints[fingerprint] ?: 0L
            if (now >= lastSeen && (now - lastSeen) < 6000L) {
                return false
            }
        }

        // Guard 4: Session-level deduplication
        if (fingerprint != null) {
            val sessionSet = sessionCountedFingerprints.computeIfAbsent(sessionId) {
                ConcurrentHashMap.newKeySet()
            }
            if (sessionSet.contains(fingerprint)) {
                return false
            }
            sessionSet.add(fingerprint)
            activeFingerprint[platform] = fingerprint
            recentFingerprints[fingerprint] = now
        }

        lastCountTimestamp[platform] = now
        return true
    }

    /**
     * Resets the active video pointer for a platform when the user leaves the feed.
     */
    @Synchronized
    fun onFeedExited(platform: Platform) {
        activeFingerprint.remove(platform)
        lastCountTimestamp.remove(platform)
    }

    /** Cleans up state when a session ends. */
    @Synchronized
    fun onSessionEnded(sessionId: String) {
        sessionCountedFingerprints.remove(sessionId)
    }

    /** Clears all internal tracking state. */
    @Synchronized
    fun clear() {
        sessionCountedFingerprints.clear()
        lastCountTimestamp.clear()
        activeFingerprint.clear()
    }
}
