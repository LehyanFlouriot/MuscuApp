package com.lehyanflouriot.muscuapp.ui.week

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.ExerciseSuggestion
import com.lehyanflouriot.muscuapp.core.domain.ExerciseWeekItem
import com.lehyanflouriot.muscuapp.core.domain.ReferenceWeek
import com.lehyanflouriot.muscuapp.core.domain.SessionView
import com.lehyanflouriot.muscuapp.core.domain.WeekOverview
import com.lehyanflouriot.muscuapp.core.domain.WeekSummary
import com.lehyanflouriot.muscuapp.core.domain.setStats
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.FillBar
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.RevealState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.StatTile
import com.lehyanflouriot.muscuapp.ui.common.TrendBadge
import com.lehyanflouriot.muscuapp.ui.common.countUp
import com.lehyanflouriot.muscuapp.ui.common.describe
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.repsTotal
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.common.setsCount
import com.lehyanflouriot.muscuapp.ui.common.weekTitle
import com.lehyanflouriot.muscuapp.ui.common.weeksAgoLabel
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.LocalDate
import kotlin.math.roundToInt

/** What a week shows: its exercises and, with a program, its sessions and the one chosen. */
private class WeekContent(val overview: WeekOverview, val sessions: List<SessionView>?, val sessionDate: LocalDate)

@Composable
fun WeekScreen(
    overview: WeekOverview,
    sessions: List<SessionView>?,
    sessionDate: LocalDate,
    sessionActions: SessionActions,
    today: LocalDate,
    hasAnyEntry: Boolean,
    suggestions: List<ExerciseSuggestion>,
    missed: SessionView? = null,
    /** How long the session of a day took, when it was timed. */
    sessionDuration: (LocalDate) -> Long? = { null },
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onThisWeek: () -> Unit,
    onLog: (Exercise) -> Unit,
    onEditEntry: (WorkoutEntry) -> Unit,
    onOpenExercise: (Exercise) -> Unit,
    onAddExercise: () -> Unit,
    onQuickAdd: (ExerciseSuggestion) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val reveal = rememberRevealState()
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val previousWeek by rememberUpdatedState(onPreviousWeek)
    val nextWeek by rememberUpdatedState(onNextWeek)
    val canGoNext by rememberUpdatedState(overview.weekStart != today.startOfWeek())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.week_screen_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        var dragTotal by remember { mutableFloatStateOf(0f) }
        AnimatedContent(
            targetState = WeekContent(overview, sessions, sessionDate),
            contentKey = { it.overview.weekStart },
            transitionSpec = {
                // Earlier weeks come from the left and later ones from the right, as when swiping.
                val direction = if (targetState.overview.weekStart > initialState.overview.weekStart) 1 else -1
                (
                    slideInHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { it * direction / 3 } +
                        fadeIn(tween(250, delayMillis = 60))
                    ).togetherWith(
                    slideOutHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { -it * direction / 3 } +
                        fadeOut(tween(150)),
                )
            },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Swipe left/right to change week.
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            when {
                                dragTotal > swipeThreshold -> previousWeek()
                                dragTotal < -swipeThreshold && canGoNext -> nextWeek()
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            dragTotal += amount
                            change.consume()
                        },
                    )
                },
            label = "week",
        ) { shown ->
            WeekList(
                overview = shown.overview,
                sessions = shown.sessions,
                sessionDate = shown.sessionDate,
                sessionActions = sessionActions,
                today = today,
                hasAnyEntry = hasAnyEntry,
                suggestions = suggestions,
                missed = missed,
                sessionDuration = sessionDuration,
                reveal = reveal,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onThisWeek = onThisWeek,
                onLog = onLog,
                onEditEntry = onEditEntry,
                onOpenExercise = onOpenExercise,
                onAddExercise = onAddExercise,
                onQuickAdd = onQuickAdd,
            )
        }
    }
}

@Composable
private fun WeekList(
    overview: WeekOverview,
    sessions: List<SessionView>?,
    sessionDate: LocalDate,
    sessionActions: SessionActions,
    today: LocalDate,
    hasAnyEntry: Boolean,
    suggestions: List<ExerciseSuggestion>,
    missed: SessionView?,
    sessionDuration: (LocalDate) -> Long?,
    reveal: RevealState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onThisWeek: () -> Unit,
    onLog: (Exercise) -> Unit,
    onEditEntry: (WorkoutEntry) -> Unit,
    onOpenExercise: (Exercise) -> Unit,
    onAddExercise: () -> Unit,
    onQuickAdd: (ExerciseSuggestion) -> Unit,
) {
    val isCurrentWeek = overview.weekStart == today.startOfWeek()
    // With a program, the session leads: the other exercises wait folded, until asked for.
    var showAll by rememberSaveable(sessions != null) { mutableStateOf(sessions == null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "selector") {
            WeekSelector(
                weekStart = overview.weekStart,
                today = today,
                isCurrentWeek = isCurrentWeek,
                onPrevious = onPreviousWeek,
                onNext = onNextWeek,
                onThisWeek = onThisWeek,
                modifier = Modifier.reveal(reveal, "selector"),
            )
        }
        if (sessions != null) {
            item(key = "session") {
                SessionCard(
                    week = sessions,
                    selected = sessionDate,
                    today = today,
                    actions = sessionActions,
                    missed = missed.takeIf { isCurrentWeek },
                    sessionDuration = sessionDuration,
                    modifier = Modifier.reveal(reveal, "session"),
                )
            }
        }
        if (overview.items.isEmpty()) {
            item(key = "empty") {
                WelcomeCard(
                    suggestions = suggestions,
                    onAddExercise = onAddExercise,
                    onQuickAdd = onQuickAdd,
                    modifier = Modifier.reveal(reveal, "empty"),
                )
            }
        } else {
            item(key = "summary") {
                WeekSummaryCard(
                    summary = overview.summary,
                    previous = overview.previousSummary,
                    isCurrentWeek = isCurrentWeek,
                    modifier = Modifier.reveal(reveal, "summary"),
                )
            }
            if (!hasAnyEntry) {
                item(key = "tip") {
                    TipCard(
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .reveal(reveal, "tip"),
                    )
                }
            }
            val done = overview.items.filter { it.done }
            val others = overview.items.filterNot { it.done }
            if (done.isNotEmpty()) {
                item(key = "done-title") {
                    SectionHeader(
                        text = stringResource(if (isCurrentWeek) R.string.week_done_title else R.string.week_done_past_title),
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .reveal(reveal, "done-title"),
                    )
                }
                items(done, key = { it.exercise.id }) { item ->
                    ExerciseWeekCard(
                        item = item,
                        onLog = { onLog(item.exercise) },
                        onEditEntry = onEditEntry,
                        onOpen = { onOpenExercise(item.exercise) },
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .reveal(reveal, item.exercise.id),
                    )
                }
            }
            item(key = "others-title") {
                SectionHeader(
                    text = pluralStringResource(
                        if (done.isEmpty()) R.plurals.week_all_exercises else R.plurals.week_other_exercises,
                        others.size,
                        others.size,
                    ),
                    modifier = Modifier
                        .animateItem(fadeInSpec = null)
                        .reveal(reveal, "others-title"),
                    action = {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(stringResource(if (showAll) R.string.action_hide else R.string.action_show))
                        }
                    },
                )
            }
            if (showAll) {
                others.groupBy { it.exercise.muscleGroup }.forEach { (group, groupItems) ->
                    item(key = "group-$group") {
                        Text(
                            text = stringResource(group.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp, top = 4.dp)
                                .animateItem(fadeInSpec = null)
                                .reveal(reveal, "group-$group"),
                        )
                    }
                    items(groupItems, key = { it.exercise.id }) { item ->
                        ExerciseWeekCard(
                            item = item,
                            onLog = { onLog(item.exercise) },
                            onEditEntry = onEditEntry,
                            onOpen = { onOpenExercise(item.exercise) },
                            modifier = Modifier
                                .animateItem(fadeInSpec = null)
                                .reveal(reveal, item.exercise.id),
                        )
                    }
                }
                item(key = "new-exercise") {
                    TextButton(
                        onClick = onAddExercise,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                        modifier = Modifier.animateItem(fadeInSpec = null),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.week_new_exercise))
                    }
                }
            }
            // While getting started, more exercises can still be added in one tap.
            if (!hasAnyEntry && suggestions.isNotEmpty()) {
                item(key = "quick-add") {
                    QuickAdd(
                        suggestions = suggestions,
                        onQuickAdd = onQuickAdd,
                        modifier = Modifier
                            .animateItem(fadeInSpec = null)
                            .reveal(reveal, "quick-add"),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekSelector(
    weekStart: LocalDate,
    today: LocalDate,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onThisWeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.week_previous),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(enabled = !isCurrentWeek, onClick = onThisWeek)
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = weekTitle(weekStart, today),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = Fmt.weekRange(weekStart, today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isCurrentWeek) {
                    Text(
                        text = stringResource(R.string.week_back_to_current),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onNext, enabled = !isCurrentWeek) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.week_next),
                )
            }
        }
    }
}

@Composable
private fun WeekSummaryCard(
    summary: WeekSummary,
    previous: WeekSummary,
    isCurrentWeek: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(tween(350, easing = Motion.EaseOut)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (summary.isEmpty) {
                Text(
                    text = stringResource(if (isCurrentWeek) R.string.week_empty_current else R.string.week_empty_past),
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                // The numbers count up, one after the other.
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        value = Fmt.number(countUp(summary.activeDays)),
                        label = pluralStringResource(R.plurals.sessions_label, summary.activeDays),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = Fmt.number(countUp(summary.sets, delayMillis = 80)),
                        label = pluralStringResource(R.plurals.sets_label, summary.sets),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = Fmt.number(countUp(summary.reps, delayMillis = 160)),
                        label = pluralStringResource(R.plurals.reps_label, summary.reps),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (summary.volume > 0) {
                    Text(
                        text = stringResource(R.string.week_volume, Fmt.volume(countUp(summary.volume, delayMillis = 240))),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (!previous.isEmpty) {
                Text(
                    text = stringResource(R.string.week_previous_summary, setsCount(previous.sets), repsTotal(previous.reps, Measure.REPS)),
                    style = MaterialTheme.typography.bodySmall,
                )
                WeekProgress(sets = summary.sets, previousSets = previous.sets)
            }
        }
    }
}

/** How far this week's sets are from the previous week's: a bar that fills up, and the percentage. */
@Composable
private fun WeekProgress(sets: Int, previousSets: Int) {
    val ratio = sets.toDouble() / previousSets
    val description = stringResource(R.string.week_progress_cd, Fmt.percent(ratio))
    val color = LocalContentColor.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        FillBar(
            fraction = ratio.toFloat(),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            delayMillis = 250,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = Fmt.percent(countUp((ratio * 100).roundToInt(), delayMillis = 250) / 100.0),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TipCard(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(text = stringResource(R.string.week_tip), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExerciseWeekCard(
    item: ExerciseWeekItem,
    onLog: () -> Unit,
    onEditEntry: (WorkoutEntry) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = item.exercise
    val press = rememberPressFeedback(pressedScale = 0.98f)
    Card(
        onClick = onOpen,
        interactionSource = press.interactionSource,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .then(press.modifier)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(tween(350, easing = Motion.EaseOut)),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // Pops in when the exercise gets done.
                    AnimatedVisibility(
                        visible = item.done,
                        enter = scaleIn(tween(400, easing = EaseOutBack)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Row {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.week_done_cd),
                                tint = MuscuTheme.extendedColors.success,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                if (item.entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.week_not_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    item.entries.forEach { entry ->
                        EntryLine(exercise = exercise, entry = entry, onClick = { onEditEntry(entry) })
                    }
                }
                item.reference?.let { reference ->
                    ReferenceLine(item = item, reference = reference)
                }
            }
            Spacer(Modifier.width(8.dp))
            val addPress = rememberPressFeedback(pressedScale = 0.88f)
            FilledTonalIconButton(
                onClick = onLog,
                interactionSource = addPress.interactionSource,
                modifier = addPress.modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.week_log_cd, exercise.name),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun EntryLine(exercise: Exercise, entry: WorkoutEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = stringResource(R.string.week_edit_entry), onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Fmt.dayOfWeek(entry.date).replaceFirstChar { it.titlecase(Fmt.locale) },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        Text(
            text = exercise.describe(entry.sets),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ReferenceLine(item: ExerciseWeekItem, reference: ReferenceWeek) {
    val exercise = item.exercise
    val description = if (reference.entries.size == 1) {
        exercise.describe(reference.entries.single().sets)
    } else {
        val stats = reference.entries.setStats()
        stringResource(
            R.string.reference_sessions,
            pluralStringResource(R.plurals.sessions_count, reference.entries.size, reference.entries.size),
            setsCount(stats.sets),
            repsTotal(stats.reps, exercise.measure),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (item.done) {
                stringResource(R.string.reference_line, weeksAgoLabel(reference.weeksAgo), description)
            } else {
                stringResource(R.string.reference_target, weeksAgoLabel(reference.weeksAgo), description)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        item.progress?.let { progress ->
            Spacer(Modifier.width(8.dp))
            TrendBadge(progress = progress, measure = exercise.measure)
        }
    }
}

@Composable
private fun WelcomeCard(
    suggestions: List<ExerciseSuggestion>,
    onAddExercise: () -> Unit,
    onQuickAdd: (ExerciseSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EmptyState(
            icon = Icons.Rounded.FitnessCenter,
            title = stringResource(R.string.welcome_title),
            text = stringResource(R.string.welcome_text),
            action = {
                val press = rememberPressFeedback()
                Button(onClick = onAddExercise, interactionSource = press.interactionSource, modifier = press.modifier) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.welcome_add))
                }
            },
        )
        if (suggestions.isNotEmpty()) {
            QuickAdd(suggestions = suggestions, onQuickAdd = onQuickAdd)
        }
    }
}

@Composable
private fun QuickAdd(
    suggestions: List<ExerciseSuggestion>,
    onQuickAdd: (ExerciseSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.welcome_quick_add))
        Text(
            text = stringResource(R.string.welcome_quick_add_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                key(suggestion.name) {
                    val press = rememberPressFeedback(pressedScale = 0.92f)
                    SuggestionChip(
                        onClick = { onQuickAdd(suggestion) },
                        label = { Text(suggestion.name) },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        interactionSource = press.interactionSource,
                        modifier = press.modifier,
                    )
                }
            }
        }
    }
}
