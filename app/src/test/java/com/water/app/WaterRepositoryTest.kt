package com.water.app

import com.water.app.data.WaterRepository
import com.water.app.data.db.WaterLogDao
import com.water.app.data.db.WaterLogEntity
import com.water.app.data.prefs.SettingsStore
import com.water.app.domain.model.UserSettings
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WaterRepositoryTest {

    private val zone = ZoneId.of("UTC")
    private lateinit var dao: FakeDao
    private lateinit var store: FakeSettings
    private lateinit var repo: WaterRepository

    @Before
    fun setUp() {
        dao = FakeDao()
        store = FakeSettings()
        repo = WaterRepository(dao, store)
    }

    @Test
    fun `logDrink inserts and today flow reflects it`() = runTest {
        repo.logDrink()
        val today = repo.observeToday(zone).first()
        assertEquals(1, today.logs.size)
    }

    @Test
    fun `deleteLog removes the entry`() = runTest {
        val id = repo.logDrink()
        repo.deleteLog(id)
        val today = repo.observeToday(zone).first()
        assertTrue(today.logs.isEmpty())
    }

    @Test
    fun `week is zero-filled to exactly 7 days oldest first`() = runTest {
        val id = repo.logDrink()
        dao.clear()
        repo.logDrink()
        val week = repo.observeLast7Days(zone).first()
        assertEquals(7, week.size)
        assertEquals(LocalDate.now(zone).toEpochDay(), week.last().epochDay)
        assertEquals(1, week.last().count)
        assertTrue(week.dropLast(1).all { it.count == 0 })
    }

    @Test
    fun `widget snapshot counts today and finds last timestamp`() = runTest {
        repo.logDrink()
        repo.logDrink()
        val snap = repo.widgetSnapshot()
        assertEquals(2, snap.todayCount)
        assertEquals(7, snap.last7Counts.size)
        assertTrue(snap.lastTodayAt != null)
    }

    @Test
    fun `settings mutations pass through`() = runTest {
        repo.setActiveHours(540, 1260)
        repo.setDailyCount(10)
        repo.setDynamicColor(true)
        repo.markOnboarded()
        val s = repo.settings.first()
        assertEquals(540, s.activeStartMin)
        assertEquals(1260, s.activeEndMin)
        assertEquals(10, s.dailyCount)
        assertTrue(s.dynamicColor)
        assertTrue(s.onboarded)
    }

    @Test
    fun `active end is clamped to at least start plus 30 minutes`() = runTest {
        store.setActiveHours(480, 480)
        val s = store.settings.first()
        assertTrue(s.activeEndMin >= s.activeStartMin + 30)
    }

    // --- fakes ---

    private class FakeDao : WaterLogDao {
        private var nextId = 1L
        private val rows = MutableStateFlow<List<WaterLogEntity>>(emptyList())

        fun clear() {
            rows.value = emptyList()
        }

        override fun observeLogsBetween(startInclusive: Long, endExclusive: Long): Flow<List<WaterLogEntity>> =
            rows

        override fun observeTimestampsSince(startInclusive: Long): Flow<List<Long>> =
            MutableStateFlow(rows.value.map { it.timestamp }.filter { it >= startInclusive }.sorted())

        override suspend fun timestampsSince(startInclusive: Long): List<Long> =
            rows.value.map { it.timestamp }.filter { it >= startInclusive }.sorted()

        override suspend fun insert(entity: WaterLogEntity): Long {
            val id = nextId++
            rows.value = rows.value + entity.copy(id = id)
            return id
        }

        override suspend fun countBetween(startInclusive: Long, endExclusive: Long): Int =
            rows.value.count { it.timestamp >= startInclusive && it.timestamp < endExclusive }

        override suspend fun latestBetween(startInclusive: Long, endExclusive: Long): Long? =
            rows.value.filter { it.timestamp >= startInclusive && it.timestamp < endExclusive }
                .maxOfOrNull { it.timestamp }

        override suspend fun deleteById(id: Long): Int {
            val before = rows.value.size
            rows.value = rows.value.filterNot { it.id == id }
            return before - rows.value.size
        }
    }

    private class FakeSettings : SettingsStore {
        private val state = MutableStateFlow(UserSettings())
        override val settings: Flow<UserSettings> = state

        override fun currentBlocking(): UserSettings = state.value

        override suspend fun setActiveHours(startMin: Int, endMin: Int) {
            val s = startMin.coerceIn(0, 23 * 60 + 55)
            state.value = state.value.copy(activeStartMin = s, activeEndMin = endMin.coerceAtLeast(s + 30))
        }

        override suspend fun setDailyCount(count: Int) {
            state.value = state.value.copy(dailyCount = count.coerceIn(1, 24))
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            state.value = state.value.copy(dynamicColor = enabled)
        }

        override suspend fun setOnboarded() {
            state.value = state.value.copy(onboarded = true)
        }
    }
}
