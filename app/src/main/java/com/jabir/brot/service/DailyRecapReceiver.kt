package com.jabir.brot.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jabir.brot.R
import com.jabir.brot.ReelCountApp
import com.jabir.brot.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyRecapReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = ReelCountApp.instance
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val recapEnabled = app.userPreferencesRepository.dailyRecapNotification.first()
                if (!recapEnabled) return@launch

                val repository = app.countingRepository
                val coordinator = app.trackingCoordinator
                val todayCount = maxOf(repository.getTodayCount().first(), coordinator.liveTodayTotal.value)
                val durationMs = maxOf(repository.getTodayDuration().first(), coordinator.liveTodayDurationMs.value)

                val durationMinutes = (durationMs / 60000L).coerceAtLeast(1L)

                sendRecapNotification(context, todayCount, durationMinutes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun sendRecapNotification(context: Context, todayCount: Int, durationMinutes: Long) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "statistics")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                DailyNotificationScheduler.REQUEST_CODE_DAILY_ALARM,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "ReelCount Daily Summary 📱"
            val body = if (todayCount > 0) {
                "You scrolled $todayCount reels today (~$durationMinutes min). Tap to check your focus stats!"
            } else {
                "Great job! You stayed reel-free today. Keep up the high focus!"
            }

            val notification = NotificationCompat.Builder(context, ReelCountApp.CHANNEL_RECAP)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(DailyNotificationScheduler.RECAP_NOTIFICATION_ID, notification)
        }
    }
}
