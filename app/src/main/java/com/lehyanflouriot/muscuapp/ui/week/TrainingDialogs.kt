package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.ExerciseCatalog
import com.lehyanflouriot.muscuapp.core.data.ExerciseSuggestion
import com.lehyanflouriot.muscuapp.core.data.MAX_RIR
import com.lehyanflouriot.muscuapp.core.data.MAX_TARGET_REPS
import com.lehyanflouriot.muscuapp.core.data.MAX_TARGET_SETS
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.ui.common.CompactStepper
import com.lehyanflouriot.muscuapp.ui.common.DialogAction
import com.lehyanflouriot.muscuapp.ui.common.DurationPickerDialog
import com.lehyanflouriot.muscuapp.ui.common.ExerciseAvatar
import com.lehyanflouriot.muscuapp.ui.common.NumberInputDialog
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.parseWholeNumber

/** Rest offered when neither the session nor the exercise has one. */
private const val DEFAULT_REST_SECONDS = 90

/** What is being typed in the dialog of an exercise of a session, until it is saved. */
@Stable
class SessionLineState(line: SessionExercise?, initialTarget: SetTarget) {
    private val target = line?.target ?: initialTarget
    var sets by mutableIntStateOf(target.sets)
    var min by mutableStateOf(target.min.toString())
    var max by mutableStateOf(if (target.max == target.min) "" else target.max.toString())
    var per by mutableStateOf(target.per)
    var rir by mutableStateOf(target.rir)
    var rest by mutableStateOf(line?.restSeconds)
    var note by mutableStateOf(line?.note.orEmpty())
    var emphasis by mutableStateOf(line?.emphasis ?: false)

    val low: Int? get() = parseWholeNumber(min, 1..MAX_TARGET_REPS)
    val high: Int? get() = if (max.isBlank()) low else parseWholeNumber(max, 1..MAX_TARGET_REPS)

    /** What the session asks, or null while the range typed is not valid. */
    fun target(): SetTarget? {
        val low = low ?: return null
        val high = high ?: return null
        return SetTarget(sets, low, maxOf(low, high), per.trim(), rir)
    }
}

/**
 * What a session asks for one of its exercises: how many sets, in which range of reps (or
 * seconds), how many reps to keep in reserve, the rest between sets, and how to do it.
 */
@Composable
fun SessionLineDialog(
    exercise: Exercise,
    dayName: String,
    line: SessionExercise?,
    initialTarget: SetTarget,
    onDismiss: () -> Unit,
    onSave: (target: SetTarget, emphasis: Boolean, note: String, restSeconds: Int?) -> Unit,
    onMove: ((Int) -> Unit)? = null,
    onReplace: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val state = remember { SessionLineState(line, initialTarget) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            SessionLineForm(
                state = state,
                exercise = exercise,
                dayName = dayName,
                onMove = onMove,
                onReplace = onReplace,
                onRemove = onRemove,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            val target = state.target()
            TextButton(
                onClick = { target?.let { onSave(it, state.emphasis, state.note.trim(), state.rest) } },
                enabled = target != null,
            ) {
                Text(stringResource(if (line == null) R.string.training_line_add else R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** The fields of the dialog of an exercise of a session, and what can be done with the exercise. */
@Composable
fun SessionLineForm(
    state: SessionLineState,
    exercise: Exercise,
    dayName: String,
    modifier: Modifier = Modifier,
    onMove: ((Int) -> Unit)? = null,
    onReplace: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    var typing by remember { mutableStateOf<String?>(null) }
    var pickingRest by remember { mutableStateOf(false) }
    val setsLabel = stringResource(R.string.field_sets)
    val rirLabel = stringResource(R.string.training_line_rir)
    val low = state.low
    val high = state.high
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.training_line_scope, dayName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepperRow(label = setsLabel) {
            CompactStepper(
                value = state.sets.toString(),
                description = setsLabel,
                onDecrement = { state.sets = (state.sets - 1).coerceAtLeast(1) },
                onIncrement = { state.sets = (state.sets + 1).coerceAtMost(MAX_TARGET_SETS) },
                onValueClick = { typing = setsLabel },
                canDecrement = state.sets > 1,
                modifier = Modifier.width(140.dp),
            )
        }
        Text(
            text = stringResource(
                if (exercise.measure == Measure.SECONDS) R.string.training_line_seconds else R.string.training_line_reps,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                value = state.min,
                onValueChange = { state.min = it },
                label = stringResource(R.string.training_line_min),
                isError = state.min.isNotBlank() && low == null,
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = state.max,
                onValueChange = { state.max = it },
                label = stringResource(R.string.training_line_max),
                placeholder = low?.toString(),
                isError = state.max.isNotBlank() && high == null,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = state.per,
            onValueChange = { state.per = it },
            label = { Text(stringResource(R.string.training_line_per)) },
            placeholder = { Text(stringResource(R.string.training_line_per_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        StepperRow(label = rirLabel, hint = stringResource(R.string.training_line_rir_hint)) {
            CompactStepper(
                value = state.rir?.toString() ?: "–",
                description = rirLabel,
                // Below 0, no reserve is asked any more.
                onDecrement = { state.rir = state.rir?.let { if (it == 0) null else it - 1 } },
                onIncrement = { state.rir = ((state.rir ?: -1) + 1).coerceAtMost(MAX_RIR) },
                onValueClick = { typing = rirLabel },
                canDecrement = state.rir != null,
                modifier = Modifier.width(140.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { pickingRest = true }
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.training_line_rest), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = state.rest?.let(Fmt::duration)
                        ?: exercise.restSeconds?.let { stringResource(R.string.training_line_rest_default, Fmt.duration(it)) }
                        ?: stringResource(R.string.training_line_rest_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.rest != null) {
                IconButton(onClick = { state.rest = null }) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.training_line_rest_reset))
                }
            } else {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = state.note,
            onValueChange = { state.note = it },
            label = { Text(stringResource(R.string.training_line_note)) },
            placeholder = { Text(stringResource(R.string.training_line_note_hint)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = state.emphasis, role = Role.Switch, onValueChange = { state.emphasis = it })
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.training_line_emphasis), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.training_line_emphasis_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = state.emphasis, onCheckedChange = null)
        }
        if (onMove != null || onReplace != null || onRemove != null) {
            Column {
                if (onMove != null) {
                    Row {
                        DialogAction(stringResource(R.string.nutrition_move_up), Icons.Rounded.ArrowUpward, { onMove(-1) })
                        DialogAction(stringResource(R.string.nutrition_move_down), Icons.Rounded.ArrowDownward, { onMove(1) })
                    }
                }
                if (onReplace != null) {
                    DialogAction(stringResource(R.string.training_line_replace), Icons.Rounded.SwapHoriz, onReplace)
                }
                if (onRemove != null) {
                    DialogAction(stringResource(R.string.training_line_remove), Icons.Rounded.DeleteOutline, onRemove, destructive = true)
                }
            }
        }
    }

    typing?.let { field ->
        val forSets = field == setsLabel
        NumberInputDialog(
            title = field,
            initialValue = (if (forSets) state.sets else state.rir)?.toString().orEmpty(),
            allowDecimal = false,
            valueRange = if (forSets) 1.0..MAX_TARGET_SETS.toDouble() else 0.0..MAX_RIR.toDouble(),
            onDismiss = { typing = null },
            onConfirm = { value ->
                if (forSets) state.sets = value.toInt() else state.rir = value.toInt()
                typing = null
            },
        )
    }
    if (pickingRest) {
        DurationPickerDialog(
            title = stringResource(R.string.training_line_rest),
            initialSeconds = state.rest ?: exercise.restSeconds ?: DEFAULT_REST_SECONDS,
            onDismiss = { pickingRest = false },
            onConfirm = {
                state.rest = it
                pickingRest = false
            },
        )
    }
}

/** A label, with an optional hint under it, and its stepper at the end of the row. */
@Composable
private fun StepperRow(label: String, hint: String? = null, stepper: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        stepper()
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

/** Chooses an exercise: one of the user's, a common one added on the way, or a new one. */
@Composable
fun ExercisePickerSheet(
    title: String,
    exercises: List<Exercise>,
    inSession: Set<String>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
    onPickSuggestion: (ExerciseSuggestion) -> Unit,
    onCreate: (name: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val needle = ExerciseCatalog.normalize(query)
    val mine = remember(exercises, needle) {
        exercises
            .filter { !it.archived && ExerciseCatalog.normalize(it.name).contains(needle) }
            .sortedBy { ExerciseCatalog.normalize(it.name) }
    }
    val suggestions = remember(exercises, query) { ExerciseCatalog.search(query, exercises.map { it.name }, limit = 6) }
    val known = needle.isNotEmpty() && exercises.any { ExerciseCatalog.normalize(it.name) == needle }
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.training_pick_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                if (!known) {
                    item(key = "create") {
                        ListItem(
                            headlineContent = {
                                Text(
                                    if (needle.isEmpty()) {
                                        stringResource(R.string.training_pick_new)
                                    } else {
                                        stringResource(R.string.training_pick_create, query.trim())
                                    },
                                )
                            },
                            leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = transparent,
                            modifier = Modifier.clickable { onCreate(query.trim()) },
                        )
                    }
                }
                if (mine.isNotEmpty()) {
                    item(key = "mine") { PickerHeader(stringResource(R.string.training_pick_mine)) }
                    items(mine, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                Text(
                                    if (exercise.id in inSession) {
                                        stringResource(R.string.training_pick_in_session)
                                    } else {
                                        stringResource(exercise.muscleGroup.labelRes)
                                    },
                                )
                            },
                            leadingContent = { ExerciseAvatar(exercise, size = 40.dp) },
                            colors = transparent,
                            modifier = Modifier.clickable { onPick(exercise) },
                        )
                    }
                }
                if (suggestions.isNotEmpty()) {
                    item(key = "catalog") { PickerHeader(stringResource(R.string.training_pick_catalog)) }
                    items(suggestions, key = { "catalog-${it.name}" }) { suggestion ->
                        val preview = Exercise(
                            id = "",
                            name = suggestion.name,
                            muscleGroup = suggestion.muscleGroup,
                            usesWeight = suggestion.usesWeight,
                            measure = suggestion.measure,
                        )
                        ListItem(
                            headlineContent = { Text(suggestion.name) },
                            supportingContent = { Text(stringResource(suggestion.muscleGroup.labelRes)) },
                            leadingContent = { ExerciseAvatar(preview, size = 40.dp) },
                            trailingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            colors = transparent,
                            modifier = Modifier.clickable { onPickSuggestion(suggestion) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
    )
}
