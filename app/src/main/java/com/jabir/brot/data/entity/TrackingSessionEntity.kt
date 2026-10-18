package com.jabir.brot.data.entity

import androidx.room.*

/**
 * Represents a tracking session during which the accessibility service is
 * actively monitoring a specific platform for short-form video consumption.
 *
 * A session begins when tracking starts on a platform and ends when the user
 * leaves the app, pauses tracking, or the service is stopped.
 */
@Entity(tableName = "tracking_sessions")
data class TrackingSessionEntity(
    @PrimaryKey
    val id: String, // UUID
    val platform: String,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long?,
    @ColumnInfo(name = "video_count")
    val videoCount: Int = 0,
    val date: String // yyyy-MM-dd
)
