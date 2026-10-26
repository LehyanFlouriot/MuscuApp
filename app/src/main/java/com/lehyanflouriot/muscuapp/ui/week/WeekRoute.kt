package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.ExerciseCatalog
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.buildWeekOverview
import com.lehyanflouriot.muscuapp.core.domain.defaultLogDate
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseEditorSheet
import com.lehyanflouriot.muscuapp.ui.log.LogEntrySheet
import com.lehyanflouriot.muscuapp.ui.log.LogRequest
import java.time.LocalDate

@Composable
fun WeekRoute(
    data: AppData,
    repository: WorkoutRepository,
    timer: RestTimer,
    onOpenExercise: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = rememberToday()
    val currentWeek = today.startOfWeek()
    var weekEpochDay by rememberSaveable { mutableLongStateOf(currentWeek.toEpochDay()) }
    // Never show a week in the future (for instance after the date changed).
    val weekStart = minOf(LocalDate.ofEpochDay(weekEpochDay), currentWeek)
    val overview = remember(data, weekStart) { buildWeekOverview(data, weekStart) }
    val suggestions = remember(data.exercises) {
        ExerciseCatalog.search("", data.exercises.map { it.name }, limit = 12)
    }
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    var logRequest by remember { mutableStateOf<LogRequest?>(null) }
    var creatingExercise by remember { mutableStateOf(false) }

    WeekScreen(
        overview = overview,
        today = today,
        hasAnyEntry = data.entries.isNotEmpty(),
        suggestions = suggestions,
        onPreviousWeek = { weekEpochDay = weekStart.minusWeeks(1).toEpochDay() },
        onNextWeek = { weekEpochDay = minOf(weekStart.plusWeeks(1), currentWeek).toEpochDay() },
        onThisWeek = { weekEpochDay = currentWeek.toEpochDay() },
        onLog = { exercise -> logRequest = LogRequest.New(exercise.id, defaultLogDate(weekStart, today)) },
        onEditEntry = { entry -> logRequest = LogRequest.Edit(entry.id) },
        onOpenExercise = { onOpenExercise(it.id) },
        onAddExercise = { creatingExercise = true },
        onQuickAdd = { suggestion ->
            repository.addExercise(suggestion.toDraft())
            messenger.show(context.getString(R.string.exercise_added, suggestion.name))
        },
        onOpenSettings = onOpenSettings,
    )

    logRequest?.let { request ->
        LogEntrySheet(
            request = request,
            data = data,
            repository = repository,
            timer = timer,
            today = today,
            onDismiss = { logRequest = null },
        )
    }

    if (creatingExercise) {
        ExerciseEditorSheet(
            initial = null,
            existingNames = data.exercises.map { it.name },
            restPresets = data.restPresets,
            onDismiss = { creatingExercise = false },
            onSave = { draft ->
                repository.addExercise(draft)
                messenger.show(context.getString(R.string.exercise_added, draft.name))
            },
        )
    }
}
