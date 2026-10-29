package com.lehyanflouriot.muscuapp.ui.week

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.SessionItem
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.WarmUp
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme

/** The warm-up done before the first exercise of a session. */
class WarmUpView(
    val warmUp: WarmUp,
    /** The exercise of emphasis of the session, warmed up by the light sets. */
    val emphasis: SessionItem?,
    /** The first exercise of the session, which comes next. */
    val first: SessionItem,
    /** When the mobility was started, null while it is not. */
    val mobilityStartedAt: Long?,
    val now: Long,
    /** Light sets ticked, in order. */
    val lightSetsDone: Int,
) {
    val mobilityMillis: Long get() = warmUp.mobilityMinutes * 60_000L

    val mobilityLeft: Long? get() = mobilityStartedAt?.let { (it + mobilityMillis - now).coerceAtLeast(0) }
}

class WarmUpActions(
    val onStartMobility: () -> Unit,
    val onStopMobility: () -> Unit,
    /** Ticks the light set of this index, or unticks it and the next ones when it was. */
    val onLightSet: (Int) -> Unit,
    val onDone: () -> Unit,
    val onEdit: () -> Unit,
)

/** "5 min de mobilité · 2 séries légères de l'exercice d'emphase", or that there is none. */
@Composable
fun warmUpSummary(warmUp: WarmUp): String {
    if (!warmUp.isActive) return stringResource(R.string.warm_up_off)
    return listOfNotNull(
        stringResource(R.string.warm_up_summary_mobility, Fmt.duration(warmUp.mobilityMinutes * 60)).takeIf { warmUp.mobilityMinutes > 0 },
        pluralStringResource(R.plurals.warm_up_summary_light, warmUp.lightSets, warmUp.lightSets).takeIf { warmUp.lightSets > 0 },
    ).joinToString(" · ")
}

/** The warm-up of the session: mobility with its countdown, then light sets of the exercise of emphasis. */
@Composable
fun WarmUpStep(view: WarmUpView, actions: WarmUpActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val warmUp = view.warmUp
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.warm_up_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.warm_up_text), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            TextButton(onClick = actions.onEdit) { Text(stringResource(R.string.action_edit)) }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (warmUp.mobilityMinutes > 0) MobilityPart(view = view, actions = actions)
            if (warmUp.mobilityMinutes > 0 && warmUp.lightSets > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            if (warmUp.lightSets > 0) LightSetsPart(view = view, onLightSet = actions.onLightSet)
        }
        Button(
            onClick = actions.onDone,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.warm_up_go), style = MaterialTheme.typography.titleSmall)
        }
        Text(
            text = stringResource(R.string.warm_up_first, view.first.exercise.name),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MobilityPart(view: WarmUpView, actions: WarmUpActions) {
    val colors = MaterialTheme.colorScheme
    val success = MuscuTheme.extendedColors.success
    val left = view.mobilityLeft
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = colors.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.warm_up_mobility, Fmt.duration(view.warmUp.mobilityMinutes * 60)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.warm_up_mobility_text), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            when {
                left == null -> FilledTonalButton(onClick = actions.onStartMobility, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.warm_up_mobility_start))
                }
                left > 0 -> {
                    val description = stringResource(R.string.warm_up_mobility_left_cd, Fmt.clockMillis(left))
                    Text(
                        text = Fmt.clockMillis(left),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = description },
                    )
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = success, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.warm_up_mobility_done), style = MaterialTheme.typography.labelLarge, color = success)
                }
            }
        }
        if (left != null && left > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { 1f - left.toFloat() / view.mobilityMillis },
                    strokeCap = StrokeCap.Round,
                    trackColor = colors.surfaceContainerHighest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = actions.onStopMobility) { Text(stringResource(R.string.warm_up_mobility_stop)) }
            }
        }
    }
}

@Composable
private fun LightSetsPart(view: WarmUpView, onLightSet: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val sets = view.warmUp.lightSets
    val exercise = (view.emphasis ?: view.first).exercise
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = colors.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(pluralStringResource(R.plurals.warm_up_light_sets, sets, sets), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.warm_up_light_text, exercise.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(sets) { index ->
                    val done = index < view.lightSetsDone
                    FilterChip(
                        selected = done,
                        onClick = { onLightSet(index) },
                        label = { Text(stringResource(R.string.warm_up_light_set, index + 1)) },
                        leadingIcon = if (done) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/** The warm-up in the program: what it holds, and the button to change it. */
@Composable
fun WarmUpCard(warmUp: WarmUp, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = colors.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.warm_up_card_title), style = MaterialTheme.typography.titleSmall)
                Text(warmUpSummary(warmUp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
        }
    }
}

/** A short buzz at the end of the mobility: the screen of the session is on, the phone at hand. */
fun buzz(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)) }
}
