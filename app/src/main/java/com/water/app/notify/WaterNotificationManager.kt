package com.water.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.water.app.MainActivity
import com.water.app.R

/**
 * Builds and posts the single, quiet reminder notification. Delivery is silent:
 * no sound, no vibration, no badge — the anti-fatigue contract. The only loud
 * thing is the "I Drank 💧" action the user chooses to press.
 */
object WaterNotificationManager {

    const val CHANNEL_ID = "water_reminders"
    const val REMINDER_NOTIFICATION_ID = 2001

    private const val ACTION_LOG_FROM_NOTIFICATION = "com.water.app.action.LOG_FROM_NOTIFICATION"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun notificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun showReminder(context: Context) {
        if (!notificationsEnabled(context)) return
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, buildReminder(context))
    }

    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(REMINDER_NOTIFICATION_ID)
    }

    private fun buildReminder(context: Context): Notification {
        // Opening the notification itself is allowed to launch the app; the
        // action button never does.
        val openApp = PendingIntent.getActivity(
            context,
            3001,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val logAction = PendingIntent.getBroadcast(
            context,
            3002,
            Intent(context, ActionReceiver::class.java).setAction(ACTION_LOG_FROM_NOTIFICATION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_water)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.reminder_body))
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.reminder_action_i_drank), logAction)
            .build()
    }
}
