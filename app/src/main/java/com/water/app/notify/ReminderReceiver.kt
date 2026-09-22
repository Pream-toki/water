package com.water.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager at a planned slot. Posts the reminder and re-verifies
 * the schedule; never logs anything and never nags again.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WaterNotificationManager.ensureChannel(appContext)
                WaterNotificationManager.showReminder(appContext)
                // Keep the pipeline warm: arm the next remaining slot.
                ReminderScheduler.reschedule(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
