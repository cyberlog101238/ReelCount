package com.jabir.brot.data.dao

import androidx.room.*
import com.jabir.brot.data.entity.TrackingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrackingSessionEntity)

    @Update
    suspend fun updateSession(session: TrackingSessionEntity)

    @Query("SELECT * FROM tracking_sessions WHERE date = :date ORDER BY start_time DESC")
    fun getSessionsForDate(date: String): Flow<List<TrackingSessionEntity>>

    @Query("SELECT SUM(CASE WHEN end_time IS NOT NULL THEN (end_time - start_time) ELSE 0 END) FROM tracking_sessions WHERE date = :date")
    fun getTotalDurationForDate(date: String): Flow<Long?>

    @Query("SELECT * FROM tracking_sessions WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    suspend fun getActiveSession(): TrackingSessionEntity?

    @Query("DELETE FROM tracking_sessions")
    suspend fun deleteAllSessions()
}
