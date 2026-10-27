package com.lehyanflouriot.muscuapp.ui.log

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.stats
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import com.lehyanflouriot.muscuapp.ui.common.CompactStepper
import com.lehyanflouriot.muscuapp.ui.common.DateChips
import com.lehyanflouriot.muscuapp.ui.common.ExerciseAvatar
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.NumberInputDialog
import com.lehyanflouriot.muscuapp.ui.common.NumberStepper
import com.lehyanflouriot.muscuapp.ui.common.PastDatePickerDialog
import com.lehyanflouriot.muscuapp.ui.common.describe
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.repsLabel
import com.lehyanflouriot.muscuapp.ui.common.repsTotal
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.common.setsCount
import java.time.LocalDate
import kotlin.math.roundToLong

private const val MAX_SETS = 20
private const val MAX_REPS = 999
private const val MAX_WEIGHT = 999.0
private const val DEFAULT_SETS = 3
private const val DEFAULT_REPS = 10
private const val DEFAULT_SECONDS = 30

/** Values being typed in the log sheet. */
@Stable
class LogFormState(
    initialDate: LocalDate,
    initialSets: List<WorkSet>,
    initialNote: String,
    measure: Measure,
) {
    val repStep: Int = if (measure == Measure.SECONDS) 5 else 1
    private val seed: List<WorkSet> = initialSets.ifEmpty {
        List(DEFAULT_SETS) { WorkSet(if (measure == Measure.SECONDS) DEFAULT_SECONDS else DEFAULT_REPS) }
    }

    var date by mutableStateOf(initialDate)
    var detailed by mutableStateOf(seed.any { it != seed.first() })
        private set
    var setCount by mutableIntStateOf(seed.size.coerceIn(1, MAX_SETS))
    var reps by mutableIntStateOf(seed.first().reps.coerceIn(1, MAX_REPS))
    var weight by mutableDoubleStateOf(seed.first().weight)
    val rows: SnapshotStateList<WorkSet> = seed.take(MAX_SETS).toMutableStateList()
    var note by mutableStateOf(initialNote)

    /** The sets as they will be saved. */
    val sets: List<WorkSet>
        get() = if (detailed) rows.toList() else List(setCount) { WorkSet(reps, weight) }

    fun showDetails() {
        rows.clear()
        rows.addAll(List(setCount) { WorkSet(reps, weight) })
        detailed = true
    }

    fun showSimple() {
        val first = rows.firstOrNull() ?: WorkSet(reps, weight)
        setCount = rows.size.coerceIn(1, MAX_SETS)
        reps = first.reps
        weight = first.weight
        detailed = false
    }

    fun addRow() {
        if (rows.size < MAX_SETS) rows.add(rows.lastOrNull() ?: WorkSet(reps, weight))
    }

    fun removeRow(index: Int) {
        if (rows.size > 1 && index in rows.indices) rows.removeAt(index)
    }

    fun updateRow(index: Int, set: WorkSet) {
        if (index in rows.indices) rows[index] = set
    }
}

private fun roundWeight(value: Double): Double = ((value * 100).roundToLong() / 100.0).coerceIn(0.0, MAX_WEIGHT)

private sealed interface EditTarget {
    data object SetCount : EditTarget
    data object Reps : EditTarget
    data object Weight : EditTarget
    data class RowReps(val index: Int) : EditTarget
    data class RowWeight(val index: Int) : EditTarget
}

/**
 * Form to note what was done for an exercise: by default "sets x reps x weight" with big
 * buttons, or one line per set when the sets were different.
 */
@Composable
fun LogEntryForm(
    exercise: Exercise,
    state: LogFormState,
    today: LocalDate,
    isEdit: Boolean,
    previous: WorkoutEntry?,
    weightStep: Double,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    restButtonText: String? = null,
    onRestClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    val repsName = repsLabel(exercise.measure)
    val unitSeconds = stringResource(R.string.unit_seconds)
    val unitKg = stringResource(R.string.unit_kg)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExerciseAvatar(exercise)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(exercise.muscleGroup.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DateChips(
            date = state.date,
            today = today,
            onSelect = { state.date = it },
            onPickDate = { pickingDate = true },
        )

        AnimatedContent(
            targetState = state.detailed,
            transitionSpec = {
                (fadeIn(tween(250, delayMillis = 80)) + slideInVertically(tween(350, easing = Motion.EaseOut)) { it / 10 })
                    .togetherWith(fadeOut(tween(120)))
                    .using(SizeTransform(clip = false))
            },
            label = "log-mode",
        ) { detailed ->
            if (!detailed) {
                SimpleSets(
                    exercise = exercise,
                    state = state,
                    repsName = repsName,
                    unitSeconds = unitSeconds,
                    unitKg = unitKg,
                    weightStep = weightStep,
                    onEdit = { editing = it },
                )
            } else {
                DetailedSets(
                    exercise = exercise,
                    state = state,
                    repsName = repsName,
                    weightStep = weightStep,
                    onEdit = { editing = it },
                )
            }
        }

        if (previous == null && !isEdit) {
            Text(
                text = stringResource(R.string.log_first_time_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(
            onClick = { if (state.detailed) state.showSimple() else state.showDetails() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (state.detailed) R.string.log_simple_mode else R.string.log_detailed_mode))
        }

        Summary(exercise = exercise, sets = state.sets, previous = previous, today = today)

        OutlinedTextField(
            value = state.note,
            onValueChange = { state.note = it.take(300) },
            label = { Text(stringResource(R.string.log_note)) },
            placeholder = { Text(stringResource(R.string.log_note_placeholder)) },
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onRestClick != null && restButtonText != null) {
                FilledTonalButton(onClick = onRestClick, modifier = Modifier.height(56.dp)) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(restButtonText, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
                }
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (isEdit) R.string.log_update else R.string.log_save),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.log_delete))
            }
        }
    }

    editing?.let { target ->
        val (title, initial, decimal) = when (target) {
            EditTarget.SetCount -> Triple(stringResource(R.string.field_sets), state.setCount.toString(), false)
            EditTarget.Reps -> Triple(repsName, state.reps.toString(), false)
            EditTarget.Weight -> Triple(stringResource(R.string.field_weight), Fmt.weight(state.weight), true)
            is EditTarget.RowReps -> Triple(
                stringResource(R.string.log_set_value, repsName, target.index + 1),
                state.rows.getOrNull(target.index)?.reps?.toString().orEmpty(),
                false,
            )
            is EditTarget.RowWeight -> Triple(
                stringResource(R.string.log_set_value, stringResource(R.string.field_weight), target.index + 1),
                state.rows.getOrNull(target.index)?.weight?.let(Fmt::weight).orEmpty(),
                true,
            )
        }
        val isWeight = target is EditTarget.Weight || target is EditTarget.RowWeight
        NumberInputDialog(
            title = title,
            initialValue = initial,
            allowDecimal = decimal,
            suffix = if (isWeight) unitKg else if (exercise.measure == Measure.SECONDS && target !is EditTarget.SetCount) unitSeconds else null,
            onDismiss = { editing = null },
            onConfirm = { value ->
                when (target) {
                    EditTarget.SetCount -> state.setCount = value.toInt().coerceIn(1, MAX_SETS)
                    EditTarget.Reps -> state.reps = value.toInt().coerceIn(1, MAX_REPS)
                    EditTarget.Weight -> state.weight = roundWeight(value)
                    is EditTarget.RowReps -> state.rows.getOrNull(target.index)?.let {
                        state.updateRow(target.index, it.copy(reps = value.toInt().coerceIn(1, MAX_REPS)))
                    }
                    is EditTarget.RowWeight -> state.rows.getOrNull(target.index)?.let {
                        state.updateRow(target.index, it.copy(weight = roundWeight(value)))
                    }
                }
                editing = null
            },
        )
    }

    if (pickingDate) {
        PastDatePickerDialog(
            initialDate = state.date,
            today = today,
            onDismiss = { pickingDate = false },
            onConfirm = {
                state.date = it
                pickingDate = false
            },
        )
    }
}

@Composable
private fun SimpleSets(
    exercise: Exercise,
    state: LogFormState,
    repsName: String,
    unitSeconds: String,
    unitKg: String,
    weightStep: Double,
    onEdit: (EditTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NumberStepper(
            label = stringResource(R.string.field_sets),
            value = state.setCount.toString(),
            onDecrement = { state.setCount = (state.setCount - 1).coerceAtLeast(1) },
            onIncrement = { state.setCount = (state.setCount + 1).coerceAtMost(MAX_SETS) },
            onValueClick = { onEdit(EditTarget.SetCount) },
            canDecrement = state.setCount > 1,
            canIncrement = state.setCount < MAX_SETS,
        )
        NumberStepper(
            label = repsName,
            value = state.reps.toString(),
            unit = if (exercise.measure == Measure.SECONDS) unitSeconds else null,
            onDecrement = { state.reps = (state.reps - state.repStep).coerceAtLeast(1) },
            onIncrement = { state.reps = (state.reps + state.repStep).coerceAtMost(MAX_REPS) },
            onValueClick = { onEdit(EditTarget.Reps) },
            canDecrement = state.reps > 1,
            canIncrement = state.reps < MAX_REPS,
        )
        if (exercise.usesWeight) {
            NumberStepper(
                label = stringResource(R.string.field_weight),
                value = Fmt.weight(state.weight),
                unit = unitKg,
                onDecrement = { state.weight = roundWeight(state.weight - weightStep) },
                onIncrement = { state.weight = roundWeight(state.weight + weightStep) },
                onValueClick = { onEdit(EditTarget.Weight) },
                canDecrement = state.weight > 0.0,
                canIncrement = state.weight < MAX_WEIGHT,
            )
        }
    }
}

@Composable
private fun DetailedSets(
    exercise: Exercise,
    state: LogFormState,
    repsName: String,
    weightStep: Double,
    onEdit: (EditTarget) -> Unit,
) {
    val weightName = stringResource(R.string.field_weight)
    // Sets glide in one after the other, and the ones added later too.
    val reveal = rememberRevealState()
    Column(
        modifier = Modifier.animateContentSize(tween(300, easing = Motion.EaseOut)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.log_set_header),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(44.dp),
            )
            Text(
                text = repsName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            if (exercise.usesWeight) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.field_weight_kg),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.width(40.dp))
        }
        state.rows.forEachIndexed { index, set ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.reveal(reveal, index)) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(44.dp),
                )
                CompactStepper(
                    value = set.reps.toString(),
                    description = stringResource(R.string.log_set_value, repsName, index + 1),
                    onDecrement = { state.updateRow(index, set.copy(reps = (set.reps - state.repStep).coerceAtLeast(1))) },
                    onIncrement = { state.updateRow(index, set.copy(reps = (set.reps + state.repStep).coerceAtMost(MAX_REPS))) },
                    onValueClick = { onEdit(EditTarget.RowReps(index)) },
                    canDecrement = set.reps > 1,
                    modifier = Modifier.weight(1f),
                )
                if (exercise.usesWeight) {
                    Spacer(Modifier.width(10.dp))
                    CompactStepper(
                        value = Fmt.weight(set.weight),
                        description = stringResource(R.string.log_set_value, weightName, index + 1),
                        onDecrement = { state.updateRow(index, set.copy(weight = roundWeight(set.weight - weightStep))) },
                        onIncrement = { state.updateRow(index, set.copy(weight = roundWeight(set.weight + weightStep))) },
                        onValueClick = { onEdit(EditTarget.RowWeight(index)) },
                        canDecrement = set.weight > 0.0,
                        modifier = Modifier.weight(1f),
                    )
                }
                IconButton(
                    onClick = { state.removeRow(index) },
                    enabled = state.rows.size > 1,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.log_remove_set, index + 1),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        TextButton(onClick = state::addRow, enabled = state.rows.size < MAX_SETS) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.log_add_set))
        }
    }
}

@Composable
private fun Summary(exercise: Exercise, sets: List<WorkSet>, previous: WorkoutEntry?, today: LocalDate) {
    val stats = sets.stats()
    val parts = buildList {
        add(setsCount(stats.sets))
        add(repsTotal(stats.reps, exercise.measure))
        if (exercise.usesWeight && stats.volume > 0) add(Fmt.volume(stats.volume))
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.log_total, parts.joinToString(" · ")),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (previous != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.log_previous,
                            relativeDay(previous.date, today),
                            exercise.describe(previous.sets),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
