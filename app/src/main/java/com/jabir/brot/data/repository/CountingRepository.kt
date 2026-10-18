package com.jabir.brot.data.repository

import com.jabir.brot.data.entity.DailyAggregateEntity
import com.jabir.brot.data.entity.ViewingEventEntity
import com.jabir.brot.domain.model.DetectionEvent
import com.jabir.brot.domain.model.Platform
import kotlinx.coroutines.flow.Flow

interface CountingRepository {
    suspend fun recordViewingEvent(event: DetectionEvent, sessionId: String)
    fun getTodayCount(): Flow<Int>
    fun getTodayCountByPlatform(platform: Platform): Flow<Int>
    fun getTodayEvents(): Flow<List<ViewingEventEntity>>
    fun getDailyAggregates(date: String): Flow<List<DailyAggregateEntity>>
    fun getRecentAggregates(): Flow<List<DailyAggregateEntity>>
    suspend fun startSession(platform: Platform): String
    suspend fun endSession(sessionId: String, videoCount: Int)
    fun getTodayDuration(): Flow<Long>
    suspend fun clearAllData()
    suspend fun exportData(): String
    suspend fun getLatestDetectionTime(): Long?
}
