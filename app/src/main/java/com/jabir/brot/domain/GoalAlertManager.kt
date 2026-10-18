package com.jabir.brot.domain

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.jabir.brot.R
import com.jabir.brot.ReelCountApp
import com.jabir.brot.ui.MainActivity

/**
 * Manages alarm and notification alerts when the user crosses their daily short video limit.
 */
object GoalAlertManager {
    private var lastAlertedDate: String = ""
    private var lastAlertedCount: Int = -1

    fun checkAndTriggerAlert(context: Context, todayTotal: Int, dailyTarget: Int, todayDate: String) {
        if (dailyTarget <= 0) return
        if (todayTotal < dailyTarget) {
            return
        }

        // Trigger on first crossing, or on every +10 reels milestone thereafter
        val isFirstTrigger = (lastAlertedDate != todayDate) || (lastAlertedCount < dailyTarget)
        val isMilestone = (todayTotal - lastAlertedCount) >= 10

        if (!isFirstTrigger && !isMilestone) {
            return
        }

        lastAlertedDate = todayDate
        lastAlertedCount = todayTotal

        triggerUrgentAlarm(context, todayTotal, dailyTarget)
    }

    fun triggerUrgentAlarm(context: Context, todayTotal: Int, dailyTarget: Int) {
        // 1. Play audio alarm / notification ringtone
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Distinct double-pulse vibration
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 450), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 450), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 350, 150, 450), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. High-Priority Heads-Up Notification on CHANNEL_ALERTS
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 1002, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ReelCountApp.CHANNEL_ALERTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🚨 Daily Reel Limit Reached!")
                .setContentText("You have watched $todayTotal reels today (Target: $dailyTarget). Time for a mindful break!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 350, 150, 450))
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(1002, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
