package com.jabir.brot.domain.model

/**
 * Supported short-form video platforms with their Android package names.
 */
enum class Platform(val packageName: String, val displayName: String) {
    INSTAGRAM("com.instagram.android", "Instagram Reels"),
    FACEBOOK("com.facebook.katana", "Facebook Reels"),
    YOUTUBE("com.google.android.youtube", "YouTube Shorts");

    companion object {
        fun fromPackageName(pkg: String): Platform? = entries.find { it.packageName == pkg }
    }
}
