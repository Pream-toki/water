package com.water.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-plans after reboot, clock/timezone changes, and app updates so the
 * deterministic day plan is always the one actually armed.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WaterNotificationManager.ensureChannel(appContext)
                ReminderScheduler.reschedule(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
