package com.lehyanflouriot.muscuapp.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lehyanflouriot.muscuapp.appContainer
import com.lehyanflouriot.muscuapp.core.domain.ReminderKind
import com.lehyanflouriot.muscuapp.core.domain.sessionToRemind
import com.lehyanflouriot.muscuapp.core.domain.weighInToRemind
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * Receives the alarms of the reminders, and the events after which they have to be set again:
 * the phone restarted, the time or time zone changed, the app was updated.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.appContainer
        val kind = ReminderKind.entries.firstOrNull { action(it) == intent.action }
        if (kind == null) {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                -> container.reminders.scheduleAll()
            }
            return
        }
        val pending = goAsync()
        container.scope.launch {
            try {
                // The data is read from the disk when the app was not running.
                val data = withTimeoutOrNull(LOAD_TIMEOUT_MS) { container.repository.data.filterNotNull().first() }
                val today = LocalDate.now()
                val notifications = ReminderNotifications(context)
                when {
                    data == null -> Unit
                    kind == ReminderKind.SESSION -> sessionToRemind(data, today)?.let(notifications::showSession)
                    weighInToRemind(data, today) -> notifications.showWeighIn()
                }
            } finally {
                // The same time tomorrow.
                container.reminders.schedule(kind)
                pending.finish()
            }
        }
    }

    companion object {
        private const val LOAD_TIMEOUT_MS = 8_000L

        private fun action(kind: ReminderKind) = "com.lehyanflouriot.muscuapp.action.REMIND_${kind.name}"

        fun pendingIntent(context: Context, kind: ReminderKind): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE + kind.ordinal,
                Intent(context, ReminderReceiver::class.java).setAction(action(kind)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private const val REQUEST_CODE = 20
    }
}
