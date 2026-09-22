package com.water.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Projection row for the weekly GROUP BY query. [day] is a local yyyy-MM-dd string. */
data class DayCountRow(val day: String, val count: Int)

@Dao
interface WaterLogDao {

    @Query(
        """
        SELECT id, timestamp FROM water_logs
        WHERE timestamp >= :startInclusive AND timestamp < :endExclusive
        ORDER BY timestamp ASC
        """
    )
    fun observeLogsBetween(startInclusive: Long, endExclusive: Long): Flow<List<WaterLogEntity>>

    /**
     * All timestamps at or after [startInclusive], ascending. Day grouping for
     * the weekly view happens in Kotlin (see WaterRepository) — deliberately
     * not in SQLite, because 'unixepoch' modifiers need SQLite 3.38+ which
     * older Android framework builds lack.
     */
    @Query("SELECT timestamp FROM water_logs WHERE timestamp >= :startInclusive ORDER BY timestamp ASC")
    fun observeTimestampsSince(startInclusive: Long): Flow<List<Long>>

    /** Exact timestamps for the adaptive planner's learning window. */
    @Query("SELECT timestamp FROM water_logs WHERE timestamp >= :startInclusive ORDER BY timestamp ASC")
    suspend fun timestampsSince(startInclusive: Long): List<Long>

    @Insert
    suspend fun insert(entity: WaterLogEntity): Long

    @Query("SELECT COUNT(*) FROM water_logs WHERE timestamp >= :startInclusive AND timestamp < :endExclusive")
    suspend fun countBetween(startInclusive: Long, endExclusive: Long): Int

    @Query(
        """
        SELECT timestamp FROM water_logs
        WHERE timestamp >= :startInclusive AND timestamp < :endExclusive
        ORDER BY timestamp DESC LIMIT 1
        """
    )
    suspend fun latestBetween(startInclusive: Long, endExclusive: Long): Long?

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
