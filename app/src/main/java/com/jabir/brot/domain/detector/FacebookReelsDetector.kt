package com.jabir.brot.domain.detector

import com.jabir.brot.domain.model.*

/**
 * Robust detector for Facebook Reels (`com.facebook.katana`).
 *
 * Engineered specifically for Meta's complex Litho framework:
 * - Employs exit hysteresis to prevent overlay flickering during comment/reaction updates.
 * - Extracts invariant fingerprints (filtering out view counts, timestamps, and comments).
 * - Enforces candidate stabilization (candidateStableCount >= 2) to eliminate auto-counting.
 * - Supports fallback composite signatures to ensure every reel is uniquely identified.
 */
class FacebookReelsDetector : PlatformDetector {
    override val platform: Platform = Platform.FACEBOOK

    private var currentState: DetectionState = DetectionState.OUTSIDE_SUPPORTED_FEED
    private var currentFingerprint: VideoFingerprint? = null
    private var lastConfirmedFingerprint: VideoFingerprint? = null
    private var candidateStableCount: Int = 0
    private var consecutiveNonFeedSnapshots: Int = 0

    companion object {
        val FB_REELS_KEYWORDS = setOf(
            "Reel by",
            "Reel from",
            "Like reel",
            "Comment on reel",
            "Share reel",
            "Browse more reels",
            "Original audio",
            "Remix"
        )

        // Exact match action words to filter out UI button labels without rejecting captions
        private val FB_ACTION_WORDS = setOf(
            "like", "comment", "share", "send", "follow", "following", "remix", "reels", "reel",
            "original audio", "browse more reels", "reactions", "suggested for you", "sponsored",
            "more options", "mute", "unmute", "pause", "play", "facebook", "search", "more",
            "audio", "double tap to play or pause video", "double tap to play or pause"
        )
    }

    override fun getCurrentState(): DetectionState = currentState

    override fun reset() {
        currentState = DetectionState.OUTSIDE_SUPPORTED_FEED
        currentFingerprint = null
        lastConfirmedFingerprint = null
        candidateStableCount = 0
        consecutiveNonFeedSnapshots = 0
    }

    override fun evaluate(snapshot: ScreenSnapshot): DetectionEvent? {
        if (snapshot.packageName != platform.packageName) {
            reset()
            return null
        }

        val isInReelsFeedNow = detectReelsFeed(snapshot)

        // Hysteresis: prevent flickering when a partial accessibility event momentarily misses container cues
        if (!isInReelsFeedNow) {
            if (currentState != DetectionState.OUTSIDE_SUPPORTED_FEED) {
                consecutiveNonFeedSnapshots++
                // If user changed to a clearly non-reels activity (e.g. settings, login) or 3 consecutive snapshots fail
                val isExplicitExit = snapshot.activityName?.let {
                    !it.contains("FbShorts", ignoreCase = true) &&
                    !it.contains("Reel", ignoreCase = true) &&
                    !it.contains("FbMainTabActivity", ignoreCase = true) &&
                    !it.contains("FbChromeActivity", ignoreCase = true)
                } ?: false

                if (isExplicitExit || consecutiveNonFeedSnapshots >= 3) {
                    currentState = DetectionState.OUTSIDE_SUPPORTED_FEED
                    currentFingerprint = null
                    candidateStableCount = 0
                    consecutiveNonFeedSnapshots = 0
                    return null
                }
                // Within hysteresis window: treat as still in feed, but do not process transitions on uncertain snapshot
                return null
            } else {
                return null
            }
        }

        // We are confirmed inside Facebook Reels
        consecutiveNonFeedSnapshots = 0
        val extractedFingerprint = extractFingerprint(snapshot)

        when (currentState) {
            DetectionState.OUTSIDE_SUPPORTED_FEED -> {
                currentState = if (extractedFingerprint != null) {
                    currentFingerprint = extractedFingerprint
                    candidateStableCount = 1
                    DetectionState.VIDEO_CANDIDATE
                } else {
                    DetectionState.FEED_DETECTED
                }
                return null
            }

            DetectionState.FEED_DETECTED,
            DetectionState.TRANSITION_PENDING,
            DetectionState.VIDEO_CONFIRMED -> {
                if (extractedFingerprint != null && extractedFingerprint != lastConfirmedFingerprint) {
                    currentFingerprint = extractedFingerprint
                    candidateStableCount = 1
                    currentState = DetectionState.VIDEO_CANDIDATE
                }
                return null
            }

            DetectionState.VIDEO_CANDIDATE -> {
                if (extractedFingerprint == null) {
                    // Temporarily unreadable during scroll animation
                    return null
                }

                if (extractedFingerprint == currentFingerprint) {
                    candidateStableCount++
                    if (candidateStableCount >= 2) {
                        lastConfirmedFingerprint = currentFingerprint
                        currentState = DetectionState.TRANSITION_PENDING

                        val evidence = "Facebook Reel confirmed: Author=${extractedFingerprint.creatorHandle ?: "Unknown"}"
                        return DetectionEvent(
                            platform = platform,
                            feedDetected = true,
                            newVideoTransition = true,
                            evidence = evidence,
                            confidence = calculateConfidence(snapshot, extractedFingerprint),
                            timestamp = snapshot.timestamp,
                            eligibleForCounting = true,
                            videoFingerprint = extractedFingerprint.computeHash()
                        )
                    }
                } else {
                    // Fingerprint changed before settling; update candidate
                    currentFingerprint = extractedFingerprint
                    candidateStableCount = 1
                }
                return null
            }

            DetectionState.TRACKING_UNCERTAIN -> {
                if (extractedFingerprint != null) {
                    currentFingerprint = extractedFingerprint
                    candidateStableCount = 1
                    currentState = DetectionState.VIDEO_CANDIDATE
                }
                return null
            }
        }
    }

    private fun detectReelsFeed(snapshot: ScreenSnapshot): Boolean {
        // High confidence signal 1: Dedicated Reels Viewer activity
        val isReelsActivity = snapshot.activityName?.let {
            it.contains("FbShorts", ignoreCase = true) ||
            it.contains("Reel", ignoreCase = true) ||
            it.contains("ShortsViewer", ignoreCase = true)
        } == true
        if (isReelsActivity) {
            return true
        }

        // High confidence signal 2: Content descriptions specific to Reels
        val hasReelsKeywordsInDesc = snapshot.contentDescriptions.any { desc ->
            FB_REELS_KEYWORDS.any { kw -> desc.contains(kw, ignoreCase = true) }
        }
        if (hasReelsKeywordsInDesc) {
            return true
        }

        // Signal 3: Text content indicating Reels
        val hasReelsKeywordsInText = snapshot.textContents.any { text ->
            text.equals("Reels", ignoreCase = true) ||
            text.contains("Browse more reels", ignoreCase = true) ||
            text.contains("Original audio", ignoreCase = true) ||
            text.contains("Remix this reel", ignoreCase = true) ||
            text.contains("Like reel", ignoreCase = true)
        }
        if (hasReelsKeywordsInText) {
            return true
        }

        // Signal 4: View IDs containing reels keywords
        val hasReelsViewId = snapshot.viewIds.any { id ->
            id.contains("fb_shorts", ignoreCase = true) ||
            id.contains("reel", ignoreCase = true) ||
            id.contains("rich_video", ignoreCase = true)
        }
        if (hasReelsViewId) {
            return true
        }

        // Signal 5: Vertical actions column (Like/Comment/Share stacked vertically)
        if (snapshot.verticalActionColumnPresent && (snapshot.fullScreenContainerPresent || snapshot.textContents.any { it.contains("reel", ignoreCase = true) })) {
            return true
        }

        return false
    }

    /**
     * Extracts an invariant video fingerprint.
     * Crucially excludes comments, like numbers, timestamps, and reaction animations.
     */
    private fun extractFingerprint(snapshot: ScreenSnapshot): VideoFingerprint? {
        val metricRegex = Regex("""^[\d.,\s]+[KkMmBbTt]?$""")

        var creator: String? = null

        // Pattern 1: Content descriptions like "Reel by {creator}" or "Profile picture of {creator}"
        for (desc in snapshot.contentDescriptions) {
            val trimmedDesc = desc.trim()
            val match = Regex("""Reel (?:by|from)\s+([^,.\n]+)""", RegexOption.IGNORE_CASE).find(trimmedDesc)
                ?: Regex("""Profile picture of\s+([^,.\n]+)""", RegexOption.IGNORE_CASE).find(trimmedDesc)
                ?: Regex("""([^,.\n]+)'s profile picture""", RegexOption.IGNORE_CASE).find(trimmedDesc)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty() && !candidate.equals("Facebook", ignoreCase = true)) {
                    creator = candidate
                    break
                }
            }
        }

        // Pattern 2: Text matching author profile (excluding metric numbers and exact UI button words)
        if (creator == null) {
            creator = snapshot.textContents.map { it.trim() }.find { text ->
                text.length in 2..40 &&
                !text.matches(metricRegex) &&
                !FB_ACTION_WORDS.any { text.equals(it, ignoreCase = true) } &&
                !text.contains("views", ignoreCase = true) &&
                !text.contains("replies", ignoreCase = true) &&
                !text.contains("comments", ignoreCase = true) &&
                !text.startsWith("http", ignoreCase = true)
            }
        }

        // Extract stable caption: longest text between 8 and 250 chars, excluding numbers and UI words
        val candidateCaptions = snapshot.textContents.map { it.trim() }.filter { text ->
            text.length in 8..250 &&
            text != creator &&
            !text.matches(metricRegex) &&
            !FB_ACTION_WORDS.any { text.equals(it, ignoreCase = true) } &&
            !text.startsWith("http", ignoreCase = true)
        }
        val caption = candidateCaptions.maxByOrNull { it.length }

        // Audio track if present
        var audioTrack: String? = null
        for (text in snapshot.textContents) {
            val trimmed = text.trim()
            if (trimmed.contains("Original audio", ignoreCase = true) || trimmed.startsWith("Audio", ignoreCase = true)) {
                audioTrack = trimmed.take(60)
                break
            }
        }

        // Fallback composite signature if structured fields are sparse
        var compositeSignature: String? = null
        if (creator == null && caption == null && audioTrack == null) {
            val nonMetricTexts = snapshot.textContents
                .map { it.trim() }
                .filter { it.length >= 2 && !it.matches(metricRegex) && !FB_ACTION_WORDS.any { w -> it.equals(w, ignoreCase = true) } }
                .take(4)

            val meaningfulDescs = snapshot.contentDescriptions
                .map { it.trim() }
                .filter { it.length >= 3 && !FB_ACTION_WORDS.any { w -> it.equals(w, ignoreCase = true) } }
                .take(3)

            if (nonMetricTexts.isNotEmpty() || meaningfulDescs.isNotEmpty()) {
                compositeSignature = (nonMetricTexts + meaningfulDescs).joinToString("||").take(120)
            } else {
                return null
            }
        }

        return VideoFingerprint(
            creatorHandle = creator,
            caption = caption?.take(80),
            audioTrack = audioTrack,
            compositeSignature = compositeSignature
        )
    }

    private fun calculateConfidence(snapshot: ScreenSnapshot, fp: VideoFingerprint?): Float {
        var score = 0.65f
        if (snapshot.activityName?.contains("FbShorts", ignoreCase = true) == true) score += 0.20f
        if (snapshot.fullScreenContainerPresent) score += 0.1f
        if (fp?.creatorHandle != null) score += 0.05f
        return score.coerceIn(0.0f, 1.0f)
    }
}
