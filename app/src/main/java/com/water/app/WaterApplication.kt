package com.water.app

import android.app.Application
import com.water.app.di.ServiceLocator
import com.water.app.notify.ReminderScheduler
import com.water.app.notify.WaterNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaterApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Warm the graph once; everything else reaches it via ServiceLocator.
        ServiceLocator.waterRepository(this)
        WaterNotificationManager.ensureChannel(this)
        appScope.launch {
            // Day-rollover / fresh-install safety net: keeps today's plan armed.
            runCatching { ReminderScheduler.reschedule(this@WaterApplication) }
        }
    }
}
