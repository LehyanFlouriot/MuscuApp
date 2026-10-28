package com.lehyanflouriot.muscuapp.ui.rest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.model.RestPreset
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.timer.RestTimer
import com.lehyanflouriot.muscuapp.ui.common.DurationPickerDialog
import com.lehyanflouriot.muscuapp.ui.common.RestPresetDialog

private sealed interface PresetEdit {
    data object New : PresetEdit
    data class Existing(val preset: RestPreset) : PresetEdit
}

@Composable
fun RestRoute(
    presets: List<RestPreset>,
    repository: WorkoutRepository,
    timer: RestTimer,
    onBack: () -> Unit,
) {
    val rawState by timer.state.collectAsStateWithLifecycle()
    val idleDuration by timer.idleDuration.collectAsStateWithLifecycle()
    val now = rememberTimerNow(rawState, timer::now)
    val state = rawState.displayed(now)
    val notificationsEnabled = rememberNotificationsEnabled()
    val enableNotifications = rememberNotificationEnabler()
    val startRest = rememberRestStarter(timer)
    var presetEdit by remember { mutableStateOf<PresetEdit?>(null) }
    var editingIdleDuration by remember { mutableStateOf(false) }

    KeepScreenOn(active = state is TimerState.Running)

    RestScreen(
        state = state,
        now = now,
        idleDurationMs = idleDuration,
        presets = presets,
        notificationsEnabled = notificationsEnabled,
        onToggle = {
            if (state is TimerState.Running || state is TimerState.Paused) {
                timer.toggle()
            } else {
                startRest((idleDuration / 1_000).toInt(), null)
            }
        },
        onAdjust = { delta ->
            if (state is TimerState.Running || state is TimerState.Paused) {
                timer.adjust(delta)
            } else {
                timer.setIdleDuration(idleDuration + delta)
            }
        },
        onStop = timer::stop,
        onRestart = {
            val total = (rawState as? TimerState.Finished)?.total ?: idleDuration
            startRest((total / 1_000).toInt(), (rawState as? TimerState.Finished)?.label)
        },
        onStartPreset = { startRest(it.seconds, null) },
        onAddPreset = { presetEdit = PresetEdit.New },
        onEditPreset = { presetEdit = PresetEdit.Existing(it) },
        onEnableNotifications = enableNotifications,
        onEditIdleDuration = { editingIdleDuration = true },
        onBack = onBack,
    )

    when (val edit = presetEdit) {
        null -> Unit
        PresetEdit.New -> RestPresetDialog(
            title = stringResource(R.string.preset_new_title),
            initialSeconds = (idleDuration / 1_000).toInt(),
            initialLabel = "",
            onDismiss = { presetEdit = null },
            onConfirm = { seconds, label ->
                repository.addPreset(seconds, label)
                presetEdit = null
            },
        )
        is PresetEdit.Existing -> RestPresetDialog(
            title = stringResource(R.string.preset_edit_title),
            initialSeconds = edit.preset.seconds,
            initialLabel = edit.preset.label,
            onDismiss = { presetEdit = null },
            onConfirm = { seconds, label ->
                repository.updatePreset(edit.preset.id, seconds, label)
                presetEdit = null
            },
            onDelete = {
                repository.deletePreset(edit.preset.id)
                presetEdit = null
            },
        )
    }

    if (editingIdleDuration) {
        DurationPickerDialog(
            title = stringResource(R.string.rest_change_duration),
            initialSeconds = (idleDuration / 1_000).toInt(),
            onDismiss = { editingIdleDuration = false },
            onConfirm = { seconds ->
                timer.setIdleDuration(seconds * 1_000L)
                editingIdleDuration = false
            },
        )
    }
}
