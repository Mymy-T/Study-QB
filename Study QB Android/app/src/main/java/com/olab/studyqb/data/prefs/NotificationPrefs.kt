package com.olab.studyqb.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.notificationDataStore by preferencesDataStore(name = "notification_settings")

/** Mirrors NotificationManager.swift's ReminderMode: 0 = daily, 1 = interval. */
object ReminderMode {
    const val DAILY = 0
    const val INTERVAL = 1
}

data class NotificationSettings(
    val notificationsEnabled: Boolean = false,
    val reminderMode: Int = ReminderMode.DAILY,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    /** Bit i = weekday i+1 (Sunday=1..Saturday=7), matching Calendar.DAY_OF_WEEK. Default: every day. */
    val selectedWeekdaysMask: Int = 127,
    val reminderIntervalHours: Int = 4
)

private object Keys {
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notificationsEnabled")
    val REMINDER_MODE = intPreferencesKey("reminderMode")
    val REMINDER_HOUR = intPreferencesKey("reminderHour")
    val REMINDER_MINUTE = intPreferencesKey("reminderMinute")
    val SELECTED_WEEKDAYS_MASK = intPreferencesKey("selectedWeekdaysMask")
    val REMINDER_INTERVAL_HOURS = intPreferencesKey("reminderIntervalHours")
}

class NotificationPrefsStore(private val context: Context) {

    val settings: Flow<NotificationSettings> = context.notificationDataStore.data.map { prefs ->
        NotificationSettings(
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            reminderMode = prefs[Keys.REMINDER_MODE] ?: ReminderMode.DAILY,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 9,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            selectedWeekdaysMask = prefs[Keys.SELECTED_WEEKDAYS_MASK] ?: 127,
            reminderIntervalHours = prefs[Keys.REMINDER_INTERVAL_HOURS] ?: 4
        )
    }

    suspend fun update(transform: (NotificationSettings) -> NotificationSettings) {
        context.notificationDataStore.edit { prefs ->
            val current = NotificationSettings(
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
                reminderMode = prefs[Keys.REMINDER_MODE] ?: ReminderMode.DAILY,
                reminderHour = prefs[Keys.REMINDER_HOUR] ?: 9,
                reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
                selectedWeekdaysMask = prefs[Keys.SELECTED_WEEKDAYS_MASK] ?: 127,
                reminderIntervalHours = prefs[Keys.REMINDER_INTERVAL_HOURS] ?: 4
            )
            val next = transform(current)
            prefs[Keys.NOTIFICATIONS_ENABLED] = next.notificationsEnabled
            prefs[Keys.REMINDER_MODE] = next.reminderMode
            prefs[Keys.REMINDER_HOUR] = next.reminderHour
            prefs[Keys.REMINDER_MINUTE] = next.reminderMinute
            prefs[Keys.SELECTED_WEEKDAYS_MASK] = next.selectedWeekdaysMask
            prefs[Keys.REMINDER_INTERVAL_HOURS] = next.reminderIntervalHours
        }
    }
}
