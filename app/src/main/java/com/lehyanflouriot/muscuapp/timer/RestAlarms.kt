package com.lehyanflouriot.muscuapp.timer

import android.app.AlarmManager
import android.content.Context
import android.os.Build

/** Schedules the end of the rest, exact when the system allows it. */
class RestAlarms(private val context: Context) {
    private val alarmManager: AlarmManager = checkNotNull(context.getSystemService(AlarmManager::class.java))

    fun schedule(triggerAt: Long) {
        val operation = RestTimerReceiver.pendingIntent(context, RestTimerReceiver.ACTION_FINISH)
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

    fun cancel() {
        alarmManager.cancel(RestTimerReceiver.pendingIntent(context, RestTimerReceiver.ACTION_FINISH))
    }
}
