package com.water.app.data

import com.water.app.domain.model.DayStatus

/** Everything a widget needs for one render pass. */
data class WidgetSnapshot(
    val todayCount: Int,
    val lastTodayAt: Long?,
    /** Counts for the last 7 local days, oldest first, today last. */
    val last7Counts: List<Int>,
    /** Statuses aligned with [last7Counts]; empty on legacy callers. */
    val last7Statuses: List<DayStatus> = emptyList(),
    /** Epoch days in the current healthy streak. */
    val streakDays: Set<Long> = emptySet(),
)
