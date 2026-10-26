package com.lehyanflouriot.muscuapp.ui.week

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.ExerciseSuggestion
import com.lehyanflouriot.muscuapp.core.domain.ExerciseWeekItem
import com.lehyanflouriot.muscuapp.core.domain.ReferenceWeek
import com.lehyanflouriot.muscuapp.core.domain.WeekOverview
import com.lehyanflouriot.muscuapp.core.domain.WeekSummary
import com.lehyanflouriot.muscuapp.core.domain.setStats
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.StatTile
import com.lehyanflouriot.muscuapp.ui.common.TrendBadge
import com.lehyanflouriot.muscuapp.ui.common.describe
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.repsTotal
import com.lehyanflouriot.muscuapp.ui.common.setsCount
import com.lehyanflouriot.muscuapp.ui.common.weekTitle
import com.lehyanflouriot.muscuapp.ui.common.weeksAgoLabel
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.LocalDate

@Composable
fun WeekScreen(
    overview: WeekOverview,
    today: LocalDate,
    hasAnyEntry: Boolean,
    suggestions: List<ExerciseSuggestion>,
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isCurrentWeek = overview.weekStart == today.startOfWeek()
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val previousWeek by rememberUpdatedState(onPreviousWeek)
    val nextWeek by rememberUpdatedState(onNextWeek)
    val canGoNext by rememberUpdatedState(!isCurrentWeek)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.week_screen_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (overview.items.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddExercise,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.week_add_exercise)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        var dragTotal by remember { mutableFloatStateOf(0f) }
        LazyColumn(
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
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
                )
            }
            if (overview.items.isEmpty()) {
                item(key = "empty") {
                    WelcomeCard(
                        suggestions = suggestions,
                        onAddExercise = onAddExercise,
                        onQuickAdd = onQuickAdd,
                    )
                }
            } else {
                item(key = "summary") {
                    WeekSummaryCard(
                        summary = overview.summary,
                        previous = overview.previousSummary,
                        isCurrentWeek = isCurrentWeek,
                    )
                }
                if (!hasAnyEntry) {
                    item(key = "tip") { TipCard() }
                }
                overview.items.groupBy { it.exercise.muscleGroup }.forEach { (group, groupItems) ->
                    item(key = "group-$group") {
                        SectionHeader(text = stringResource(group.labelRes))
                    }
                    items(groupItems, key = { it.exercise.id }) { item ->
                        ExerciseWeekCard(
                            item = item,
                            onLog = { onLog(item.exercise) },
                            onEditEntry = onEditEntry,
                            onOpen = { onOpenExercise(item.exercise) },
                        )
                    }
                }
                // While getting started, more exercises can still be added in one tap.
                if (!hasAnyEntry && suggestions.isNotEmpty()) {
                    item(key = "quick-add") {
                        QuickAdd(suggestions = suggestions, onQuickAdd = onQuickAdd)
                    }
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
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
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
private fun WeekSummaryCard(summary: WeekSummary, previous: WeekSummary, isCurrentWeek: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (summary.isEmpty) {
                Text(
                    text = stringResource(if (isCurrentWeek) R.string.week_empty_current else R.string.week_empty_past),
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        value = Fmt.number(summary.activeDays),
                        label = pluralStringResource(R.plurals.sessions_label, summary.activeDays),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = Fmt.number(summary.sets),
                        label = pluralStringResource(R.plurals.sets_label, summary.sets),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = Fmt.number(summary.reps),
                        label = pluralStringResource(R.plurals.reps_label, summary.reps),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (summary.volume > 0) {
                    Text(
                        text = stringResource(R.string.week_volume, Fmt.volume(summary.volume)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (!previous.isEmpty) {
                Text(
                    text = stringResource(R.string.week_previous_summary, setsCount(previous.sets), repsTotal(previous.reps, Measure.REPS)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TipCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
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
) {
    val exercise = item.exercise
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.done) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.week_done_cd),
                            tint = MuscuTheme.extendedColors.success,
                            modifier = Modifier.size(18.dp),
                        )
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
            FilledTonalIconButton(onClick = onLog, modifier = Modifier.size(52.dp)) {
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EmptyState(
            icon = Icons.Rounded.FitnessCenter,
            title = stringResource(R.string.welcome_title),
            text = stringResource(R.string.welcome_text),
            action = {
                Button(onClick = onAddExercise) {
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
private fun QuickAdd(suggestions: List<ExerciseSuggestion>, onQuickAdd: (ExerciseSuggestion) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.welcome_quick_add))
        Text(
            text = stringResource(R.string.welcome_quick_add_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onQuickAdd(suggestion) },
                    label = { Text(suggestion.name) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}
