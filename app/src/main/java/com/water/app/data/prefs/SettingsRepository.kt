package com.water.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.water.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "water_settings")

class SettingsRepository(private val context: Context) : SettingsStore {

    private object Keys {
        val ACTIVE_START_MIN = intPreferencesKey("active_start_min")
        val ACTIVE_END_MIN = intPreferencesKey("active_end_min")
        val DAILY_COUNT = intPreferencesKey("daily_count")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    override val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            activeStartMin = p[Keys.ACTIVE_START_MIN] ?: UserSettings.Defaults.ACTIVE_START_MIN,
            activeEndMin = p[Keys.ACTIVE_END_MIN] ?: UserSettings.Defaults.ACTIVE_END_MIN,
            dailyCount = p[Keys.DAILY_COUNT] ?: UserSettings.Defaults.DAILY_COUNT,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: false,
            onboarded = p[Keys.ONBOARDED] ?: false,
        )
    }

    /** One-shot read; used by receivers where a Flow subscription is awkward. */
    override fun currentBlocking(): UserSettings = runBlocking { settings.first() }

    override suspend fun setActiveHours(startMin: Int, endMin: Int) {
        val s = startMin.coerceIn(0, 23 * 60 + 55)
        val e = endMin.coerceAtLeast(s + 30)
        context.dataStore.edit { p ->
            p[Keys.ACTIVE_START_MIN] = s
            p[Keys.ACTIVE_END_MIN] = e
        }
    }

    override suspend fun setDailyCount(count: Int) {
        context.dataStore.edit { it[Keys.DAILY_COUNT] = count.coerceIn(1, 24) }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setOnboarded() {
        context.dataStore.edit { it[Keys.ONBOARDED] = true }
    }
}
