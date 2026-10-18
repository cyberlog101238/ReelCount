package com.jabir.brot.domain

import com.jabir.brot.domain.model.Platform

/**
 * 7 stages of digital wellbeing brain states inspired by BrainPal.
 */
enum class BrainState(
    val stageName: String,
    val minCount: Int,
    val maxCount: Int,
    val description: String,
    val colorHex: Long
) {
    FRESH("Fresh", 0, 9, "Optimal focus and clear mind.", 0xFF43E97B),
    BRUISED("Bruised", 10, 24, "Mild cognitive fatigue settling in.", 0xFF6C63FF),
    FOGGY("Foggy", 25, 49, "Attention span is drifting.", 0xFF38B2AC),
    FRIED("Fried", 50, 99, "Dopamine receptors overloaded.", 0xFFED8936),
    MUSHY("Mushy", 100, 174, "Significant doomscrolling fatigue.", 0xFFE53E3E),
    ROTTING("Rotting", 175, 249, "Heavy cognitive strain and time loss.", 0xFF805AD5),
    BRAIN_ROT("Brain Rot", 250, Int.MAX_VALUE, "Critical consumption level! Put the phone down.", 0xFFE53E3E);

    companion object {
        fun fromCount(count: Int): BrainState {
            return entries.find { count in it.minCount..it.maxCount } ?: BRAIN_ROT
        }
    }
}

/**
 * Daily statistics calculations and aggregations.
 */
class DailyStatisticsCalculator {

    fun calculateBrainState(todayTotal: Int): BrainState {
        return BrainState.fromCount(todayTotal)
    }

    /**
     * Calculates reels per hour based on total count and duration in milliseconds.
     * Requires at least 20 seconds of watching to compute a realistic velocity.
     */
    fun calculateReelsPerHour(totalCount: Int, durationMs: Long): Float {
        if (durationMs < 20_000L || totalCount <= 0) return 0f
        val hours = durationMs.toDouble() / 3_600_000.0
        val rate = (totalCount / hours).toFloat()
        return rate.coerceIn(0f, 300f)
    }

    /**
     * Calculates average dwell time (seconds per reel).
     */
    fun calculateSecondsPerReel(totalCount: Int, durationMs: Long): Float {
        if (totalCount <= 0 || durationMs <= 0) return 0f
        val seconds = durationMs.toFloat() / 1000f
        return (seconds / totalCount).coerceIn(0.5f, 600f)
    }

    /**
     * Formats duration in milliseconds into a readable "Xh Ym" or "Ym Zs" string.
     */
    fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Compact format for metric cards: e.g. "20m", "1h 15m", "45s".
     */
    fun formatDurationCompact(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            seconds > 0 -> "${seconds}s"
            else -> "0m"
        }
    }
}
