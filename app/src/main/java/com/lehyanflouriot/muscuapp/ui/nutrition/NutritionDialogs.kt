package com.lehyanflouriot.muscuapp.ui.nutrition

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.MAX_MEAL_KCAL
import com.lehyanflouriot.muscuapp.core.data.MAX_MEAL_PROTEIN
import com.lehyanflouriot.muscuapp.core.domain.FoodView
import com.lehyanflouriot.muscuapp.core.domain.ProgramInfo
import com.lehyanflouriot.muscuapp.core.domain.ProgramStatus
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.MealMoment
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.ui.common.FutureDatePickerDialog
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

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
)

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

/** The food of a meal: what it is, how much, and an optional note. */
@Composable
fun FoodDialog(
    title: String,
    quantity: String,
    name: String,
    note: String,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: String, note: String) -> Unit,
    supportingText: String? = null,
    onMove: ((Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    FormDialog(
        title = title,
        supportingText = supportingText,
        fields = listOf(
            FormField(
                label = stringResource(R.string.nutrition_food_name),
                initial = name,
                capitalization = KeyboardCapitalization.None,
                required = true,
            ),
            FormField(
                label = stringResource(R.string.nutrition_food_quantity),
                initial = quantity,
                placeholder = stringResource(R.string.nutrition_food_quantity_hint),
                capitalization = KeyboardCapitalization.None,
            ),
            FormField(label = stringResource(R.string.nutrition_food_note), initial = note),
        ),
        onDismiss = onDismiss,
        onConfirm = { (newName, newQuantity, newNote) -> onSave(newName, newQuantity, newNote) },
        extraContent = if (onMove == null && onDelete == null) {
            null
        } else {
            {
                Column {
                    if (onMove != null) {
                        Row {
                            DialogAction(stringResource(R.string.nutrition_move_up), Icons.Rounded.ArrowUpward, { onMove(-1) })
                            DialogAction(stringResource(R.string.nutrition_move_down), Icons.Rounded.ArrowDownward, { onMove(1) })
                        }
                    }
                    if (onDelete != null) {
                        DialogAction(stringResource(R.string.nutrition_food_delete), Icons.Rounded.DeleteOutline, onDelete, destructive = true)
                    }
                }
            }
        },
    )
}

/** Eats something else, for a single day: ideas come from the substitutions of the program. */
@Composable
fun SwapDialog(
    food: FoodView,
    dayName: String,
    substitutes: List<String>,
    onDismiss: () -> Unit,
    onSwap: (name: String, quantity: String) -> Unit,
    onRestore: () -> Unit,
    onEditInPlan: () -> Unit,
) {
    var name by remember { mutableStateOf(food.swap?.name.orEmpty()) }
    var quantity by remember { mutableStateOf(food.quantity) }
    val planned = listOf(food.item.quantity, food.item.name).filter { it.isNotBlank() }.joinToString(" ")
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
        title = { Text(stringResource(R.string.nutrition_swap_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.nutrition_swap_planned, Fmt.typography(planned)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (substitutes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.nutrition_swap_ideas),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            substitutes.forEach { substitute ->
                                FilterChip(
                                    selected = name.trim().equals(substitute, ignoreCase = true),
                                    onClick = { name = substitute },
                                    label = { Text(substitute) },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.nutrition_food_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.nutrition_food_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSwap(name, quantity) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.nutrition_swap_hint, dayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                    if (food.swap != null) {
                        DialogAction(stringResource(R.string.nutrition_swap_restore), Icons.AutoMirrored.Rounded.Undo, onRestore)
                    }
                    DialogAction(stringResource(R.string.nutrition_swap_edit_plan), Icons.Rounded.EditNote, onEditInPlan)
                }
            }
            // Ideas are a tap away: the keyboard only opens at once when there are none.
            if (substitutes.isEmpty()) LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        },
        confirmButton = {
            TextButton(onClick = { onSwap(name, quantity) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.nutrition_swap_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// region Programs

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
    current: MealPlan?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onCreate: (name: String, copyOf: String?, start: LocalDate?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var copy by remember { mutableStateOf(current != null) }
    var start by remember { mutableStateOf<LocalDate?>(nextMonday(today)) }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_new)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.plan_name)) },
                    placeholder = { Text(stringResource(R.string.program_name_hint)) },
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
    plan: MealPlan,
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

@Composable
private fun statusText(info: ProgramInfo, today: LocalDate): String {
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
fun ProgramsSheet(
    infos: List<ProgramInfo>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onEdit: (MealPlan) -> Unit,
    onStart: (MealPlan) -> Unit,
    onCancelStart: (ProgramInfo) -> Unit,
    onDelete: (MealPlan) -> Unit,
    onNew: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.programs_title),
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
                Text(stringResource(R.string.program_new))
            }
        }
    }
}

@Composable
private fun ProgramRow(
    info: ProgramInfo,
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
        supportingContent = { Text(statusText(info, today)) },
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

// endregion

// region Plan editor

/** Copies the meals of a day to the other days the user picks. */
@Composable
fun CopyDayDialog(from: DayOfWeek, onDismiss: () -> Unit, onCopy: (Set<DayOfWeek>) -> Unit) {
    var targets by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    val fromName = from.getDisplayName(TextStyle.FULL, Fmt.locale)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_copy_day_title, fromName)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.plan_copy_day_text, fromName),
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
                        Text(
                            text = day.getDisplayName(TextStyle.FULL, Fmt.locale).replaceFirstChar { it.titlecase(Fmt.locale) },
                            style = MaterialTheme.typography.bodyLarge,
                        )
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

/** A meal of every day: its name, when to eat it, its icon and its usual targets. */
@Composable
fun SlotDialog(
    slot: MealSlot?,
    onDismiss: () -> Unit,
    onSave: (name: String, hint: String, moment: MealMoment, kcal: Int?, protein: Int?) -> Unit,
    onMove: ((Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var moment by remember { mutableStateOf(slot?.moment ?: MealMoment.SNACK) }
    FormDialog(
        title = stringResource(if (slot == null) R.string.plan_slot_new else R.string.plan_slot_edit),
        fields = listOf(
            FormField(stringResource(R.string.plan_slot_name), slot?.name.orEmpty(), required = true),
            FormField(stringResource(R.string.plan_slot_hint), slot?.hint.orEmpty()),
            FormField(
                label = stringResource(R.string.plan_kcal),
                initial = slot?.kcal?.toString().orEmpty(),
                keyboardType = KeyboardType.Number,
                isValid = { parseWholeNumber(it, 0..MAX_MEAL_KCAL) != null },
            ),
            FormField(
                label = stringResource(R.string.plan_protein),
                initial = slot?.protein?.toString().orEmpty(),
                keyboardType = KeyboardType.Number,
                isValid = { parseWholeNumber(it, 0..MAX_MEAL_PROTEIN) != null },
            ),
        ),
        onDismiss = onDismiss,
        onConfirm = { (name, hint, kcal, protein) ->
            onSave(name, hint, moment, parseWholeNumber(kcal, 0..MAX_MEAL_KCAL), parseWholeNumber(protein, 0..MAX_MEAL_PROTEIN))
        },
        extraContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.plan_slot_icon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealMoment.entries.forEach { option ->
                        FilterChip(
                            selected = option == moment,
                            onClick = { moment = option },
                            label = { Text(stringResource(option.labelRes)) },
                            leadingIcon = {
                                Icon(option.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                            },
                        )
                    }
                }
                if (onMove != null) {
                    Row {
                        DialogAction(stringResource(R.string.nutrition_move_up), Icons.Rounded.ArrowUpward, { onMove(-1) })
                        DialogAction(stringResource(R.string.nutrition_move_down), Icons.Rounded.ArrowDownward, { onMove(1) })
                    }
                }
                if (onDelete != null) {
                    DialogAction(stringResource(R.string.plan_slot_delete), Icons.Rounded.DeleteOutline, onDelete, destructive = true)
                }
            }
        },
    )
}

// endregion
