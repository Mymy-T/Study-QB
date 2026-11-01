package com.olab.studyqb.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.olab.studyqb.MainActivity

/**
 * Fires the "Time to study!" notification (matching NotificationManager.swift's makeContent())
 * with "Study Now"/"Snooze - 1 Hour" actions, then — since this is a one-shot exact alarm, not
 * a repeating one — reschedules its own next occurrence (weekday+7d, or +intervalHours).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        postNotification(context)

        when (intent.getStringExtra(NotificationScheduler.EXTRA_TYPE)) {
            NotificationScheduler.TYPE_DAILY -> {
                val weekday = intent.getIntExtra(NotificationScheduler.EXTRA_WEEKDAY, -1)
                val hour = intent.getIntExtra(NotificationScheduler.EXTRA_HOUR, 9)
                val minute = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTE, 0)
                if (weekday in 1..7) NotificationScheduler.scheduleWeekday(context, weekday, hour, minute)
            }
            NotificationScheduler.TYPE_INTERVAL -> {
                val hours = intent.getIntExtra(NotificationScheduler.EXTRA_INTERVAL_HOURS, 4)
                NotificationScheduler.scheduleInterval(context, hours)
            }
            NotificationScheduler.TYPE_SNOOZE -> {
                // One-shot; nothing to reschedule.
            }
        }
    }

    private fun postNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val studyNowIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val studyNowPendingIntent = PendingIntent.getActivity(
            context, 0, studyNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, NotificationScheduler.SNOOZE_REQUEST_CODE, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.STUDY_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to study!")
            .setContentText("Let's review a couple cards now.")
            .setContentIntent(studyNowPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Study Now", studyNowPendingIntent)
            .addAction(0, "Snooze - 1 Hour", snoozePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NotificationScheduler.NOTIFICATION_ID, notification)
    }
}
