package com.jabir.brot.data.entity

import androidx.room.*

/**
 * Pre-computed daily aggregate statistics per platform.
 *
 * This table is updated transactionally whenever a new viewing event is recorded,
 * providing fast lookups for dashboard and history views without needing to
 * re-aggregate raw events each time.
 */
@Entity(
    tableName = "daily_aggregates",
    indices = [Index(value = ["date", "platform"], unique = true)]
)
data class DailyAggregateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val platform: String,
    @ColumnInfo(name = "confirmed_count")
    val confirmedCount: Int = 0,
    @ColumnInfo(name = "estimated_count")
    val estimatedCount: Int = 0,
    @ColumnInfo(name = "uncertain_count")
    val uncertainCount: Int = 0,
    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long = 0,
    @ColumnInfo(name = "session_count")
    val sessionCount: Int = 0,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
