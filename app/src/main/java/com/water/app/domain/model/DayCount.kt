package com.water.app.domain.model

/** Total drinks logged on one local calendar day ([epochDay] = [java.time.LocalDate.toEpochDay]). */
data class DayCount(
    val epochDay: Long,
    val count: Int,
)
