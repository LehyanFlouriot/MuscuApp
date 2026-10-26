package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt

/** Parses "62,5" or "62.5". Returns null for anything else. */
fun parseNumber(text: String, allowDecimal: Boolean): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value < 0 || value.isNaN() || value.isInfinite()) return null
    if (!allowDecimal && value % 1.0 != 0.0) return null
    return value
}

/**
 * Lets the user type a value instead of tapping +/- many times. Values outside [valueRange] are
 * refused, with [rangeError] under the field.
 */
@Composable
fun NumberInputDialog(
    title: String,
    initialValue: String,
    allowDecimal: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    suffix: String? = null,
    valueRange: ClosedFloatingPointRange<Double>? = null,
    rangeError: String? = null,
) {
    var text by remember {
        mutableStateOf(TextFieldValue(initialValue, selection = TextRange(0, initialValue.length)))
    }
    val number = parseNumber(text.text, allowDecimal)
    val parsed = number?.takeIf { valueRange == null || it in valueRange }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                isError = text.text.isNotBlank() && parsed == null,
                suffix = suffix?.let { { Text(it) } },
                supportingText = if (number != null && parsed == null && rangeError != null) {
                    { Text(rangeError) }
                } else {
                    null
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { parsed?.let(onConfirm) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Minutes and seconds editor used for rest durations. */
@Composable
fun DurationEditor(
    seconds: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minSeconds: Int = 5,
    maxSeconds: Int = 60 * 60,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = Fmt.clock(seconds.toLong()),
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        var typing by remember { mutableStateOf<Boolean?>(null) }
        NumberStepper(
            label = stringResource(R.string.field_minutes),
            value = (seconds / 60).toString(),
            onDecrement = { onChange((seconds - 60).coerceIn(minSeconds, maxSeconds)) },
            onIncrement = { onChange((seconds + 60).coerceIn(minSeconds, maxSeconds)) },
            onValueClick = { typing = true },
            canDecrement = seconds - 60 >= minSeconds,
            canIncrement = seconds + 60 <= maxSeconds,
        )
        NumberStepper(
            label = stringResource(R.string.field_seconds),
            value = (seconds % 60).toString(),
            onDecrement = { onChange((seconds - 5).coerceIn(minSeconds, maxSeconds)) },
            onIncrement = { onChange((seconds + 5).coerceIn(minSeconds, maxSeconds)) },
            onValueClick = { typing = false },
            canDecrement = seconds - 5 >= minSeconds,
            canIncrement = seconds + 5 <= maxSeconds,
        )
        typing?.let { minutes ->
            NumberInputDialog(
                title = stringResource(if (minutes) R.string.field_minutes else R.string.field_seconds),
                initialValue = (if (minutes) seconds / 60 else seconds % 60).toString(),
                allowDecimal = false,
                onDismiss = { typing = null },
                onConfirm = { value ->
                    val typed = value.toInt()
                    val total = if (minutes) typed * 60 + seconds % 60 else (seconds / 60) * 60 + typed.coerceAtMost(59)
                    onChange(total.coerceIn(minSeconds, maxSeconds))
                    typing = null
                },
            )
        }
    }
}

@Composable
fun DurationPickerDialog(
    title: String,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var seconds by remember { mutableIntStateOf(initialSeconds) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { DurationEditor(seconds = seconds, onChange = { seconds = it }) },
        confirmButton = {
            TextButton(onClick = { onConfirm(seconds) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Creates or edits a rest preset: a duration and an optional name. */
@Composable
fun RestPresetDialog(
    title: String,
    initialSeconds: Int,
    initialLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (seconds: Int, label: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var seconds by remember { mutableIntStateOf(initialSeconds) }
    var label by remember { mutableStateOf(initialLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DurationEditor(seconds = seconds, onChange = { seconds = it })
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(24) },
                    label = { Text(stringResource(R.string.preset_label_field)) },
                    placeholder = { Text(stringResource(R.string.preset_label_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.preset_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seconds, label) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
