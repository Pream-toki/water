package com.water.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Battery-friendly fallback when exact alarms are not permitted: fires roughly
 * on time (>=15 min drift possible) and chains the next slot by re-rescheduling.
 */
class ReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        WaterNotificationManager.ensureChannel(appContext)
        WaterNotificationManager.showReminder(appContext)
        ReminderScheduler.reschedule(appContext)
        return Result.success()
    }
}
