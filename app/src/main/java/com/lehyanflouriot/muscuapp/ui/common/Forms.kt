package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.normalizeFood

/** A text field of a [FormDialog]. */
class FormField(
    val label: String,
    val initial: String,
    val placeholder: String? = null,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    val singleLine: Boolean = true,
    val required: Boolean = false,
    val isValid: (String) -> Boolean = { true },
    /** Values already used elsewhere, offered while typing so that the same thing keeps the same name. */
    val suggestions: List<String> = emptyList(),
)

private const val MAX_SUGGESTIONS = 6

/** A dialog with a few text fields, the first one ready to be typed in. */
@Composable
fun FormDialog(
    title: String,
    fields: List<FormField>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    supportingText: String? = null,
    confirmLabel: String? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val values = remember { fields.map { mutableStateOf(it.initial) } }
    val valid = fields.indices.all { index ->
        val value = values[index].value.trim()
        (!fields[index].required || value.isNotEmpty()) && (value.isEmpty() || fields[index].isValid(value))
    }
    val confirm = { if (valid) onConfirm(values.map { it.value.trim() }) }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                fields.forEachIndexed { index, field ->
                    val value = values[index].value
                    OutlinedTextField(
                        value = value,
                        onValueChange = { values[index].value = it },
                        label = { Text(field.label) },
                        placeholder = field.placeholder?.let { { Text(it) } },
                        singleLine = field.singleLine,
                        minLines = if (field.singleLine) 1 else 3,
                        isError = value.isNotBlank() && !field.isValid(value.trim()),
                        keyboardOptions = KeyboardOptions(
                            capitalization = field.capitalization,
                            keyboardType = field.keyboardType,
                            imeAction = if (index == fields.lastIndex && field.singleLine) ImeAction.Done else ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onDone = { confirm() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                    )
                    if (field.suggestions.isNotEmpty()) {
                        val matches = remember(value, field.suggestions) {
                            val typed = normalizeFood(value)
                            if (typed.isEmpty()) {
                                emptyList()
                            } else {
                                field.suggestions
                                    .filter { normalizeFood(it).let { name -> name != typed && name.contains(typed) } }
                                    .take(MAX_SUGGESTIONS)
                            }
                        }
                        if (matches.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                matches.forEach { suggestion ->
                                    SuggestionChip(onClick = { values[index].value = suggestion }, label = { Text(suggestion) })
                                }
                            }
                        }
                    }
                }
                extraContent?.invoke()
            }
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = valid) {
                Text(confirmLabel ?: stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

fun parseWholeNumber(text: String, range: IntRange): Int? = text.trim().toIntOrNull()?.takeIf { it in range }

fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').replace('−', '-').toDoubleOrNull()

/** A button of a dialog for a secondary action, in red when it removes something. */
@Composable
fun DialogAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        colors = if (destructive) {
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.textButtonColors()
        },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
