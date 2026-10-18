package com.jabir.brot.domain.model

/**
 * A platform-agnostic snapshot of the current screen extracted from the
 * accessibility node tree. This abstraction keeps the domain layer free
 * of Android framework dependencies.
 *
 * The accessibility service is responsible for walking the node tree and
 * populating this data class before handing it to the detection engine.
 */
data class ScreenSnapshot(
    /** The package name of the foreground application. */
    val packageName: String,

    /** The fully-qualified activity class name, if available. */
    val activityName: String?,

    /** All resource-id values found in the accessibility node tree. */
    val viewIds: Set<String>,

    /** All non-null content-description strings from the tree. */
    val contentDescriptions: List<String>,

    /** All non-null text content strings from the tree. */
    val textContents: List<String>,

    /** Physical screen width in pixels. */
    val screenWidth: Int,

    /** Physical screen height in pixels. */
    val screenHeight: Int,

    /**
     * `true` when the tree contains a view whose height spans more than 85%
     * of [screenHeight], indicating a full-screen container typical of
     * short-form video players.
     */
    val fullScreenContainerPresent: Boolean,

    /**
     * `true` when Like/Comment/Share action buttons are stacked vertically
     * on the right side of the screen — a strong signal for a Reels/Shorts feed.
     */
    val verticalActionColumnPresent: Boolean,

    /**
     * The label of the currently selected bottom-navigation tab, or `null`
     * if the bottom nav is not visible or no tab is selected.
     */
    val bottomNavSelectedTab: String?,

    /** Epoch-millis timestamp when this snapshot was captured. */
    val timestamp: Long
)
