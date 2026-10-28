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
import com.lehyanflouriot.muscuapp.core.domain.buildWeekSessions
import com.lehyanflouriot.muscuapp.core.domain.defaultLogDate
import com.lehyanflouriot.muscuapp.core.domain.programAt
import com.lehyanflouriot.muscuapp.core.domain.programOf
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseEditorSheet
import com.lehyanflouriot.muscuapp.ui.log.LogEntrySheet
import com.lehyanflouriot.muscuapp.ui.log.LogRequest
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun WeekRoute(
    data: AppData,
    repository: WorkoutRepository,
    timer: RestTimer,
    onOpenExercise: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProgram: (programId: String, day: DayOfWeek) -> Unit,
) {
    val today = rememberToday()
    val currentWeek = today.startOfWeek()
    var weekEpochDay by rememberSaveable { mutableLongStateOf(currentWeek.toEpochDay()) }
    // Never show a week in the future (for instance after the date changed).
    val weekStart = minOf(LocalDate.ofEpochDay(weekEpochDay), currentWeek)
    val overview = remember(data, weekStart) { buildWeekOverview(data, weekStart) }
    val sessions = remember(data, weekStart) { buildWeekSessions(data, weekStart) }
    // The day of the session shown, 1 for Monday; null for the one of today, even the next day.
    var pickedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val sessionDate = weekStart.plusDays((pickedDay ?: today.dayOfWeek.value) - 1L)
    val suggestions = remember(data.exercises) {
        ExerciseCatalog.search("", data.exercises.map { it.name }, limit = 12)
    }
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    var logRequest by remember { mutableStateOf<LogRequest?>(null) }
    var creatingExercise by remember { mutableStateOf(false) }

    WeekScreen(
        overview = overview,
        sessions = sessions,
        sessionDate = sessionDate,
        sessionActions = SessionActions(
            onSelectDay = { date -> pickedDay = date.dayOfWeek.value.takeIf { it != today.dayOfWeek.value } },
            onLog = { item ->
                logRequest = LogRequest.New(
                    exerciseId = item.exercise.id,
                    // A session of a day still to come is done today.
                    date = minOf(sessionDate, today),
                    goal = item.goal,
                    restSeconds = item.line.restSeconds,
                    target = listOf(Fmt.target(item.line.target, item.exercise.measure), Fmt.typography(item.line.note))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                )
            },
            onEditEntry = { entry -> logRequest = LogRequest.Edit(entry.id) },
            onOpenProgram = {
                val training = data.training
                // The program of the day shown, or the one to come when that day has none yet.
                val program = training.programAt(sessionDate)
                    ?: training.schedule.firstOrNull()?.let { training.program(it.planId) }?.let(training::programOf)
                program?.let { onOpenProgram(it.id, sessionDate.dayOfWeek) }
            },
        ),
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
