package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.isoWeekNumber
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.domain.weeksBetween
import com.lehyanflouriot.muscuapp.core.format.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Today's date, refreshed every time the app comes back to the foreground. */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = LocalDate.now()
        if (now != today) today = now
    }
    return today
}

/** Material date pickers work with UTC midnights. */
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun utcMillisToLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** "Cette semaine", "Semaine dernière" or "Semaine 42". */
@Composable
fun weekTitle(weekStart: LocalDate, today: LocalDate): String =
    when (weeksBetween(weekStart, today)) {
        0 -> stringResource(R.string.week_this)
        1 -> stringResource(R.string.week_last)
        else -> stringResource(R.string.week_number, weekStart.isoWeekNumber)
    }

/** "Aujourd'hui", "Hier" or "lun. 20 oct.". */
@Composable
fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.date_today)
    today.minusDays(1) -> stringResource(R.string.date_yesterday)
    else -> Fmt.day(date)
}

/** "Sem. dernière", "Il y a 3 sem." */
@Composable
fun weeksAgoLabel(weeksAgo: Int): String =
    if (weeksAgo <= 1) stringResource(R.string.reference_last_week) else stringResource(R.string.reference_weeks_ago, weeksAgo)

/** Short label of a week for charts: "S42". */
fun weekShortLabel(weekStart: LocalDate): String = "S${weekStart.startOfWeek().isoWeekNumber}"
