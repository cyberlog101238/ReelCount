package com.jabir.brot.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.jabir.brot.ReelCountApp
import com.jabir.brot.domain.model.Platform
import com.jabir.brot.domain.model.ScreenSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Core AccessibilityService monitoring short video feeds on Instagram, Facebook, and YouTube.
 *
 * Designed with strict performance, stability, and privacy engineering:
 * - Direct FloatingOverlayController management: NO FOREGROUND SERVICE or persistent status bar notification!
 * - Event filtering: only semantic content changes, window state, and scroll events
 * - Coroutine debouncing (120ms) to allow UI hierarchy to settle
 * - Two-step stabilization verification for video candidate transitions
 * - Mandatory node recycling to prevent Binder buffer exhaustion
 * - Deep node inspection (max 120 nodes) to reach Litho containers, author tags, and descriptions
 * - Strict privacy: NO passwords, form data, or private chat messages inspected
 */
class ReelCountAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var debounceJob: Job? = null
    private var floatingOverlayController: FloatingOverlayController? = null

    companion object {
        val MONITORED_PACKAGES = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.youtube"
        )
        var isRunning: Boolean = false
            private set
    }

    private var currentActivityName: String? = null
    private var lastContentEvaluationTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        ReelCountApp.instance.trackingCoordinator.onServiceConnected()

        // Safely cancel and stop any legacy foreground service notification
        try {
            stopService(Intent(this, FloatingCounterService::class.java))
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(FloatingCounterService.NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }

        // Initialize direct overlay controller (runs completely without persistent notification)
        floatingOverlayController = FloatingOverlayController(this)

        val coordinator = ReelCountApp.instance.trackingCoordinator
        val prefs = ReelCountApp.instance.userPreferencesRepository

        // Observe feed presence and user overlay preference
        serviceScope.launch {
            combine(
                prefs.overlayEnabled,
                coordinator.isInsideReelFeed
            ) { isEnabled, isInsideFeed ->
                isEnabled to isInsideFeed
            }.collect { (isEnabled, isInsideFeed) ->
                if (isEnabled && isInsideFeed) {
                    floatingOverlayController?.show()
                } else {
                    floatingOverlayController?.hide(debounced = true)
                }
            }
        }

        // Observe live count updates to keep round counter badge up to date
        serviceScope.launch {
            combine(
                coordinator.liveTodayTotal,
                prefs.dailyTarget
            ) { total, target ->
                total to target
            }.collect { (total, target) ->
                floatingOverlayController?.updateCount(total, target)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName !in MONITORED_PACKAGES) {
            // Ignore transient overlays (system UI, keyboards, volume popups) so counter doesn't flash
            val isTransient = pkgName == "android" ||
                    pkgName == "com.android.systemui" ||
                    pkgName.contains("inputmethod", ignoreCase = true) ||
                    pkgName.contains("keyboard", ignoreCase = true) ||
                    pkgName == packageName

            if (!isTransient && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                currentActivityName = null
                ReelCountApp.instance.trackingCoordinator.onExitMonitoredApp()
            }
            return
        }

        val now = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentActivityName = event.className?.toString()
                lastContentEvaluationTime = now
                evaluateScreenAsync(immediate = true)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // User swiped or scrolled to a new reel: evaluate with short settle delay
                lastContentEvaluationTime = now
                evaluateScreenAsync(immediate = false)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Rate-limit: video playback ticks (time elapsed, comments, likes) fire dozens of times/sec
                // Throttle to at most once every 350ms to keep system UI buttery smooth
                if (now - lastContentEvaluationTime < 350L) return

                val mask = AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE or
                           AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT or
                           AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
                if ((event.contentChangeTypes and mask) == 0) return

                lastContentEvaluationTime = now
                evaluateScreenAsync(immediate = false)
            }
        }
    }

    private fun evaluateScreenAsync(immediate: Boolean) {
        if (ReelCountApp.instance.trackingCoordinator.isTrackingPaused) return

        debounceJob?.cancel()
        debounceJob = serviceScope.launch(Dispatchers.Default) {
            if (!immediate) {
                delay(120) // Allow UI hierarchy to settle after swipe/scroll
            }

            val root = rootInActiveWindow ?: return@launch
            try {
                val pkg = root.packageName?.toString() ?: ""
                val platform = Platform.fromPackageName(pkg)

                val snapshot = buildScreenSnapshot(root)
                var confirmedEvent = ReelCountApp.instance.trackingCoordinator.processSnapshot(snapshot)

                // If a new video candidate is pending stabilization, verify with second settled snapshot
                if (confirmedEvent == null && platform != null && ReelCountApp.instance.trackingCoordinator.isCandidatePending(platform)) {
                    delay(120) // Stabilization delay
                    val secondRoot = rootInActiveWindow
                    if (secondRoot != null) {
                        try {
                            val secondSnapshot = buildScreenSnapshot(secondRoot)
                            confirmedEvent = ReelCountApp.instance.trackingCoordinator.processSnapshot(secondSnapshot)
                        } finally {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                @Suppress("DEPRECATION")
                                secondRoot.recycle()
                            }
                        }
                    }
                }

                if (confirmedEvent != null) {
                    val sessionId = ReelCountApp.instance.trackingCoordinator.sessionManager.currentSessionId
                    ReelCountApp.instance.countingRepository.recordViewingEvent(confirmedEvent, sessionId)
                }
            } finally {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    @Suppress("DEPRECATION")
                    root.recycle()
                }
            }
        }
    }

    private fun buildScreenSnapshot(root: AccessibilityNodeInfo): ScreenSnapshot {
        val viewIds = mutableSetOf<String>()
        val contentDescriptions = mutableListOf<String>()
        val textContents = mutableListOf<String>()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        var fullScreenContainer = false
        var verticalActionColumn = false
        var bottomNavSelected: String? = null

        val nodeQueue = ArrayDeque<AccessibilityNodeInfo>()
        nodeQueue.add(root)

        var nodesVisited = 0
        val maxNodes = 120 // Traversal depth balancing Litho component depth with ultra-low latency

        while (nodeQueue.isNotEmpty() && nodesVisited < maxNodes) {
            val node = nodeQueue.removeFirst()
            nodesVisited++

            node.viewIdResourceName?.let { viewIds.add(it) }
            node.contentDescription?.let { contentDescriptions.add(it.toString()) }
            node.text?.let { textContents.add(it.toString()) }

            val rect = Rect()
            node.getBoundsInScreen(rect)

            // Geometry check 1: View occupying >=75% screen height and >=65% screen width
            if (rect.height() >= (screenHeight * 0.75f).toInt() && rect.width() >= (screenWidth * 0.65f).toInt()) {
                fullScreenContainer = true
            }

            // Geometry check 2: Action column on right edge (right >= 68% screen width)
            if (rect.left >= (screenWidth * 0.68f).toInt() && rect.bottom > (screenHeight * 0.35f).toInt()) {
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                if (desc.contains("like") || desc.contains("comment") || desc.contains("share") || desc.contains("remix") ||
                    text.contains("like") || text.contains("comment") || text.contains("share")) {
                    verticalActionColumn = true
                }
            }

            // Bottom nav check: tab item with isSelected == true
            if (node.isSelected && rect.top >= (screenHeight * 0.85f).toInt()) {
                bottomNavSelected = node.contentDescription?.toString() ?: node.text?.toString()
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { nodeQueue.add(it) }
            }

            if (node !== root && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }

        return ScreenSnapshot(
            packageName = root.packageName?.toString() ?: "",
            activityName = currentActivityName,
            viewIds = viewIds,
            contentDescriptions = contentDescriptions,
            textContents = textContents,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            fullScreenContainerPresent = fullScreenContainer,
            verticalActionColumnPresent = verticalActionColumn,
            bottomNavSelectedTab = bottomNavSelected,
            timestamp = System.currentTimeMillis()
        )
    }

    override fun onInterrupt() {
        isRunning = false
        floatingOverlayController?.destroy()
        floatingOverlayController = null
        ReelCountApp.instance.trackingCoordinator.onServiceDisconnected()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        floatingOverlayController?.destroy()
        floatingOverlayController = null
        serviceScope.cancel()
        ReelCountApp.instance.trackingCoordinator.onServiceDisconnected()
    }
}
