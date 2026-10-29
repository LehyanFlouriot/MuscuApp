package com.lehyanflouriot.muscuapp.ui.progress

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.ChartMetric
import com.lehyanflouriot.muscuapp.core.domain.averageSessionDuration
import com.lehyanflouriot.muscuapp.core.domain.buildMuscleStats
import com.lehyanflouriot.muscuapp.core.domain.buildProgressItems
import com.lehyanflouriot.muscuapp.core.domain.isInWeek
import com.lehyanflouriot.muscuapp.core.domain.metricValue
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.domain.weekStreak
import com.lehyanflouriot.muscuapp.core.domain.weeklySeries
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.ui.ProgressSection
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.weight.WeightSection

private const val ACTIVITY_WEEKS = 12

@Composable
fun ProgressRoute(
    data: AppData,
    repository: WorkoutRepository,
    section: ProgressSection,
    onSectionChange: (ProgressSection) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenMuscle: (MuscleGroup) -> Unit,
    onOpenSettings: () -> Unit,
) {
    ProgressFrame(section = section, onSectionChange = onSectionChange, onOpenSettings = onOpenSettings) { shown, padding ->
        when (shown) {
            ProgressSection.EXERCISES -> ExercisesSection(data = data, padding = padding, onOpenExercise = onOpenExercise)
            ProgressSection.MUSCLES -> {
                val today = rememberToday()
                val muscles = remember(data, today) { buildMuscleStats(data, today) }
                MusclesProgress(padding = padding, muscles = muscles, onOpenMuscle = onOpenMuscle)
            }
            ProgressSection.WEIGHT -> WeightSection(data = data, repository = repository, padding = padding)
        }
    }
}

@Composable
private fun ExercisesSection(data: AppData, padding: PaddingValues, onOpenExercise: (String) -> Unit) {
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
    ExercisesProgress(
        padding = padding,
        items = items,
        activity = activity,
        streak = streak,
        trainedThisWeek = trainedThisWeek,
        today = today,
        onOpenExercise = { onOpenExercise(it.id) },
        averageDuration = data.training.averageSessionDuration(today),
    )
}
