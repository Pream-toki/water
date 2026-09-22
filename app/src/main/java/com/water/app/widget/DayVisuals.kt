package com.water.app.widget

import androidx.compose.ui.graphics.Color
import com.water.app.domain.model.DayStatus

/**
 * The exact dot visuals for one day, shared by the in-app weekly row and the
 * Glance widgets so every surface tells the same story.
 *
 * Glance has no stroke API, so "hollow" states (missed, pending) are rendered
 * as a thin ring: an unfilled circle over an accent-colored disc.
 */
data class DayVisuals(
    val status: DayStatus,
    /** Disc/ring fill color. */
    val color: Color,
    /** Opacity of the fill on top of the base color. */
    val alpha: Float,
    /** True for ring states: draw a base disc in [trackColor] beneath [color]. */
    val isRing: Boolean = false,
    /** Underlying track color for ring states. */
    val trackColor: Color? = null,
    /** True when the day is part of the current healthy streak. */
    val streak: Boolean = false,
    /** Soft halo behind the dot (great day, today). */
    val halo: Boolean = false,
) {
    companion object {
        fun of(
            status: DayStatus,
            accent: Color,
            missedTrack: Color,
            streak: Boolean = false,
        ): DayVisuals = when (status) {
            DayStatus.MISSED -> DayVisuals(
                status = status,
                color = missedTrack,
                alpha = 0.9f,
                isRing = true,
                trackColor = Color.Transparent,
                streak = streak,
            )
            DayStatus.LOW -> DayVisuals(status, accent, alpha = 0.35f, streak = streak)
            DayStatus.ON_TRACK -> DayVisuals(status, accent, alpha = 0.65f, streak = streak)
            DayStatus.HEALTHY -> DayVisuals(status, accent, alpha = 1f, streak = streak)
            DayStatus.GREAT -> DayVisuals(
                status = status,
                color = accent,
                alpha = 1f,
                streak = streak,
                halo = true,
            )
            DayStatus.PENDING -> DayVisuals(
                status = status,
                color = accent,
                alpha = 1f,
                isRing = true,
                trackColor = accent,
                halo = true,
            )
            DayStatus.FUTURE -> DayVisuals(status, missedTrack, alpha = 0.3f)
        }
    }
}
