package com.lehyanflouriot.muscuapp.ui.exercise

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.ChartMetric
import com.lehyanflouriot.muscuapp.core.domain.chartMetrics
import com.lehyanflouriot.muscuapp.core.domain.history
import com.lehyanflouriot.muscuapp.core.domain.metricValue
import com.lehyanflouriot.muscuapp.core.domain.records
import com.lehyanflouriot.muscuapp.core.domain.weeklySeries
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.ui.common.ConfirmDialog
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.show
import com.lehyanflouriot.muscuapp.ui.log.LogEntrySheet
import com.lehyanflouriot.muscuapp.ui.log.LogRequest
import com.lehyanflouriot.muscuapp.ui.rest.rememberRestStarter

private const val CHART_WEEKS = 12

@Composable
fun ExerciseDetailRoute(
    exerciseId: String,
    data: AppData,
    repository: WorkoutRepository,
    timer: RestTimer,
    onBack: () -> Unit,
) {
    val exercise = data.exercise(exerciseId)
    if (exercise == null) {
        // Deleted (or unknown): nothing to show.
        LaunchedEffect(exerciseId) { onBack() }
        return
    }
    val today = rememberToday()
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val entries = remember(data.entries, exerciseId) { data.entriesOf(exerciseId) }
    val weeks = remember(entries) { history(entries) }
    val records = remember(exercise, entries) { records(exercise, entries) }
    val metrics = chartMetrics(exercise)
    var metricName by rememberSaveable(exerciseId) { mutableStateOf(metrics.first().name) }
    val metric = ChartMetric.valueOf(metricName).takeIf { it in metrics } ?: metrics.first()
    val series = remember(entries, metric, today) {
        weeklySeries(entries, today, CHART_WEEKS) { metricValue(it, metric) }
    }
    val idleDuration by timer.idleDuration.collectAsStateWithLifecycle()
    val restSeconds = exercise.restSeconds ?: (idleDuration / 1_000).toInt()
    val startRest = rememberRestStarter(timer)

    var logRequest by remember { mutableStateOf<LogRequest?>(null) }
    var editing by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    ExerciseDetailScreen(
        exercise = exercise,
        weeks = weeks,
        records = records,
        metric = metric,
        series = series,
        today = today,
        restLabel = stringResource(R.string.log_rest_button, Fmt.clock(restSeconds.toLong())),
        onMetricChange = { metricName = it.name },
        onBack = onBack,
        onEdit = { editing = true },
        onToggleArchive = {
            val archive = !exercise.archived
            repository.setExerciseArchived(exercise.id, archive)
            messenger.show(
                context.getString(if (archive) R.string.detail_archived_message else R.string.detail_unarchived_message),
                context.getString(R.string.action_undo),
            ) { repository.setExerciseArchived(exercise.id, !archive) }
        },
        onDelete = { confirmingDelete = true },
        onLog = { logRequest = LogRequest.New(exercise.id, today) },
        onStartRest = { startRest(restSeconds, exercise.name) },
        onEditEntry = { logRequest = LogRequest.Edit(it.id) },
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

    if (editing) {
        ExerciseEditorSheet(
            initial = exercise,
            existingNames = data.exercises.map { it.name },
            restPresets = data.restPresets,
            onDismiss = { editing = false },
            onSave = { draft -> repository.updateExercise(exercise.id, draft) },
        )
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_title, exercise.name),
            text = stringResource(R.string.detail_delete_text),
            confirmLabel = stringResource(R.string.action_delete),
            icon = Icons.Rounded.Delete,
            destructive = true,
            onDismiss = { confirmingDelete = false },
            onConfirm = {
                confirmingDelete = false
                val deleted = repository.deleteExercise(exercise.id)
                if (deleted != null) {
                    messenger.show(
                        context.getString(R.string.detail_deleted_message, deleted.exercise.name),
                        context.getString(R.string.action_undo),
                    ) { repository.restoreExercise(deleted) }
                }
                onBack()
            },
        )
    }
}
