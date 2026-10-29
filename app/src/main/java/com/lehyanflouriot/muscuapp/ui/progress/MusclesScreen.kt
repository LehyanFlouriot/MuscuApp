package com.lehyanflouriot.muscuapp.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.ADVISED_WEEKLY_SETS
import com.lehyanflouriot.muscuapp.core.domain.MuscleStats
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.ui.common.EmptyState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.MuscleAvatar
import com.lehyanflouriot.muscuapp.ui.common.RatioBadge
import com.lehyanflouriot.muscuapp.ui.common.labelRes
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import kotlin.math.floor
import kotlin.math.max

/** "8,5 séries / sem.": sets of a week, which a muscle worked too counts for half. */
@Composable
fun weeklySetsText(sets: Double): String =
    pluralStringResource(R.plurals.muscle_sets_per_week, floor(sets).toInt(), Fmt.number(sets, 1))

/** The scale shared by the bars of the muscles, so that they compare at a glance: a bit more than the advised range. */
fun muscleScale(sets: Double): Double = max(ADVISED_WEEKLY_SETS.endInclusive + 4.0, sets)

/** How each muscle is trained: its sets of a week against the advised range, and how it progresses. */
@Composable
fun MusclesProgress(
    padding: PaddingValues,
    muscles: List<MuscleStats>,
    onOpenMuscle: (MuscleGroup) -> Unit,
) {
    if (muscles.none { stats -> stats.weeks.any { it.value > 0.0 } }) {
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
        return
    }
    val reveal = rememberRevealState()
    val scale = muscleScale(muscles.maxOf { it.averageSets })
    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") { MusclesIntro(modifier = Modifier.reveal(reveal, "intro")) }
        items(muscles, key = { it.muscle.name }) { stats ->
            MuscleRow(
                stats = stats,
                scale = scale,
                onClick = { onOpenMuscle(stats.muscle) },
                modifier = Modifier.reveal(reveal, stats.muscle.name),
            )
        }
    }
}

/** What the bars mean: sets of a week on average, counted for half by the muscles worked too, and the advised range. */
@Composable
private fun MusclesIntro(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.muscles_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AdvisedSwatch(Modifier.size(width = 24.dp, height = 10.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.muscles_advised),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The color of the advised range, as a legend. */
@Composable
private fun AdvisedSwatch(modifier: Modifier = Modifier) {
    val zone = advisedZoneColor()
    Canvas(modifier) {
        drawRoundRect(zone, cornerRadius = CornerRadius(size.height / 2))
    }
}

/** The advised range, in the soft green of what is done well. */
@Composable
private fun advisedZoneColor() = MuscuTheme.extendedColors.successContainer

@Composable
private fun MuscleRow(stats: MuscleStats, scale: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val press = rememberPressFeedback(pressedScale = 0.98f)
    val name = stringResource(stats.muscle.labelRes)
    val average = weeklySetsText(stats.averageSets)
    val barDescription = stringResource(R.string.muscle_bar_cd, name, average)
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
            MuscleAvatar(stats.muscle, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    stats.progress?.let { progress ->
                        val description = stringResource(R.string.muscle_progress_cd, Fmt.signedPercent(progress))
                        RatioBadge(ratio = progress, modifier = Modifier.clearAndSetSemantics { contentDescription = description })
                    }
                }
                MuscleVolumeBar(
                    sets = stats.averageSets,
                    scale = scale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .semantics { contentDescription = barDescription },
                )
                val thisWeek = stringResource(R.string.muscle_this_week, Fmt.number(stats.thisWeekSets, 1))
                Text(
                    text = "$average · $thisWeek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The sets of a week of a muscle on a scale of [scale] sets, over the advised range drawn behind. */
@Composable
fun MuscleVolumeBar(sets: Double, scale: Double, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val zone = advisedZoneColor()
    val fill = MaterialTheme.colorScheme.primary
    val target = (sets / scale).toFloat().coerceIn(0f, 1f)
    // The bar fills in when it shows, then follows its value.
    val fraction = remember { Animatable(0f) }
    LaunchedEffect(target) { fraction.animateTo(target, tween(600, easing = Motion.EaseOut)) }
    Canvas(modifier) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        val start = (ADVISED_WEEKLY_SETS.start / scale).toFloat() * size.width
        val end = (ADVISED_WEEKLY_SETS.endInclusive / scale).toFloat() * size.width
        drawRect(zone, topLeft = Offset(start, 0f), size = Size(end - start, size.height))
        val width = fraction.value * size.width
        if (width > 0f) {
            drawRoundRect(fill, size = Size(max(width, size.height), size.height), cornerRadius = radius)
        }
    }
}
