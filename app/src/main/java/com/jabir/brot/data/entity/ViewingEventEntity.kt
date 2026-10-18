package com.jabir.brot.data.entity

import androidx.room.*

/**
 * Represents a single detected video viewing event.
 *
 * The unique constraint on (fingerprint_hash, session_id) ensures that the same
 * video (identified by its perceptual fingerprint) is not counted twice within
 * the same tracking session. This is the primary deduplication mechanism at the
 * database level.
 */
@Entity(
    tableName = "viewing_events",
    indices = [
        Index(value = ["fingerprint_hash", "session_id"], unique = true),
        Index(value = ["date"]),
        Index(value = ["platform"])
    ]
)
data class ViewingEventEntity(
    @PrimaryKey
    val id: String, // UUID
    val platform: String, // Platform enum name
    @ColumnInfo(name = "fingerprint_hash")
    val fingerprintHash: String,
    @ColumnInfo(name = "creator_handle")
    val creatorHandle: String?,
    val caption: String?, // First 100 chars only
    @ColumnInfo(name = "audio_track")
    val audioTrack: String?,
    val confidence: Float,
    val timestamp: Long, // epoch millis
    val date: String, // yyyy-MM-dd in local timezone
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "is_confirmed")
    val isConfirmed: Boolean = true
)
