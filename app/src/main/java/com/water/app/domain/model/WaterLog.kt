package com.water.app.domain.model

/** One logged drink. Timestamp is epoch millis, as the user's device clock saw it. */
data class WaterLog(
    val id: Long,
    val timestamp: Long,
)
