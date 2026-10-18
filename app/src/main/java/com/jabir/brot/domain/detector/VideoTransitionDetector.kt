package com.jabir.brot.domain.detector

import com.jabir.brot.domain.model.DetectionEvent
import com.jabir.brot.domain.model.DetectionState
import com.jabir.brot.domain.model.Platform
import com.jabir.brot.domain.model.ScreenSnapshot

/**
 * High-level transition coordinator that orchestrates platform detectors.
 * Manages transition stabilization and cross-platform detection dispatch.
 */
class VideoTransitionDetector(
    private val instagramDetector: InstagramReelsDetector = InstagramReelsDetector(),
    private val facebookDetector: FacebookReelsDetector = FacebookReelsDetector(),
    private val youtubeDetector: YouTubeShortsDetector = YouTubeShortsDetector()
) {
    private val detectors: Map<Platform, PlatformDetector> = mapOf(
        Platform.INSTAGRAM to instagramDetector,
        Platform.FACEBOOK to facebookDetector,
        Platform.YOUTUBE to youtubeDetector
    )

    fun getDetector(platform: Platform): PlatformDetector =
        detectors[platform] ?: throw IllegalArgumentException("Unsupported platform: $platform")

    fun evaluate(snapshot: ScreenSnapshot): DetectionEvent? {
        val platform = Platform.fromPackageName(snapshot.packageName) ?: return null
        val detector = detectors[platform] ?: return null
        return detector.evaluate(snapshot)
    }

    fun getCurrentState(platform: Platform): DetectionState {
        return detectors[platform]?.getCurrentState() ?: DetectionState.OUTSIDE_SUPPORTED_FEED
    }

    fun resetAll() {
        detectors.values.forEach { it.reset() }
    }

    fun reset(platform: Platform) {
        detectors[platform]?.reset()
    }
}
