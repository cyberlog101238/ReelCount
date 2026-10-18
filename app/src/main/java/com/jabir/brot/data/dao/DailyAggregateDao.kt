package com.jabir.brot.data.dao

import androidx.room.*
import com.jabir.brot.data.entity.DailyAggregateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyAggregateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(aggregate: DailyAggregateEntity)

    @Query("SELECT * FROM daily_aggregates WHERE date = :date")
    fun getAggregatesForDate(date: String): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates WHERE date = :date AND platform = :platform LIMIT 1")
    suspend fun getAggregateForDateAndPlatform(date: String, platform: String): DailyAggregateEntity?

    @Query("SELECT * FROM daily_aggregates WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getAggregatesForDateRange(startDate: String, endDate: String): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates ORDER BY date DESC, platform ASC LIMIT 30")
    fun getRecentAggregates(): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates ORDER BY date ASC")
    suspend fun getAllAggregatesSync(): List<DailyAggregateEntity>

    @Query("DELETE FROM daily_aggregates")
    suspend fun deleteAll()
}
