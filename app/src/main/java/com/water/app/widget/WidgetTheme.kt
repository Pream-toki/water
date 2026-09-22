package com.water.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.unit.ColorProvider

/** Widget palette: system day/night surfaces, fixed quiet sky accent. */
object WidgetTheme {
    val Sky = Color(0xFF38BDF8)
    val SkyDeep = Color(0xFF0369A1)
    val MissedTrackLight = Color(0xFFCBD5E1)
    val MissedTrackDark = Color(0xFF3A4552)
    val HaloLight = Color(0x1A38BDF8)
    val HaloDark = Color(0x2E38BDF8)

    fun dayNight(day: Color, night: Color): ColorProvider = DayNightColor(day = day, night = night)

    fun surface(): ColorProvider =
        dayNight(day = Color(0xFFF8FAFC), night = Color(0xFF1A1D23))

    fun ink(): ColorProvider =
        dayNight(day = Color(0xFF0F172A), night = Color(0xFFE7EAEE))

    fun inkMuted(): ColorProvider =
        dayNight(day = Color(0xFF64748B), night = Color(0xFF9AA3AD))

    fun accent(): ColorProvider =
        dayNight(day = SkyDeep, night = Sky)

    fun missedTrack(): ColorProvider =
        dayNight(day = MissedTrackLight, night = MissedTrackDark)

    fun halo(): ColorProvider =
        dayNight(day = HaloLight, night = HaloDark)
}
