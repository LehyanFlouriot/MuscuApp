package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.DoneSet
import com.lehyanflouriot.muscuapp.core.domain.buildSession
import com.lehyanflouriot.muscuapp.core.domain.complete
import com.lehyanflouriot.muscuapp.core.domain.currentIndex
import com.lehyanflouriot.muscuapp.core.domain.doneSets
import com.lehyanflouriot.muscuapp.core.domain.nextSetGoal
import com.lehyanflouriot.muscuapp.core.domain.plannedSets
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.ui.common.DialogAction
import com.lehyanflouriot.muscuapp.ui.common.FormDialog
import com.lehyanflouriot.muscuapp.ui.common.FormField
import com.lehyanflouriot.muscuapp.ui.common.parseDecimal
import com.lehyanflouriot.muscuapp.ui.common.parseWholeNumber
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.repsLabel
import com.lehyanflouriot.muscuapp.ui.rest.KeepScreenOn
import com.lehyanflouriot.muscuapp.ui.rest.displayed
import com.lehyanflouriot.muscuapp.ui.rest.rememberRestStarter
import com.lehyanflouriot.muscuapp.ui.rest.rememberTimerNow
import java.time.LocalDate

/** Time added to the rest by its "+15 s" button. */
private const val REST_ADD_MS = 15_000L

/** A set done, being corrected. */
private class EditedSet(val done: DoneSet, val number: Int, val exercise: Exercise)

@Composable
fun GuidedSessionRoute(
    date: LocalDate,
    data: AppData,
    repository: WorkoutRepository,
    timer: RestTimer,
    onBack: () -> Unit,
) {
    val session = remember(data, date) { buildSession(data, date) }
    if (session == null || session.isRest) {
        // No session that day any more (program changed, backup restored...).
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val today = rememberToday()
    // The phone stays awake during the session: the rest and the next set are always in sight.
    KeepScreenOn(true)
    val rawTimerState by timer.state.collectAsStateWithLifecycle()
    val idleDuration by timer.idleDuration.collectAsStateWithLifecycle()
    val now = rememberTimerNow(rawTimerState, timer::now)
    val startRest = rememberRestStarter(timer)
    // The exercise chosen by the user, or else the first one with sets still to do.
    var pickedLine by rememberSaveable { mutableStateOf<String?>(null) }
    val index = session.items.indexOfFirst { it.line.id == pickedLine }.takeIf { it >= 0 }
        ?: session.currentIndex()
        ?: session.items.lastIndex
    val item = session.items[index]
    // Values changed for the next set of each exercise, until it is done.
    val drafts = remember { mutableStateMapOf<String, WorkSet>() }
    val draft = drafts[item.line.id] ?: item.nextSetGoal()
    var editing by remember { mutableStateOf<EditedSet?>(null) }

    GuidedSessionScreen(
        session = session,
        index = index,
        draft = draft,
        timerState = rawTimerState.displayed(now),
        now = now,
        today = today,
        weightStep = data.settings.weightStep,
        actions = GuidedActions(
            onSelect = { selected -> session.items.getOrNull(selected)?.let { pickedLine = it.line.id } },
            onDraftChange = { drafts[item.line.id] = it },
            onSetDone = {
                repository.addSet(item.exercise.id, date, draft)
                drafts.remove(item.line.id)
                val exerciseDone = item.doneSets().size + 1 >= item.plannedSets
                val sessionDone = exerciseDone && session.items.all { it === item || it.complete }
                if (!sessionDone) {
                    val rest = item.line.restSeconds ?: item.exercise.restSeconds ?: (idleDuration / 1_000).toInt()
                    startRest(rest, item.exercise.name)
                }
                // Its last set done, the exercise gives way to the next one with sets to do.
                if (exerciseDone) pickedLine = null
            },
            onEditSet = { done, number -> editing = EditedSet(done, number, item.exercise) },
            onRestAdd = { timer.adjust(REST_ADD_MS) },
            onRestToggle = {
                when (rawTimerState) {
                    is TimerState.Running -> timer.pause()
                    is TimerState.Paused -> timer.resume()
                    else -> Unit
                }
            },
            onRestSkip = timer::stop,
            onClose = onBack,
        ),
    )

    editing?.let { edited ->
        val exercise = edited.exercise
        val set = edited.done.set
        val repsRange = 1..999
        val weightValid = { text: String -> parseDecimal(text)?.takeIf { it in 0.0..999.0 } }
        FormDialog(
            title = stringResource(R.string.guided_set, edited.number),
            fields = buildList {
                add(
                    FormField(
                        label = repsLabel(exercise.measure),
                        initial = set.reps.toString(),
                        keyboardType = KeyboardType.Number,
                        required = true,
                        isValid = { parseWholeNumber(it, repsRange) != null },
                    ),
                )
                if (exercise.usesWeight) {
                    add(
                        FormField(
                            label = stringResource(R.string.field_weight_kg),
                            initial = Fmt.weight(set.weight),
                            keyboardType = KeyboardType.Decimal,
                            required = true,
                            isValid = { weightValid(it) != null },
                        ),
                    )
                }
            },
            onDismiss = { editing = null },
            onConfirm = { values ->
                val reps = parseWholeNumber(values[0], repsRange) ?: set.reps
                val weight = values.getOrNull(1)?.let(weightValid) ?: set.weight
                repository.updateSet(edited.done.entryId, edited.done.index, WorkSet(reps, weight))
                editing = null
            },
            extraContent = {
                DialogAction(
                    text = stringResource(R.string.guided_delete_set),
                    icon = Icons.Rounded.DeleteOutline,
                    destructive = true,
                    onClick = {
                        repository.updateSet(edited.done.entryId, edited.done.index, null)
                        editing = null
                    },
                )
            },
        )
    }
}
