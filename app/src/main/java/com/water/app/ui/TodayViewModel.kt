package com.water.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.water.app.data.WaterRepository
import com.water.app.di.ServiceLocator
import com.water.app.domain.model.DayCount
import com.water.app.domain.model.FrequencyPreset
import com.water.app.domain.model.Streaks
import com.water.app.domain.model.UserSettings
import com.water.app.domain.model.WaterLog
import com.water.app.notify.ReminderScheduler
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val count: Int = 0,
    val logs: List<WaterLog> = emptyList(),
    val week: List<DayCount> = emptyList(),
    val settings: UserSettings? = null,
    /** Epoch days in the current healthy streak (may include today). */
    val streakDays: Set<Long> = emptySet(),
)

class TodayViewModel(private val repository: WaterRepository) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        repository.observeToday(),
        repository.observeLast7Days(),
        repository.settings,
    ) { today, week, settings ->
        val todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        TodayUiState(
            count = today.logs.size,
            logs = today.logs,
            week = week,
            settings = settings,
            streakDays = Streaks.currentStreakDays(week, todayEpochDay),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun logDrink(context: Context) = viewModelScope.launch {
        runCatching { ServiceLocator.logDrinkUseCase(context.applicationContext)(context.applicationContext) }
    }

    fun deleteLog(id: Long) = viewModelScope.launch { repository.deleteLog(id) }

    /** Undo for swipe-to-delete: re-insert with the original timestamp. */
    fun restoreLog(timestamp: Long) = viewModelScope.launch { repository.logDrink(timestamp) }

    fun setFrequency(context: Context, preset: FrequencyPreset) = viewModelScope.launch {
        repository.setDailyCount(preset.perDay)
        reschedule(context)
    }

    fun setActiveHours(context: Context, startMin: Int, endMin: Int) = viewModelScope.launch {
        repository.setActiveHours(startMin, endMin)
        reschedule(context)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        repository.setDynamicColor(enabled)
    }

    private suspend fun reschedule(context: Context) {
        runCatching { ReminderScheduler.reschedule(context.applicationContext) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                TodayViewModel(ServiceLocator.waterRepository(app))
            }
        }
    }
}
