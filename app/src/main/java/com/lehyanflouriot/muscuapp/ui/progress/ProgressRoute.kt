package com.lehyanflouriot.muscuapp.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lehyanflouriot.muscuapp.core.domain.ChartMetric
import com.lehyanflouriot.muscuapp.core.domain.buildProgressItems
import com.lehyanflouriot.muscuapp.core.domain.isInWeek
import com.lehyanflouriot.muscuapp.core.domain.metricValue
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.domain.weekStreak
import com.lehyanflouriot.muscuapp.core.domain.weeklySeries
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.ui.common.rememberToday

private const val ACTIVITY_WEEKS = 12

@Composable
fun ProgressRoute(data: AppData, onOpenExercise: (String) -> Unit) {
    val today = rememberToday()
    val items = remember(data, today) { buildProgressItems(data, today) }
    val activity = remember(data.entries, today) {
        weeklySeries(data.entries, today, ACTIVITY_WEEKS) { metricValue(it, ChartMetric.SETS) }
    }
    val streak = remember(data.entries, today) { weekStreak(data.entries, today) }
    val trainedThisWeek = remember(data.entries, today) {
        val week = today.startOfWeek()
        data.entries.any { it.date.isInWeek(week) }
    }
    ProgressScreen(
        items = items,
        activity = activity,
        streak = streak,
        trainedThisWeek = trainedThisWeek,
        today = today,
        onOpenExercise = { onOpenExercise(it.id) },
    )
}
