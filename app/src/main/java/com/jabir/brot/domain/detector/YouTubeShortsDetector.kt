package com.jabir.brot.domain.detector

import com.jabir.brot.domain.model.*

/**
 * Robust detector for YouTube Shorts (`com.google.android.youtube`).
 *
 * Distinguishes full-screen Shorts from:
 * - Standard 16:9 YouTube video player (`watch_player`)
 * - Home feed horizontal Shorts carousels / shelves
 * - Subscriptions / library screens
 *
 * Utilizes:
 * - High-confidence View IDs (`reel_watch_fragment_root`, `reel_recycler`, `reel_player`)
 * - Bottom nav Pivot bar "Shorts" selection
 * - Action buttons ("Like this video along with ...", "Dislike this video", "Remix")
 * - Channel handle (@name) and title extraction
 */
class YouTubeShortsDetector : PlatformDetector {
    override val platform: Platform = Platform.YOUTUBE

    private var currentState: DetectionState = DetectionState.OUTSIDE_SUPPORTED_FEED
    private var currentFingerprint: VideoFingerprint? = null
    private var lastConfirmedFingerprint: VideoFingerprint? = null
    private var candidateStableCount: Int = 0

    companion object {
        val SHORTS_VIEW_IDS = setOf(
            "reel_watch_fragment_root",
            "reel_watch_fragment",
            "reel_recycler",
            "reel_player",
            "reel_progress_bar",
            "reel_player_overlay"
        )
        private val YT_IGNORED = setOf(
            "subscribe", "subscribed", "comments", "dislike", "like", "remix", "share", "shorts"
        )
    }

    private var consecutiveNonFeedSnapshots: Int = 0

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

        val isInShorts = detectShortsFeed(snapshot)
        if (!isInShorts) {
            if (currentState != DetectionState.OUTSIDE_SUPPORTED_FEED) {
                consecutiveNonFeedSnapshots++
                val isExplicitExit = snapshot.activityName?.let {
                    !it.contains("Short", ignoreCase = true) &&
                    !it.contains("Reel", ignoreCase = true) &&
                    !it.contains("Watch", ignoreCase = true)
                } ?: false

                if (isExplicitExit || consecutiveNonFeedSnapshots >= 3) {
                    currentState = DetectionState.OUTSIDE_SUPPORTED_FEED
                    currentFingerprint = null
                    candidateStableCount = 0
                    consecutiveNonFeedSnapshots = 0
                    return null
                }
                return null
            }
            return null
        }
        consecutiveNonFeedSnapshots = 0

        val extractedFingerprint = extractFingerprint(snapshot)
        val evidenceBuilder = StringBuilder("YouTube Shorts detected. ")

        when (currentState) {
            DetectionState.OUTSIDE_SUPPORTED_FEED -> {
                currentState = if (extractedFingerprint != null) DetectionState.VIDEO_CANDIDATE else DetectionState.FEED_DETECTED
                currentFingerprint = extractedFingerprint
                candidateStableCount = 1
                return null
            }

            DetectionState.FEED_DETECTED,
            DetectionState.TRANSITION_PENDING -> {
                if (extractedFingerprint != null && extractedFingerprint != lastConfirmedFingerprint) {
                    currentFingerprint = extractedFingerprint
                    currentState = DetectionState.VIDEO_CANDIDATE
                    candidateStableCount = 1
                }
                return null
            }

            DetectionState.VIDEO_CANDIDATE -> {
                if (extractedFingerprint == null) {
                    currentState = DetectionState.TRACKING_UNCERTAIN
                    return DetectionEvent(
                        platform = platform,
                        feedDetected = true,
                        newVideoTransition = false,
                        evidence = "YouTube Shorts fingerprint lost during transition.",
                        confidence = 0.4f,
                        timestamp = snapshot.timestamp,
                        eligibleForCounting = false,
                        videoFingerprint = null
                    )
                }

                if (extractedFingerprint == currentFingerprint) {
                    candidateStableCount++
                    if (candidateStableCount >= 2) {
                        currentState = DetectionState.VIDEO_CONFIRMED
                        lastConfirmedFingerprint = extractedFingerprint

                        evidenceBuilder.append("Confirmed Short: Channel=${extractedFingerprint.creatorHandle ?: "Unknown"}, ")
                        evidenceBuilder.append("Title=${extractedFingerprint.caption?.take(30) ?: "None"}")

                        val event = DetectionEvent(
                            platform = platform,
                            feedDetected = true,
                            newVideoTransition = true,
                            evidence = evidenceBuilder.toString(),
                            confidence = calculateConfidence(snapshot, extractedFingerprint),
                            timestamp = snapshot.timestamp,
                            eligibleForCounting = true,
                            videoFingerprint = extractedFingerprint.computeHash()
                        )

                        currentState = DetectionState.TRANSITION_PENDING
                        return event
                    }
                } else {
                    currentFingerprint = extractedFingerprint
                    candidateStableCount = 1
                }
                return null
            }

            DetectionState.VIDEO_CONFIRMED -> {
                currentState = DetectionState.TRANSITION_PENDING
                return null
            }

            DetectionState.TRACKING_UNCERTAIN -> {
                if (extractedFingerprint != null) {
                    currentFingerprint = extractedFingerprint
                    currentState = DetectionState.VIDEO_CANDIDATE
                    candidateStableCount = 1
                }
                return null
            }
        }
    }

    private fun detectShortsFeed(snapshot: ScreenSnapshot): Boolean {
        // Exclude standard YouTube video player
        val hasStandardPlayerIds = snapshot.viewIds.any { id ->
            id.contains("watch_player", ignoreCase = true) ||
            id.contains("watch_list", ignoreCase = true)
        }
        if (hasStandardPlayerIds && !snapshot.viewIds.any { it.contains("reel_watch", ignoreCase = true) }) {
            return false
        }

        // Signal 1: High-confidence Shorts view IDs
        val hasShortsViewId = snapshot.viewIds.any { id ->
            SHORTS_VIEW_IDS.any { target -> id.contains(target, ignoreCase = true) }
        }

        // Signal 2: Content descriptions
        val hasShortsDesc = snapshot.contentDescriptions.any { desc ->
            desc.contains("Like this video along with", ignoreCase = true) ||
            desc.contains("Dislike this video", ignoreCase = true) ||
            desc.contains("Remix", ignoreCase = true)
        }

        // Signal 3: Pivot bar tab
        val isShortsTabSelected = snapshot.bottomNavSelectedTab?.contains("shorts", ignoreCase = true) == true

        val isFullScreen = snapshot.fullScreenContainerPresent

        if (!isFullScreen && !hasShortsViewId) {
            return false
        }

        return (hasShortsViewId && isFullScreen) ||
               (hasShortsDesc && isFullScreen) ||
               (isShortsTabSelected && isFullScreen && (hasShortsViewId || snapshot.verticalActionColumnPresent))
    }

    private fun extractFingerprint(snapshot: ScreenSnapshot): VideoFingerprint? {
        // Channel handle: starts with @
        var channel = snapshot.textContents.find { it.startsWith("@") }?.trim()

        // Fallback channel: near "Subscribe" button
        if (channel == null) {
            val subscribeIdx = snapshot.textContents.indexOfFirst {
                it.equals("Subscribe", ignoreCase = true) || it.equals("Subscribed", ignoreCase = true)
            }
            if (subscribeIdx > 0) {
                channel = snapshot.textContents[subscribeIdx - 1]
            }
        }

        // Title: look for text containing #shorts or longest descriptive title
        var title = snapshot.textContents.find { it.contains("#shorts", ignoreCase = true) }
        if (title == null) {
            val candidateTitles = snapshot.textContents.filter { text ->
                text.length > 5 &&
                text != channel &&
                !YT_IGNORED.any { text.contains(it, ignoreCase = true) } &&
                !text.all { it.isDigit() || it == 'K' || it == 'M' || it == '.' || it == ',' }
            }
            title = candidateTitles.maxByOrNull { it.length }
        }

        var compositeSig: String? = null
        if (channel == null && title == null) {
            val distinctTexts = snapshot.textContents
                .filter { text -> text.length > 2 && !YT_IGNORED.contains(text.lowercase()) }
                .take(6)
            if (distinctTexts.isNotEmpty()) {
                compositeSig = distinctTexts.joinToString(";")
            } else {
                val descs = snapshot.contentDescriptions
                    .filter { it.isNotBlank() && !YT_IGNORED.contains(it.lowercase()) }
                    .take(5)
                if (descs.isNotEmpty()) {
                    compositeSig = descs.joinToString(";")
                }
            }
        }

        if (channel == null && title == null && compositeSig == null) return null

        return VideoFingerprint(
            creatorHandle = channel,
            caption = title?.take(100),
            audioTrack = null,
            compositeSignature = compositeSig
        )
    }

    private fun calculateConfidence(snapshot: ScreenSnapshot, fp: VideoFingerprint?): Float {
        var score = 0.5f
        if (snapshot.viewIds.any { id -> SHORTS_VIEW_IDS.any { id.contains(it, ignoreCase = true) } }) score += 0.25f
        if (snapshot.fullScreenContainerPresent) score += 0.15f
        if (snapshot.verticalActionColumnPresent) score += 0.1f
        if (fp?.creatorHandle != null) score += 0.05f
        return score.coerceIn(0.0f, 1.0f)
    }
}
