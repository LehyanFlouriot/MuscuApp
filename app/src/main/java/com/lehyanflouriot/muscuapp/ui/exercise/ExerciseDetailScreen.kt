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
import androidx.compose.material3.AssistChip
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
import com.lehyanflouriot.muscuapp.ui.common.describe
import com.lehyanflouriot.muscuapp.ui.common.icon
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.repsTotal
import com.lehyanflouriot.muscuapp.ui.common.setsCount
import com.lehyanflouriot.muscuapp.ui.common.weekShortLabel
import com.lehyanflouriot.muscuapp.ui.common.weekTitle
import java.time.LocalDate

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
                Header(exercise = exercise, onUnarchive = onToggleArchive)
            }
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onLog,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.detail_log))
                    }
                    FilledTonalButton(onClick = onStartRest, modifier = Modifier.height(52.dp)) {
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
                item(key = "records") { RecordsCard(exercise = exercise, records = records) }
                item(key = "chart") {
                    ChartCard(exercise = exercise, metric = metric, series = series, onMetricChange = onMetricChange)
                }
                item(key = "history-title") { SectionHeader(text = stringResource(R.string.detail_history)) }
                weeks.forEach { week ->
                    item(key = "week-${week.weekStart}") {
                        WeekHeader(exercise = exercise, week = week, today = today)
                    }
                    items(week.entries, key = { it.id }) { entry ->
                        HistoryEntry(exercise = exercise, entry = entry, today = today, onClick = { onEditEntry(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(exercise: Exercise, onUnarchive: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(stringResource(exercise.muscleGroup.labelRes)) })
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(
                            when {
                                exercise.measure == Measure.SECONDS -> R.string.detail_type_timed
                                exercise.usesWeight -> R.string.detail_type_weighted
                                else -> R.string.detail_type_bodyweight
                            },
                        ),
                    )
                },
                leadingIcon = { Icon(exercise.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            exercise.restSeconds?.let { seconds ->
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.detail_rest_chip, Fmt.clock(seconds.toLong()))) },
                    leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
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
private fun RecordsCard(exercise: Exercise, records: ExerciseRecords) {
    val tiles = buildList {
        records.heaviest?.let { best ->
            add(
                Triple(
                    stringResource(R.string.record_heaviest),
                    Fmt.kg(best.set.weight),
                    stringResource(R.string.record_heaviest_caption, best.set.reps, Fmt.day(best.date)),
                ),
            )
        }
        records.estimatedOneRepMax?.let { oneRepMax ->
            add(
                Triple(
                    stringResource(R.string.record_one_rep_max),
                    "≈ ${Fmt.kg(oneRepMax.toLong().toDouble())}",
                    stringResource(R.string.record_one_rep_max_caption),
                ),
            )
        }
        records.mostReps?.let { best ->
            add(
                Triple(
                    stringResource(if (exercise.measure == Measure.SECONDS) R.string.record_longest else R.string.record_most_reps),
                    Fmt.reps(best.set.reps, exercise.measure),
                    if (exercise.usesWeight && best.set.weight > 0) {
                        stringResource(R.string.record_at_weight, Fmt.kg(best.set.weight))
                    } else {
                        Fmt.day(best.date)
                    },
                ),
            )
        }
        records.bestWeek?.let { week ->
            add(
                Triple(
                    stringResource(R.string.record_best_week),
                    if (records.bestWeekByVolume) {
                        Fmt.volume(week.value)
                    } else {
                        repsTotal(week.value.toInt(), exercise.measure)
                    },
                    weekShortLabel(week.weekStart),
                ),
            )
        }
        add(
            Triple(
                stringResource(R.string.record_sessions),
                Fmt.number(records.sessions),
                stringResource(R.string.record_sessions_caption),
            ),
        )
    }.take(4)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.record_title), style = MaterialTheme.typography.titleMedium)
            }
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, value, caption) ->
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
) {
    val metrics = chartMetrics(exercise)
    val unitSeconds = stringResource(R.string.unit_seconds)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
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
private fun WeekHeader(exercise: Exercise, week: HistoryWeek, today: LocalDate) {
    Column(Modifier.padding(top = 8.dp)) {
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
private fun HistoryEntry(exercise: Exercise, entry: WorkoutEntry, today: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
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
