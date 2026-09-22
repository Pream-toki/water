package com.water.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water.app.domain.model.DayCount
import com.water.app.domain.model.DayStatus
import com.water.app.ui.theme.waterStatusColors
import com.water.app.widget.DayVisuals
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Weekly visualizer with a quiet state language:
 *
 *  - missed (past, 0):     hollow gray ring
 *  - low (1-3):            faint accent dot
 *  - on track (4-7):       medium accent dot
 *  - healthy (8-11, ~2 L): full accent dot, streak underline when in a run
 *  - great (12+, ~3 L):    full accent dot + soft halo
 *  - today, not yet drunk: accent ring (invitation, not guilt)
 *
 * The current healthy streak is drawn as thin underline bars beneath the dots
 * it covers, plus a caption.
 */
@Composable
fun WeeklyDotsRow(
    week: List<DayCount>,
    modifier: Modifier = Modifier,
    streakDays: Set<Long> = emptySet(),
    showLegend: Boolean = true,
) {
    val colors = waterStatusColors()
    val today = LocalDate.now().toEpochDay()

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            week.forEach { day ->
                val isToday = day.epochDay == today
                val isFuture = day.epochDay > today
                val status = DayStatus.of(day.count, isToday, isFuture)
                val visuals = DayVisuals.of(
                    status = status,
                    accent = colors.healthy,
                    missedTrack = colors.missedTrack,
                    streak = day.epochDay in streakDays,
                )
                DayDot(
                    visuals = visuals,
                    haloColor = colors.halo,
                    letter = dayLetter(day.epochDay),
                    isToday = isToday,
                )
            }
        }

        val streakLength = streakDays.size
        if (streakLength >= 2) {
            Text(
                text = "$streakLength-day healthy streak",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (showLegend) {
            StatusLegend(modifier = Modifier.padding(top = 14.dp))
        }
    }
}

@Composable
private fun DayDot(
    visuals: DayVisuals,
    haloColor: Color,
    letter: String,
    isToday: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            if (visuals.halo) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(haloColor),
                )
            }
            if (visuals.isRing) {
                // Ring = hollow: an accent/track disc with a hole punched out.
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(visuals.color.copy(alpha = visuals.alpha)),
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(visuals.color.copy(alpha = visuals.alpha)),
                )
            }
        }

        // Streak underline: a bar only under streak days.
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .width(12.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (visuals.streak) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                ),
        )

        Text(
            text = letter,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Quiet one-line legend for the state language. */
@Composable
fun StatusLegend(modifier: Modifier = Modifier) {
    val colors = waterStatusColors()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(MaterialTheme.colorScheme.outline, "missed")
        LegendItem(colors.low, "low")
        LegendItem(colors.onTrack, "okay")
        LegendItem(colors.healthy, "healthy")
        LegendItem(colors.great, "great", halo = true, haloColor = colors.halo)
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    halo: Boolean = false,
    haloColor: Color = Color.Transparent,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            if (halo) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(haloColor),
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private fun dayLetter(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).dayOfWeek
        .getDisplayName(JavaTextStyle.NARROW, Locale.getDefault())
        .take(1)
