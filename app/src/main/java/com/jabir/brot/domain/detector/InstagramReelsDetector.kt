package com.jabir.brot.domain.detector

import com.jabir.brot.domain.model.*

/**
 * Robust detector for Instagram Reels (`com.instagram.android`).
 *
 * Implements a deterministic multi-signal heuristic:
 * - High-confidence View IDs (`clips_viewer`, `clips_video_container`, etc.)
 * - Content descriptions ("Reel by ...", "Profile picture of ...", "Remix this reel")
 * - Layout structure (full-screen container + vertical right action column)
 * - Bottom navigation tab selection
 *
 * Deduplication & State Machine:
 * Transitions through:
 * OUTSIDE_SUPPORTED_FEED -> FEED_DETECTED -> VIDEO_CANDIDATE -> VIDEO_CONFIRMED -> TRANSITION_PENDING
 */
class InstagramReelsDetector : PlatformDetector {
    override val platform: Platform = Platform.INSTAGRAM

    private var currentState: DetectionState = DetectionState.OUTSIDE_SUPPORTED_FEED
    private var currentFingerprint: VideoFingerprint? = null
    private var lastConfirmedFingerprint: VideoFingerprint? = null
    private var candidateStableCount: Int = 0

    companion object {
        val REELS_VIEW_IDS = setOf(
            "clips_viewer",
            "clips_video_container",
            "clips_swipe_refresh_container",
            "clips_media_item",
            "clips_view_pager",
            "reel_viewer_root",
            "clips_author_name",
            "clips_caption",
            "clips_like_button",
            "clips_comment_button",
            "clips_share_button",
            "like_button",
            "comment_button",
            "share_button"
        )

        private val IGNORED_WORDS = setOf(
            "like", "comment", "share", "remix", "follow", "following",
            "sponsored", "suggested for you", "send message", "original audio", "audio", "reel"
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

        val isInReels = detectReelsFeed(snapshot)
        if (!isInReels) {
            if (currentState != DetectionState.OUTSIDE_SUPPORTED_FEED) {
                consecutiveNonFeedSnapshots++
                val isExplicitExit = snapshot.activityName?.let {
                    !it.contains("Reel", ignoreCase = true) &&
                    !it.contains("Main", ignoreCase = true)
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

        // We are inside Instagram Reels
        val extractedFingerprint = extractFingerprint(snapshot)
        val evidenceBuilder = StringBuilder("Instagram Reels detected. ")

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
                        evidence = "Fingerprint lost during candidate stabilization.",
                        confidence = 0.4f,
                        timestamp = snapshot.timestamp,
                        eligibleForCounting = false,
                        videoFingerprint = null
                    )
                }

                if (extractedFingerprint == currentFingerprint) {
                    candidateStableCount++
                    // Require at least 2 consecutive settled snapshots
                    if (candidateStableCount >= 2) {
                        currentState = DetectionState.VIDEO_CONFIRMED
                        lastConfirmedFingerprint = extractedFingerprint

                        evidenceBuilder.append("New video confirmed: Creator=${extractedFingerprint.creatorHandle ?: "Unknown"}, ")
                        evidenceBuilder.append("Caption=${extractedFingerprint.caption?.take(30) ?: "None"}.")

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

                        // Immediately transition to TRANSITION_PENDING to prevent counting same video again
                        currentState = DetectionState.TRANSITION_PENDING
                        return event
                    }
                } else {
                    // Fingerprint changed rapidly before settling (fast swipe)
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

    private fun detectReelsFeed(snapshot: ScreenSnapshot): Boolean {
        // Signal 1: Explicit matching view IDs
        val hasReelsViewId = snapshot.viewIds.any { id ->
            REELS_VIEW_IDS.any { target -> id.contains(target, ignoreCase = true) }
        }

        // Signal 2: Content descriptions
        val hasReelsDesc = snapshot.contentDescriptions.any { desc ->
            desc.contains("Reel by", ignoreCase = true) ||
            desc.contains("Like reel", ignoreCase = true) ||
            desc.contains("Remix this reel", ignoreCase = true)
        }

        // Signal 3: Bottom tab selection
        val isReelsTabSelected = snapshot.bottomNavSelectedTab?.contains("reels", ignoreCase = true) == true

        // Signal 4: Layout geometry: must have full screen container
        val isFullScreen = snapshot.fullScreenContainerPresent

        // Feed shelves/carousels in home feed should NOT count as full-screen reels
        if (!isFullScreen && !hasReelsViewId) {
            return false
        }

        return (hasReelsViewId && isFullScreen) ||
               (hasReelsDesc && isFullScreen) ||
               (isReelsTabSelected && isFullScreen && (hasReelsViewId || snapshot.verticalActionColumnPresent))
    }

    private fun extractFingerprint(snapshot: ScreenSnapshot): VideoFingerprint? {
        var creator: String? = null

        // Signal A: Content descriptions: "Reel by <username>"
        for (desc in snapshot.contentDescriptions) {
            val match = Regex("""Reel by\s+([^,.\n]+)""", RegexOption.IGNORE_CASE).find(desc)
            if (match != null) {
                creator = match.groupValues[1].trim()
                break
            }
        }

        // Signal B: Profile picture / user profile content descriptions
        if (creator == null) {
            for (desc in snapshot.contentDescriptions) {
                val match = Regex("""Profile picture of\s+([^,.\n]+)""", RegexOption.IGNORE_CASE).find(desc)
                    ?: Regex("""([a-zA-Z0-9._]+)'s profile""", RegexOption.IGNORE_CASE).find(desc)
                if (match != null) {
                    creator = match.groupValues[1].trim()
                    break
                }
            }
        }

        // Signal C: Text starting with @
        if (creator == null) {
            val handleText = snapshot.textContents.find { it.startsWith("@") }
            if (handleText != null) {
                creator = handleText.removePrefix("@").trim()
            }
        }

        // Signal D: Short username token near "Follow" or "•"
        if (creator == null) {
            val candidateHandle = snapshot.textContents.find { text ->
                text.length in 3..25 &&
                Regex("""^[a-zA-Z0-9._]+$""").matches(text) &&
                !IGNORED_WORDS.contains(text.lowercase())
            }
            if (candidateHandle != null) {
                creator = candidateHandle
            }
        }

        // Signal E: Audio track
        val audio: String? = snapshot.textContents.find {
            it.contains("Original audio", ignoreCase = true) ||
            it.contains("Audio", ignoreCase = true)
        } ?: snapshot.contentDescriptions.find {
            it.contains("Audio", ignoreCase = true)
        }

        // Signal F: Caption text (longest non-button, non-counter text)
        val candidateCaptions = snapshot.textContents.filter { text ->
            text.length > 5 &&
            text != creator &&
            text != "@$creator" &&
            text != audio &&
            !IGNORED_WORDS.any { it.equals(text, ignoreCase = true) } &&
            !text.all { it.isDigit() || it == 'k' || it == 'm' || it == ',' || it == '.' }
        }
        val caption: String? = candidateCaptions.maxByOrNull { it.length }

        // Fallback: If both structured creator and caption are absent, construct composite signature
        var compositeSig: String? = null
        if (creator == null && caption == null) {
            val distinctTexts = snapshot.textContents
                .filter { text ->
                    text.length > 2 &&
                    !IGNORED_WORDS.contains(text.lowercase()) &&
                    !text.all { it.isDigit() || it == 'k' || it == 'm' }
                }
                .take(6)
            if (distinctTexts.isNotEmpty()) {
                compositeSig = distinctTexts.joinToString(";")
            } else {
                val descs = snapshot.contentDescriptions
                    .filter { it.isNotBlank() && !IGNORED_WORDS.contains(it.lowercase()) }
                    .take(5)
                if (descs.isNotEmpty()) {
                    compositeSig = descs.joinToString(";")
                }
            }
        }

        if (creator == null && caption == null && compositeSig == null) {
            return null
        }

        return VideoFingerprint(
            creatorHandle = creator,
            caption = caption?.take(100),
            audioTrack = audio?.take(50),
            compositeSignature = compositeSig
        )
    }

    private fun calculateConfidence(snapshot: ScreenSnapshot, fp: VideoFingerprint?): Float {
        var score = 0.5f
        if (snapshot.viewIds.any { id -> REELS_VIEW_IDS.any { id.contains(it, ignoreCase = true) } }) score += 0.2f
        if (snapshot.fullScreenContainerPresent) score += 0.15f
        if (snapshot.verticalActionColumnPresent) score += 0.1f
        if (fp?.creatorHandle != null) score += 0.05f
        return score.coerceIn(0.0f, 1.0f)
    }
}
