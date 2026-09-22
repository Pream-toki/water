package com.water.app.domain.timing

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Local calendar-day math in one place, so queries, planner and UI agree. */
object DayMath {

    fun startOfDay(epochMilli: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMilli).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayExclusive(epochMilli: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDay(epochMilli, zone) + MILLIS_PER_DAY

    fun startOfToday(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    fun epochDayOf(epochMilli: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMilli).atZone(zone).toLocalDate().toEpochDay()

    /** Epoch millis for [minutes]-after-midnight on the local day containing [epochMilli]. */
    fun atMinutes(epochMilli: Long, minutes: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(epochMilli).atZone(zone).toLocalDate()
        return LocalDateTime.of(date, LocalTime.MIN).plusMinutes(minutes.toLong())
            .atZone(zone).toInstant().toEpochMilli()
    }

    fun millisSinceStartOfDay(epochMilli: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        epochMilli - startOfDay(epochMilli, zone)

    const val MILLIS_PER_DAY: Long = 24L * 60 * 60 * 1000
}
