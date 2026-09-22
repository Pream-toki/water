package com.water.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Central widget refresh: one call refreshes every water widget on any surface. */
object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching { CompactWaterWidget().updateAll(context) }
        runCatching { MediumWaterWidget().updateAll(context) }
    }
}
