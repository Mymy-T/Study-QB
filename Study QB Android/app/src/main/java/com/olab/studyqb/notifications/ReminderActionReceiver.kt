package com.olab.studyqb.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/** Handles the "Snooze - 1 Hour" notification action, matching NotificationManager.swift's `scheduleSnooze()`. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE) {
            NotificationScheduler.scheduleSnooze(context)
            NotificationManagerCompat.from(context).cancel(NotificationScheduler.NOTIFICATION_ID)
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.olab.studyqb.action.SNOOZE"
    }
}
