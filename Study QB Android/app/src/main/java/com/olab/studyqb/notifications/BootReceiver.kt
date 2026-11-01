package com.olab.studyqb.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.olab.studyqb.data.prefs.NotificationPrefsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/** Re-arms all alarms after a reboot, since AlarmManager alarms don't survive one. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = NotificationPrefsStore(context.applicationContext).settings.first()
                if (settings.notificationsEnabled) {
                    NotificationScheduler.rescheduleAll(context.applicationContext, settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
