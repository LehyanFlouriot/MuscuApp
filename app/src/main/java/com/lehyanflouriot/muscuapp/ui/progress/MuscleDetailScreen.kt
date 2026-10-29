package com.lehyanflouriot.muscuapp.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.MuscleExercise
import com.lehyanflouriot.muscuapp.core.domain.MuscleStats
import com.lehyanflouriot.muscuapp.core.domain.buildMuscleStats
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.ui.common.ExerciseAvatar
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.RatioBadge
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.WeeklyBarChart
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import com.lehyanflouriot.muscuapp.ui.common.relativeDay
import com.lehyanflouriot.muscuapp.ui.common.reveal
import java.time.LocalDate
import kotlin.math.floor

@Composable
fun MuscleDetailRoute(
    muscle: MuscleGroup,
    data: AppData,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
) {
    val today = rememberToday()
    val stats = remember(data, today, muscle) { buildMuscleStats(data, today).firstOrNull { it.muscle == muscle } }
    if (stats == null) {
        // No exercise works it any more.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    MuscleDetailScreen(stats = stats, today = today, onBack = onBack, onOpenExercise = { onOpenExercise(it.id) })
}

/** A muscle: its sets of each week, how it progresses, and the exercises that work it. */
@Composable
fun MuscleDetailScreen(
    stats: MuscleStats,
    today: LocalDate,
    onBack: () -> Unit,
    onOpenExercise: (Exercise) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val reveal = rememberRevealState()
    val main = stats.exercises.filter { it.main }
    val secondary = stats.exercises.filterNot { it.main }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(stats.muscle.labelRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            item(key = "summary") { SummaryCard(stats = stats, modifier = Modifier.reveal(reveal, "summary")) }
            item(key = "chart") { WeeksCard(stats = stats, modifier = Modifier.reveal(reveal, "chart")) }
            item(key = "main-title") {
                SectionHeader(text = stringResource(R.string.muscle_main_exercises), modifier = Modifier.reveal(reveal, "main-title"))
            }
            if (main.isEmpty()) {
                item(key = "main-none") {
                    Text(
                        text = stringResource(R.string.muscle_none_main),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(main, key = { "main-${it.exercise.id}" }) { item ->
                ExerciseRow(
                    item = item,
                    today = today,
                    onClick = { onOpenExercise(item.exercise) },
                    modifier = Modifier.reveal(reveal, item.exercise.id),
                )
            }
            if (secondary.isNotEmpty()) {
                item(key = "secondary-title") {
                    Column(Modifier.reveal(reveal, "secondary-title")) {
                        SectionHeader(text = stringResource(R.string.muscle_secondary_exercises))
                        Text(
                            text = stringResource(R.string.muscle_secondary_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                items(secondary, key = { "secondary-${it.exercise.id}" }) { item ->
                    ExerciseRow(
                        item = item,
                        today = today,
                        onClick = { onOpenExercise(item.exercise) },
                        modifier = Modifier.reveal(reveal, item.exercise.id),
                    )
                }
            }
        }
    }
}

/** Its sets of a week on average, against the advised range, and how it progresses. */
@Composable
private fun SummaryCard(stats: MuscleStats, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Fmt.number(stats.averageSets, 1),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = pluralStringResource(R.plurals.muscle_sets_unit, floor(stats.averageSets).toInt()),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.muscle_average_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            MuscleVolumeBar(
                sets = stats.averageSets,
                scale = muscleScale(stats.averageSets),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
            )
            Text(
                text = listOf(
                    stringResource(R.string.muscle_this_week, Fmt.number(stats.thisWeekSets, 1)),
                    stringResource(R.string.muscles_advised),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.muscle_progress_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                stats.progress?.let { RatioBadge(ratio = it) }
            }
            Text(
                text = stringResource(if (stats.progress == null) R.string.muscle_progress_unknown else R.string.muscle_progress_text),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeksCard(stats: MuscleStats, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.progress_activity_title), style = MaterialTheme.typography.titleMedium)
            WeeklyBarChart(
                points = stats.weeks,
                formatValue = { value ->
                    resources.getQuantityString(R.plurals.sets_count, floor(value).toInt(), Fmt.number(value, 1))
                },
                description = stringResource(R.string.progress_activity_title),
            )
        }
    }
}

/** An exercise that works the muscle: its sets of a week as they count for it, and how it progresses. */
@Composable
private fun ExerciseRow(item: MuscleExercise, today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val press = rememberPressFeedback(pressedScale = 0.98f)
    Card(
        onClick = onClick,
        interactionSource = press.interactionSource,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .then(press.modifier)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseAvatar(item.exercise, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val lastTime = item.lastDate?.let { stringResource(R.string.progress_last_session, relativeDay(it, today)) }
                Text(
                    text = lastTime?.let { "${weeklySetsText(item.weeklySets)} · $it" } ?: stringResource(R.string.progress_never),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            item.progress?.let { progress ->
                Spacer(Modifier.width(8.dp))
                RatioBadge(ratio = progress)
            }
        }
    }
}
