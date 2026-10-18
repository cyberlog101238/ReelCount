package com.jabir.brot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jabir.brot.data.dao.DailyAggregateDao
import com.jabir.brot.data.dao.TrackingSessionDao
import com.jabir.brot.data.dao.ViewingEventDao
import com.jabir.brot.data.entity.DailyAggregateEntity
import com.jabir.brot.data.entity.TrackingSessionEntity
import com.jabir.brot.data.entity.ViewingEventEntity

@Database(
    entities = [
        ViewingEventEntity::class,
        TrackingSessionEntity::class,
        DailyAggregateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ReelCountDatabase : RoomDatabase() {
    abstract fun viewingEventDao(): ViewingEventDao
    abstract fun trackingSessionDao(): TrackingSessionDao
    abstract fun dailyAggregateDao(): DailyAggregateDao

    companion object {
        @Volatile
        private var INSTANCE: ReelCountDatabase? = null

        fun getInstance(context: Context): ReelCountDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReelCountDatabase::class.java,
                    "reelcount.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
