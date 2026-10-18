package com.jabir.brot.data.dao

import androidx.room.*
import com.jabir.brot.data.entity.ViewingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewingEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: ViewingEventEntity): Long

    @Query("SELECT * FROM viewing_events WHERE date = :date ORDER BY timestamp DESC")
    fun getEventsForDate(date: String): Flow<List<ViewingEventEntity>>

    @Query("SELECT * FROM viewing_events WHERE date = :date AND platform = :platform ORDER BY timestamp DESC")
    fun getEventsForDateAndPlatform(date: String, platform: String): Flow<List<ViewingEventEntity>>

    @Query("SELECT COUNT(*) FROM viewing_events WHERE date = :date AND is_confirmed = 1")
    fun getCountForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM viewing_events WHERE date = :date AND platform = :platform AND is_confirmed = 1")
    fun getCountForDateAndPlatform(date: String, platform: String): Flow<Int>

    @Query("SELECT * FROM viewing_events WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getEventsBetweenDates(startDate: String, endDate: String): Flow<List<ViewingEventEntity>>

    @Query("SELECT * FROM viewing_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEvent(): ViewingEventEntity?

    @Query("SELECT * FROM viewing_events ORDER BY timestamp DESC")
    suspend fun getAllEventsSync(): List<ViewingEventEntity>

    @Query("DELETE FROM viewing_events")
    suspend fun deleteAllEvents()
}
