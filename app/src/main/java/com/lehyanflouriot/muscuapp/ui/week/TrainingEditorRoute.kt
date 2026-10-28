package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.data.moveLine
import com.lehyanflouriot.muscuapp.core.data.removeLine
import com.lehyanflouriot.muscuapp.core.data.updateDay
import com.lehyanflouriot.muscuapp.core.domain.ProgramStatus
import com.lehyanflouriot.muscuapp.core.domain.defaultTarget
import com.lehyanflouriot.muscuapp.core.domain.programAt
import com.lehyanflouriot.muscuapp.core.domain.programInfos
import com.lehyanflouriot.muscuapp.core.domain.trainingHistory
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import com.lehyanflouriot.muscuapp.ui.ProgramRequest
import com.lehyanflouriot.muscuapp.ui.common.ConfirmDialog
import com.lehyanflouriot.muscuapp.ui.common.CopyDayDialog
import com.lehyanflouriot.muscuapp.ui.common.DialogAction
import com.lehyanflouriot.muscuapp.ui.common.FormDialog
import com.lehyanflouriot.muscuapp.ui.common.FormField
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.NewProgramDialog
import com.lehyanflouriot.muscuapp.ui.common.ProgramsSheet
import com.lehyanflouriot.muscuapp.ui.common.StartProgramDialog
import com.lehyanflouriot.muscuapp.ui.common.announceStart
import com.lehyanflouriot.muscuapp.ui.common.fullName
import com.lehyanflouriot.muscuapp.ui.common.programStatusText
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseEditorSheet
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * The Programme tab: the program followed today, or another one chosen in "Mes programmes".
 * [request] asks for a program and a day of it, from the session of the week.
 */
@Composable
fun TrainingEditorRoute(
    data: AppData,
    repository: WorkoutRepository,
    request: ProgramRequest?,
    onRequestDone: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = rememberToday()
    val training = data.training
    // The program chosen in "Mes programmes", null for the one followed today.
    var programId by rememberSaveable { mutableStateOf<String?>(null) }
    var scrollTo by remember { mutableStateOf<DayOfWeek?>(null) }
    LaunchedEffect(request) {
        if (request != null) {
            programId = request.programId
            scrollTo = request.day
            onRequestDone()
        }
    }
    val program = programId?.let(training::program)?.takeIf { it.versionOf == null }
        ?: training.programAt(today)
        ?: training.programInfos(today).firstOrNull()?.plan
    var creating by rememberSaveable { mutableStateOf(false) }
    if (program == null) {
        NoTrainingProgram(onCreate = { creating = true }, onOpenSettings = onOpenSettings)
        if (creating) {
            NewProgramDialog(
                current = null,
                today = today,
                nameHint = stringResource(R.string.training_program_name_hint),
                onDismiss = { creating = false },
                onCreate = { name, _, start ->
                    programId = repository.createTrainingProgram(name, null, start ?: today)
                    creating = false
                },
            )
        }
        return
    }
    val id = program.id
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    var edit by remember { mutableStateOf<TrainingEdit?>(null) }
    var showPrograms by rememberSaveable { mutableStateOf(false) }
    var starting by remember { mutableStateOf<TrainingProgram?>(null) }
    var deleting by remember { mutableStateOf<TrainingProgram?>(null) }
    val update: ((TrainingProgram) -> TrainingProgram) -> Unit = { repository.updateTrainingProgram(id, it) }
    val close = { edit = null }
    val exercises = remember(data.exercises) { data.exercises.associateBy { it.id } }
    val infos = remember(training, today) { training.programInfos(today) }

    /** Goes on once the exercise of a session is chosen: what the session asks for it comes next. */
    fun picked(day: DayOfWeek, replacing: SessionExercise?, exerciseId: String) {
        edit = if (replacing == null) {
            TrainingEdit.Line(day, null, exerciseId)
        } else {
            repository.replaceSessionExercise(id, replacing.id, exerciseId)
            TrainingEdit.Line(day, replacing.copy(exerciseId = exerciseId), exerciseId)
        }
    }

    TrainingEditorScreen(
        program = program,
        exercises = exercises,
        status = infos.firstOrNull { it.plan.id == id }?.let { programStatusText(it, today) },
        keepsHistory = remember(data, id) { data.trainingHistory(id) },
        today = today.dayOfWeek,
        scrollTo = scrollTo,
        onScrolled = { scrollTo = null },
        onOpen = { edit = it },
        onOpenPrograms = { showPrograms = true },
        onOpenSettings = onOpenSettings,
    )

    when (val current = edit) {
        null -> Unit
        TrainingEdit.Program -> FormDialog(
            title = stringResource(R.string.training_program_edit),
            fields = listOf(
                FormField(stringResource(R.string.plan_name), program.name, required = true),
                FormField(stringResource(R.string.plan_info), program.info, singleLine = false),
            ),
            onDismiss = close,
            onConfirm = { (name, info) ->
                update { it.copy(name = name, info = info) }
                close()
            },
        )
        is TrainingEdit.Day -> {
            val day = program.day(current.day)
            FormDialog(
                title = current.day.fullName(),
                fields = listOf(
                    FormField(
                        label = stringResource(R.string.training_day_title),
                        initial = day.title,
                        placeholder = stringResource(R.string.training_day_title_hint),
                    ),
                    FormField(
                        label = stringResource(R.string.training_day_focus),
                        initial = day.focus,
                        placeholder = stringResource(R.string.training_day_focus_hint),
                    ),
                    FormField(stringResource(R.string.training_day_note), day.note, singleLine = false),
                ),
                onDismiss = close,
                onConfirm = { (title, focus, note) ->
                    update { it.updateDay(current.day) { d -> d.copy(title = title, focus = focus, note = note) } }
                    close()
                },
            )
        }
        is TrainingEdit.CopyDay -> {
            val dayName = current.day.getDisplayName(TextStyle.FULL, Fmt.locale)
            CopyDayDialog(
                from = current.day,
                title = stringResource(R.string.training_copy_day_title, dayName),
                text = stringResource(R.string.training_copy_day_text, dayName),
                onDismiss = close,
                onCopy = { targets ->
                    repository.copyTrainingDay(id, current.day, targets)
                    close()
                    messenger.show(context.getString(R.string.training_copy_done))
                },
            )
        }
        is TrainingEdit.Pick -> {
            val replacing = current.replacing
            ExercisePickerSheet(
                title = if (replacing == null) {
                    stringResource(R.string.training_add_exercise)
                } else {
                    stringResource(R.string.training_replace_title, exercises[replacing.exerciseId]?.name.orEmpty())
                },
                exercises = data.exercises,
                inSession = program.day(current.day).exercises.mapTo(HashSet()) { it.exerciseId },
                onDismiss = close,
                onPick = { picked(current.day, replacing, it.id) },
                onPickSuggestion = { suggestion ->
                    picked(current.day, replacing, repository.addExercise(suggestion.toDraft()))
                    messenger.show(context.getString(R.string.exercise_added, suggestion.name))
                },
                onCreate = { name -> edit = TrainingEdit.NewExercise(current.day, name, replacing) },
            )
        }
        is TrainingEdit.NewExercise -> ExerciseEditorSheet(
            initial = null,
            existingNames = data.exercises.map { it.name },
            restPresets = data.restPresets,
            initialName = current.name,
            // Once saved, the new exercise goes on to its session: only giving up closes everything.
            onDismiss = { if (edit === current) close() },
            onSave = { draft ->
                picked(current.day, current.replacing, repository.addExercise(draft))
                messenger.show(context.getString(R.string.exercise_added, draft.name))
            },
        )
        is TrainingEdit.Line -> key(current) {
            val exercise = data.exercise(current.exerciseId)
            val line = current.line
            if (exercise == null) {
                LaunchedEffect(Unit) { close() }
            } else {
                SessionLineDialog(
                    exercise = exercise,
                    dayName = current.day.getDisplayName(TextStyle.FULL, Fmt.locale),
                    line = line,
                    initialTarget = defaultTarget(program, exercise),
                    onDismiss = close,
                    onSave = { target, emphasis, note, rest ->
                        if (line == null) {
                            repository.addSessionExercise(id, current.day, exercise.id, target, emphasis, note, rest)
                        } else {
                            repository.updateSessionExercise(id, line.id, target, emphasis, note, rest)
                        }
                        close()
                    },
                    onMove = line?.let { { delta -> update { it.moveLine(line.id, delta) } } },
                    onReplace = line?.let { { edit = TrainingEdit.Pick(current.day, line) } },
                    onRemove = line?.let {
                        {
                            val index = program.day(current.day).exercises.indexOfFirst { it.id == line.id }
                            update { it.removeLine(line.id) }
                            close()
                            messenger.show(
                                context.getString(R.string.training_line_removed, exercise.name),
                                context.getString(R.string.action_undo),
                            ) {
                                repository.updateTrainingProgram(id) { program ->
                                    program.updateDay(current.day) { day ->
                                        day.copy(exercises = day.exercises.toMutableList().apply { add(index.coerceIn(0, size), line) })
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
        is TrainingEdit.Note -> {
            val note = current.note
            FormDialog(
                title = stringResource(if (note == null) R.string.plan_note_new else R.string.plan_note_edit),
                fields = listOf(
                    FormField(stringResource(R.string.plan_note_title), note?.title.orEmpty(), required = true),
                    FormField(stringResource(R.string.plan_note_text), note?.text.orEmpty(), singleLine = false),
                ),
                onDismiss = close,
                onConfirm = { (title, text) ->
                    if (note == null) {
                        repository.addTrainingNote(id, title, text)
                    } else {
                        update { it.copy(notes = it.notes.map { n -> if (n.id == note.id) PlanNote(n.id, title, text) else n }) }
                    }
                    close()
                },
                extraContent = if (note == null) {
                    null
                } else {
                    {
                        DialogAction(
                            text = stringResource(R.string.plan_note_delete),
                            icon = Icons.Rounded.DeleteOutline,
                            destructive = true,
                            onClick = {
                                update { it.copy(notes = it.notes.filterNot { n -> n.id == note.id }) }
                                close()
                            },
                        )
                    }
                },
            )
        }
    }

    if (showPrograms) {
        ProgramsSheet(
            infos = infos,
            today = today,
            onDismiss = { showPrograms = false },
            onEdit = { other ->
                programId = other.id
                showPrograms = false
            },
            onStart = { starting = it },
            onCancelStart = { info ->
                info.date?.let { repository.cancelTrainingProgramStart(info.plan.id, it) }
            },
            onDelete = { other ->
                val active = infos.any { it.plan.id == other.id && it.status == ProgramStatus.ACTIVE }
                if (active || data.trainingHistory(other.id)) {
                    messenger.show(context.getString(R.string.program_cannot_delete))
                } else {
                    deleting = other
                }
            },
            onNew = { creating = true },
        )
    }

    if (creating) {
        NewProgramDialog(
            current = program,
            today = today,
            nameHint = stringResource(R.string.training_program_name_hint),
            onDismiss = { creating = false },
            onCreate = { name, copyOf, start ->
                val newId = repository.createTrainingProgram(name, copyOf, start)
                creating = false
                showPrograms = false
                if (start != null) announceStart(messenger, context, name.trim(), start, today)
                programId = newId
            },
        )
    }

    starting?.let { other ->
        StartProgramDialog(
            plan = other,
            today = today,
            onDismiss = { starting = null },
            onStart = { start ->
                repository.startTrainingProgram(other.id, start)
                starting = null
                announceStart(messenger, context, other.name, start, today)
            },
        )
    }

    deleting?.let { other ->
        ConfirmDialog(
            title = stringResource(R.string.program_delete_title),
            text = stringResource(R.string.program_delete_text, other.name),
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Rounded.DeleteOutline,
            destructive = true,
            onConfirm = {
                if (repository.deleteTrainingProgram(other.id)) {
                    messenger.show(context.getString(R.string.program_deleted))
                    // The program shown is gone: the one followed today takes its place.
                    if (other.id == id) programId = null
                }
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
