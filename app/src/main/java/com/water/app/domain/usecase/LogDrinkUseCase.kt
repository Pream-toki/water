package com.water.app.domain.usecase

import android.content.Context
import com.water.app.data.WaterRepository
import com.water.app.util.Haptics
import com.water.app.widget.WidgetUpdater

/**
 * The only way a drink gets logged. Every caller path — in-app button,
 * widget "+1", notification "I Drank 💧" — ends here, so feedback and widget
 * refresh behave identically no matter where the tap came from.
 */
class LogDrinkUseCase(
    private val repository: WaterRepository,
) {
    suspend operator fun invoke(context: Context): Long {
        val id = repository.logDrink()
        Haptics.confirm(context)
        WidgetUpdater.updateAll(context)
        return id
    }
}
