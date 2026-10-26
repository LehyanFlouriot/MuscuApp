package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import kotlinx.coroutines.delay

/**
 * Big "- value +" control. Holding a button repeats it; tapping the value lets the user type it.
 */
@Composable
fun NumberStepper(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        StepButton(
            icon = Icons.Rounded.Remove,
            contentDescription = stringResource(R.string.cd_decrease, label),
            enabled = canDecrement,
            onStep = onDecrement,
        )
        Box(
            modifier = Modifier
                .widthIn(min = 96.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = stringResource(R.string.cd_type_value), onClick = onValueClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
                if (unit != null) {
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
        StepButton(
            icon = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.cd_increase, label),
            enabled = canIncrement,
            onStep = onIncrement,
        )
    }
}

/** Smaller stepper used for each line of the detailed sets. */
@Composable
fun CompactStepper(
    value: String,
    description: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        StepButton(
            icon = Icons.Rounded.Remove,
            contentDescription = stringResource(R.string.cd_decrease, description),
            enabled = canDecrement,
            onStep = onDecrement,
            size = 36.dp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClickLabel = stringResource(R.string.cd_type_value), onClick = onValueClick)
                .padding(vertical = 8.dp),
        )
        StepButton(
            icon = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.cd_increase, description),
            enabled = true,
            onStep = onIncrement,
            size = 36.dp,
        )
    }
}

/** Round tonal button that repeats its action while it is held down. */
@Composable
fun StepButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onStep: () -> Unit,
    size: Dp = 48.dp,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnStep by rememberUpdatedState(onStep)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var repeating by remember { mutableStateOf(false) }

    LaunchedEffect(repeating, pressed) {
        if (repeating && pressed) {
            while (true) {
                currentOnStep()
                delay(90)
            }
        } else {
            repeating = false
        }
    }

    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (enabled) colors.secondaryContainer else colors.onSurface.copy(alpha = 0.08f),
        contentColor = if (enabled) colors.onSecondaryContainer else colors.onSurface.copy(alpha = 0.38f),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onLongClick = { repeating = true },
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnStep()
                },
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size / 2))
        }
    }
}
