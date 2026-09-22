package com.water.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.water.app.di.ServiceLocator
import com.water.app.util.Haptics
import com.water.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "I Drank 💧": logs instantly from the shade, gives a quiet confirmation
 * haptic, cancels the notification — all without opening the app.
 */
class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ServiceLocator.waterRepository(appContext).logDrink()
                Haptics.confirm(appContext)
                WaterNotificationManager.cancelReminder(appContext)
                // Explicit dual-widget refresh right after the Room write.
                WidgetUpdater.updateAll(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
