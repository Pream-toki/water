package com.water.app.data.prefs

import com.water.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Settings storage abstraction; implemented by the DataStore-backed repository. */
interface SettingsStore {
    val settings: Flow<UserSettings>

    /** One-shot read for receivers where a Flow subscription is awkward. */
    fun currentBlocking(): UserSettings

    suspend fun setActiveHours(startMin: Int, endMin: Int)
    suspend fun setDailyCount(count: Int)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setOnboarded()
}
