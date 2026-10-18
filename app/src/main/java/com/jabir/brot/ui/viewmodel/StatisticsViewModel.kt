package com.jabir.brot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jabir.brot.ReelCountApp
import com.jabir.brot.data.entity.DailyAggregateEntity
import com.jabir.brot.data.entity.ViewingEventEntity
import com.jabir.brot.domain.model.Platform
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HourlyItem(
    val hour: Int,
    val label: String,
    val count: Int,
    val instagramCount: Int = 0,
    val facebookCount: Int = 0,
    val youtubeCount: Int = 0
)

data class DailyBarItem(
    val dayName: String,
    val dateStr: String,
    val count: Int,
    val durationMs: Long,
    val instagramCount: Int = 0,
    val facebookCount: Int = 0,
    val youtubeCount: Int = 0
)

data class DaySummary(
    val date: String,
    val totalCount: Int,
    val totalDurationMs: Long,
    val instagramCount: Int,
    val facebookCount: Int,
    val youtubeCount: Int
)

data class StatisticsUiState(
    val selectedTab: Int = 0, // 0 = Daily, 1 = Weekly
    val dayOffset: Int = 0,   // 0 = Today, 1 = Yesterday, etc.
    val dateDisplayLabel: String = "Today",
    val hourlyItems: List<HourlyItem> = emptyList(),
    val weeklyItems: List<DailyBarItem> = emptyList(),
    val displayTotalReels: Int = 0,
    val displayDurationMs: Long = 0L,
    val peakCount: Int = 0,
    val dailySummaries: List<DaySummary> = emptyList(),
    val todayTotal: Int = 0,
    val todayDurationMs: Long = 0L,
    val averageDailyVideos: Float = 0f,
    val totalTimeSpentMs: Long = 0L,
    val allTimeTotalVideos: Int = 0,
    val peakHourLabel: String = "None",
    val instagramPercentage: Float = 0f,
    val facebookPercentage: Float = 0f,
    val youtubePercentage: Float = 0f,
    val exportJsonString: String? = null,
    val isLoading: Boolean = false
)

class StatisticsViewModel : ViewModel() {
    private val app = ReelCountApp.instance
    private val repository = app.countingRepository
    private val coordinator = app.trackingCoordinator

    private val _selectedTab = MutableStateFlow(0) // 0: Daily, 1: Weekly
    private val _dayOffset = MutableStateFlow(0)   // 0: Today/This Week, 1: Yesterday/Last Week
    private val _uiState = MutableStateFlow(StatisticsUiState(isLoading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        observeAllAnalytics()
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
        _dayOffset.value = 0 // Reset date offset when switching tabs
        _uiState.update { it.copy(selectedTab = tab, dayOffset = 0) }
    }

    fun navigatePrevious() {
        _dayOffset.update { it + 1 }
    }

    fun navigateNext() {
        _dayOffset.update { (it - 1).coerceAtLeast(0) }
    }

    private fun observeAllAnalytics() {
        viewModelScope.launch {
            val liveStatsFlow = combine(
                coordinator.liveTodayTotal,
                coordinator.livePlatformCounts,
                coordinator.liveTodayDurationMs,
                _selectedTab,
                _dayOffset
            ) { total, platforms, duration, tab, offset ->
                LiveStatsTuple(total, platforms, duration, tab, offset)
            }

            combine(
                repository.getTodayEvents().catch { emit(emptyList()) },
                repository.getRecentAggregates().catch { emit(emptyList()) },
                liveStatsFlow
            ) { todayEvents, recentAggregates, live ->
                buildUiState(
                    todayEvents = todayEvents,
                    recentAggregates = recentAggregates,
                    liveTotal = live.total,
                    livePlatforms = live.platforms,
                    liveDuration = live.duration,
                    selectedTab = live.tab,
                    dayOffset = live.offset
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildUiState(
        todayEvents: List<ViewingEventEntity>,
        recentAggregates: List<DailyAggregateEntity>,
        liveTotal: Int,
        livePlatforms: Map<Platform, Int>,
        liveDuration: Long,
        selectedTab: Int,
        dayOffset: Int
    ): StatisticsUiState {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. Build 24-hour distribution for Daily view
        val hourlyMap = mutableMapOf<Int, MutableList<ViewingEventEntity>>()
        for (h in 0..23) {
            hourlyMap[h] = mutableListOf()
        }

        val cal = Calendar.getInstance()
        todayEvents.forEach { ev ->
            cal.timeInMillis = ev.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourlyMap[hour]?.add(ev)
        }

        val hourlyItems = (0..23).map { h ->
            val eventsInHour = hourlyMap[h] ?: emptyList()
            val ig = eventsInHour.count { it.platform == "INSTAGRAM" }
            val fb = eventsInHour.count { it.platform == "FACEBOOK" }
            val yt = eventsInHour.count { it.platform == "YOUTUBE" }
            val label = when {
                h == 0 -> "12A"
                h < 12 -> "${h}A"
                h == 12 -> "12P"
                else -> "${h - 12}P"
            }
            HourlyItem(
                hour = h,
                label = label,
                count = eventsInHour.size,
                instagramCount = ig,
                facebookCount = fb,
                youtubeCount = yt
            )
        }

        val peakHour = hourlyItems.maxByOrNull { it.count }
        val peakHourCount = peakHour?.count ?: 0
        val peakHourLabel = if (peakHour != null && peakHour.count > 0) {
            "${peakHour.label} (${peakHour.count} reels)"
        } else {
            "No peak yet"
        }

        // 2. Build 7-day Weekly distribution
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val weeklyItems = (6 downTo 0).map { daysAgo ->
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -(daysAgo + dayOffset * 7))
            }
            val dateKey = dateFormat.format(targetCal.time)
            val dayName = if (daysAgo == 0 && dayOffset == 0) "Today" else dayFormat.format(targetCal.time)

            val dayAggregates = recentAggregates.filter { it.date == dateKey }
            val ig = dayAggregates.find { it.platform == "INSTAGRAM" }?.confirmedCount ?: 0
            val fb = dayAggregates.find { it.platform == "FACEBOOK" }?.confirmedCount ?: 0
            val yt = dayAggregates.find { it.platform == "YOUTUBE" }?.confirmedCount ?: 0
            val aggTotal = dayAggregates.sumOf { it.confirmedCount }
            val aggDuration = dayAggregates.sumOf { it.totalDurationMs }

            if (daysAgo == 0 && dayOffset == 0) {
                val todayCount = maxOf(liveTotal, aggTotal, todayEvents.size)
                val todayIg = maxOf(livePlatforms[Platform.INSTAGRAM] ?: 0, ig)
                val todayFb = maxOf(livePlatforms[Platform.FACEBOOK] ?: 0, fb)
                val todayYt = maxOf(livePlatforms[Platform.YOUTUBE] ?: 0, yt)
                val todayDur = maxOf(liveDuration, aggDuration)
                DailyBarItem(dayName, dateKey, todayCount, todayDur, todayIg, todayFb, todayYt)
            } else {
                DailyBarItem(dayName, dateKey, aggTotal, aggDuration, ig, fb, yt)
            }
        }

        // 3. Daily history list
        val byDate = recentAggregates.groupBy { it.date }.map { (date, entries) ->
            val ig = entries.find { it.platform == "INSTAGRAM" }?.confirmedCount ?: 0
            val fb = entries.find { it.platform == "FACEBOOK" }?.confirmedCount ?: 0
            val yt = entries.find { it.platform == "YOUTUBE" }?.confirmedCount ?: 0
            val count = entries.sumOf { it.confirmedCount }
            val dur = entries.sumOf { it.totalDurationMs }

            if (date == todayStr) {
                DaySummary(
                    date = date,
                    totalCount = maxOf(liveTotal, count, todayEvents.size),
                    totalDurationMs = maxOf(liveDuration, dur),
                    instagramCount = maxOf(livePlatforms[Platform.INSTAGRAM] ?: 0, ig),
                    facebookCount = maxOf(livePlatforms[Platform.FACEBOOK] ?: 0, fb),
                    youtubeCount = maxOf(livePlatforms[Platform.YOUTUBE] ?: 0, yt)
                )
            } else {
                DaySummary(date, count, dur, ig, fb, yt)
            }
        }.toMutableList()

        if (byDate.none { it.date == todayStr }) {
            byDate.add(
                0,
                DaySummary(
                    date = todayStr,
                    totalCount = maxOf(liveTotal, todayEvents.size),
                    totalDurationMs = liveDuration,
                    instagramCount = livePlatforms[Platform.INSTAGRAM] ?: 0,
                    facebookCount = livePlatforms[Platform.FACEBOOK] ?: 0,
                    youtubeCount = livePlatforms[Platform.YOUTUBE] ?: 0
                )
            )
        }

        val sortedSummaries = byDate.sortedByDescending { it.date }

        val totalVideos = recentAggregates.sumOf { it.confirmedCount } +
                (if (recentAggregates.none { it.date == todayStr }) liveTotal else 0)
        val totalTime = recentAggregates.sumOf { it.totalDurationMs } + liveDuration
        val daysCount = sortedSummaries.size.coerceAtLeast(1)
        val avg = totalVideos.toFloat() / daysCount.toFloat()

        val igTotal = recentAggregates.filter { it.platform == "INSTAGRAM" }.sumOf { it.confirmedCount } + (livePlatforms[Platform.INSTAGRAM] ?: 0)
        val fbTotal = recentAggregates.filter { it.platform == "FACEBOOK" }.sumOf { it.confirmedCount } + (livePlatforms[Platform.FACEBOOK] ?: 0)
        val ytTotal = recentAggregates.filter { it.platform == "YOUTUBE" }.sumOf { it.confirmedCount } + (livePlatforms[Platform.YOUTUBE] ?: 0)
        val allPlatformsTotal = (igTotal + fbTotal + ytTotal).coerceAtLeast(1)

        val igPct = igTotal.toFloat() / allPlatformsTotal.toFloat()
        val fbPct = fbTotal.toFloat() / allPlatformsTotal.toFloat()
        val ytPct = ytTotal.toFloat() / allPlatformsTotal.toFloat()

        // 4. Compute display values for date navigator & bottom metric card
        val dateDisplayLabel: String
        val displayTotalReels: Int
        val displayDurationMs: Long
        val peakCount: Int

        if (selectedTab == 0) {
            // Daily Mode
            if (dayOffset == 0) {
                dateDisplayLabel = "Today"
                displayTotalReels = maxOf(liveTotal, todayEvents.size)
                displayDurationMs = maxOf(liveDuration, coordinator.liveTodayDurationMs.value)
                peakCount = peakHourCount
            } else {
                val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dayOffset) }
                dateDisplayLabel = if (dayOffset == 1) "Yesterday" else SimpleDateFormat("MMM d", Locale.getDefault()).format(targetCal.time)
                val targetDateKey = dateFormat.format(targetCal.time)
                val dayAggs = recentAggregates.filter { it.date == targetDateKey }
                displayTotalReels = dayAggs.sumOf { it.confirmedCount }
                displayDurationMs = dayAggs.sumOf { it.totalDurationMs }
                peakCount = displayTotalReels
            }
        } else {
            // Weekly Mode
            dateDisplayLabel = when (dayOffset) {
                0 -> "This Week"
                1 -> "Last Week"
                else -> "$dayOffset Weeks Ago"
            }
            displayTotalReels = weeklyItems.sumOf { it.count }
            displayDurationMs = weeklyItems.sumOf { it.durationMs }
            peakCount = weeklyItems.maxOfOrNull { it.count } ?: 0
        }

        return StatisticsUiState(
            selectedTab = selectedTab,
            dayOffset = dayOffset,
            dateDisplayLabel = dateDisplayLabel,
            hourlyItems = hourlyItems,
            weeklyItems = weeklyItems,
            displayTotalReels = displayTotalReels,
            displayDurationMs = displayDurationMs,
            peakCount = peakCount,
            dailySummaries = sortedSummaries,
            todayTotal = maxOf(liveTotal, todayEvents.size),
            todayDurationMs = liveDuration,
            averageDailyVideos = avg,
            totalTimeSpentMs = totalTime,
            allTimeTotalVideos = totalVideos,
            peakHourLabel = peakHourLabel,
            instagramPercentage = igPct,
            facebookPercentage = fbPct,
            youtubePercentage = ytPct,
            isLoading = false
        )
    }

    fun exportData(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportData()
            _uiState.update { it.copy(exportJsonString = json) }
            onReady(json)
        }
    }

    private data class LiveStatsTuple(
        val total: Int,
        val platforms: Map<Platform, Int>,
        val duration: Long,
        val tab: Int,
        val offset: Int
    )
}
