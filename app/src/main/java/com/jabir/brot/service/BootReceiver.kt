package com.jabir.brot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver restoring scheduled background operations (like the daily summary recap) on device restart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        DailyNotificationScheduler.scheduleDailyRecap(context)

        // Stop legacy FloatingCounterService if present
        try {
            context.stopService(Intent(context, FloatingCounterService::class.java))
        } catch (e: Exception) {
            // Ignore
        }
    }
}
