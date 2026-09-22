package com.water.app

import com.water.app.domain.model.DayCount
import com.water.app.domain.model.DayStatus
import com.water.app.domain.model.Streaks
import org.junit.Assert.assertEquals
import org.junit.Test

class DayStatusTest {

    private fun day(epochDay: Long, count: Int) = DayCount(epochDay, count)

    @Test
    fun `status thresholds classify counts`() {
        assertEquals(DayStatus.MISSED, DayStatus.of(0, isToday = false))
        assertEquals(DayStatus.LOW, DayStatus.of(1, isToday = false))
        assertEquals(DayStatus.LOW, DayStatus.of(3, isToday = false))
        assertEquals(DayStatus.ON_TRACK, DayStatus.of(4, isToday = false))
        assertEquals(DayStatus.ON_TRACK, DayStatus.of(7, isToday = false))
        assertEquals(DayStatus.HEALTHY, DayStatus.of(8, isToday = false))
        assertEquals(DayStatus.HEALTHY, DayStatus.of(11, isToday = false))
        assertEquals(DayStatus.GREAT, DayStatus.of(12, isToday = false))
        assertEquals(DayStatus.GREAT, DayStatus.of(20, isToday = false))
    }

    @Test
    fun `today before first drink is pending not missed`() {
        assertEquals(DayStatus.PENDING, DayStatus.of(0, isToday = true))
        assertEquals(DayStatus.HEALTHY, DayStatus.of(9, isToday = true))
    }

    @Test
    fun `future days are future regardless of count`() {
        assertEquals(DayStatus.FUTURE, DayStatus.of(0, isToday = false, isFuture = true))
        assertEquals(DayStatus.FUTURE, DayStatus.of(9, isToday = false, isFuture = true))
    }

    @Test
    fun `streak counts consecutive healthy days ending today`() {
        val days = listOf(
            day(10, 2), // low — below the bar
            day(11, 8),
            day(12, 9),
            day(13, 8), // today
        )
        val run = Streaks.currentStreakDays(days, todayEpochDay = 13)
        assertEquals(setOf(13L, 12L, 11L), run)
        assertEquals(3, Streaks.currentStreakLength(days, 13))
    }

    @Test
    fun `unfinished today extends grace without counting`() {
        val days = listOf(
            day(11, 8),
            day(12, 9),
            day(13, 0), // today, nothing yet
        )
        assertEquals(setOf(12L, 11L), Streaks.currentStreakDays(days, 13))
    }

    @Test
    fun `a low today does not break yesterday's run`() {
        val days = listOf(
            day(10, 8),
            day(11, 8),
            day(12, 2), // today barely started
        )
        assertEquals(setOf(11L, 10L), Streaks.currentStreakDays(days, 12))
    }

    @Test
    fun `a zero day yesterday breaks the streak`() {
        val days = listOf(
            day(10, 8),
            day(11, 0), // missed
            day(12, 0), // today
        )
        assertEquals(emptySet<Long>(), Streaks.currentStreakDays(days, 12))
    }

    @Test
    fun `no history no streak`() {
        assertEquals(emptySet<Long>(), Streaks.currentStreakDays(emptyList(), 100))
    }
}
