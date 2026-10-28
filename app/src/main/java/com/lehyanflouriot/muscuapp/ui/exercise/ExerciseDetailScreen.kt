package com.lehyanflouriot.muscuapp.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.ChartMetric
import com.lehyanflouriot.muscuapp.core.domain.ExerciseRecords
import com.lehyanflouriot.muscuapp.core.domain.HistoryWeek
import com.lehyanflouriot.muscuapp.core.domain.WeekPoint
import com.lehyanflouriot.muscuapp.core.domain.chartMetrics
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.WeeklyBarChart
import com.lehyanflouriot.muscuapp.ui.common.countUp
import com.lehyanflouriot.muscuapp.ui.common.describe
import com.lehyanflouriot.muscuapp.ui.common.icon
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.repsTotal
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.common.setsCount
import com.lehyanflouriot.muscuapp.ui.common.typeIcon
import com.lehyanflouriot.muscuapp.ui.common.weekShortLabel
import com.lehyanflouriot.muscuapp.ui.common.weekTitle
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    weeks: List<HistoryWeek>,
    records: ExerciseRecords,
    metric: ChartMetric,
    series: List<WeekPoint>,
    today: LocalDate,
    restLabel: String,
    onMetricChange: (ChartMetric) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onLog: () -> Unit,
    onStartRest: () -> Unit,
    onEditEntry: (WorkoutEntry) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var menuOpen by remember { mutableStateOf(false) }
    val reveal = rememberRevealState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(exercise.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.detail_edit))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.detail_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (exercise.archived) R.string.detail_unarchive else R.string.detail_archive)) },
                                leadingIcon = {
                                    Icon(if (exercise.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    onToggleArchive()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_delete)) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onDelete()
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Header(exercise = exercise, onUnarchive = onToggleArchive, modifier = Modifier.reveal(reveal, "header"))
            }
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.reveal(reveal, "actions")) {
                    val logPress = rememberPressFeedback()
                    Button(
                        onClick = onLog,
                        interactionSource = logPress.interactionSource,
                        modifier = logPress.modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.detail_log))
                    }
                    val restPress = rememberPressFeedback()
                    FilledTonalButton(
                        onClick = onStartRest,
                        interactionSource = restPress.interactionSource,
                        modifier = restPress.modifier.height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(restLabel)
                    }
                }
            }
            if (records.isEmpty) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.detail_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                item(key = "records") {
                    RecordsCard(exercise = exercise, records = records, modifier = Modifier.reveal(reveal, "records"))
                }
                item(key = "chart") {
                    ChartCard(
                        exercise = exercise,
                        metric = metric,
                        series = series,
                        onMetricChange = onMetricChange,
                        modifier = Modifier.reveal(reveal, "chart"),
                    )
                }
                item(key = "history-title") {
                    SectionHeader(text = stringResource(R.string.detail_history), modifier = Modifier.reveal(reveal, "history-title"))
                }
                weeks.forEach { week ->
                    val weekKey = "week-${week.weekStart}"
                    item(key = weekKey) {
                        WeekHeader(
                            exercise = exercise,
                            week = week,
                            today = today,
                            modifier = Modifier
                                .animateItem(fadeInSpec = null)
                                .reveal(reveal, weekKey),
                        )
                    }
                    items(week.entries, key = { it.id }) { entry ->
                        HistoryEntry(
                            exercise = exercise,
                            entry = entry,
                            today = today,
                            onClick = { onEditEntry(entry) },
                            modifier = Modifier
                                .animateItem(fadeInSpec = null)
                                .reveal(reveal, entry.id),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(exercise: Exercise, onUnarchive: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // What the exercise is: plain tags, which do not look like buttons. Its rest is on the Repos button.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoTag(text = stringResource(exercise.muscleGroup.labelRes), icon = exercise.icon)
            InfoTag(
                text = stringResource(
                    when {
                        exercise.measure == Measure.SECONDS -> R.string.detail_type_timed
                        exercise.usesWeight -> R.string.detail_type_weighted
                        else -> R.string.detail_type_bodyweight
                    },
                ),
                icon = exercise.typeIcon,
            )
        }
        if (exercise.archived) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.detail_archived),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                    )
                    TextButton(onClick = onUnarchive) { Text(stringResource(R.string.detail_unarchive)) }
                }
            }
        }
        if (exercise.notes.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(exercise.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun InfoTag(text: String, icon: ImageVector) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A record: its value counts up when the card appears, then shows exactly. */
private class RecordTile(
    val label: String,
    val value: Double,
    val format: @Composable (Double) -> String,
    val caption: String,
)

@Composable
private fun RecordsCard(exercise: Exercise, records: ExerciseRecords, modifier: Modifier = Modifier) {
    val tiles = buildList {
        records.heaviest?.let { best ->
            add(
                RecordTile(
                    label = stringResource(R.string.record_heaviest),
                    value = best.set.weight,
                    format = { Fmt.kg(it) },
                    caption = stringResource(R.string.record_heaviest_caption, best.set.reps, Fmt.day(best.date)),
                ),
            )
        }
        records.estimatedOneRepMax?.let { oneRepMax ->
            add(
                RecordTile(
                    label = stringResource(R.string.record_one_rep_max),
                    value = oneRepMax.toLong().toDouble(),
                    format = { "≈ ${Fmt.kg(it)}" },
                    caption = stringResource(R.string.record_one_rep_max_caption),
                ),
            )
        }
        records.mostReps?.let { best ->
            add(
                RecordTile(
                    label = stringResource(if (exercise.measure == Measure.SECONDS) R.string.record_longest else R.string.record_most_reps),
                    value = best.set.reps.toDouble(),
                    format = { Fmt.reps(it.roundToInt(), exercise.measure) },
                    caption = if (exercise.usesWeight && best.set.weight > 0) {
                        stringResource(R.string.record_at_weight, Fmt.kg(best.set.weight))
                    } else {
                        Fmt.day(best.date)
                    },
                ),
            )
        }
        records.bestWeek?.let { week ->
            add(
                RecordTile(
                    label = stringResource(R.string.record_best_week),
                    value = week.value,
                    format = { if (records.bestWeekByVolume) Fmt.volume(it) else repsTotal(it.roundToInt(), exercise.measure) },
                    caption = stringResource(R.string.record_week_of, weekShortLabel(week.weekStart)),
                ),
            )
        }
        add(
            RecordTile(
                label = stringResource(R.string.record_sessions),
                value = records.sessions.toDouble(),
                format = { Fmt.number(it.roundToInt()) },
                caption = stringResource(R.string.record_sessions_caption),
            ),
        )
    }.take(4)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.record_title), style = MaterialTheme.typography.titleMedium)
            }
            tiles.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { columnIndex, tile ->
                        Column(Modifier.weight(1f)) {
                            Text(tile.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val shown = countUp(tile.value, delayMillis = (rowIndex * 2 + columnIndex) * 90)
                            // Whole numbers while counting, the exact record at the end.
                            val settled = abs(shown - tile.value) <= max(0.001, abs(tile.value) * 1e-6)
                            Text(
                                text = tile.format(if (settled) tile.value else shown.roundToLong().toDouble()),
                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(tile.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    exercise: Exercise,
    metric: ChartMetric,
    series: List<WeekPoint>,
    onMetricChange: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = chartMetrics(exercise)
    val unitSeconds = stringResource(R.string.unit_seconds)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.chart_title), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                metrics.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = item == metric,
                        onClick = { onMetricChange(item) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = metrics.size),
                        icon = {},
                    ) {
                        Text(metricLabel(item, exercise.measure), maxLines = 1)
                    }
                }
            }
            WeeklyBarChart(
                points = series,
                formatValue = { value ->
                    when (metric) {
                        ChartMetric.VOLUME -> Fmt.volume(value)
                        ChartMetric.MAX_WEIGHT -> Fmt.kg(value)
                        ChartMetric.REPS -> if (exercise.measure == Measure.SECONDS) "${Fmt.number(value, 0)} $unitSeconds" else Fmt.number(value, 0)
                        ChartMetric.SETS -> Fmt.number(value, 0)
                    }
                },
                description = stringResource(R.string.chart_description, metricLabel(metric, exercise.measure)),
            )
            Text(
                text = stringResource(R.string.chart_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun metricLabel(metric: ChartMetric, measure: Measure): String = stringResource(
    when (metric) {
        ChartMetric.VOLUME -> R.string.metric_volume
        ChartMetric.MAX_WEIGHT -> R.string.metric_max_weight
        ChartMetric.REPS -> if (measure == Measure.SECONDS) R.string.metric_seconds else R.string.metric_reps
        ChartMetric.SETS -> R.string.metric_sets
    },
)

@Composable
private fun WeekHeader(exercise: Exercise, week: HistoryWeek, today: LocalDate, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 8.dp)) {
        HorizontalDivider()
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(weekTitle(week.weekStart, today), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = Fmt.weekRange(week.weekStart, today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = listOf(setsCount(week.stats.sets), repsTotal(week.stats.reps, exercise.measure)).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryEntry(
    exercise: Exercise,
    entry: WorkoutEntry,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.week_edit_entry), onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(exercise.describe(entry.sets), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = relativeDay(entry.date, today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.Rounded.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
