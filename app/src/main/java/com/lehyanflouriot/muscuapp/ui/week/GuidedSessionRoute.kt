package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import com.lehyanflouriot.muscuapp.core.domain.sessionDuration
import com.lehyanflouriot.muscuapp.core.domain.sessionStartedAt
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.WarmUp
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.ui.common.DialogAction
import com.lehyanflouriot.muscuapp.ui.common.FormDialog
import com.lehyanflouriot.muscuapp.ui.common.FormField
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.parseDecimal
import com.lehyanflouriot.muscuapp.ui.common.parseWholeNumber
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.repsLabel
import com.lehyanflouriot.muscuapp.ui.rest.KeepScreenOn
import com.lehyanflouriot.muscuapp.ui.rest.displayed
import com.lehyanflouriot.muscuapp.ui.rest.rememberRestStarter
import com.lehyanflouriot.muscuapp.ui.rest.rememberTimerNow
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Time added to the rest by its "+15 s" button. */
private const val REST_ADD_MS = 15_000L

/** How often the countdown of the mobility moves, and how late its end still buzzes. */
private const val MOBILITY_TICK_MS = 250L
private const val MOBILITY_BUZZ_LATE_MS = 2_000L

/** How often the time the session has taken moves: it shows in minutes. */
private const val CLOCK_TICK_MS = 10_000L

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
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.action_undo)
    // The exercise chosen by the user, or else the first one with sets still to do. A session
    // already done is looked over from its first exercise.
    var pickedLine by rememberSaveable { mutableStateOf(session.items.first().line.id.takeIf { session.complete }) }
    val index = session.items.indexOfFirst { it.line.id == pickedLine }.takeIf { it >= 0 }
        ?: session.currentIndex()
        ?: session.items.lastIndex
    val item = session.items[index]
    // Values changed for the next set of each exercise, until it is done.
    val drafts = remember { mutableStateMapOf<String, WorkSet>() }
    val draft = drafts[item.line.id] ?: item.nextSetGoal()
    var editing by remember { mutableStateOf<EditedSet?>(null) }

    // The warm-up comes first, in a session started today with nothing noted yet.
    val program = session.program
    val warmUp = program?.warmUp ?: WarmUp(enabled = false)
    val hasWarmUp = warmUp.isActive && date == today
    var warmingUp by rememberSaveable { mutableStateOf(hasWarmUp && session.doneSets().isEmpty()) }
    var mobilityStartedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var lightSetsDone by rememberSaveable { mutableIntStateOf(0) }
    var editingWarmUp by remember { mutableStateOf(false) }

    // The clock of the screen: the countdown of the mobility, and the time the session has taken.
    var clockNow by remember { mutableLongStateOf(timer.now()) }
    LaunchedEffect(warmingUp) {
        while (true) {
            clockNow = timer.now()
            delay(if (warmingUp) MOBILITY_TICK_MS else CLOCK_TICK_MS)
        }
    }
    // The end of the mobility buzzes once, when it comes, not when the screen comes back long after.
    val mobilityEnd = mobilityStartedAt?.let { it + warmUp.mobilityMinutes * 60_000L }
    LaunchedEffect(mobilityEnd) {
        val wait = (mobilityEnd ?: return@LaunchedEffect) - timer.now()
        if (wait < -MOBILITY_BUZZ_LATE_MS) return@LaunchedEffect
        if (wait > 0) delay(wait)
        buzz(context)
    }
    // The session is timed from its opening, warm-up included, when nothing is noted yet: once,
    // even when the screen is made again (a turn of the phone).
    var clockStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!clockStarted && date == today && session.doneSets().isEmpty()) repository.startSessionClock(date)
        clockStarted = true
    }
    val duration = data.training.sessionDuration(date)
    val startedAt = data.training.sessionStartedAt(date)
    val elapsed = when {
        session.complete -> duration
        startedAt != null && date == today -> clockNow - startedAt
        else -> duration
    }

    GuidedSessionScreen(
        session = session,
        index = index,
        draft = draft,
        timerState = rawTimerState.displayed(now),
        now = now,
        today = today,
        weightStep = data.settings.weightStep,
        warmUp = if (warmingUp && hasWarmUp) {
            WarmUpView(
                warmUp = warmUp,
                emphasis = session.items.firstOrNull { it.line.emphasis },
                first = session.items.first(),
                mobilityStartedAt = mobilityStartedAt,
                now = clockNow,
                lightSetsDone = lightSetsDone,
            )
        } else {
            null
        },
        warmUpActions = WarmUpActions(
            onStartMobility = {
                clockNow = timer.now()
                mobilityStartedAt = clockNow
            },
            onStopMobility = { mobilityStartedAt = null },
            onLightSet = { set -> lightSetsDone = if (set < lightSetsDone) set else set + 1 },
            onDone = {
                warmingUp = false
                pickedLine = null
            },
            onEdit = { editingWarmUp = true },
        ),
        hasWarmUp = hasWarmUp,
        elapsed = elapsed,
        duration = duration,
        actions = GuidedActions(
            onSelect = { selected ->
                if (selected < 0) {
                    if (hasWarmUp) warmingUp = true
                } else {
                    session.items.getOrNull(selected)?.let {
                        pickedLine = it.line.id
                        warmingUp = false
                    }
                }
            },
            onDraftChange = { drafts[item.line.id] = it },
            onSetDone = {
                val line = item.line.id
                val set = draft
                val number = item.doneSets().size + 1
                val change = repository.addSet(item.exercise.id, date, set)
                drafts.remove(line)
                val exerciseDone = number >= item.plannedSets
                val sessionDone = exerciseDone && session.items.all { it === item || it.complete }
                if (!sessionDone) {
                    val rest = item.line.restSeconds ?: item.exercise.restSeconds ?: (idleDuration / 1_000).toInt()
                    startRest(rest, item.exercise.name)
                }
                // Its last set done, the exercise gives way to the next one with sets to do, if any.
                if (exerciseDone && !sessionDone) pickedLine = null
                if (change != null) {
                    messenger.show(context.getString(R.string.guided_set_saved, number), undoLabel) {
                        if (repository.undo(change)) {
                            // Back to the set as it was before it was noted, without the rest it started.
                            drafts[line] = set
                            pickedLine = line
                            val restState = timer.state.value
                            val resting = restState is TimerState.Running || restState is TimerState.Paused
                            if (resting && TimerLogic.label(restState) == item.exercise.name) timer.stop()
                        }
                    }
                }
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

    if (editingWarmUp) {
        WarmUpDialog(
            initial = warmUp,
            onDismiss = { editingWarmUp = false },
            onSave = {
                if (program != null) repository.updateWarmUp(program.id, it)
                editingWarmUp = false
            },
        )
    }

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
                val change = repository.updateSet(edited.done.entryId, edited.done.index, WorkSet(reps, weight))
                editing = null
                if (change != null && change.changed) {
                    messenger.show(context.getString(R.string.guided_set_updated, edited.number), undoLabel) { repository.undo(change) }
                }
            },
            extraContent = {
                DialogAction(
                    text = stringResource(R.string.guided_delete_set),
                    icon = Icons.Rounded.DeleteOutline,
                    destructive = true,
                    onClick = {
                        val change = repository.updateSet(edited.done.entryId, edited.done.index, null)
                        editing = null
                        if (change != null) {
                            messenger.show(context.getString(R.string.guided_set_deleted, edited.number), undoLabel) { repository.undo(change) }
                        }
                    },
                )
            },
        )
    }
}
