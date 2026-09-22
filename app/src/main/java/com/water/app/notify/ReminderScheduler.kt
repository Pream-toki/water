package com.water.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.water.app.di.ServiceLocator
import com.water.app.domain.timing.DayMath
import java.util.concurrent.TimeUnit

/**
 * Arms exact alarms for today's remaining reminder slots, falling back to a
 * WorkManager chain when exact alarms are not permitted (API 31+).
 *
 * A fixed pool of request codes is used so stale alarms can always be cancelled
 * without persisting the last plan. All PendingIntents are
 * FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE.
 */
object ReminderScheduler {

    private const val FIRE_ACTION = "com.water.app.action.FIRE_REMINDER"
    private const val ALARM_CODE_BASE = 1000
    private const val MAX_SLOTS = 24
    private const val FALLBACK_WORK_NAME = "water_reminder_fallback"

    private fun fireIntent(context: Context): Intent =
        Intent(context, ReminderReceiver::class.java).setAction(FIRE_ACTION)

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Re-plans from current settings + history and re-arms everything pending. */
    suspend fun reschedule(context: Context) {
        val repo = ServiceLocator.waterRepository(context)
        val planner = ServiceLocator.planner()
        val settings = repo.currentSettingsBlocking()
        val now = System.currentTimeMillis()

        // Slots for today; when today's plan is exhausted, seed tomorrow so the
        // pipeline never goes cold.
        val history = repo.historySinceForPlanner()
        var slots = planner.pendingSlots(now, settings, history)
        if (slots.isEmpty()) {
            val tomorrow = now + DayMath.MILLIS_PER_DAY
            slots = planner.plan(tomorrow, settings, history)
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        cancelAlarms(context, alarmManager)
        WorkManager.getInstance(context).cancelUniqueWork(FALLBACK_WORK_NAME)

        if (slots.isEmpty()) return

        if (alarmManager != null && canScheduleExact(alarmManager)) {
            slots.take(MAX_SLOTS).forEachIndexed { index, slot ->
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_CODE_BASE + index,
                    fireIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (slot - now).coerceAtLeast(0),
                    pendingIntent,
                )
            }
        } else {
            // Approximate fallback: one worker now, chain re-arms the rest.
            enqueueFallback(context, slots.first() - now)
        }
    }

    fun cancelAll(context: Context) {
        cancelAlarms(context, context.getSystemService(AlarmManager::class.java))
        WorkManager.getInstance(context).cancelUniqueWork(FALLBACK_WORK_NAME)
    }

    private fun enqueueFallback(context: Context, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(FALLBACK_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun cancelAlarms(context: Context, alarmManager: AlarmManager?) {
        alarmManager ?: return
        for (code in ALARM_CODE_BASE until ALARM_CODE_BASE + MAX_SLOTS) {
            alarmManager.cancel(
                PendingIntent.getBroadcast(
                    context,
                    code,
                    fireIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
        }
    }
}
