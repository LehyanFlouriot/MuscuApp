package com.lehyanflouriot.muscuapp.core.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

// Weeks follow ISO-8601, as in France: they start on Monday.

fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun LocalDate.endOfWeek(): LocalDate = startOfWeek().plusDays(6)

val LocalDate.isoWeekNumber: Int get() = get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

fun LocalDate.isInWeek(weekStart: LocalDate): Boolean = !isBefore(weekStart) && isBefore(weekStart.plusDays(7))

/** Number of whole weeks between the weeks containing [from] and [to]. */
fun weeksBetween(from: LocalDate, to: LocalDate): Int =
    ChronoUnit.WEEKS.between(from.startOfWeek(), to.startOfWeek()).toInt()

/** The day of [weekStart]'s week matching today's day of week, never later than [today]. */
fun defaultLogDate(weekStart: LocalDate, today: LocalDate): LocalDate {
    if (today.isInWeek(weekStart)) return today
    val sameDay = weekStart.plusDays((today.dayOfWeek.value - 1).toLong())
    return if (sameDay.isAfter(today)) today else sameDay
}
