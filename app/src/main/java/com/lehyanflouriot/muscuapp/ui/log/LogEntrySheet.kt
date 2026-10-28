package com.lehyanflouriot.muscuapp.ui.log

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.lastEntry
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.show
import com.lehyanflouriot.muscuapp.ui.rest.rememberRestStarter
import com.lehyanflouriot.muscuapp.ui.rest.rememberTimerNow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Why the log sheet is open. */
sealed interface LogRequest {
    data class New(
        val exerciseId: String,
        val date: LocalDate,
        /** The goal of the session of the program, to start from instead of the last time. */
        val goal: List<WorkSet>? = null,
        /** The rest the session asks between two sets. */
        val restSeconds: Int? = null,
        /** What the session asks: "4 × 6–12 · RIR 1 · Mains sur 2 livres". */
        val target: String? = null,
    ) : LogRequest

    data class Edit(val entryId: String) : LogRequest
}

private class LogTarget(val exercise: Exercise, val entry: WorkoutEntry?, val previous: WorkoutEntry?)

private fun resolve(request: LogRequest, data: AppData): LogTarget? {
    val entry = (request as? LogRequest.Edit)?.let { edit -> data.entries.firstOrNull { it.id == edit.entryId } }
    val exerciseId = entry?.exerciseId ?: (request as? LogRequest.New)?.exerciseId ?: return null
    val exercise = data.exercise(exerciseId) ?: return null
    val previous = lastEntry(data.entriesOf(exerciseId).filter { it.id != entry?.id })
    return LogTarget(exercise, entry, previous)
}

/** Bottom sheet to note (or correct) what was done for an exercise. */
@Composable
fun LogEntrySheet(
    request: LogRequest,
    data: AppData,
    repository: WorkoutRepository,
    timer: RestTimer,
    today: LocalDate,
    onDismiss: () -> Unit,
) {
    val target = remember(request) { resolve(request, data) }
    if (target == null) {
        LaunchedEffect(request) { onDismiss() }
        return
    }
    val exercise = target.exercise
    val entry = target.entry
    val new = request as? LogRequest.New
    val formState = remember(request) {
        LogFormState(
            initialDate = entry?.date ?: new?.date ?: today,
            initialSets = entry?.sets ?: new?.goal ?: target.previous?.sets.orEmpty(),
            initialNote = entry?.note.orEmpty(),
            measure = exercise.measure,
        )
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    val timerState by timer.state.collectAsStateWithLifecycle()
    val idleDuration by timer.idleDuration.collectAsStateWithLifecycle()
    val now = rememberTimerNow(timerState, timer::now)
    val startRest = rememberRestStarter(timer)
    val restSeconds = new?.restSeconds ?: exercise.restSeconds ?: (idleDuration / 1_000).toInt()
    val restText = when (val state = timerState) {
        is TimerState.Running -> Fmt.clockMillis(TimerLogic.remaining(state, now))
        is TimerState.Paused -> Fmt.clockMillis(state.remaining)
        else -> stringResource(R.string.log_rest_button, Fmt.clock(restSeconds.toLong()))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LogEntryForm(
            exercise = exercise,
            state = formState,
            today = today,
            isEdit = entry != null,
            previous = target.previous,
            sessionTarget = new?.target,
            weightStep = data.settings.weightStep,
            restButtonText = restText,
            onRestClick = { startRest(restSeconds, exercise.name) },
            onSave = {
                val change = repository.saveEntry(entry?.id, exercise.id, formState.date, formState.sets, formState.note)
                val message = context.getString(if (entry == null) R.string.log_saved else R.string.log_updated)
                if (change.changed) {
                    messenger.show(message, context.getString(R.string.action_undo)) { repository.undo(change) }
                } else {
                    messenger.show(message)
                }
                close()
            },
            onDelete = entry?.let { existing ->
                {
                    val deleted = repository.deleteEntry(existing.id)
                    if (deleted != null) {
                        messenger.show(
                            context.getString(R.string.log_deleted),
                            context.getString(R.string.action_undo),
                        ) { repository.restoreEntry(deleted) }
                    }
                    close()
                }
            },
        )
    }
}
