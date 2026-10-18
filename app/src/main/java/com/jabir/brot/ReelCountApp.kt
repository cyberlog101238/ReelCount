package com.jabir.brot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jabir.brot.data.ReelCountDatabase
import com.jabir.brot.data.repository.CountingRepository
import com.jabir.brot.data.repository.CountingRepositoryImpl
import com.jabir.brot.data.repository.UserPreferencesRepository
import com.jabir.brot.domain.DailyStatisticsCalculator
import com.jabir.brot.domain.TrackingCoordinator
import com.jabir.brot.domain.model.Platform
import com.jabir.brot.service.DailyNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReelCountApp : Application() {

    lateinit var database: ReelCountDatabase
        private set

    lateinit var countingRepository: CountingRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var trackingCoordinator: TrackingCoordinator
        private set

    lateinit var statisticsCalculator: DailyStatisticsCalculator
        private set

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = ReelCountDatabase.getInstance(this)
        countingRepository = CountingRepositoryImpl(database)
        userPreferencesRepository = UserPreferencesRepository(this)
        trackingCoordinator = TrackingCoordinator()
        statisticsCalculator = DailyStatisticsCalculator()

        createNotificationChannels()
        syncPersistedCounts()
        DailyNotificationScheduler.scheduleDailyRecap(this)
    }

    private fun syncPersistedCounts() {
        appScope.launch {
            try {
                val total = countingRepository.getTodayCount().first()
                val ig = countingRepository.getTodayCountByPlatform(Platform.INSTAGRAM).first()
                val fb = countingRepository.getTodayCountByPlatform(Platform.FACEBOOK).first()
                val yt = countingRepository.getTodayCountByPlatform(Platform.YOUTUBE).first()
                val duration = countingRepository.getTodayDuration().first()
                val lastTime = countingRepository.getLatestDetectionTime()
                val isPaused = userPreferencesRepository.trackingPaused.first()
                trackingCoordinator.setTrackingPaused(isPaused)
                trackingCoordinator.syncInitialCounts(total, ig, fb, yt, duration, lastTime)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the short video tracking service active"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Goal Limit Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when your daily short video limit is reached"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 450)
            }

            val recapChannel = NotificationChannel(
                CHANNEL_RECAP,
                "Daily Summary Recap",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily evening summaries of total reels scrolled and time spent"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertChannel)
            manager.createNotificationChannel(recapChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "reelcount_service_channel"
        const val CHANNEL_ALERTS = "reelcount_alerts_channel"
        const val CHANNEL_RECAP = "reelcount_recap_channel"

        lateinit var instance: ReelCountApp
            private set
    }
}
