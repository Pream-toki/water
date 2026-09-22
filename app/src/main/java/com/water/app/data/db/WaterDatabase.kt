package com.water.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WaterLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WaterDatabase : RoomDatabase() {
    abstract fun waterLogDao(): WaterLogDao

    companion object {
        const val FILE_NAME = "water.db"
    }
}
