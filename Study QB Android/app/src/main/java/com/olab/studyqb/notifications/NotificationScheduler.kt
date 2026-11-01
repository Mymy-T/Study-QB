package com.olab.studyqb.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.olab.studyqb.data.prefs.NotificationSettings
import com.olab.studyqb.data.prefs.ReminderMode
import java.util.Calendar

/**
 * AlarmManager-based scheduling matching NotificationManager.swift's `rescheduleReminders`:
 * one self-rescheduling exact alarm per selected weekday for Daily mode (Calendar.DAY_OF_WEEK
 * convention, Sunday=1..Saturday=7 — matches the iOS weekday bitmask directly, no translation
 * needed), or one self-rescheduling recurring alarm for Interval mode. Each alarm is a
 * one-shot `setExactAndAllowWhileIdle` that reschedules its own next occurrence when it fires
 * (see [ReminderReceiver]), rather than `setRepeating`, which is inexact on modern Android.
 */
object NotificationScheduler {
    const val EXTRA_TYPE = "type"
    const val EXTRA_WEEKDAY = "weekday"
    const val EXTRA_HOUR = "hour"
    const val EXTRA_MINUTE = "minute"
    const val EXTRA_INTERVAL_HOURS = "intervalHours"

    const val TYPE_DAILY = "daily"
    const val TYPE_INTERVAL = "interval"
    const val TYPE_SNOOZE = "snooze"

    const val NOTIFICATION_ID = 42

    private const val WEEKDAY_REQUEST_BASE = 1000
    private const val INTERVAL_REQUEST_CODE = 2000
    const val SNOOZE_REQUEST_CODE = 3000

    fun rescheduleAll(context: Context, settings: NotificationSettings) {
        cancelAll(context)
        if (!settings.notificationsEnabled) return
        when (settings.reminderMode) {
            ReminderMode.DAILY -> for (weekday in 1..7) {
                if ((settings.selectedWeekdaysMask and (1 shl (weekday - 1))) != 0) {
                    scheduleWeekday(context, weekday, settings.reminderHour, settings.reminderMinute)
                }
            }
            ReminderMode.INTERVAL -> scheduleInterval(context, settings.reminderIntervalHours)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = alarmManager(context) ?: return
        for (weekday in 1..7) {
            alarmManager.cancel(weekdayPendingIntent(context, weekday))
        }
        alarmManager.cancel(intervalPendingIntent(context))
        alarmManager.cancel(snoozePendingIntent(context))
    }

    fun scheduleWeekday(context: Context, weekday: Int, hour: Int, minute: Int) {
        val triggerAt = nextOccurrenceMillis(weekday, hour, minute)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_DAILY)
            putExtra(EXTRA_WEEKDAY, weekday)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        setExact(context, triggerAt, weekdayPendingIntent(context, weekday, intent))
    }

    fun scheduleInterval(context: Context, hours: Int) {
        val millis = maxOf(hours * 3600_000L, 60_000L)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_INTERVAL)
            putExtra(EXTRA_INTERVAL_HOURS, hours)
        }
        setExact(context, System.currentTimeMillis() + millis, intervalPendingIntent(context, intent))
    }

    fun scheduleSnooze(context: Context) {
        setExact(context, System.currentTimeMillis() + 3600_000L, snoozePendingIntent(context))
    }

    private fun setExact(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val alarmManager = alarmManager(context) ?: return
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun alarmManager(context: Context): AlarmManager? = context.getSystemService(AlarmManager::class.java)

    private fun weekdayPendingIntent(context: Context, weekday: Int, intent: Intent = Intent(context, ReminderReceiver::class.java)): PendingIntent =
        PendingIntent.getBroadcast(
            context, WEEKDAY_REQUEST_BASE + weekday, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun intervalPendingIntent(context: Context, intent: Intent = Intent(context, ReminderReceiver::class.java)): PendingIntent =
        PendingIntent.getBroadcast(
            context, INTERVAL_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun snoozePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply { putExtra(EXTRA_TYPE, TYPE_SNOOZE) }
        return PendingIntent.getBroadcast(
            context, SNOOZE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Next occurrence of [weekday] (Calendar.DAY_OF_WEEK, Sunday=1..Saturday=7) at [hour]:[minute], today if not yet passed. */
    private fun nextOccurrenceMillis(weekday: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        val currentWeekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        var daysUntil = weekday - currentWeekday
        if (daysUntil < 0) daysUntil += 7
        calendar.add(Calendar.DAY_OF_YEAR, daysUntil)
        if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 7)
        return calendar.timeInMillis
    }
}
