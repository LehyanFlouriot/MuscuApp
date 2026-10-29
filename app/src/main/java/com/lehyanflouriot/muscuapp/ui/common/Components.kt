package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.Progress
import com.lehyanflouriot.muscuapp.core.domain.ProgressMetric
import com.lehyanflouriot.muscuapp.core.domain.Trend
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import kotlin.math.abs
import kotlin.math.roundToLong

/** Snackbars of the whole app go through this host. */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    // The icon pops in with a little bounce.
    val pop = remember { Animatable(0.5f) }
    LaunchedEffect(pop) { pop.animateTo(1f, tween(650, delayMillis = 150, easing = EaseOutBack)) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(44.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}

/** Round icon showing the kind of exercise. */
@Composable
fun ExerciseAvatar(exercise: Exercise, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    MuscleAvatar(muscle = exercise.muscleGroup, modifier = modifier, size = size)
}

/** The icon of a muscle group in its circle, as the exercises that work it show it. */
@Composable
fun MuscleAvatar(muscle: MuscleGroup, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(muscle.icon, contentDescription = null, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** "▲ +12 %" in green, "▼ −2 reps" in red, "→ Stable"... */
@Composable
fun TrendBadge(progress: Progress, measure: Measure, modifier: Modifier = Modifier) {
    val text = when {
        progress.trend == Trend.SAME -> stringResource(R.string.trend_same)
        progress.metric == ProgressMetric.VOLUME && progress.ratio != null -> Fmt.signedPercent(progress.ratio!!)
        measure == Measure.SECONDS -> stringResource(R.string.trend_seconds, Fmt.signed(progress.delta))
        else -> pluralStringResource(R.plurals.trend_reps, abs(progress.delta).toInt(), Fmt.signed(progress.delta))
    }
    TrendChip(trend = progress.trend, text = text, modifier = modifier)
}

/** A change given as a ratio: "▲ +12 %", "▼ −3 %", or "→ Stable" once rounded to nothing. */
@Composable
fun RatioBadge(ratio: Double, modifier: Modifier = Modifier) {
    val percent = (ratio * 100).roundToLong()
    val trend = when {
        percent > 0 -> Trend.UP
        percent < 0 -> Trend.DOWN
        else -> Trend.SAME
    }
    val text = if (trend == Trend.SAME) stringResource(R.string.trend_same) else Fmt.signedPercent(ratio)
    TrendChip(trend = trend, text = text, modifier = modifier)
}

@Composable
private fun TrendChip(trend: Trend, text: String, modifier: Modifier = Modifier) {
    val extended = MuscuTheme.extendedColors
    val colors = MaterialTheme.colorScheme
    val (container, content, icon) = when (trend) {
        Trend.UP -> Triple(extended.successContainer, extended.onSuccessContainer, Icons.AutoMirrored.Rounded.TrendingUp)
        Trend.DOWN -> Triple(colors.errorContainer, colors.onErrorContainer, Icons.AutoMirrored.Rounded.TrendingDown)
        Trend.SAME -> Triple(colors.surfaceContainerHighest, colors.onSurfaceVariant, Icons.AutoMirrored.Rounded.TrendingFlat)
    }
    Surface(shape = CircleShape, color = container, contentColor = content, modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** A number and its caption, used in summaries. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The gear at the top of each tab, which opens the settings. */
@Composable
fun SettingsAction(onOpenSettings: () -> Unit) {
    IconButton(onClick = onOpenSettings) {
        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
    }
}
