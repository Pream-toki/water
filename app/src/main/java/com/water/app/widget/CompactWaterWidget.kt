package com.water.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.water.app.MainActivity
import com.water.app.data.WidgetSnapshot
import com.water.app.di.ServiceLocator

/**
 * Compact widget (1x1 / 2x1): today's count and a large, tactile "+1".
 * Tapping "+1" logs instantly without launching the app.
 */
class CompactWaterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = ServiceLocator.waterRepository(context).widgetSnapshot()
        provideContent { Content(snapshot, context) }
    }

    @Composable
    private fun Content(snapshot: WidgetSnapshot, context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetTheme.surface())
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = snapshot.todayCount.toString(),
                    style = TextStyle(
                        color = WidgetTheme.ink(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "drinks",
                    style = TextStyle(color = WidgetTheme.inkMuted(), fontSize = 12.sp),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "+1",
                    modifier = GlanceModifier
                        .background(WidgetTheme.accent())
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(actionRunCallback<LogOneAction>()),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Shared "+1" background action: logs a drink; the use case refreshes widgets. */
class LogOneAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ServiceLocator.logDrinkUseCase(context)(context)
    }
}
