package com.water.app.di

import android.content.Context
import androidx.room.Room
import com.water.app.data.WaterRepository
import com.water.app.data.db.WaterDatabase
import com.water.app.data.prefs.SettingsRepository
import com.water.app.domain.timing.ReminderPlanner
import com.water.app.domain.usecase.LogDrinkUseCase

/**
 * Deliberately boring manual DI. Everything the app needs is reachable from any
 * context (Activity, BroadcastReceiver, WorkManager worker) without a framework.
 */
object ServiceLocator {

    @Volatile private var database: WaterDatabase? = null
    @Volatile private var settingsRepository: SettingsRepository? = null
    @Volatile private var waterRepository: WaterRepository? = null
    @Volatile private var planner: ReminderPlanner? = null

    fun db(context: Context): WaterDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                WaterDatabase::class.java,
                WaterDatabase.FILE_NAME,
            ).build().also { database = it }
        }

    fun settings(context: Context): SettingsRepository =
        settingsRepository ?: synchronized(this) {
            settingsRepository ?: SettingsRepository(context.applicationContext).also { settingsRepository = it }
        }

    fun waterRepository(context: Context): WaterRepository =
        waterRepository ?: synchronized(this) {
            waterRepository ?: WaterRepository(
                logDao = db(context).waterLogDao(),
                settingsRepository = settings(context),
            ).also { waterRepository = it }
        }

    fun planner(): ReminderPlanner =
        planner ?: synchronized(this) {
            planner ?: ReminderPlanner().also { planner = it }
        }

    /** Fresh use-case instance per call; cheap and stateless. */
    fun logDrinkUseCase(context: Context): LogDrinkUseCase =
        LogDrinkUseCase(waterRepository(context))
}
