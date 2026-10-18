package com.jabir.brot.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Deprecated: Overlay is now managed directly by ReelCountAccessibilityService
 * via WindowManager with ZERO persistent status-bar notifications.
 */
class FloatingCounterService : Service() {

    companion object {
        const val ACTION_UPDATE_COUNT = "com.jabir.brot.ACTION_UPDATE_COUNT"
        const val NOTIFICATION_ID = 2001
        var isOverlayShowing: Boolean = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
        stopSelf()
    }
}
