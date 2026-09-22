package com.water.app.domain.timing

import com.water.app.domain.model.UserSettings
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * Pure, deterministic planner. Phase 1 spreads N slots across the active window
 * with ±[JITTER_MINUTES] jitter; phase 2 biases 75% of slots toward historical
 * drink hours (same-weekday logs weighted) while keeping 25% fully random.
 *
 * Deterministic on (date, settings, history, seed): re-planning after a reboot
 * or settings change never reshuffles the rest of the day.
 */
class ReminderPlanner(
    private val random: Random = Random.Default,
) {

    /**
     * @param nowMillis        current time (epoch millis)
     * @param historyTimestamps drink logs from the learning window, ascending
     * @param zone             device timezone; plans are always local-time based
     */
    fun plan(
        nowMillis: Long,
        settings: UserSettings,
        historyTimestamps: List<Long>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Long> {
        val dayStart = DayMath.startOfDay(nowMillis, zone)
        val windowStartMs = DayMath.atMinutes(dayStart, settings.activeStartMin, zone)
        val windowEndMs = DayMath.atMinutes(dayStart, settings.activeEndMin, zone)
        if (windowEndMs <= windowStartMs) return emptyList()

        val clusters = if (historyTimestamps.isEmpty()) {
            emptyList()
        } else {
            clusteredHours(historyTimestamps, nowMillis, zone)
        }

        val slots = ArrayList<Long>(settings.dailyCount)
        val minGapMs = MIN_GAP_MINUTES * MILLIS_PER_MINUTE
        for (i in 0 until settings.dailyCount) {
            val candidate = if (clusters.isEmpty()) {
                phase1Slot(i, settings.dailyCount, windowStartMs, windowEndMs)
            } else {
                phase2Slot(clusters, dayStart, windowStartMs, windowEndMs)
            }
            val placed = placeWithMinGap(candidate, slots, minGapMs, windowStartMs, windowEndMs)
                ?: continue // window too dense for another slot
            slots.add(placed)
        }
        return slots.sorted()
    }

    /** Slots that are still in the future, i.e. what the scheduler should arm. */
    fun pendingSlots(
        nowMillis: Long,
        settings: UserSettings,
        historyTimestamps: List<Long>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Long> = plan(nowMillis, settings, historyTimestamps, zone).filter { it > nowMillis }

    // Phase 1: even spread + jitter, clamped inside the window.
    private fun phase1Slot(index: Int, count: Int, windowStartMs: Long, windowEndMs: Long): Long {
        val span = (windowEndMs - windowStartMs).coerceAtLeast(1)
        val even = windowStartMs + span * index / count
        val jitter = (random.nextDouble(-1.0, 1.0) * JITTER_MINUTES * MILLIS_PER_MINUTE).toLong()
        return (even + jitter).coerceIn(windowStartMs, windowEndMs - MILLIS_PER_MINUTE)
    }

    // Phase 2: 75% near a historical cluster, 25% fully random (exploration).
    private fun phase2Slot(
        clusters: List<Double>,
        dayStartMs: Long,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Long {
        val windowMinutes = ((windowEndMs - windowStartMs) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
        val nearCluster = clusters.isNotEmpty() && random.nextDouble() < CLUSTER_PROBABILITY
        return if (nearCluster) {
            val hour = clusters[random.nextInt(clusters.size)]
            val minutesOfDay = (hour * 60.0 + random.nextDouble(-1.0, 1.0) * CLUSTER_JITTER_MINUTES)
                .toInt().coerceIn(0, MINUTES_PER_DAY - 1)
            // minutesOfDay counts from local midnight; anchor to the day, not the window.
            (dayStartMs + minutesOfDay * MILLIS_PER_MINUTE)
                .coerceIn(windowStartMs, windowEndMs - MILLIS_PER_MINUTE)
        } else {
            val minutesOfDay = random.nextInt(windowMinutes)
            windowStartMs + minutesOfDay * MILLIS_PER_MINUTE
        }
    }

    /**
     * Retry jittered placements so the plan respects a comfortable minimum gap.
     * If random retries fail, fall back to a deterministic scan so the daily
     * count is honored whenever the window can physically fit the slots.
     */
    private fun placeWithMinGap(
        candidate: Long,
        placed: List<Long>,
        minGapMs: Long,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Long? {
        var c = candidate
        var attempts = 0
        while (attempts < MAX_GAP_ATTEMPTS) {
            if (placed.all { kotlin.math.abs(it - c) >= minGapMs }) return c
            val shift = (if (random.nextBoolean()) 1 else -1) *
                (random.nextLong(MIN_GAP_MINUTES * MILLIS_PER_MINUTE) + MILLIS_PER_MINUTE)
            c = (c + shift).coerceIn(windowStartMs, windowEndMs - MILLIS_PER_MINUTE)
            attempts++
        }
        if (placed.all { kotlin.math.abs(it - c) >= minGapMs }) return c
        // Deterministic scan (5-minute grid) for the first free position.
        var p = windowStartMs
        while (p <= windowEndMs - MILLIS_PER_MINUTE) {
            if (placed.all { kotlin.math.abs(it - p) >= minGapMs }) return p
            p += SCAN_STEP_MINUTES * MILLIS_PER_MINUTE
        }
        return null
    }

    /**
     * Weighted hour-of-day histogram of drinks. Logs from the same weekday as
     * today count triple; other logs count once. Returns candidate hours (0..24)
     * whose weight is at least [MIN_CLUSTER_WEIGHT] of the heaviest hour.
     */
    private fun clusteredHours(
        historyTimestamps: List<Long>,
        nowMillis: Long,
        zone: ZoneId,
    ): List<Double> {
        val nowDay = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val weights = DoubleArray(MINUTES_PER_DAY) // minute-of-day buckets
        for (ts in historyTimestamps) {
            val z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), zone)
            val weight = if (z.dayOfWeek == nowDay.dayOfWeek) 3.0 else 1.0
            val minuteOfDay = z.hour * 60 + z.minute
            weights[minuteOfDay] += weight
        }
        // Aggregate minute buckets into whole-hour clusters.
        val hourScores = DoubleArray(MINUTES_PER_DAY / 60)
        for (minute in 0 until MINUTES_PER_DAY) {
            if (weights[minute] <= 0.0) continue
            val hour = minute / 60
            hourScores[hour] += weights[minute]
        }
        val max = hourScores.maxOrNull() ?: return emptyList()
        if (max <= 0.0) return emptyList()
        return hourScores
            .withIndex()
            .filter { it.value >= max * MIN_CLUSTER_WEIGHT && it.value > 0.0 }
            .map { it.index.toDouble() + 0.5 } // center of the hour
    }

    companion object {
        const val JITTER_MINUTES = 35
        const val CLUSTER_JITTER_MINUTES = 20
        const val CLUSTER_PROBABILITY = 0.75 // 25% of slots stay random
        const val MIN_GAP_MINUTES = 45L
        const val MAX_GAP_ATTEMPTS = 12
        const val SCAN_STEP_MINUTES = 5L
        const val MIN_CLUSTER_WEIGHT = 0.35
        const val MILLIS_PER_MINUTE = 60_000L
        const val MINUTES_PER_DAY = 24 * 60
    }
}
