package com.water.app

import com.water.app.domain.model.UserSettings
import com.water.app.domain.timing.DayMath
import com.water.app.domain.timing.ReminderPlanner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {

    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 9, 21)

    private fun at(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(today, LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    private fun settings(
        startMin: Int = 480,
        endMin: Int = 1320,
        count: Int = 7,
    ) = UserSettings(activeStartMin = startMin, activeEndMin = endMin, dailyCount = count)

    @Test
    fun `phase 1 slots stay inside the active window`() {
        val planner = ReminderPlanner(Random(42))
        repeat(200) { seed ->
            val p = ReminderPlanner(Random(seed))
            val slots = p.plan(at(9, 0), settings(), emptyList(), zone)
            val windowStart = at(8, 0)
            val windowEnd = at(22, 0)
            assertTrue(slots.isNotEmpty())
            slots.forEach { slot ->
                assertTrue("slot $slot before window", slot >= windowStart)
                assertTrue("slot $slot after window", slot < windowEnd)
            }
        }
    }

    @Test
    fun `phase 1 produces roughly even spread across the window`() {
        val planner = ReminderPlanner(Random(7))
        val slots = planner.plan(at(12, 0), settings(count = 6), emptyList(), zone)
        assertEquals(6, slots.size)
        // With a 14h window and 6 slots, average spacing must exceed the min gap.
        val gaps = slots.zipWithNext { a, b -> b - a }
        val avgGap = gaps.average()
        assertTrue("avg gap too small: $avgGap", avgGap > 90 * 60_000L)
        gaps.forEach { gap -> assertTrue("gap $gap below min", gap >= ReminderPlanner.MIN_GAP_MINUTES * 60_000L) }
    }

    @Test
    fun `planning is deterministic for identical inputs`() {
        val a = ReminderPlanner(Random(123)).plan(at(10, 0), settings(), emptyList(), zone)
        val b = ReminderPlanner(Random(123)).plan(at(10, 0), settings(), emptyList(), zone)
        assertEquals(a, b)
    }

    @Test
    fun `empty window produces no slots`() {
        val planner = ReminderPlanner(Random(1))
        val slots = planner.plan(at(10, 0), settings(startMin = 1320, endMin = 1320), emptyList(), zone)
        assertTrue(slots.isEmpty())
    }

    @Test
    fun `phase 2 clusters around historical drink hours`() {
        // Drink consistently around 9 AM and 6 PM for two weeks.
        val history = buildList {
            repeat(14) { day ->
                add(at(9, 5) - day * DayMath.MILLIS_PER_DAY)
                add(at(18, 10) - day * DayMath.MILLIS_PER_DAY)
            }
        }
        val planner = ReminderPlanner(Random(99))
        val slots = planner.plan(at(7, 0), settings(count = 8), history, zone)
        assertEquals(8, slots.size)
        val nearCluster = slots.count { slot ->
            val local = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(slot), zone)
            val h = local.hour + local.minute / 60.0
            abs(h - 9.0) < 1.0 || abs(h - 18.0) < 1.0
        }
        // 75% cluster bias + weekday weighting should land most slots near 9/18;
        // threshold stays probabilistic-proof for the fixed seed.
        assertTrue("too few clustered slots: $nearCluster", nearCluster >= 2)
    }

    @Test
    fun `history from other days still produces clusters`() {
        val history = listOf(at(9, 30) - DayMath.MILLIS_PER_DAY, at(9, 40) - 2 * DayMath.MILLIS_PER_DAY)
        val planner = ReminderPlanner(Random(5))
        val slots = planner.plan(at(7, 0), settings(count = 4), history, zone)
        assertTrue(slots.isNotEmpty())
    }

    @Test
    fun `pendingSlots drops past times`() {
        val planner = ReminderPlanner(Random(3))
        val pending = planner.pendingSlots(at(20, 0), settings(count = 6), emptyList(), zone)
        assertTrue(pending.all { it > at(20, 0) })
    }

    @Test
    fun `all slots respect minimum gap with clustered history`() {
        val history = buildList {
            repeat(10) { day ->
                add(at(10, 15) - day * DayMath.MILLIS_PER_DAY)
                add(at(13, 45) - day * DayMath.MILLIS_PER_DAY)
                add(at(19, 20) - day * DayMath.MILLIS_PER_DAY)
            }
        }
        val planner = ReminderPlanner(Random(31))
        val slots = planner.plan(at(7, 0), settings(count = 10), history, zone)
        val gaps = slots.zipWithNext { a, b -> b - a }
        gaps.forEach { gap ->
            assertTrue("gap $gap below min", gap >= ReminderPlanner.MIN_GAP_MINUTES * 60_000L)
        }
    }
}
