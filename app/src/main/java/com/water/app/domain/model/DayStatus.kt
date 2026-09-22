package com.water.app.domain.model

import com.water.app.domain.model.DayStatus.Companion.GREAT_MIN
import com.water.app.domain.model.DayStatus.Companion.HEALTHY_MIN

/**
 * Quiet four-state day language for the weekly visualizer.
 *
 * One drink ≈ 250 ml, so 8 drinks ≈ 2 L (healthy) and 12 ≈ 3 L (great).
 * There is no "bad" color anywhere: a missed day reads as quiet absence, and
 * an unfinished today reads as an open invitation (pending), never guilt.
 */
enum class DayStatus {
    /** Past day with nothing logged. */
    MISSED,

    /** 1–3 drinks. */
    LOW,

    /** 4–7 drinks. */
    ON_TRACK,

    /** 8–11 drinks (~2 L). Counts toward the streak. */
    HEALTHY,

    /** 12+ drinks (~3 L). */
    GREAT,

    /** Today before the first drink. */
    PENDING,

    /** A day after today (never rendered in the 7-day window). */
    FUTURE;

    companion object {
        const val HEALTHY_MIN = 8
        const val GREAT_MIN = 12

        fun of(count: Int, isToday: Boolean, isFuture: Boolean = false): DayStatus = when {
            isFuture -> FUTURE
            isToday && count <= 0 -> PENDING
            count <= 0 -> MISSED
            count < 4 -> LOW
            count < HEALTHY_MIN -> ON_TRACK
            count < GREAT_MIN -> HEALTHY
            else -> GREAT
        }
    }
}

/**
 * A "streak" is a run of consecutive healthy days (≥ [HEALTHY_MIN]) ending
 * today — or ending yesterday, while today is still in progress (grace: an
 * unfinished day neither extends nor breaks the run).
 */
object Streaks {

    /** Epoch days belonging to the current streak; empty when there is none. */
    fun currentStreakDays(days: List<DayCount>, todayEpochDay: Long): Set<Long> {
        val byDay = days.associate { it.epochDay to it.count }
        val run = mutableSetOf<Long>()
        if ((byDay[todayEpochDay] ?: 0) >= HEALTHY_MIN) {
            run.add(todayEpochDay)
        }
        var day = todayEpochDay - 1
        while ((byDay[day] ?: 0) >= HEALTHY_MIN) {
            run.add(day)
            day--
        }
        return run
    }

    fun currentStreakLength(days: List<DayCount>, todayEpochDay: Long): Int =
        currentStreakDays(days, todayEpochDay).size
}
