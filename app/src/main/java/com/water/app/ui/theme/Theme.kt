package com.water.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Per-status dot visuals for the weekly visualizer. */
data class DayStatusColors(
    val low: Color,
    val onTrack: Color,
    val healthy: Color,
    val great: Color,
    val pendingRing: Color,
    val missedTrack: Color,
    val halo: Color,
)

val LocalDayStatusColors = staticCompositionLocalOf {
    DayStatusColors(
        low = SkyDeep.copy(alpha = 0.30f),
        onTrack = SkyDeep.copy(alpha = 0.60f),
        healthy = SkyDeep,
        great = SkyDeep,
        pendingRing = SkyDeep,
        missedTrack = MissedTrackLight,
        halo = HaloLight,
    )
}

/** Day-status colors derived from the active scheme; one accent does all the talking. */
private fun statusColorsOf(scheme: ColorScheme, dark: Boolean): DayStatusColors =
    DayStatusColors(
        low = scheme.primary.copy(alpha = 0.30f),
        onTrack = scheme.primary.copy(alpha = 0.60f),
        healthy = scheme.primary,
        great = scheme.primary,
        pendingRing = scheme.primary,
        missedTrack = if (dark) MissedTrackDark else MissedTrackLight,
        halo = if (dark) HaloDark else HaloLight,
    )

private fun Color.luminance(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)

/** Day-status colors for the current theme (respects dynamic color + dark mode). */
@Composable
fun waterStatusColors(): DayStatusColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return statusColorsOf(scheme, dark)
}

private val LightColors = lightColorScheme(
    primary = SkyDeep,
    onPrimary = SurfaceLight,
    secondary = InkSoft,
    onSecondary = SurfaceLight,
    background = BgLight,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Muted,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = Sky,
    onPrimary = BgDark,
    secondary = InkSoftDark,
    onSecondary = BgDark,
    background = BgDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
)

/** Monochrome slate theme with an optional Material You dynamic-color mode. */
@Composable
fun WaterTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, typography = WaterTypography) {
        CompositionLocalProvider(LocalDayStatusColors provides statusColorsOf(scheme, dark)) {
            content()
        }
    }
}
