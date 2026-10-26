package com.lehyanflouriot.muscuapp.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.ExerciseProgressItem
import com.lehyanflouriot.muscuapp.core.domain.Trend
import com.lehyanflouriot.muscuapp.core.domain.WeekPoint
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.ExerciseAvatar
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.Sparkline
import com.lehyanflouriot.muscuapp.ui.common.WeeklyBarChart
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.repsTotal
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.LocalDate

@Composable
fun ProgressScreen(
    items: List<ExerciseProgressItem>,
    activity: List<WeekPoint>,
    streak: Int,
    trainedThisWeek: Boolean,
    today: LocalDate,
    onOpenExercise: (Exercise) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hasData = items.any { it.lastDate != null }
    val active = items.filterNot { it.exercise.archived }
    val archived = items.filter { it.exercise.archived }
    var showArchived by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.progress_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        if (!hasData) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Rounded.Insights,
                    title = stringResource(R.string.progress_empty_title),
                    text = stringResource(R.string.progress_empty_text),
                )
            }
        } else {
            ProgressList(
                padding = padding,
                active = active,
                archived = archived,
                showArchived = showArchived,
                onToggleArchived = { showArchived = !showArchived },
                streak = streak,
                trainedThisWeek = trainedThisWeek,
                activity = activity,
                today = today,
                onOpenExercise = onOpenExercise,
            )
        }
    }
}

@Composable
private fun ProgressList(
    padding: PaddingValues,
    active: List<ExerciseProgressItem>,
    archived: List<ExerciseProgressItem>,
    showArchived: Boolean,
    onToggleArchived: () -> Unit,
    streak: Int,
    trainedThisWeek: Boolean,
    activity: List<WeekPoint>,
    today: LocalDate,
    onOpenExercise: (Exercise) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "streak") { StreakCard(streak = streak, trainedThisWeek = trainedThisWeek) }
        item(key = "activity") { ActivityCard(activity = activity) }
        item(key = "exercises-title") { SectionHeader(text = stringResource(R.string.progress_exercises)) }
        items(active, key = { it.exercise.id }) { item ->
            ProgressRow(item = item, today = today, onClick = { onOpenExercise(item.exercise) })
        }
        if (archived.isNotEmpty()) {
            item(key = "archived-title") {
                SectionHeader(
                    text = stringResource(R.string.progress_archived, archived.size),
                    action = {
                        TextButton(onClick = onToggleArchived) {
                            Text(stringResource(if (showArchived) R.string.action_hide else R.string.action_show))
                        }
                    },
                )
            }
            if (showArchived) {
                items(archived, key = { it.exercise.id }) { item ->
                    ProgressRow(
                        item = item,
                        today = today,
                        onClick = { onOpenExercise(item.exercise) },
                        modifier = Modifier.alpha(0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCard(streak: Int, trainedThisWeek: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (streak > 0) {
                        pluralStringResource(R.plurals.streak_weeks, streak, streak)
                    } else {
                        stringResource(R.string.streak_none)
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(
                        when {
                            streak == 0 -> R.string.streak_start
                            trainedThisWeek -> R.string.streak_keep_going
                            else -> R.string.streak_dont_break
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: List<WeekPoint>) {
    val resources = LocalResources.current
    val weeksWithData = activity.filter { it.value > 0 }
    val average = if (weeksWithData.isEmpty()) 0.0 else weeksWithData.sumOf { it.value } / weeksWithData.size
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.progress_activity_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.progress_activity_average, Fmt.number(average, 1)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WeeklyBarChart(
                points = activity,
                formatValue = { value ->
                    val count = value.toInt()
                    resources.getQuantityString(R.plurals.sets_count, count, Fmt.number(count))
                },
                description = stringResource(R.string.progress_activity_title),
            )
        }
    }
}

@Composable
private fun ProgressRow(
    item: ExerciseProgressItem,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = item.exercise
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseAvatar(exercise, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = recordLine(item),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.lastDate?.let { stringResource(R.string.progress_last_session, relativeDay(it, today)) }
                        ?: stringResource(R.string.progress_never),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.lastDate != null) {
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Sparkline(
                        values = item.spark,
                        modifier = Modifier
                            .width(72.dp)
                            .height(32.dp),
                    )
                    item.progress?.let { progress ->
                        val (icon, tint) = when (progress.trend) {
                            Trend.UP -> Icons.AutoMirrored.Rounded.TrendingUp to MuscuTheme.extendedColors.success
                            Trend.DOWN -> Icons.AutoMirrored.Rounded.TrendingDown to MaterialTheme.colorScheme.error
                            Trend.SAME -> Icons.AutoMirrored.Rounded.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            icon,
                            contentDescription = stringResource(
                                when (progress.trend) {
                                    Trend.UP -> R.string.trend_up_cd
                                    Trend.DOWN -> R.string.trend_down_cd
                                    Trend.SAME -> R.string.trend_same_cd
                                },
                            ),
                            tint = tint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun recordLine(item: ExerciseProgressItem): String {
    val exercise = item.exercise
    val records = item.records
    val heaviest = records.heaviest
    val mostReps = records.mostReps
    return when {
        heaviest != null -> stringResource(
            R.string.progress_record_weight,
            Fmt.kg(heaviest.set.weight),
            Fmt.reps(heaviest.set.reps, exercise.measure),
        )
        mostReps != null -> stringResource(R.string.progress_record_reps, repsTotal(mostReps.set.reps, exercise.measure))
        else -> stringResource(R.string.progress_no_record)
    }
}
