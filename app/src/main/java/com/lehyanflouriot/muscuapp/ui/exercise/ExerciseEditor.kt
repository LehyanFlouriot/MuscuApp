package com.lehyanflouriot.muscuapp.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.ExerciseCatalog
import com.lehyanflouriot.muscuapp.core.data.ExerciseDraft
import com.lehyanflouriot.muscuapp.core.data.ExerciseSuggestion
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.RestPreset
import com.lehyanflouriot.muscuapp.ui.common.DurationPickerDialog
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import kotlinx.coroutines.launch

private const val DEFAULT_REST_SECONDS = 90

@Stable
class ExerciseFormState(initial: Exercise?, initialName: String = "") {
    var name by mutableStateOf(initial?.name ?: initialName)
    var muscleGroup by mutableStateOf(initial?.muscleGroup ?: MuscleGroup.OTHER)
    var usesWeight by mutableStateOf(initial?.usesWeight ?: true)
    var measure by mutableStateOf(initial?.measure ?: Measure.REPS)
    var restSeconds by mutableStateOf(if (initial == null) DEFAULT_REST_SECONDS else initial.restSeconds)
    var notes by mutableStateOf(initial?.notes.orEmpty())

    fun apply(suggestion: ExerciseSuggestion) {
        name = suggestion.name
        muscleGroup = suggestion.muscleGroup
        usesWeight = suggestion.usesWeight
        measure = suggestion.measure
        restSeconds = suggestion.restSeconds
    }

    fun toDraft(): ExerciseDraft = ExerciseDraft(
        name = name.trim(),
        muscleGroup = muscleGroup,
        usesWeight = usesWeight,
        measure = measure,
        restSeconds = restSeconds,
        notes = notes.trim(),
    )
}

@Composable
fun ExerciseEditorSheet(
    initial: Exercise?,
    existingNames: List<String>,
    restPresets: List<RestPreset>,
    onDismiss: () -> Unit,
    onSave: (ExerciseDraft) -> Unit,
    /** The name a new exercise starts with. */
    initialName: String = "",
) {
    val state = remember(initial?.id) { ExerciseFormState(initial, initialName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ExerciseEditorForm(
            state = state,
            isNew = initial == null,
            existingNames = existingNames.filterNot { it == initial?.name },
            restPresets = restPresets,
            onCancel = close,
            onSave = {
                onSave(state.toDraft())
                close()
            },
        )
    }
}

@Composable
fun ExerciseEditorForm(
    state: ExerciseFormState,
    isNew: Boolean,
    existingNames: List<String>,
    restPresets: List<RestPreset>,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedName = ExerciseCatalog.normalize(state.name)
    val duplicate = normalizedName.isNotEmpty() && existingNames.any { ExerciseCatalog.normalize(it) == normalizedName }
    val canSave = state.name.isNotBlank() && !duplicate
    var pickingRest by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(if (isNew) R.string.editor_new_title else R.string.editor_edit_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = state.name,
            onValueChange = { state.name = it.take(60) },
            label = { Text(stringResource(R.string.editor_name)) },
            placeholder = { Text(stringResource(R.string.editor_name_placeholder)) },
            singleLine = true,
            isError = duplicate,
            supportingText = if (duplicate) {
                { Text(stringResource(R.string.editor_duplicate)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (isNew) {
            val suggestions = remember(state.name, existingNames) {
                ExerciseCatalog.search(state.name, existingNames).filterNot {
                    ExerciseCatalog.normalize(it.name) == normalizedName
                }
            }
            if (suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.editor_suggestions),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { state.apply(suggestion) },
                                label = { Text(suggestion.name) },
                            )
                        }
                    }
                }
            }
        }

        FieldTitle(stringResource(R.string.editor_group))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuscleGroup.entries.forEach { group ->
                FilterChip(
                    selected = state.muscleGroup == group,
                    onClick = { state.muscleGroup = group },
                    label = { Text(stringResource(group.labelRes)) },
                )
            }
        }

        FieldTitle(stringResource(R.string.editor_measure))
        val measures = Measure.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            measures.forEachIndexed { index, measure ->
                SegmentedButton(
                    selected = state.measure == measure,
                    onClick = { state.measure = measure },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = measures.size),
                ) {
                    Text(stringResource(if (measure == Measure.SECONDS) R.string.editor_measure_seconds else R.string.editor_measure_reps))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.editor_weight), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.editor_weight_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = state.usesWeight, onCheckedChange = { state.usesWeight = it })
        }

        FieldTitle(stringResource(R.string.editor_rest))
        val restOptions = remember(restPresets, state.restSeconds) {
            (restPresets.map { it.seconds } + listOfNotNull(state.restSeconds)).distinct().sorted()
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.restSeconds == null,
                onClick = { state.restSeconds = null },
                label = { Text(stringResource(R.string.editor_rest_none)) },
            )
            restOptions.forEach { seconds ->
                FilterChip(
                    selected = state.restSeconds == seconds,
                    onClick = { state.restSeconds = seconds },
                    label = { Text(Fmt.clock(seconds.toLong())) },
                    leadingIcon = if (state.restSeconds == seconds) {
                        { Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
            FilterChip(
                selected = false,
                onClick = { pickingRest = true },
                label = { Text(stringResource(R.string.editor_rest_other)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }

        OutlinedTextField(
            value = state.notes,
            onValueChange = { state.notes = it.take(500) },
            label = { Text(stringResource(R.string.editor_notes)) },
            placeholder = { Text(stringResource(R.string.editor_notes_placeholder)) },
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) { Text(stringResource(R.string.action_cancel)) }
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) { Text(stringResource(if (isNew) R.string.editor_create else R.string.action_save)) }
        }
    }

    if (pickingRest) {
        DurationPickerDialog(
            title = stringResource(R.string.editor_rest),
            initialSeconds = state.restSeconds ?: DEFAULT_REST_SECONDS,
            onDismiss = { pickingRest = false },
            onConfirm = {
                state.restSeconds = it
                pickingRest = false
            },
        )
    }
}

@Composable
private fun FieldTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}
