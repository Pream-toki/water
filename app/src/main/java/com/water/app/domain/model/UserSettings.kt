package com.water.app.domain.model

/**
 * User configuration. Active hours are minutes from local midnight
 * (480 = 8:00 AM, 1320 = 10:00 PM). The default schedule needs zero setup.
 */
data class UserSettings(
    val activeStartMin: Int = Defaults.ACTIVE_START_MIN,
    val activeEndMin: Int = Defaults.ACTIVE_END_MIN,
    val dailyCount: Int = Defaults.DAILY_COUNT,
    val dynamicColor: Boolean = false,
    val onboarded: Boolean = false,
) {
    object Defaults {
        const val ACTIVE_START_MIN = 480
        const val ACTIVE_END_MIN = 1320
        const val DAILY_COUNT = 7
    }

    /** Frequency presets shown in settings: Low 4 · Med 7 · High 10. */
    val frequencyPreset: FrequencyPreset
        get() = when (dailyCount) {
            4 -> FrequencyPreset.LOW
            10 -> FrequencyPreset.HIGH
            else -> FrequencyPreset.MEDIUM
        }
}

enum class FrequencyPreset(val perDay: Int) {
    LOW(4),
    MEDIUM(7),
    HIGH(10),
}
