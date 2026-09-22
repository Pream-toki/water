package com.water.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.Image
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.water.app.MainActivity
import com.water.app.R
import com.water.app.data.WidgetSnapshot
import com.water.app.di.ServiceLocator
import com.water.app.domain.model.DayStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Medium widget (4x2): today's total, last logged time, a quiet 7-dot week,
 * and a prominent "+1 Water" button. Tapping the info area opens the app.
 */
class MediumWaterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = ServiceLocator.waterRepository(context).widgetSnapshot()
        provideContent { Content(snapshot, context) }
    }

    @Composable
    private fun Content(snapshot: WidgetSnapshot, context: Context) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetTheme.surface())
                .padding(12.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = countLabel(snapshot.todayCount),
                    style = TextStyle(
                        color = WidgetTheme.ink(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "+1 Water",
                    modifier = GlanceModifier
                        .background(WidgetTheme.accent())
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable(actionRunCallback<LogOneAction>()),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = lastLabel(context, snapshot.lastTodayAt),
                style = TextStyle(color = WidgetTheme.inkMuted(), fontSize = 12.sp),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            WeekDots(
                context = context,
                counts = snapshot.last7Counts,
                statuses = snapshot.last7Statuses,
                streakDays = snapshot.streakDays,
            )
        }
    }

    /**
     * Quiet 7-dot week in the shared day-status language. Discs carry the
     * count states; ring bitmaps mark missed days and a pending today; the
     * current healthy streak gets a tiny underline bar. The row keeps ≤ 10
     * children (Glance cap): one cell per day.
     */
    @Composable
    private fun WeekDots(
        context: Context,
        counts: List<Int>,
        statuses: List<DayStatus>,
        streakDays: Set<Long>,
    ) {
        val dark = context.isDarkTheme()
        val accent = if (dark) WidgetTheme.Sky else WidgetTheme.SkyDeep
        val track = if (dark) WidgetTheme.MissedTrackDark else WidgetTheme.MissedTrackLight
        val todayEpochDay = LocalDate.now().toEpochDay()

        Row(verticalAlignment = Alignment.CenterVertically) {
            statuses.forEachIndexed { index, status ->
                val isToday = index == statuses.lastIndex
                val epochDay = todayEpochDay - (statuses.lastIndex - index)
                val visuals = DayVisuals.of(
                    status = status,
                    accent = accent,
                    missedTrack = track,
                    streak = epochDay in streakDays,
                )
                DayCell(visuals = visuals, context = context)
            }
        }
    }

    /** One day cell: dot (or ring) on top, streak bar below, 16 dp pitch. */
    @Composable
    private fun DayCell(visuals: DayVisuals, context: Context) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (visuals.isRing) {
                val ringColor = visuals.color.copy(alpha = maxOf(visuals.alpha, 0.9f))
                Image(
                    provider = ImageProvider(ringBitmap(ringColor, sizePx(context, 12), sizePx(context, 2))),
                    contentDescription = null,
                    modifier = GlanceModifier.width(12.dp).height(12.dp),
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .width(12.dp)
                        .height(12.dp)
                        .background(ColorProvider(visuals.color.copy(alpha = visuals.alpha))),
                ) {}
            }
            Box(
                modifier = GlanceModifier
                    .padding(top = 2.dp)
                    .width(12.dp)
                    .height(2.dp)
                    .background(
                        if (visuals.streak) {
                            ColorProvider(visuals.color)
                        } else {
                            ColorProvider(Color.Transparent)
                        },
                    ),
            ) {}
        }
    }

    private fun countLabel(count: Int): String =
        if (count == 1) "1 drink" else "$count drinks"

    private fun lastLabel(context: Context, lastAt: Long?): String {
        if (lastAt == null) return context.getString(R.string.widget_last_at, "—")
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .format(Instant.ofEpochMilli(lastAt).atZone(ZoneId.systemDefault()).toLocalDateTime())
        return context.getString(R.string.widget_last_at, time)
    }

    private companion object {
        /** Hollow-ring bitmap: Glance has no stroke API, so we draw one. */
        fun ringBitmap(color: Color, sizePx: Int, strokePx: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokePx.toFloat()
                this.color = color.toArgb()
            }
            val inset = strokePx / 2f
            canvas.drawOval(RectF(inset, inset, sizePx - inset, sizePx - inset), paint)
            return bitmap
        }

        fun sizePx(context: Context, dp: Int): Int =
            (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

        fun Context.isDarkTheme(): Boolean =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}
