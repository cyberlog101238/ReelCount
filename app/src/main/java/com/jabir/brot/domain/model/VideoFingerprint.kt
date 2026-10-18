package com.jabir.brot.domain.model

import java.security.MessageDigest

/**
 * A composite fingerprint uniquely identifying a specific short-form video
 * based on observable metadata extracted from the accessibility tree.
 *
 * Two fingerprints are considered equal if their [computeHash] values match,
 * allowing deduplication across detection cycles.
 */
data class VideoFingerprint(
    /** The creator's handle or display name, e.g. "@username". */
    val creatorHandle: String?,
    /** The video caption or title text. */
    val caption: String?,
    /** The audio track label, if available. */
    val audioTrack: String?,
    /** Fallback composite signature when structured fields are sparse. */
    val compositeSignature: String? = null
) {
    /**
     * Computes a SHA-256 hex digest from the combined fields.
     *
     * Null fields are represented as an empty string so the hash is always
     * deterministic regardless of which fields are available.
     *
     * @return A lowercase hex-encoded SHA-256 hash string.
     */
    fun computeHash(): String {
        val composite = buildString {
            append("creator=")
            append(creatorHandle.orEmpty().trim().lowercase())
            append("|caption=")
            append(caption.orEmpty().trim().lowercase())
            append("|audio=")
            append(audioTrack.orEmpty().trim().lowercase())
            if (!compositeSignature.isNullOrBlank()) {
                append("|sig=")
                append(compositeSignature.trim().lowercase())
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(composite.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns `true` when at least one identifying field is non-null and non-blank,
     * meaning the fingerprint carries useful information for deduplication.
     */
    fun isUsable(): Boolean =
        !creatorHandle.isNullOrBlank() ||
        !caption.isNullOrBlank() ||
        !audioTrack.isNullOrBlank() ||
        !compositeSignature.isNullOrBlank()
}
