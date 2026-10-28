package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.DoneSet
import com.lehyanflouriot.muscuapp.core.domain.SessionItem
import com.lehyanflouriot.muscuapp.core.domain.SessionView
import com.lehyanflouriot.muscuapp.core.domain.complete
import com.lehyanflouriot.muscuapp.core.domain.doneSets
import com.lehyanflouriot.muscuapp.core.domain.plannedSets
import com.lehyanflouriot.muscuapp.core.domain.stats
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.ui.common.ExerciseAvatar
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.NumberInputDialog
import com.lehyanflouriot.muscuapp.ui.common.NumberStepper
import com.lehyanflouriot.muscuapp.ui.common.describe
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.repsLabel
import com.lehyanflouriot.muscuapp.ui.common.repsTotal
import com.lehyanflouriot.muscuapp.ui.common.setsCount
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.LocalDate
import kotlin.math.roundToLong

private const val MAX_REPS = 999
private const val MAX_WEIGHT = 999.0

private fun roundWeight(value: Double): Double = ((value * 100).roundToLong() / 100.0).coerceIn(0.0, MAX_WEIGHT)

/** What the guided session can do. */
class GuidedActions(
    val onSelect: (Int) -> Unit,
    val onDraftChange: (WorkSet) -> Unit,
    val onSetDone: () -> Unit,
    val onEditSet: (DoneSet, Int) -> Unit,
    val onRestAdd: () -> Unit,
    val onRestToggle: () -> Unit,
    val onRestSkip: () -> Unit,
    val onClose: () -> Unit,
)

/** "12 reps · 8 kg", "45 s". */
@Composable
private fun describeSet(set: WorkSet, exercise: Exercise): String = listOfNotNull(
    repsTotal(set.reps, exercise.measure),
    Fmt.kg(set.weight).takeIf { exercise.usesWeight && set.weight > 0.0 },
).joinToString(" · ")

/**
 * The session done exercise by exercise and set by set: each set is noted as soon as it is done
 * and starts the rest the session asks, and the next set starts from its goal.
 */
@Composable
fun GuidedSessionScreen(
    session: SessionView,
    index: Int,
    draft: WorkSet,
    timerState: TimerState,
    now: Long,
    today: LocalDate,
    weightStep: Double,
    actions: GuidedActions,
) {
    val item = session.items[index]
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(dayTitle(session.day), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = stringResource(R.string.guided_exercise_of, index + 1, session.items.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.guided_close))
                    }
                },
            )
        },
        bottomBar = {
            RestBar(
                state = timerState,
                now = now,
                // What the screen shows next: the exercise already moved on when its last set was done.
                next = item.takeIf { !it.complete },
                actions = actions,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExerciseStrip(items = session.items, current = index, onSelect = actions.onSelect)
            AnimatedVisibility(
                visible = session.complete,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SessionDone(session = session, onFinish = actions.onClose)
            }
            ExerciseHeader(item = item, today = today)
            SetsCard(
                item = item,
                draft = draft,
                weightStep = weightStep,
                hasNext = index < session.items.lastIndex,
                actions = actions,
                onNext = { actions.onSelect(index + 1) },
            )
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { actions.onSelect(index - 1) }, enabled = index > 0) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.guided_previous))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { actions.onSelect(index + 1) }, enabled = index < session.items.lastIndex) {
                    Text(stringResource(if (item.complete) R.string.guided_next else R.string.guided_skip))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** One segment per exercise, filled as its sets are done; the one shown stands out. */
@Composable
private fun ExerciseStrip(items: List<SessionItem>, current: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val success = MuscuTheme.extendedColors.success
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, item ->
            val done = item.doneSets().size
            val fraction = (done.toFloat() / item.plannedSets).coerceIn(0f, 1f)
            val selected = index == current
            val height by animateDpAsState(if (selected) 10.dp else 6.dp, tween(250), label = "strip-height")
            val fill by animateColorAsState(if (item.complete) success else colors.primary, tween(250), label = "strip-color")
            val description = stringResource(R.string.guided_strip_cd, index + 1, item.exercise.name, done, item.plannedSets)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .semantics { contentDescription = description },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(CircleShape)
                        .background(if (selected) colors.primary.copy(alpha = 0.25f) else colors.surfaceContainerHighest),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(fill),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDone(session: SessionView, onFinish: () -> Unit) {
    val stats = session.doneSets().stats()
    Surface(
        color = MuscuTheme.extendedColors.highlight,
        contentColor = MuscuTheme.extendedColors.onHighlight,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.session_complete), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOf(setsCount(stats.sets), repsTotal(stats.reps, Measure.REPS)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = onFinish) { Text(stringResource(R.string.guided_finish)) }
        }
    }
}

@Composable
private fun ExerciseHeader(item: SessionItem, today: LocalDate) {
    val exercise = item.exercise
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExerciseAvatar(exercise)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
                val rest = item.line.restSeconds ?: exercise.restSeconds
                Text(
                    text = listOfNotNull(
                        Fmt.target(item.line.target, exercise.measure),
                        rest?.let { stringResource(R.string.training_rest, Fmt.duration(it)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
            if (item.line.emphasis) {
                Spacer(Modifier.width(8.dp))
                EmphasisBadge()
            }
        }
        if (item.line.note.isNotBlank()) {
            Text(Fmt.typography(item.line.note), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        item.last?.let { last ->
            Text(
                text = stringResource(R.string.guided_last, relativeDay(last.date, today), exercise.describe(last.sets)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        if (item.topReached) {
            Text(stringResource(R.string.session_top_reached), style = MaterialTheme.typography.bodySmall, color = colors.tertiary)
        }
    }
}

/** The sets of the exercise: those done, the one to do now with its values, and those still to come. */
@Composable
private fun SetsCard(
    item: SessionItem,
    draft: WorkSet,
    weightStep: Double,
    hasNext: Boolean,
    actions: GuidedActions,
    onNext: () -> Unit,
) {
    val exercise = item.exercise
    val done = item.doneSets()
    val colors = MaterialTheme.colorScheme
    val success = MuscuTheme.extendedColors.success
    // Once every set is done, one more can still be added.
    var extra by rememberSaveable(item.line.id) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .animateContentSize(tween(300, easing = Motion.EaseOut)),
        ) {
            done.forEachIndexed { position, set ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = stringResource(R.string.action_edit)) { actions.onEditSet(set, position + 1) }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = success, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.guided_set, position + 1), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text(describeSet(set.set, exercise), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
            if (done.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.guided_edit_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 50.dp, end = 16.dp, bottom = 4.dp),
                )
            }
            if (!item.complete || extra) {
                SetEditor(
                    number = done.size + 1,
                    planned = item.plannedSets,
                    goal = item.goal.getOrNull(done.size),
                    exercise = exercise,
                    draft = draft,
                    weightStep = weightStep,
                    onDraftChange = actions.onDraftChange,
                    onDone = {
                        extra = false
                        actions.onSetDone()
                    },
                )
            }
            for (number in done.size + 2..item.plannedSets) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = colors.outline, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.guided_set, number),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    item.goal.getOrNull(number - 1)?.let { goal ->
                        Text(
                            text = stringResource(R.string.session_goal, describeSet(goal, exercise)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            if (item.complete && !extra) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.guided_exercise_done),
                        style = MaterialTheme.typography.titleMedium,
                        color = success,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasNext) {
                            Button(onClick = onNext) {
                                Text(stringResource(R.string.guided_next_exercise))
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        TextButton(onClick = { extra = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.guided_add_set))
                        }
                    }
                }
            }
        }
    }
}

private enum class TypedValue { REPS, WEIGHT }

/** The set to do now: its goal, its values to adjust, and the button to note it. */
@Composable
private fun SetEditor(
    number: Int,
    planned: Int,
    goal: WorkSet?,
    exercise: Exercise,
    draft: WorkSet,
    weightStep: Double,
    onDraftChange: (WorkSet) -> Unit,
    onDone: () -> Unit,
) {
    var typing by remember { mutableStateOf<TypedValue?>(null) }
    val repsName = repsLabel(exercise.measure)
    val step = if (exercise.measure == Measure.SECONDS) 5 else 1
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (number <= planned) {
                        stringResource(R.string.guided_set_of, number, planned)
                    } else {
                        stringResource(R.string.guided_extra_set)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (goal != null) {
                    Text(
                        text = stringResource(R.string.session_goal, describeSet(goal, exercise)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            NumberStepper(
                label = repsName,
                value = draft.reps.toString(),
                unit = if (exercise.measure == Measure.SECONDS) stringResource(R.string.unit_seconds) else null,
                onDecrement = { onDraftChange(draft.copy(reps = (draft.reps - step).coerceAtLeast(1))) },
                onIncrement = { onDraftChange(draft.copy(reps = (draft.reps + step).coerceAtMost(MAX_REPS))) },
                onValueClick = { typing = TypedValue.REPS },
                canDecrement = draft.reps > 1,
                canIncrement = draft.reps < MAX_REPS,
            )
            if (exercise.usesWeight) {
                NumberStepper(
                    label = stringResource(R.string.field_weight),
                    value = Fmt.weight(draft.weight),
                    unit = stringResource(R.string.unit_kg),
                    onDecrement = { onDraftChange(draft.copy(weight = roundWeight(draft.weight - weightStep))) },
                    onIncrement = { onDraftChange(draft.copy(weight = roundWeight(draft.weight + weightStep))) },
                    onValueClick = { typing = TypedValue.WEIGHT },
                    canDecrement = draft.weight > 0.0,
                    canIncrement = draft.weight < MAX_WEIGHT,
                )
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.guided_set_done), style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    typing?.let { value ->
        val weight = value == TypedValue.WEIGHT
        NumberInputDialog(
            title = if (weight) stringResource(R.string.field_weight) else repsName,
            initialValue = if (weight) Fmt.weight(draft.weight) else draft.reps.toString(),
            allowDecimal = weight,
            suffix = if (weight) stringResource(R.string.unit_kg) else if (exercise.measure == Measure.SECONDS) stringResource(R.string.unit_seconds) else null,
            onDismiss = { typing = null },
            onConfirm = { typed ->
                onDraftChange(
                    if (weight) draft.copy(weight = roundWeight(typed)) else draft.copy(reps = typed.toInt().coerceIn(1, MAX_REPS)),
                )
                typing = null
            },
        )
    }
}

/** The rest running between two sets, always in sight, with what comes next. */
@Composable
private fun RestBar(state: TimerState, now: Long, next: SessionItem?, actions: GuidedActions) {
    val visible = state !is TimerState.Idle
    AnimatedVisibility(visible = visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val colors = MaterialTheme.colorScheme
        val success = MuscuTheme.extendedColors.success
        val finished = state is TimerState.Finished
        Surface(color = colors.surfaceContainerHigh, tonalElevation = 3.dp) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { TimerLogic.fractionRemaining(state, now) },
                    color = if (finished) success else colors.primary,
                    trackColor = colors.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(if (finished) R.string.guided_rest_done else R.string.guided_rest),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (finished) success else colors.onSurfaceVariant,
                        )
                        if (!finished) {
                            Text(
                                text = Fmt.clockMillis(TimerLogic.remaining(state, now)),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (!finished) {
                        TextButton(onClick = actions.onRestAdd) { Text(stringResource(R.string.rest_plus_15)) }
                        IconButton(onClick = actions.onRestToggle) {
                            Icon(
                                imageVector = if (state is TimerState.Paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = stringResource(if (state is TimerState.Paused) R.string.rest_resume else R.string.rest_pause),
                            )
                        }
                    }
                    FilledTonalButton(onClick = actions.onRestSkip) {
                        Text(stringResource(if (finished) R.string.guided_rest_close else R.string.guided_rest_skip))
                    }
                }
                if (next != null) {
                    Text(
                        text = stringResource(R.string.guided_then, next.exercise.name, next.doneSets().size + 1, next.plannedSets),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
