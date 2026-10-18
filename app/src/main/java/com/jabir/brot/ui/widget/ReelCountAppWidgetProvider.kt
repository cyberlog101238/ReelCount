package com.jabir.brot.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.jabir.brot.R
import com.jabir.brot.ReelCountApp
import com.jabir.brot.domain.BrainState
import com.jabir.brot.domain.DailyStatisticsCalculator
import com.jabir.brot.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android Home Screen AppWidget displaying live reel count, duration, limit and brain state.
 */
class ReelCountAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refreshWidgetData(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(
            context: Context,
            totalCount: Int,
            durationMs: Long,
            target: Int,
            brainState: BrainState
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, ReelCountAppWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            if (widgetIds.isEmpty()) return

            val calculator = DailyStatisticsCalculator()
            val timeText = "⏱️ " + calculator.formatDuration(durationMs)
            val targetText = "🎯 Goal: $target"
            val brainStateText = when (brainState) {
                BrainState.FRESH -> "🧠 Fresh"
                BrainState.BRUISED -> "🧠 Bruised"
                BrainState.FOGGY -> "🧠 Foggy"
                BrainState.FRIED -> "🧠 Fried"
                BrainState.MUSHY -> "🧠 Mushy"
                BrainState.ROTTING -> "🧠 Rotting"
                BrainState.BRAIN_ROT -> "💀 Brain Rot"
            }

            for (widgetId in widgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_reel_count)
                views.setTextViewText(R.id.widget_count, "$totalCount")
                views.setTextViewText(R.id.widget_brain_state, brainStateText)
                views.setTextViewText(R.id.widget_time, timeText)
                views.setTextViewText(R.id.widget_limit, targetText)

                // Launch MainActivity when user taps anywhere on the widget
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        fun refreshWidgetData(
            context: Context,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
            appWidgetIds: IntArray? = null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = ReelCountApp.instance
                    val repository = app.countingRepository
                    val coordinator = app.trackingCoordinator
                    val prefs = app.userPreferencesRepository

                    val totalCount = maxOf(
                        repository.getTodayCount().first(),
                        coordinator.liveTodayTotal.value
                    )
                    val durationMs = maxOf(
                        repository.getTodayDuration().first(),
                        coordinator.liveTodayDurationMs.value
                    )
                    val target = prefs.dailyTarget.first()
                    val brainState = app.statisticsCalculator.calculateBrainState(totalCount)

                    val ids = appWidgetIds ?: appWidgetManager.getAppWidgetIds(
                        ComponentName(context, ReelCountAppWidgetProvider::class.java)
                    ) ?: return@launch

                    for (widgetId in ids) {
                        val views = RemoteViews(context.packageName, R.layout.widget_reel_count)
                        views.setTextViewText(R.id.widget_count, "$totalCount")
                        views.setTextViewText(R.id.widget_brain_state, "🧠 ${brainState.stageName}")
                        views.setTextViewText(R.id.widget_time, "⏱️ " + app.statisticsCalculator.formatDuration(durationMs))
                        views.setTextViewText(R.id.widget_limit, "🎯 Goal: $target")

                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            widgetId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * Triggers the native Android 8.0+ pinned widget request to pin ReelCount widget
         * directly to the home screen.
         */
        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
                val myProvider = ComponentName(context, ReelCountAppWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    return appWidgetManager.requestPinAppWidget(myProvider, null, null)
                }
            }
            return false
        }
    }
}
