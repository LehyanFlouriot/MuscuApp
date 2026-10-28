package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.AppData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The daily reminders the app can give. */
enum class ReminderKind(val defaultTime: LocalTime) {
    /** The session of the day, when it is not done. */
    SESSION(LocalTime.of(18, 0)),

    /** The weight of the morning, when it is not noted. */
    WEIGH_IN(LocalTime.of(7, 30)),
}

/** A daily reminder: whether it is on, and at what time. */
data class Reminder(val enabled: Boolean, val time: LocalTime)

/** When a daily reminder set at [time] goes off next, after [now]. */
fun nextReminder(now: LocalDateTime, time: LocalTime): LocalDateTime {
    val sameDay = now.toLocalDate().atTime(time)
    return if (sameDay.isAfter(now)) sameDay else sameDay.plusDays(1)
}

/** The session to remind of on [date]: a session of the program with exercises not noted yet. */
fun sessionToRemind(data: AppData, date: LocalDate): SessionView? =
    buildSession(data, date)?.takeIf { session -> !session.isRest && !session.items.all { it.done } }

/** Whether the weight of [date] still has to be noted. */
fun weighInToRemind(data: AppData, date: LocalDate): Boolean = data.bodyWeights.none { it.date == date }
