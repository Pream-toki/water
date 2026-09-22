package com.water.app.data

import com.water.app.data.db.WaterLogDao
import com.water.app.data.db.WaterLogEntity
import com.water.app.data.prefs.SettingsStore
import com.water.app.domain.model.DayCount
import com.water.app.domain.model.DayStatus
import com.water.app.domain.model.Streaks
import com.water.app.domain.model.UserSettings
import com.water.app.domain.model.WaterLog
import com.water.app.domain.timing.DayMath
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WaterRepository(
    private val logDao: WaterLogDao,
    private val settingsRepository: SettingsStore,
) {

    val settings: Flow<UserSettings> = settingsRepository.settings

    fun observeToday(zone: ZoneId = ZoneId.systemDefault()): Flow<TodayData> {
        val start = DayMath.startOfToday(zone)
        return logDao.observeLogsBetween(start, start + DayMath.MILLIS_PER_DAY)
            .map { rows ->
                TodayData(
                    logs = rows.map { WaterLog(it.id, it.timestamp) },
                    weekCounts = emptyList(),
                )
            }
    }

    /** Last 7 local days including today; missing days are zero-filled, oldest first. */
    fun observeLast7Days(zone: ZoneId = ZoneId.systemDefault()): Flow<List<DayCount>> {
        val today = LocalDate.now(zone)
        val start = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        return logDao.observeTimestampsSince(start).map { timestamps ->
            val byDay = timestamps.countByLocalDay(zone)
            (0..6).map { offset ->
                val date = today.minusDays((6 - offset).toLong())
                DayCount(epochDay = date.toEpochDay(), count = byDay[date] ?: 0)
            }
        }
    }

    suspend fun logDrink(atMillis: Long = System.currentTimeMillis()): Long =
        logDao.insert(WaterLogEntity(timestamp = atMillis))

    suspend fun deleteLog(id: Long) {
        logDao.deleteById(id)
    }

    suspend fun historySinceForPlanner(days: Long = 14, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        val cutoff = DayMath.startOfToday(zone) - days * DayMath.MILLIS_PER_DAY
        return logDao.timestampsSince(cutoff)
    }

    /** One-shot snapshot for Glance widget renders (inside provideGlance). */
    suspend fun widgetSnapshot(): WidgetSnapshot {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val timestamps = logDao.timestampsSince(weekStart)
        val byDay = timestamps.countByLocalDay(zone)
        val last7 = (0..6).map { offset ->
            val date = today.minusDays((6 - offset).toLong())
            byDay[date] ?: 0
        }
        val statuses = (0..6).map { offset ->
            val date = today.minusDays((6 - offset).toLong())
            DayStatus.of(byDay[date] ?: 0, isToday = offset == 6)
        }
        val weekCounts = (0..6).map { offset ->
            val date = today.minusDays((6 - offset).toLong())
            DayCount(epochDay = date.toEpochDay(), count = byDay[date] ?: 0)
        }
        val todayTimestamps = timestamps.filter {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
        }
        return WidgetSnapshot(
            todayCount = todayTimestamps.size,
            lastTodayAt = todayTimestamps.maxOrNull(),
            last7Counts = last7,
            last7Statuses = statuses,
            streakDays = Streaks.currentStreakDays(weekCounts, today.toEpochDay()),
        )
    }

    // Settings pass-throughs
    suspend fun setActiveHours(startMin: Int, endMin: Int) = settingsRepository.setActiveHours(startMin, endMin)
    suspend fun setDailyCount(count: Int) = settingsRepository.setDailyCount(count)
    suspend fun setDynamicColor(enabled: Boolean) = settingsRepository.setDynamicColor(enabled)
    suspend fun markOnboarded() = settingsRepository.setOnboarded()
    fun currentSettingsBlocking(): UserSettings = settingsRepository.currentBlocking()

    data class TodayData(val logs: List<WaterLog>, val weekCounts: List<DayCount>)
}

private fun List<Long>.countByLocalDay(zone: ZoneId): Map<LocalDate, Int> =
    groupBy { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        .mapValues { (_, v) -> v.size }
