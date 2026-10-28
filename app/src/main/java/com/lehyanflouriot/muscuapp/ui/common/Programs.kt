package com.lehyanflouriot.muscuapp.ui.common

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.domain.ProgramInfo
import com.lehyanflouriot.muscuapp.core.domain.ProgramStatus
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.Program
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

/** "Lun", "Mar"... */
fun DayOfWeek.shortName(): String =
    getDisplayName(TextStyle.SHORT, Fmt.locale).removeSuffix(".").replaceFirstChar { it.titlecase(Fmt.locale) }

fun DayOfWeek.fullName(): String = getDisplayName(TextStyle.FULL, Fmt.locale).replaceFirstChar { it.titlecase(Fmt.locale) }

/** "mercredi", in the middle of a sentence. */
fun DayOfWeek.lowerName(): String = getDisplayName(TextStyle.FULL, Fmt.locale)

private fun nextMonday(today: LocalDate): LocalDate = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))

/** "Aujourd'hui", "Lundi prochain", another day, and "Plus tard" when [allowLater]. Null means later. */
@Composable
private fun StartChoice(
    start: LocalDate?,
    today: LocalDate,
    allowLater: Boolean,
    onChange: (LocalDate?) -> Unit,
) {
    val monday = nextMonday(today)
    var picking by remember { mutableStateOf(false) }
    val custom = start?.takeIf { it != today && it != monday }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = start == today,
            onClick = { onChange(today) },
            label = { Text(stringResource(R.string.program_start_today)) },
        )
        FilterChip(
            selected = start == monday,
            onClick = { onChange(monday) },
            label = { Text(stringResource(R.string.program_start_next_monday)) },
        )
        FilterChip(
            selected = custom != null,
            onClick = { picking = true },
            label = { Text(custom?.let(Fmt::day) ?: stringResource(R.string.program_start_pick)) },
            leadingIcon = {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
            },
        )
        if (allowLater) {
            FilterChip(
                selected = start == null,
                onClick = { onChange(null) },
                label = { Text(stringResource(R.string.program_start_later)) },
            )
        }
    }
    if (picking) {
        FutureDatePickerDialog(
            initialDate = start ?: monday,
            today = today,
            onDismiss = { picking = false },
            onConfirm = {
                onChange(it)
                picking = false
            },
        )
    }
}

@Composable
private fun ChoiceRow(selected: Boolean, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A new program, copied from the current one or empty, starting on the chosen day. */
@Composable
fun NewProgramDialog(
    current: Program?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onCreate: (name: String, copyOf: String?, start: LocalDate?) -> Unit,
    nameHint: String = stringResource(R.string.program_name_hint),
    title: String = stringResource(R.string.program_new),
) {
    var name by remember { mutableStateOf("") }
    var copy by remember { mutableStateOf(current != null) }
    var start by remember { mutableStateOf<LocalDate?>(nextMonday(today)) }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.plan_name)) },
                    placeholder = { Text(nameHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                if (current != null) {
                    Column(Modifier.selectableGroup()) {
                        ChoiceRow(copy, stringResource(R.string.program_copy_of, current.name)) { copy = true }
                        ChoiceRow(!copy, stringResource(R.string.program_blank)) { copy = false }
                    }
                }
                Text(
                    text = stringResource(R.string.program_start),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StartChoice(start = start, today = today, allowLater = true, onChange = { start = it })
            }
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, current?.id?.takeIf { copy }, start) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.program_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Chooses when a program starts. */
@Composable
fun StartProgramDialog(
    plan: Program,
    today: LocalDate,
    onDismiss: () -> Unit,
    onStart: (LocalDate) -> Unit,
) {
    var start by remember { mutableStateOf<LocalDate?>(today) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.EventRepeat, contentDescription = null) },
        title = { Text(stringResource(R.string.program_start_title, plan.name)) },
        text = { StartChoice(start = start, today = today, allowLater = false, onChange = { start = it }) },
        confirmButton = {
            TextButton(onClick = { start?.let(onStart) }, enabled = start != null) {
                Text(stringResource(R.string.program_start_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Tells that the program [name] starts today, or on the day it will. */
fun announceStart(messenger: Messenger, context: Context, name: String, start: LocalDate, today: LocalDate) {
    val text = if (start <= today) {
        context.getString(R.string.program_started_today, name)
    } else {
        context.getString(R.string.program_started_on, name, Fmt.longDay(start))
    }
    messenger.show(text)
}

/** "En cours depuis le 26 oct.", "Commence le 2 nov."... */
@Composable
fun programStatusText(info: ProgramInfo<*>, today: LocalDate): String {
    val date = info.date
    fun day(date: LocalDate) = Fmt.dayMonth(date, withYear = date.year != today.year)
    return when {
        info.status == ProgramStatus.ACTIVE && date != null -> stringResource(R.string.program_active_since, day(date))
        info.status == ProgramStatus.PLANNED && date != null -> stringResource(R.string.program_planned, day(date))
        info.status == ProgramStatus.PAST && date != null -> stringResource(R.string.program_past, day(date))
        else -> stringResource(R.string.program_unused)
    }
}

/** The programs, the current one first: to change one, start another one, or create a new one. */
@Composable
fun <P : Program> ProgramsSheet(
    infos: List<ProgramInfo<P>>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onEdit: (P) -> Unit,
    onStart: (P) -> Unit,
    onCancelStart: (ProgramInfo<P>) -> Unit,
    onDelete: (P) -> Unit,
    onNew: () -> Unit,
    title: String = stringResource(R.string.programs_title),
    newLabel: String = stringResource(R.string.program_new),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            infos.forEach { info ->
                ProgramRow(
                    info = info,
                    today = today,
                    onEdit = { onEdit(info.plan) },
                    onStart = { onStart(info.plan) },
                    onCancelStart = { onCancelStart(info) },
                    onDelete = { onDelete(info.plan) },
                )
            }
            FilledTonalButton(
                onClick = onNew,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(newLabel)
            }
        }
    }
}

@Composable
private fun ProgramRow(
    info: ProgramInfo<*>,
    today: LocalDate,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onCancelStart: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val active = info.status == ProgramStatus.ACTIVE
    ListItem(
        headlineContent = { Text(info.plan.name) },
        supportingContent = { Text(programStatusText(info, today)) },
        leadingContent = {
            Icon(
                imageVector = when (info.status) {
                    ProgramStatus.ACTIVE -> Icons.Rounded.CheckCircle
                    ProgramStatus.PLANNED -> Icons.Rounded.EventRepeat
                    ProgramStatus.PAST -> Icons.Rounded.History
                    ProgramStatus.UNUSED -> Icons.Rounded.EditNote
                },
                contentDescription = null,
                tint = if (active) MuscuTheme.extendedColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.program_options, info.plan.name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                        onClick = {
                            menu = false
                            onEdit()
                        },
                    )
                    if (!active) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.program_start_action)) },
                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                            onClick = {
                                menu = false
                                onStart()
                            },
                        )
                    }
                    if (info.status == ProgramStatus.PLANNED) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.program_cancel_start)) },
                            leadingIcon = { Icon(Icons.Rounded.EventBusy, contentDescription = null) },
                            onClick = {
                                menu = false
                                onCancelStart()
                            },
                        )
                    }
                    if (!active) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menu = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onEdit)
            .padding(horizontal = 8.dp),
    )
}

// region Program editors

/** Copies a day of a program to the other days the user picks: [title] and [text] say what is copied. */
@Composable
fun CopyDayDialog(
    from: DayOfWeek,
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onCopy: (Set<DayOfWeek>) -> Unit,
) {
    var targets by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                DayOfWeek.entries.filter { it != from }.forEach { day ->
                    val checked = day in targets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { targets = if (it) targets + day else targets - day },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Text(text = day.fullName(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCopy(targets) }, enabled = targets.isNotEmpty()) {
                Text(stringResource(R.string.plan_copy_day_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * The name and description of the program being changed, with [status] when given. When the
 * program [keepsHistory], it tells that the days already lived keep what they had.
 */
@Composable
fun ProgramHeader(
    name: String,
    info: String,
    keepsHistory: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
) {
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            if (status != null) {
                Text(text = status, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = info.ifBlank { stringResource(R.string.plan_info_empty) },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (keepsHistory) {
                Row(Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.plan_editor_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** A titled group of rows, with an optional button to add one more. */
@Composable
fun EditorSection(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    addLabel: String? = null,
    onAdd: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SectionHeader(text = title)
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                content()
                if (addLabel != null) {
                    TextButton(
                        onClick = onAdd,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(addLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun EditorRow(icon: ImageVector, title: String, onClick: () -> Unit, subtitle: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = Fmt.typography(title), style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    text = Fmt.typography(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// endregion
