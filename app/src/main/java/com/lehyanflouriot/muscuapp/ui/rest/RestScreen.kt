package com.lehyanflouriot.muscuapp.ui.rest

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.RestPreset
import com.lehyanflouriot.muscuapp.core.timer.TimerLogic
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.RevealState
import com.lehyanflouriot.muscuapp.ui.common.SectionHeader
import com.lehyanflouriot.muscuapp.ui.common.rememberPressFeedback
import com.lehyanflouriot.muscuapp.ui.common.rememberRevealState
import com.lehyanflouriot.muscuapp.ui.common.reveal
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme

/** Step of the -/+ buttons of the timer. */
const val ADJUST_STEP_MS = 15_000L

@Composable
fun RestScreen(
    state: TimerState,
    now: Long,
    idleDurationMs: Long,
    presets: List<RestPreset>,
    notificationsEnabled: Boolean,
    onToggle: () -> Unit,
    onAdjust: (Long) -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onStartPreset: (RestPreset) -> Unit,
    onAddPreset: () -> Unit,
    onEditPreset: (RestPreset) -> Unit,
    onEnableNotifications: () -> Unit,
    onEditIdleDuration: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val reveal = rememberRevealState()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(R.string.rest_title)) }, scrollBehavior = scrollBehavior)
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AnimatedVisibility(
                visible = !notificationsEnabled,
                enter = expandVertically(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) + fadeIn(),
                exit = shrinkVertically(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) + fadeOut(),
            ) {
                NotificationsBanner(onEnable = onEnableNotifications, modifier = Modifier.reveal(reveal, "banner"))
            }
            TimerDial(
                state = state,
                now = now,
                idleDurationMs = idleDurationMs,
                onClick = {
                    when (state) {
                        is TimerState.Finished -> onRestart()
                        else -> onToggle()
                    }
                },
                onIdleTimeClick = onEditIdleDuration,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.82f)
                    .reveal(reveal, "dial"),
            )
            TimerControls(
                state = state,
                onToggle = onToggle,
                onAdjust = onAdjust,
                onStop = onStop,
                onRestart = onRestart,
                modifier = Modifier.reveal(reveal, "controls"),
            )
            PresetsSection(
                presets = presets,
                activeTotalMs = if (state is TimerState.Idle) null else TimerLogic.total(state),
                reveal = reveal,
                onStart = onStartPreset,
                onAdd = onAddPreset,
                onEdit = onEditPreset,
            )
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .reveal(reveal, "tip"),
            ) {
                Icon(
                    Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.rest_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NotificationsBanner(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.NotificationsOff, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.rest_notifications_off),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEnable) { Text(stringResource(R.string.rest_notifications_enable)) }
        }
    }
}

@Composable
fun TimerDial(
    state: TimerState,
    now: Long,
    idleDurationMs: Long,
    onClick: () -> Unit,
    onIdleTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val success = MuscuTheme.extendedColors.success
    val progress = rememberRestProgress(state, now)
    val arcColor by animateColorAsState(
        targetValue = when (state) {
            is TimerState.Finished -> success
            is TimerState.Paused -> colors.outline
            TimerState.Idle -> colors.primary.copy(alpha = 0.35f)
            is TimerState.Running -> colors.primary
        },
        animationSpec = tween(400),
        label = "rest-color",
    )
    // Once the rest is over, the empty ring lies on a green track.
    val finished = state is TimerState.Finished
    val trackColor by animateColorAsState(
        targetValue = if (finished) success.copy(alpha = 0.3f) else colors.surfaceContainerHighest,
        animationSpec = tween(400),
        label = "rest-track",
    )
    // The end of the rest pulses a few times to catch the eye.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(finished) {
        pulse.snapTo(1f)
        if (finished) {
            repeat(3) {
                pulse.animateTo(1.04f, tween(420, easing = Motion.EaseInOut))
                pulse.animateTo(1f, tween(420, easing = Motion.EaseInOut))
            }
        }
    }
    val timeText = when (state) {
        TimerState.Idle -> Fmt.clockMillis(idleDurationMs)
        is TimerState.Finished -> Fmt.clock(0)
        else -> Fmt.clockMillis(TimerLogic.remaining(state, now))
    }
    val subtitle = when (state) {
        TimerState.Idle -> stringResource(R.string.rest_ready)
        is TimerState.Running -> stringResource(R.string.rest_of_total, Fmt.clockMillis(state.total))
        is TimerState.Paused -> stringResource(R.string.rest_paused)
        is TimerState.Finished -> stringResource(R.string.rest_done)
    }
    val label = TimerLogic.label(state)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .clip(CircleShape)
            .clickable(onClickLabel = stringResource(R.string.rest_dial_action), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            val shown = progress()
            if (shown > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * shown,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp),
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = if (state is TimerState.Finished) success else colors.onSurface,
                maxLines = 1,
                modifier = if (state is TimerState.Idle) {
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClickLabel = stringResource(R.string.rest_change_duration), onClick = onIdleTimeClick)
                        .padding(horizontal = 8.dp)
                } else {
                    Modifier
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = if (state is TimerState.Finished) success else colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (state is TimerState.Finished) {
                Text(
                    text = stringResource(R.string.rest_overtime, Fmt.clockMillis(TimerLogic.overtime(state, now))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TimerControls(
    state: TimerState,
    onToggle: () -> Unit,
    onAdjust: (Long) -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it is TimerState.Finished },
        transitionSpec = {
            (fadeIn(tween(250, delayMillis = 90)) + scaleIn(tween(350, delayMillis = 90, easing = Motion.EaseOut), initialScale = 0.9f))
                .togetherWith(fadeOut(tween(90)))
                .using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "rest-controls",
    ) { shown ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (shown is TimerState.Finished) {
                val press = rememberPressFeedback()
                FilledTonalButton(
                    onClick = onRestart,
                    interactionSource = press.interactionSource,
                    modifier = press.modifier.height(64.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.rest_restart, Fmt.clockMillis(shown.total)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(onClick = onStop) { Text(stringResource(R.string.rest_finish)) }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    AdjustButton(text = stringResource(R.string.rest_minus_15), onClick = { onAdjust(-ADJUST_STEP_MS) })
                    val running = shown is TimerState.Running
                    val press = rememberPressFeedback(pressedScale = 0.9f)
                    FilledIconButton(
                        onClick = onToggle,
                        interactionSource = press.interactionSource,
                        modifier = press.modifier.size(84.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(),
                    ) {
                        val description = stringResource(
                            when (shown) {
                                is TimerState.Running -> R.string.rest_pause
                                is TimerState.Paused -> R.string.rest_resume
                                else -> R.string.rest_start
                            },
                        )
                        AnimatedContent(
                            targetState = running,
                            transitionSpec = {
                                (scaleIn(tween(300, easing = EaseOutBack), initialScale = 0.5f) + fadeIn(tween(150)))
                                    .togetherWith(scaleOut(tween(150), targetScale = 0.5f) + fadeOut(tween(150)))
                            },
                            label = "play-pause",
                        ) { isRunning ->
                            Icon(
                                imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = description,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                    AdjustButton(text = stringResource(R.string.rest_plus_15), onClick = { onAdjust(ADJUST_STEP_MS) })
                }
                AnimatedVisibility(
                    visible = shown is TimerState.Running || shown is TimerState.Paused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TextButton(onClick = onStop) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.rest_stop))
                    }
                }
                if (shown is TimerState.Idle) Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun AdjustButton(text: String, onClick: () -> Unit) {
    val press = rememberPressFeedback(pressedScale = 0.92f)
    FilledTonalButton(
        onClick = onClick,
        interactionSource = press.interactionSource,
        modifier = press.modifier.width(96.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun PresetsSection(
    presets: List<RestPreset>,
    activeTotalMs: Long?,
    reveal: RevealState,
    onStart: (RestPreset) -> Unit,
    onAdd: () -> Unit,
    onEdit: (RestPreset) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            text = stringResource(R.string.rest_presets_title),
            modifier = Modifier.reveal(reveal, "presets-title"),
            action = {
                if (presets.isNotEmpty()) {
                    TextButton(onClick = { editing = !editing }) {
                        Text(stringResource(if (editing) R.string.action_done else R.string.action_edit))
                    }
                }
            },
        )
        Text(
            text = stringResource(if (editing) R.string.rest_presets_edit_hint else R.string.rest_presets_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val tiles: List<RestPreset?> = presets + null
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { preset ->
                    if (preset == null) {
                        AddPresetTile(
                            onClick = onAdd,
                            modifier = Modifier
                                .weight(1f)
                                .reveal(reveal, "preset-add"),
                        )
                    } else {
                        key(preset.id) {
                            PresetTile(
                                preset = preset,
                                active = activeTotalMs == preset.seconds * 1_000L,
                                editing = editing,
                                onClick = { if (editing) onEdit(preset) else onStart(preset) },
                                modifier = Modifier
                                    .weight(1f)
                                    .reveal(reveal, preset.id),
                            )
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PresetTile(
    preset: RestPreset,
    active: Boolean,
    editing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val press = rememberPressFeedback(pressedScale = 0.94f)
    val container by animateColorAsState(if (active) colors.primaryContainer else colors.surfaceContainerHigh, tween(300), label = "preset")
    val content by animateColorAsState(if (active) colors.onPrimaryContainer else colors.onSurface, tween(300), label = "preset-text")
    Card(
        onClick = onClick,
        interactionSource = press.interactionSource,
        modifier = modifier
            .then(press.modifier)
            .heightIn(min = 76.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = Fmt.clock(preset.seconds.toLong()),
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = preset.label.ifBlank { Fmt.duration(preset.seconds) },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
            if (editing) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.rest_preset_edit),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp),
                    tint = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun AddPresetTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val press = rememberPressFeedback(pressedScale = 0.94f)
    OutlinedCard(
        onClick = onClick,
        interactionSource = press.interactionSource,
        modifier = modifier
            .then(press.modifier)
            .heightIn(min = 76.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(8.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.rest_preset_add),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
