package com.olab.studyqb.data.repository

import android.content.Context
import com.olab.studyqb.data.prefs.NotificationPrefsStore
import com.olab.studyqb.data.prefs.NotificationSettings
import com.olab.studyqb.notifications.NotificationScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Each setter derives its change from the live DataStore value (via [applyAndReschedule]),
 * not a StateFlow snapshot the caller happens to be holding — this keeps concurrent setting
 * changes race-free.
 */
class SettingsRepository(private val context: Context) {
    private val store = NotificationPrefsStore(context)

    val settings: Flow<NotificationSettings> = store.settings

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        applyAndReschedule { it.copy(notificationsEnabled = enabled) }

    suspend fun setReminderMode(mode: Int) =
        applyAndReschedule { it.copy(reminderMode = mode) }

    suspend fun setReminderTime(hour: Int, minute: Int) =
        applyAndReschedule { it.copy(reminderHour = hour, reminderMinute = minute) }

    suspend fun setWeekdayEnabled(weekday: Int, enabled: Boolean) = applyAndReschedule {
        val bit = 1 shl (weekday - 1)
        it.copy(selectedWeekdaysMask = if (enabled) it.selectedWeekdaysMask or bit else it.selectedWeekdaysMask and bit.inv())
    }

    suspend fun setReminderIntervalHours(hours: Int) =
        applyAndReschedule { it.copy(reminderIntervalHours = hours) }

    /**
     * [transform] runs inside NotificationPrefsStore.update's atomic DataStore.edit{} block,
     * so it always sees the live on-disk value — this makes concurrent setting changes
     * (e.g. rapid-tapping two weekday chips) race-free, unlike passing a precomputed value
     * derived from a possibly-stale StateFlow snapshot.
     */
    private suspend fun applyAndReschedule(transform: (NotificationSettings) -> NotificationSettings) {
        var result = NotificationSettings()
        store.update { current -> transform(current).also { result = it } }
        NotificationScheduler.rescheduleAll(context, result)
    }
}
