package com.jabir.brot.data.repository

import androidx.room.withTransaction
import com.jabir.brot.data.ReelCountDatabase
import com.jabir.brot.data.entity.DailyAggregateEntity
import com.jabir.brot.data.entity.TrackingSessionEntity
import com.jabir.brot.data.entity.ViewingEventEntity
import com.jabir.brot.domain.model.DetectionEvent
import com.jabir.brot.domain.model.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class CountingRepositoryImpl(
    private val database: ReelCountDatabase
) : CountingRepository {

    private val viewingEventDao = database.viewingEventDao()
    private val trackingSessionDao = database.trackingSessionDao()
    private val dailyAggregateDao = database.dailyAggregateDao()

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override suspend fun recordViewingEvent(event: DetectionEvent, sessionId: String) {
        val today = getTodayDateString()
        val entity = ViewingEventEntity(
            id = event.id,
            platform = event.platform.name,
            fingerprintHash = event.videoFingerprint ?: UUID.randomUUID().toString(),
            creatorHandle = null,
            caption = event.evidence.take(100),
            audioTrack = null,
            confidence = event.confidence,
            timestamp = event.timestamp,
            date = today,
            sessionId = sessionId,
            isConfirmed = event.eligibleForCounting
        )

        database.withTransaction {
            val insertedRowId = viewingEventDao.insertEvent(entity)
            if (insertedRowId != -1L) {
                // Successfully inserted a new distinct event (not rejected by unique constraint)
                val existing = dailyAggregateDao.getAggregateForDateAndPlatform(today, event.platform.name)
                val estimatedPerReelMs = 20_000L // 20 seconds standard dwell time estimate
                val updated = if (existing != null) {
                    existing.copy(
                        confirmedCount = existing.confirmedCount + 1,
                        totalDurationMs = existing.totalDurationMs + estimatedPerReelMs,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    DailyAggregateEntity(
                        date = today,
                        platform = event.platform.name,
                        confirmedCount = 1,
                        totalDurationMs = estimatedPerReelMs,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                dailyAggregateDao.insertOrUpdate(updated)
            }
        }
    }

    override fun getTodayCount(): Flow<Int> {
        return viewingEventDao.getCountForDate(getTodayDateString())
    }

    override fun getTodayCountByPlatform(platform: Platform): Flow<Int> {
        return viewingEventDao.getCountForDateAndPlatform(getTodayDateString(), platform.name)
    }

    override fun getTodayEvents(): Flow<List<ViewingEventEntity>> {
        return viewingEventDao.getEventsForDate(getTodayDateString())
    }

    override fun getDailyAggregates(date: String): Flow<List<DailyAggregateEntity>> {
        return dailyAggregateDao.getAggregatesForDate(date)
    }

    override fun getRecentAggregates(): Flow<List<DailyAggregateEntity>> {
        return dailyAggregateDao.getRecentAggregates()
    }

    override suspend fun startSession(platform: Platform): String {
        val id = UUID.randomUUID().toString()
        val today = getTodayDateString()
        val session = TrackingSessionEntity(
            id = id,
            platform = platform.name,
            startTime = System.currentTimeMillis(),
            endTime = null,
            videoCount = 0,
            date = today
        )
        trackingSessionDao.insertSession(session)
        return id
    }

    override suspend fun endSession(sessionId: String, videoCount: Int) {
        val active = trackingSessionDao.getActiveSession()
        if (active != null && active.id == sessionId) {
            val now = System.currentTimeMillis()
            val duration = (now - active.startTime).coerceAtLeast(0L)
            val updated = active.copy(
                endTime = now,
                videoCount = videoCount
            )
            trackingSessionDao.updateSession(updated)

            // Accumulate actual session duration into daily aggregates
            val today = getTodayDateString()
            val existing = dailyAggregateDao.getAggregateForDateAndPlatform(today, active.platform)
            if (existing != null && duration > 0L) {
                dailyAggregateDao.insertOrUpdate(
                    existing.copy(
                        totalDurationMs = maxOf(existing.totalDurationMs, duration),
                        sessionCount = existing.sessionCount + 1,
                        lastUpdated = now
                    )
                )
            }
        }
    }

    override fun getTodayDuration(): Flow<Long> {
        return trackingSessionDao.getTotalDurationForDate(getTodayDateString()).map { it ?: 0L }
    }

    override suspend fun clearAllData() {
        database.withTransaction {
            viewingEventDao.deleteAllEvents()
            trackingSessionDao.deleteAllSessions()
            dailyAggregateDao.deleteAll()
        }
    }

    override suspend fun exportData(): String {
        val events = viewingEventDao.getAllEventsSync()
        val aggregates = dailyAggregateDao.getAllAggregatesSync()

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"export_timestamp\": ${System.currentTimeMillis()},\n")
        sb.append("  \"total_events\": ${events.size},\n")
        sb.append("  \"aggregates\": [\n")
        aggregates.forEachIndexed { idx, agg ->
            sb.append("    {\"date\":\"${agg.date}\",\"platform\":\"${agg.platform}\",\"confirmedCount\":${agg.confirmedCount},\"durationMs\":${agg.totalDurationMs}}")
            if (idx < aggregates.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"events\": [\n")
        events.take(500).forEachIndexed { idx, ev ->
            sb.append("    {\"id\":\"${ev.id}\",\"platform\":\"${ev.platform}\",\"timestamp\":${ev.timestamp},\"date\":\"${ev.date}\"}")
            if (idx < events.size - 1 && idx < 499) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    override suspend fun getLatestDetectionTime(): Long? {
        return viewingEventDao.getLatestEvent()?.timestamp
    }
}
