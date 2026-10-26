package com.lehyanflouriot.muscuapp.ui.rest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState

/** Compact view of the running rest, shown at the bottom of the other screens. */
@Composable
fun MiniTimerBar(
    state: TimerState,
    now: Long,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    applyNavigationBarInsets: Boolean = false,
) {
    if (state is TimerState.Idle) return
    val finished = state is TimerState.Finished
    Surface(
        onClick = onOpen,
        color = if (finished) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (finished) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(if (applyNavigationBarInsets) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier) {
            if (!finished) {
                LinearProgressIndicator(
                    progress = { TimerLogic.fractionRemaining(state, now) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = Color.Transparent,
                    drawStopIndicator = {},
                )
            }
            val buttonColors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val title = when (state) {
                        is TimerState.Running -> stringResource(R.string.mini_running, Fmt.clockMillis(TimerLogic.remaining(state, now)))
                        is TimerState.Paused -> stringResource(R.string.mini_paused, Fmt.clockMillis(state.remaining))
                        else -> stringResource(R.string.mini_done)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        maxLines = 1,
                    )
                    TimerLogic.label(state)?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                when (state) {
                    is TimerState.Running -> {
                        TextButton(onClick = onAdd, colors = buttonColors) { Text(stringResource(R.string.rest_plus_15)) }
                        IconButton(onClick = onToggle) {
                            Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.rest_pause))
                        }
                    }
                    is TimerState.Paused -> IconButton(onClick = onToggle) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.rest_resume))
                    }
                    else -> TextButton(onClick = onRestart, colors = buttonColors) { Text(stringResource(R.string.mini_restart)) }
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.rest_stop))
                }
            }
        }
    }
}
