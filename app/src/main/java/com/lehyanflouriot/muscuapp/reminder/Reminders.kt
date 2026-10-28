package com.lehyanflouriot.muscuapp.reminder

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.lehyanflouriot.muscuapp.core.domain.Reminder
import com.lehyanflouriot.muscuapp.core.domain.ReminderKind
import com.lehyanflouriot.muscuapp.core.domain.nextReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The daily reminders of this device. They stay on the device, each phone reminding on its own,
 * and an alarm goes off at their time to see whether they are still needed.
 */
class Reminders(context: Context, private val clock: () -> LocalDateTime = LocalDateTime::now) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager: AlarmManager = checkNotNull(appContext.getSystemService(AlarmManager::class.java))

    private val _state = MutableStateFlow(ReminderKind.entries.associateWith(::read))
    val state: StateFlow<Map<ReminderKind, Reminder>> = _state.asStateFlow()

    fun set(kind: ReminderKind, reminder: Reminder) {
        prefs.edit {
            putBoolean(kind.key(KEY_ENABLED), reminder.enabled)
            putInt(kind.key(KEY_MINUTES), reminder.time.hour * 60 + reminder.time.minute)
        }
        _state.update { it + (kind to reminder) }
        schedule(kind)
    }

    /** Puts the alarms back: after the phone restarted, the time changed or the app was updated. */
    fun scheduleAll() = ReminderKind.entries.forEach(::schedule)

    /** Sets the alarm of the next time [kind] goes off, or removes it when the reminder is off. */
    fun schedule(kind: ReminderKind) {
        val reminder = _state.value.getValue(kind)
        val operation = ReminderReceiver.pendingIntent(appContext, kind)
        if (!reminder.enabled) {
            alarmManager.cancel(operation)
            return
        }
        val triggerAt = nextReminder(clock(), reminder.time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun read(kind: ReminderKind): Reminder {
        val default = kind.defaultTime.hour * 60 + kind.defaultTime.minute
        val minutes = prefs.getInt(kind.key(KEY_MINUTES), default).coerceIn(0, MINUTES_PER_DAY - 1)
        return Reminder(
            enabled = prefs.getBoolean(kind.key(KEY_ENABLED), false),
            time = LocalTime.of(minutes / 60, minutes % 60),
        )
    }

    private fun ReminderKind.key(suffix: String) = name.lowercase() + "_" + suffix

    private companion object {
        const val PREFS_NAME = "reminders"
        const val KEY_ENABLED = "enabled"
        const val KEY_MINUTES = "minutes"
        const val MINUTES_PER_DAY = 24 * 60
    }
}
